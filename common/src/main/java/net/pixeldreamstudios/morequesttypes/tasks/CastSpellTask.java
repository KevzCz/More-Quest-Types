package net.pixeldreamstudios.morequesttypes.tasks;

import com.mojang.datafixers.util.Either;
import dev.architectury.registry.registries.RegistrarManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.pixeldreamstudios.morequesttypes.compat.SpellEngineCompat;
import net.pixeldreamstudios.morequesttypes.config.MultiSelectListConfig;
import net.pixeldreamstudios.morequesttypes.event.SpellCastEventBuffer;
import net.pixeldreamstudios.morequesttypes.network.MQTBiomesRequest;
import net.pixeldreamstudios.morequesttypes.network.MQTStructuresRequest;
import net.pixeldreamstudios.morequesttypes.network.MQTWorldsRequest;
import net.pixeldreamstudios.morequesttypes.network.NetworkHelper;
import net.pixeldreamstudios.morequesttypes.util.SpellDisplayHelper;
import net.pixeldreamstudios.morequesttypes.util.SpellTooltipHelper;
import net.pixeldreamstudios.morequesttypes.util.TaskLocationHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class CastSpellTask extends Task {
    private final List<String> spells = new ArrayList<>();
    private long value = 1L;
    private boolean anyTarget = true;
    private ResourceLocation targetEntityTypeId = ResourceLocation.withDefaultNamespace("zombie");
    private TagKey<EntityType<?>> targetEntityTypeTag = null;
    private static final ResourceLocation DEFAULT_STRUCTURE = ResourceLocation.withDefaultNamespace("mineshaft");
    private static final List<String> KNOWN_STRUCTURES = new ArrayList<>();
    private Either<ResourceKey<Structure>, TagKey<Structure>> structure = null;
    private static final List<String> KNOWN_DIMENSIONS = new ArrayList<>();
    private String dimension = "";
    private static final List<String> KNOWN_BIOMES = new ArrayList<>();
    private String biome = "";

    public CastSpellTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.CAST_SPELL;
    }

    @Override
    public long getMaxProgress() {
        return Math.max(1L, value);
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 1;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;
        if (!SpellEngineCompat.isLoaded()) return;

        var online = teamData.getOnlineMembers();
        if (online == null || online.isEmpty()) return;

        UUID leaderId = online.stream()
                .map(ServerPlayer::getUUID)
                .min(Comparator.comparing(UUID::toString))
                .orElse(player.getUUID());
        if (!player.getUUID().equals(leaderId)) return;

        var events = SpellCastEventBuffer.snapshotUnprocessed(player.getUUID(), id);
        if (events.isEmpty()) return;

        long tick = events.getLast().gameTime();
        if (player.level().getGameTime() - tick > 1) return;

        long count = 0L;
        List<SpellCastEventBuffer.CastEvent> matched = new ArrayList<>();
        for (var event : events) {
            if (!spellMatches(event.spellId())) continue;
            if (!targetMatches(event.targets())) continue;
            if (!locationMatches(player)) continue;
            count++;
            matched.add(event);
        }

        if (count > 0) {
            long cur = teamData.getProgress(this);
            long next = Math.min(getMaxProgress(), cur + count);
            if (next != cur) teamData.setProgress(this, next);
            SpellCastEventBuffer.markProcessed(id, matched);
        }
    }

    private boolean spellMatches(ResourceLocation spellId) {
        if (spells.isEmpty()) return spellId != null;
        return spellId != null && spells.contains(spellId.toString());
    }

    private boolean targetMatches(List<Entity> targets) {
        if (anyTarget) return true;
        if (targets == null || targets.isEmpty()) return false;
        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity le)) continue;
            boolean ok = targetEntityTypeTag == null
                    ? targetEntityTypeId.equals(RegistrarManager.getId(le.getType(), Registries.ENTITY_TYPE))
                    : le.getType().is(targetEntityTypeTag);
            if (ok) return true;
        }
        return false;
    }

    private boolean locationMatches(ServerPlayer player) {
        if (structure == null && (dimension == null || dimension.isEmpty()) && (biome == null || biome.isEmpty())) {
            return true;
        }
        if (!(player.level() instanceof ServerLevel level)) return false;
        return TaskLocationHelper.matches(level, player.blockPosition(), structure, dimension, biome);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        return Component.translatable("morequesttypes.task.cast_spell.title", formatMaxProgress(), spells.size());
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return SpellDisplayHelper.spellListIcon(spells);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        list.blankLine();
        list.add(Component.translatable("morequesttypes.task.cast_spell.tooltip.header")
                .withStyle(ChatFormatting.GRAY));
        SpellTooltipHelper.addSpellLines(list, spells,
                Component.translatable("morequesttypes.task.cast_spell.tooltip.any_spell"));

        list.add(Component.translatable("morequesttypes.task.cast_spell.tooltip.count", formatMaxProgress())
                .withStyle(ChatFormatting.YELLOW));

        if (anyTarget) {
            list.add(Component.translatable("morequesttypes.task.cast_spell.tooltip.target_any")
                    .withStyle(ChatFormatting.GRAY));
        } else if (targetEntityTypeTag != null) {
            list.add(Component.translatable("morequesttypes.task.cast_spell.tooltip.target_tag",
                            targetEntityTypeTag.location().toString())
                    .withStyle(ChatFormatting.GRAY));
        } else {
            list.add(Component.translatable("morequesttypes.task.cast_spell.tooltip.target_entity",
                            Component.translatable("entity." + targetEntityTypeId.toLanguageKey()))
                    .withStyle(ChatFormatting.GRAY));
        }

        String structureLabel = getStructure();
        if (!structureLabel.isEmpty()) {
            list.add(Component.translatable("morequesttypes.task.cast_spell.tooltip.structure", structureLabel)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (dimension != null && !dimension.isEmpty()) {
            list.add(Component.translatable("morequesttypes.task.cast_spell.tooltip.dimension", dimension)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (biome != null && !biome.isEmpty()) {
            list.add(Component.translatable("morequesttypes.task.cast_spell.tooltip.biome", biome)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        config.add("spells", new MultiSelectListConfig(
                CastSpellTask::spellChoices,
                "morequesttypes.task.cast_spell.spells",
                SpellDisplayHelper::spellName
        ), spells, v -> {
            spells.clear();
            if (v != null) spells.addAll(v);
        }, new ArrayList<>()).setNameKey("morequesttypes.task.cast_spell.spells");

        config.addLong("value", value, v -> value = Math.max(1L, v), 1L, 1L, Long.MAX_VALUE)
                .setNameKey("morequesttypes.task.cast_spell.value");

        config.addBool("any_target", anyTarget, v -> anyTarget = v, true)
                .setNameKey("morequesttypes.task.cast_spell.any_target");

        var ids = new ArrayList<ResourceLocation>();
        BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            try {
                if (type.create(FTBQuestsClient.getClientLevel()) instanceof LivingEntity) {
                    ids.add(type.arch$registryName());
                }
            } catch (Exception ignored) {
            }
        });
        ids.sort((a, b) -> Component.translatable("entity." + a.toLanguageKey()).getString()
                .compareTo(Component.translatable("entity." + b.toLanguageKey()).getString()));

        var ENTITY_NAME_MAP = NameMap.of(targetEntityTypeId, ids)
                .nameKey(id -> "entity." + id.toLanguageKey())
                .create();
        var ENTITY_TAG_MAP = NameMap.of("",
                BuiltInRegistries.ENTITY_TYPE.getTags()
                        .map(p -> p.getFirst().location().toString())
                        .sorted()
                        .toArray(String[]::new)).create();

        config.addEnum("target_entity", targetEntityTypeId, v -> targetEntityTypeId = v, ENTITY_NAME_MAP, targetEntityTypeId)
                .setCanEdit(!anyTarget)
                .setNameKey("morequesttypes.task.cast_spell.target_entity");
        config.addEnum("target_entity_tag", getTargetTypeTagStr(), v -> targetEntityTypeTag = CastSpellTask.parseTypeTag(v), ENTITY_TAG_MAP)
                .setCanEdit(!anyTarget)
                .setNameKey("morequesttypes.task.cast_spell.target_entity_tag");

        addLocationConfig(config);
    }

    @Environment(EnvType.CLIENT)
    private static List<String> spellChoices() {
        List<String> choices = new ArrayList<>();
        try {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level != null && SpellEngineCompat.isLoaded()) {
                SpellEngineCompat.getAllSpells(minecraft.level).stream()
                        .map(ResourceLocation::toString)
                        .sorted()
                        .forEach(choices::add);
            }
        } catch (Throwable ignored) {
        }
        return choices;
    }

    @Environment(EnvType.CLIENT)
    private void addLocationConfig(ConfigGroup config) {
        if (CastSpellTask.KNOWN_STRUCTURES.isEmpty()) NetworkHelper.sendToServer(new MQTStructuresRequest());
        List<String> structureChoices = new ArrayList<>();
        structureChoices.add("");
        if (CastSpellTask.KNOWN_STRUCTURES.isEmpty()) structureChoices.add(CastSpellTask.DEFAULT_STRUCTURE.toString());
        else structureChoices.addAll(CastSpellTask.KNOWN_STRUCTURES);
        config.addEnum("structure", getStructure(), this::setStructure,
                        NameMap.of(getStructure(), structureChoices)
                                .name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "None" : s)).create())
                .setNameKey("morequesttypes.task.structure");

        if (CastSpellTask.KNOWN_DIMENSIONS.isEmpty()) NetworkHelper.sendToServer(new MQTWorldsRequest());
        List<String> dimChoices = new ArrayList<>();
        dimChoices.add("");
        if (CastSpellTask.KNOWN_DIMENSIONS.isEmpty()) {
            dimChoices.add("minecraft:overworld");
            dimChoices.add("minecraft:the_nether");
            dimChoices.add("minecraft:the_end");
        } else dimChoices.addAll(CastSpellTask.KNOWN_DIMENSIONS);
        config.addEnum("dimension", dimension, v -> dimension = v,
                        NameMap.of(dimension, dimChoices).name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "Any" : s)).create())
                .setNameKey("morequesttypes.task.dimension");

        if (CastSpellTask.KNOWN_BIOMES.isEmpty()) NetworkHelper.sendToServer(new MQTBiomesRequest());
        List<String> biomeChoices = new ArrayList<>();
        biomeChoices.add("");
        if (CastSpellTask.KNOWN_BIOMES.isEmpty()) {
            biomeChoices.add("minecraft:plains");
            biomeChoices.add("minecraft:forest");
            biomeChoices.add("minecraft:desert");
        } else biomeChoices.addAll(CastSpellTask.KNOWN_BIOMES);
        config.addEnum("biome", biome, v -> biome = v,
                        NameMap.of(biome, biomeChoices).name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "Any" : s)).create())
                .setNameKey("morequesttypes.task.biome");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (!spells.isEmpty()) {
            ListTag list = new ListTag();
            for (String s : spells) list.add(StringTag.valueOf(s));
            nbt.put("spells", list);
        }
        nbt.putLong("value", value);
        if (!anyTarget) nbt.putBoolean("any_target", false);
        nbt.putString("target_entity", targetEntityTypeId.toString());
        if (targetEntityTypeTag != null) nbt.putString("target_entity_tag", targetEntityTypeTag.location().toString());
        String s = getStructure();
        if (!s.isEmpty()) nbt.putString("structure", s);
        if (!dimension.isEmpty()) nbt.putString("dimension", dimension);
        if (!biome.isEmpty()) nbt.putString("biome", biome);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        spells.clear();
        if (nbt.contains("spells")) {
            var list = nbt.getList("spells", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String entry = list.getString(i);
                if (!entry.isBlank()) spells.add(entry.trim());
            }
        }
        value = Math.max(1L, nbt.getLong("value"));
        anyTarget = !nbt.contains("any_target") || nbt.getBoolean("any_target");
        targetEntityTypeId = ResourceLocation.tryParse(nbt.getString("target_entity"));
        if (targetEntityTypeId == null) targetEntityTypeId = ResourceLocation.withDefaultNamespace("zombie");
        targetEntityTypeTag = CastSpellTask.parseTypeTag(nbt.getString("target_entity_tag"));
        String s = nbt.getString("structure");
        if (!s.isEmpty()) setStructure(s);
        else structure = null;
        dimension = nbt.getString("dimension").trim();
        biome = nbt.getString("biome").trim();
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeVarInt(spells.size());
        for (String s : spells) buf.writeUtf(s == null ? "" : s);
        buf.writeVarLong(value);
        buf.writeBoolean(anyTarget);
        buf.writeUtf(targetEntityTypeId.toString());
        buf.writeUtf(targetEntityTypeTag == null ? "" : targetEntityTypeTag.location().toString());
        buf.writeUtf(getStructure());
        buf.writeUtf(dimension);
        buf.writeUtf(biome);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        spells.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            String s = buf.readUtf();
            if (!s.isBlank()) spells.add(s.trim());
        }
        value = Math.max(1L, buf.readVarLong());
        anyTarget = buf.readBoolean();
        targetEntityTypeId = ResourceLocation.tryParse(buf.readUtf());
        if (targetEntityTypeId == null) targetEntityTypeId = ResourceLocation.withDefaultNamespace("zombie");
        targetEntityTypeTag = CastSpellTask.parseTypeTag(buf.readUtf());
        String s = buf.readUtf();
        if (!s.isEmpty()) setStructure(s);
        else structure = null;
        dimension = buf.readUtf();
        biome = buf.readUtf();
    }

    private void setStructure(String resLoc) {
        structure = TaskLocationHelper.parseStructure(resLoc, CastSpellTask.DEFAULT_STRUCTURE);
    }

    private String getStructure() {
        return TaskLocationHelper.formatStructure(structure);
    }

    private String getTargetTypeTagStr() {
        return targetEntityTypeTag == null ? "" : targetEntityTypeTag.location().toString();
    }

    private static TagKey<EntityType<?>> parseTypeTag(String str) {
        if (str == null || str.isEmpty()) return null;
        String s = str.startsWith("#") ? str.substring(1) : str;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        return (rl != null && !rl.getPath().isEmpty()) ? TagKey.create(Registries.ENTITY_TYPE, rl) : null;
    }

    public static void syncKnownStructureList(List<String> data) {
        CastSpellTask.KNOWN_STRUCTURES.clear();
        CastSpellTask.KNOWN_STRUCTURES.addAll(data);
    }

    public static void syncKnownDimensionList(List<String> data) {
        CastSpellTask.KNOWN_DIMENSIONS.clear();
        CastSpellTask.KNOWN_DIMENSIONS.addAll(data);
    }

    public static void syncKnownBiomeList(List<String> data) {
        CastSpellTask.KNOWN_BIOMES.clear();
        CastSpellTask.KNOWN_BIOMES.addAll(data);
    }
}
