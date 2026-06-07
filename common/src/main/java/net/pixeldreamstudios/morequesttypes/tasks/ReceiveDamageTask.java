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
import net.minecraft.client.gui.screens.Screen;
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
import net.pixeldreamstudios.morequesttypes.client.LastReceivedDamageClientCache;
import net.pixeldreamstudios.morequesttypes.config.MultiSelectListConfig;
import net.pixeldreamstudios.morequesttypes.event.ReceiveDamageEventBuffer;
import net.pixeldreamstudios.morequesttypes.network.MQTBiomesRequest;
import net.pixeldreamstudios.morequesttypes.network.MQTStructuresRequest;
import net.pixeldreamstudios.morequesttypes.network.MQTWorldsRequest;
import net.pixeldreamstudios.morequesttypes.network.NetworkHelper;
import net.pixeldreamstudios.morequesttypes.util.DamageTypeHelper;
import net.pixeldreamstudios.morequesttypes.util.TaskLocationHelper;

import java.util.ArrayList;
import java.util.List;

public final class ReceiveDamageTask extends Task {
    public enum Mode {TOTAL, HIGHEST}

    private boolean anyDamageType = true;
    private boolean anyEntity = true;
    private ResourceLocation sourceEntityTypeId = ResourceLocation.withDefaultNamespace("zombie");
    private TagKey<EntityType<?>> sourceEntityTypeTag = null;
    private final List<String> damageTypes = new ArrayList<>();
    private Mode mode = Mode.TOTAL;
    private long value = 100L;
    private static final ResourceLocation DEFAULT_STRUCTURE = ResourceLocation.withDefaultNamespace("mineshaft");
    private static final List<String> KNOWN_STRUCTURES = new ArrayList<>();
    private Either<ResourceKey<Structure>, TagKey<Structure>> structure = null;
    private static final List<String> KNOWN_DIMENSIONS = new ArrayList<>();
    private String dimension = "";
    private static final List<String> KNOWN_BIOMES = new ArrayList<>();
    private String biome = "";

    public ReceiveDamageTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.RECEIVE_DAMAGE;
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

        var hits = ReceiveDamageEventBuffer.snapshotUnprocessed(player.getUUID(), id);
        if (hits.isEmpty()) return;

        long sum = 0L;
        long best = 0L;
        List<ReceiveDamageEventBuffer.ReceiveHit> matched = new ArrayList<>();

        for (var hit : hits) {
            if (!damageTypeMatches(hit.damageType())) continue;
            if (!sourceMatches(hit.source())) continue;
            if (!locationMatches(player)) continue;

            long amt = Math.max(1L, hit.amountRounded());
            sum += amt;
            best = Math.max(best, amt);
            matched.add(hit);
        }

        if (!matched.isEmpty()) {
            long cur = teamData.getProgress(this);
            long next = switch (mode) {
                case TOTAL -> Math.min(getMaxProgress(), cur + sum);
                case HIGHEST -> Math.min(getMaxProgress(), Math.max(cur, best));
            };
            if (next != cur) teamData.setProgress(this, next);
            ReceiveDamageEventBuffer.markProcessed(id, matched);
        }
    }

    private boolean damageTypeMatches(ResourceLocation type) {
        if (anyDamageType) return true;
        if (damageTypes.isEmpty() || type == null) return false;
        return damageTypes.contains(type.toString());
    }

    private boolean sourceMatches(Entity source) {
        if (anyEntity) return true;
        if (!(source instanceof LivingEntity le)) return false;
        return sourceEntityTypeTag == null
                ? sourceEntityTypeId.equals(RegistrarManager.getId(le.getType(), Registries.ENTITY_TYPE))
                : le.getType().is(sourceEntityTypeTag);
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
        String source = anyEntity ? "any entity"
                : (sourceEntityTypeTag == null)
                ? Component.translatable("entity." + sourceEntityTypeId.toLanguageKey()).getString()
                : "#" + sourceEntityTypeTag.location();
        String how = mode == Mode.TOTAL ? "total" : "highest";
        return Component.translatable("morequesttypes.task.receive_damage.title", formatMaxProgress(), source, how);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return Icon.getIcon("minecraft:item/diamond_chestplate");
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        list.blankLine();
        list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.header")
                .withStyle(ChatFormatting.GRAY));
        if (anyDamageType) {
            list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.any_damage")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else if (damageTypes.isEmpty()) {
            list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.no_damage")
                    .withStyle(ChatFormatting.RED));
        } else {
            for (String id : damageTypes) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                MutableComponent line = rl == null
                        ? Component.literal(id)
                        : Component.translatable("damage_type." + rl.getNamespace() + "." + rl.getPath());
                list.add(Component.literal(" - ").append(line).withStyle(ChatFormatting.AQUA));
            }
        }

        if (anyEntity) {
            list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.entity_any")
                    .withStyle(ChatFormatting.GRAY));
        } else if (sourceEntityTypeTag != null) {
            list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.source_tag",
                            sourceEntityTypeTag.location().toString())
                    .withStyle(ChatFormatting.GRAY));
        } else {
            list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.source_entity",
                            Component.translatable("entity." + sourceEntityTypeId.toLanguageKey()))
                    .withStyle(ChatFormatting.GRAY));
        }

        String modeKey = mode == Mode.TOTAL
                ? "morequesttypes.task.receive_damage.tooltip.mode_total"
                : "morequesttypes.task.receive_damage.tooltip.mode_highest";
        list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.amount",
                        formatMaxProgress(), Component.translatable(modeKey))
                .withStyle(ChatFormatting.YELLOW));

        String structureLabel = getStructure();
        if (!structureLabel.isEmpty()) {
            list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.structure", structureLabel)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (dimension != null && !dimension.isEmpty()) {
            list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.dimension", dimension)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (biome != null && !biome.isEmpty()) {
            list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.biome", biome)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        if (Screen.hasShiftDown()) {
            addShiftDamageDebugTooltip(list);
        }
    }

    @Environment(EnvType.CLIENT)
    private void addShiftDamageDebugTooltip(TooltipList list) {
        list.blankLine();
        list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.shift.header")
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        if (LastReceivedDamageClientCache.hasData()) {
            ResourceLocation type = LastReceivedDamageClientCache.damageType();
            MutableComponent typeName = Component.translatable(
                    "damage_type." + type.getNamespace() + "." + type.getPath());
            list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.shift.type", typeName)
                    .withStyle(ChatFormatting.AQUA));

            ResourceLocation sourceId = LastReceivedDamageClientCache.sourceEntityType();
            if (sourceId != null) {
                list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.shift.source",
                                Component.translatable("entity." + sourceId.toLanguageKey()))
                        .withStyle(ChatFormatting.GRAY));
            } else {
                list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.shift.source_unknown")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }

            list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.shift.amount",
                            LastReceivedDamageClientCache.amount())
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        list.add(Component.translatable("morequesttypes.task.receive_damage.tooltip.shift.none")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        config.addBool("any_damage_type", anyDamageType, v -> anyDamageType = v, true)
                .setNameKey("morequesttypes.task.receive_damage.any_damage_type");

        config.add("damage_types", new MultiSelectListConfig(
                        () -> DamageTypeHelper.clientDamageTypeChoices(FTBQuestsClient.getClientLevel()),
                        "morequesttypes.task.receive_damage.damage_types",
                        id -> {
                            ResourceLocation rl = ResourceLocation.tryParse(id);
                            return rl == null
                                    ? Component.literal(id)
                                    : Component.translatable("damage_type." + rl.getNamespace() + "." + rl.getPath());
                        }
                ), damageTypes, v -> {
                    damageTypes.clear();
                    if (v != null) damageTypes.addAll(v);
                }, new ArrayList<>())
                .setNameKey("morequesttypes.task.receive_damage.damage_types");

        config.addBool("any_entity", anyEntity, v -> anyEntity = v, true)
                .setNameKey("morequesttypes.task.receive_damage.any_entity");

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

        var ENTITY_NAME_MAP = NameMap.of(sourceEntityTypeId, ids)
                .nameKey(id -> "entity." + id.toLanguageKey())
                .create();
        var ENTITY_TAG_MAP = NameMap.of("",
                BuiltInRegistries.ENTITY_TYPE.getTags()
                        .map(p -> p.getFirst().location().toString())
                        .sorted()
                        .toArray(String[]::new)).create();

        config.addEnum("source_entity", sourceEntityTypeId, v -> sourceEntityTypeId = v, ENTITY_NAME_MAP, sourceEntityTypeId)
                .setNameKey("morequesttypes.task.receive_damage.source_entity");
        config.addEnum("source_entity_tag", getSourceTypeTagStr(), v -> sourceEntityTypeTag = ReceiveDamageTask.parseTypeTag(v), ENTITY_TAG_MAP)
                .setNameKey("morequesttypes.task.receive_damage.source_entity_tag");

        var MODES = NameMap.of(Mode.TOTAL, Mode.values()).create();
        config.addEnum("mode", mode, v -> mode = v, MODES).setNameKey("morequesttypes.task.damage.mode");
        config.addLong("value", value, v -> value = Math.max(1L, v), 100L, 1L, Long.MAX_VALUE)
                .setNameKey("morequesttypes.task.damage.value");

        addLocationConfig(config);
    }

    @Environment(EnvType.CLIENT)
    private void addLocationConfig(ConfigGroup config) {
        ReceiveDamageTask.maybeRequestStructureSync();
        List<String> choices = new ArrayList<>();
        choices.add("");
        if (ReceiveDamageTask.KNOWN_STRUCTURES.isEmpty()) choices.add(ReceiveDamageTask.DEFAULT_STRUCTURE.toString());
        else choices.addAll(ReceiveDamageTask.KNOWN_STRUCTURES);
        var STRUCTURE_MAP = NameMap.of(getStructure(), choices)
                .name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "None" : s))
                .create();
        config.addEnum("structure", getStructure(), this::setStructure, STRUCTURE_MAP)
                .setNameKey("morequesttypes.task.structure");

        ReceiveDamageTask.maybeRequestWorldSync();
        List<String> dimChoices = new ArrayList<>();
        dimChoices.add("");
        if (ReceiveDamageTask.KNOWN_DIMENSIONS.isEmpty()) {
            dimChoices.add("minecraft:overworld");
            dimChoices.add("minecraft:the_nether");
            dimChoices.add("minecraft:the_end");
        } else dimChoices.addAll(ReceiveDamageTask.KNOWN_DIMENSIONS);
        config.addEnum("dimension", dimension, v -> dimension = v,
                        NameMap.of(dimension, dimChoices).name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "Any" : s)).create())
                .setNameKey("morequesttypes.task.dimension");

        ReceiveDamageTask.maybeRequestBiomeSync();
        List<String> biomeChoices = new ArrayList<>();
        biomeChoices.add("");
        if (ReceiveDamageTask.KNOWN_BIOMES.isEmpty()) {
            biomeChoices.add("minecraft:plains");
            biomeChoices.add("minecraft:forest");
            biomeChoices.add("minecraft:desert");
        } else biomeChoices.addAll(ReceiveDamageTask.KNOWN_BIOMES);
        config.addEnum("biome", biome, v -> biome = v,
                        NameMap.of(biome, biomeChoices).name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "Any" : s)).create())
                .setNameKey("morequesttypes.task.biome");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putBoolean("any_entity", anyEntity);
        nbt.putBoolean("any_damage_type", anyDamageType);
        nbt.putString("source_entity", sourceEntityTypeId.toString());
        if (sourceEntityTypeTag != null) nbt.putString("source_entity_tag", sourceEntityTypeTag.location().toString());
        if (!anyDamageType && !damageTypes.isEmpty()) {
            ListTag list = new ListTag();
            for (String s : damageTypes) list.add(StringTag.valueOf(s));
            nbt.put("damage_types", list);
        }
        nbt.putString("mode", mode.name());
        nbt.putLong("value", value);
        String s = getStructure();
        if (!s.isEmpty()) nbt.putString("structure", s);
        if (!dimension.isEmpty()) nbt.putString("dimension", dimension);
        if (!biome.isEmpty()) nbt.putString("biome", biome);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        if (nbt.contains("any_entity")) {
            anyEntity = nbt.getBoolean("any_entity");
        } else {
            anyEntity = !nbt.contains("any_source") || nbt.getBoolean("any_source");
        }
        sourceEntityTypeId = ResourceLocation.tryParse(nbt.getString("source_entity"));
        if (sourceEntityTypeId == null) sourceEntityTypeId = ResourceLocation.withDefaultNamespace("zombie");
        sourceEntityTypeTag = ReceiveDamageTask.parseTypeTag(nbt.getString("source_entity_tag"));
        damageTypes.clear();
        if (nbt.contains("damage_types")) {
            var list = nbt.getList("damage_types", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String entry = list.getString(i);
                if (!entry.isBlank()) damageTypes.add(entry.trim());
            }
        }
        if (nbt.contains("any_damage_type")) {
            anyDamageType = nbt.getBoolean("any_damage_type");
        } else {
            anyDamageType = damageTypes.isEmpty();
        }
        try {
            mode = Mode.valueOf(nbt.getString("mode"));
        } catch (Throwable ignored) {
            mode = Mode.TOTAL;
        }
        value = Math.max(1L, nbt.getLong("value"));
        String s = nbt.getString("structure");
        if (!s.isEmpty()) setStructure(s);
        else structure = null;
        dimension = nbt.getString("dimension").trim();
        biome = nbt.getString("biome").trim();
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeBoolean(anyEntity);
        buf.writeBoolean(anyDamageType);
        buf.writeUtf(sourceEntityTypeId.toString());
        buf.writeUtf(sourceEntityTypeTag == null ? "" : sourceEntityTypeTag.location().toString());
        buf.writeVarInt(damageTypes.size());
        for (String s : damageTypes) buf.writeUtf(s == null ? "" : s);
        buf.writeEnum(mode);
        buf.writeVarLong(value);
        buf.writeUtf(getStructure());
        buf.writeUtf(dimension);
        buf.writeUtf(biome);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        anyEntity = buf.readBoolean();
        anyDamageType = buf.readBoolean();
        sourceEntityTypeId = ResourceLocation.tryParse(buf.readUtf());
        if (sourceEntityTypeId == null) sourceEntityTypeId = ResourceLocation.withDefaultNamespace("zombie");
        sourceEntityTypeTag = ReceiveDamageTask.parseTypeTag(buf.readUtf());
        damageTypes.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            String s = buf.readUtf();
            if (!s.isBlank()) damageTypes.add(s.trim());
        }
        mode = buf.readEnum(Mode.class);
        value = Math.max(1L, buf.readVarLong());
        String s = buf.readUtf();
        if (!s.isEmpty()) setStructure(s);
        else structure = null;
        dimension = buf.readUtf();
        biome = buf.readUtf();
    }

    private void setStructure(String resLoc) {
        structure = TaskLocationHelper.parseStructure(resLoc, ReceiveDamageTask.DEFAULT_STRUCTURE);
    }

    private String getStructure() {
        return TaskLocationHelper.formatStructure(structure);
    }

    private String getSourceTypeTagStr() {
        return sourceEntityTypeTag == null ? "" : sourceEntityTypeTag.location().toString();
    }

    private static TagKey<EntityType<?>> parseTypeTag(String str) {
        if (str == null || str.isEmpty()) return null;
        String s = str.startsWith("#") ? str.substring(1) : str;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        return (rl != null && !rl.getPath().isEmpty()) ? TagKey.create(Registries.ENTITY_TYPE, rl) : null;
    }

    private static void maybeRequestStructureSync() {
        if (ReceiveDamageTask.KNOWN_STRUCTURES.isEmpty()) NetworkHelper.sendToServer(new MQTStructuresRequest());
    }

    private static void maybeRequestWorldSync() {
        if (ReceiveDamageTask.KNOWN_DIMENSIONS.isEmpty()) NetworkHelper.sendToServer(new MQTWorldsRequest());
    }

    private static void maybeRequestBiomeSync() {
        if (ReceiveDamageTask.KNOWN_BIOMES.isEmpty()) NetworkHelper.sendToServer(new MQTBiomesRequest());
    }

    public static void syncKnownStructureList(List<String> data) {
        ReceiveDamageTask.KNOWN_STRUCTURES.clear();
        ReceiveDamageTask.KNOWN_STRUCTURES.addAll(data);
    }

    public static void syncKnownDimensionList(List<String> data) {
        ReceiveDamageTask.KNOWN_DIMENSIONS.clear();
        ReceiveDamageTask.KNOWN_DIMENSIONS.addAll(data);
    }

    public static void syncKnownBiomeList(List<String> data) {
        ReceiveDamageTask.KNOWN_BIOMES.clear();
        ReceiveDamageTask.KNOWN_BIOMES.addAll(data);
    }
}
