package net.pixeldreamstudios.morequesttypes.tasks;

import com.mojang.datafixers.util.Either;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.registries.RegistrarManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.pixeldreamstudios.morequesttypes.api.ITaskDungeonDifficultyExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskDynamicDifficultyExtension;
import net.pixeldreamstudios.morequesttypes.compat.DungeonDifficultyCompat;
import net.pixeldreamstudios.morequesttypes.compat.DynamicDifficultyCompat;
import net.pixeldreamstudios.morequesttypes.event.DamageEventBuffer;
import net.pixeldreamstudios.morequesttypes.network.MQTBiomesRequest;
import net.pixeldreamstudios.morequesttypes.network.MQTStructuresRequest;
import net.pixeldreamstudios.morequesttypes.network.MQTWorldsRequest;
import net.pixeldreamstudios.morequesttypes.util.ComparisonManager;
import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class DamageTask extends Task {
    public enum Mode { TOTAL, HIGHEST }
    private ResourceLocation entityTypeId = ResourceLocation.withDefaultNamespace("zombie");
    private TagKey<EntityType<?>> entityTypeTag = null;
    private String customName = "";
    private final List<String> scoreboardTags = new ArrayList<>();
    private int    minTagsRequired = 0;
    private String nbtFilterSnbt = "";
    private transient Tag nbtFilterParsed = null;
    private ItemStack heldItemFilter = ItemStack.EMPTY;
    private String    heldItemTagStr = "";
    private transient TagKey<net.minecraft.world.item.Item> heldItemTag;
    private Mode mode = Mode.TOTAL;
    private long value = 100L;
    private static final ResourceLocation DEFAULT_STRUCTURE = ResourceLocation.withDefaultNamespace("mineshaft");
    private static final List<String> KNOWN_STRUCTURES = new ArrayList<>();
    private Either<ResourceKey<Structure>, TagKey<Structure>> structure = null;
    private static final List<String> KNOWN_DIMENSIONS = new ArrayList<>();
    private String dimension = "";
    private static final List<String> KNOWN_BIOMES = new ArrayList<>();
    private String biome = "";
    public DamageTask(long id, dev.ftb.mods.ftbquests.quest.Quest quest) {
        super(id, quest);
    }
    @Override
    public TaskType getType() {
        return MoreTasksTypes.DAMAGE;
    }
    @Override
    public long getMaxProgress() {
        return Math.max(1L, value);
    }
    @Override
    public boolean hideProgressNumbers() {
        return false;
    }
    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        String who = (entityTypeTag == null)
                ? Component.translatable("entity." + entityTypeId.toLanguageKey()).getString()
                : "#" + entityTypeTag.location();
        String how = mode == Mode.TOTAL ?  "total" : "highest";

        MutableComponent baseTitle = Component.translatable("morequesttypes.task.damage.title", formatMaxProgress(), who, how);

        if (DynamicDifficultyCompat.isLoaded()) {
            ITaskDynamicDifficultyExtension ext = (ITaskDynamicDifficultyExtension)(Object) this;
            if (ext.shouldCheckDynamicDifficultyLevel()) {
                String levelReq = mqt$formatLevelRequirement(
                        ext.getDynamicDifficultyComparison(),
                        ext.getDynamicDifficultyFirst(),
                        ext.getDynamicDifficultySecond()
                );
                baseTitle = Component.translatable("morequesttypes.task.damage.title_with_dynamic_difficulty",
                        baseTitle, levelReq);
            }
        }

        if (DungeonDifficultyCompat.isLoaded()) {
            ITaskDungeonDifficultyExtension dungeonExt = (ITaskDungeonDifficultyExtension)(Object) this;
            if (dungeonExt.shouldCheckDungeonDifficultyLevel()) {
                String difficultyReq = mqt$formatLevelRequirement(
                        dungeonExt.getDungeonDifficultyComparison(),
                        dungeonExt.getDungeonDifficultyFirst(),
                        dungeonExt.getDungeonDifficultySecond()
                );
                baseTitle = Component.translatable("morequesttypes.task.damage.title_with_dungeon_difficulty",
                        baseTitle, difficultyReq);
            }
        }

        return baseTitle;
    }

    @Unique
    private String mqt$formatLevelRequirement(ComparisonMode mode, int first, int second) {
        return switch (mode) {
            case EQUALS -> "= " + first;
            case GREATER_THAN -> "> " + first;
            case LESS_THAN -> "< " + first;
            case GREATER_OR_EQUAL -> "≥ " + first;
            case LESS_OR_EQUAL -> "≤ " + first;
            case RANGE -> first + " > x > " + second;
            case RANGE_EQUAL -> first + " ≥ x ≥ " + second;
            case RANGE_EQUAL_FIRST -> first + " ≥ x > " + second;
            case RANGE_EQUAL_SECOND -> first + " > x ≥ " + second;
        };
    }
    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return Icon.getIcon("minecraft:item/wooden_sword");
    }
    @Override
    public int autoSubmitOnPlayerTick() {
        return 1;
    }
    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        var online = teamData.getOnlineMembers();
        if (online == null || online.isEmpty()) return;

        UUID leaderId = online.stream()
                .map(ServerPlayer::getUUID)
                .min(Comparator.comparing(UUID::toString))
                .orElse(player.getUUID());

        if (!player.getUUID().equals(leaderId)) return;

        var hits = DamageEventBuffer.snapshotUnprocessed(player.getUUID(), this.id);
        if (hits.isEmpty()) return;

        long tick = hits.get(hits.size() - 1).gameTime();
        long now = player.level().getGameTime();

        if (now - tick > 1) {
            return;
        }

        long sum = 0L;
        long best = 0L;
        List<DamageEventBuffer.Hit> matchedHits = new ArrayList<>();

        for (var h : hits) {
            Entity e = h.victim();
            if (!(e instanceof LivingEntity le)) continue;
            if (! entityMatches(le)) continue;
            if (! heldItemMatches(h.stack())) continue;

            long amt = Math.max(0L, h.amountBaselineRounded());
            sum += amt;
            best = Math.max(best, amt);
            matchedHits.add(h);
        }

        if (sum > 0 || best > 0) {
            long cur = teamData.getProgress(this);
            long next = switch (mode) {
                case TOTAL -> Math.min(getMaxProgress(), cur + sum);
                case HIGHEST -> Math.min(getMaxProgress(), Math.max(cur, best));
            };
            if (next != cur) {
                teamData.setProgress(this, next);
            }

            // Mark these hits as processed by THIS task
            DamageEventBuffer.markProcessed(this.id, matchedHits);
        }
    }

    private boolean entityMatches(LivingEntity e) {
        boolean baseOk = (entityTypeTag == null)
                ? entityTypeId.equals(RegistrarManager.getId(e.getType(), Registries.ENTITY_TYPE)) && nameMatchOK(e)
                : e.getType().is(entityTypeTag) && nameMatchOK(e);
        if (!baseOk) return false;

        if (!scoreboardTags.isEmpty()) {
            var present = e.getTags();
            int count = 0;
            for (var r : scoreboardTags) if (present.contains(r)) count++;
            int need = (minTagsRequired <= 0) ? scoreboardTags.size() : minTagsRequired;
            if (count < need) return false;
        }

        if (nbtFilterParsed != null) {
            var actual = new CompoundTag();
            e.saveWithoutId(actual);
            if (!(nbtFilterParsed instanceof CompoundTag filter) || !nbtSubsetMatches(actual, filter)) {
                return false;
            }
        }

        if (structure != null || (dimension != null && !dimension.isEmpty()) || (biome != null && !biome.isEmpty())) {
            if (!(e.level() instanceof ServerLevel level)) return false;
            if (structure != null && !isInsideStructureOrTag(level, e.blockPosition())) return false;
            if (!isInsideDimension(level)) return false;
            if (!isInsideBiome(level, e.blockPosition())) return false;
        }
        if (DynamicDifficultyCompat.isLoaded()) {
            ITaskDynamicDifficultyExtension ext = (ITaskDynamicDifficultyExtension)(Object) this;
            if (ext.shouldCheckDynamicDifficultyLevel() && DynamicDifficultyCompat.canHaveLevel(e)) {
                int mobLevel = DynamicDifficultyCompat.getLevel(e);
                if (! ComparisonManager.compare(mobLevel,
                        ext.getDynamicDifficultyComparison(),
                        ext.getDynamicDifficultyFirst(),
                        ext.getDynamicDifficultySecond())) {
                    return false;
                }
            }
        }

        if (DungeonDifficultyCompat.isLoaded()) {
            ITaskDungeonDifficultyExtension dungeonExt = (ITaskDungeonDifficultyExtension)(Object) this;
            if (dungeonExt.shouldCheckDungeonDifficultyLevel() && DungeonDifficultyCompat.canHaveLevel(e)) {
                int dungeonLevel = DungeonDifficultyCompat.getLevel(e);
                if (!ComparisonManager.compare(dungeonLevel,
                        dungeonExt.getDungeonDifficultyComparison(),
                        dungeonExt.getDungeonDifficultyFirst(),
                        dungeonExt.getDungeonDifficultySecond())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isInsideStructureOrTag(ServerLevel level, net.minecraft.core.BlockPos pos) {
        StructureManager mgr = level.structureManager();
        return structure.map(
                key -> {
                    var holder = mgr.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(key);
                    if (holder.isEmpty()) return false;
                    var structure = holder.get().value();
                    return mgr.getStructureWithPieceAt(pos, structure).isValid();
                },
                tag -> mgr.registryAccess()
                        .registryOrThrow(Registries.STRUCTURE)
                        .getTag(tag)
                        .map(hs -> {
                            for (var h : hs) {
                                var structure = h.value();
                                if (mgr.getStructureWithPieceAt(pos, structure).isValid()) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .orElse(false)
        );
    }
    private boolean isInsideDimension(ServerLevel level) {
        if (dimension == null || dimension.isEmpty()) return true;
        String cur = level.dimension().location().toString();
        return dimension.equals(cur);
    }
    private boolean isInsideBiome(ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (biome == null || biome.isEmpty()) return true;
        Holder<Biome> h = level.getBiome(pos);
        if (biome.startsWith("#")) {
            String s = biome.substring(1);
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl == null) return false;
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, rl);
            return h.is(tag);
        } else {
            return h.unwrapKey().map(k -> k.location().toString().equals(biome)).orElse(false);
        }
    }

    private boolean nameMatchOK(LivingEntity e) {
        if (!customName.isEmpty()) {
            if (e instanceof Player p) return p.getGameProfile().getName().equals(customName);
            return e.getName().getString().equals(customName);
        }
        return true;
    }

    private boolean heldItemMatches(ItemStack stack) {
        if (heldItemTag != null) return !stack.isEmpty() && stack.is(heldItemTag);
        if (heldItemFilter.isEmpty()) return true;
        return !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, heldItemFilter);
    }

    private static List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        String[] parts = csv.split(",");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static boolean nbtSubsetMatches(Tag actual, Tag filter) {
        if (filter instanceof CompoundTag f) {
            if (!(actual instanceof CompoundTag a)) return false;
            for (String key : f.getAllKeys()) {
                Tag fVal = f.get(key);
                Tag aVal = a.get(key);
                if (aVal == null || !nbtSubsetMatches(aVal, fVal)) return false;
            }
            return true;
        }
        if (filter instanceof ListTag fList) {
            if (!(actual instanceof ListTag aList)) return false;
            List<Tag> remaining = new ArrayList<>(aList);
            outer:
            for (Tag fEl : fList) {
                for (int j = 0; j < remaining.size(); j++) {
                    if (nbtSubsetMatches(remaining.get(j), fEl)) {
                        remaining.remove(j);
                        continue outer;
                    }
                }
                return false;
            }
            return true;
        }
        return NbtUtils.compareNbt(filter, actual, false) && NbtUtils.compareNbt(actual, filter, false);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var ids = new java.util.ArrayList<ResourceLocation>();
        BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            if (type.create(FTBQuestsClient.getClientLevel()) instanceof net.minecraft.world.entity.LivingEntity) {
                ids.add(type.arch$registryName());
            }
        });
        ids.sort((a, b) -> {
            var c1 = Component.translatable("entity." + a.toLanguageKey());
            var c2 = Component.translatable("entity." + b.toLanguageKey());
            return c1.getString().compareTo(c2.getString());
        });
        var ENTITY_NAME_MAP = NameMap.of(entityTypeId, ids)
                .nameKey(id -> "entity." + id.toLanguageKey())
                .create();

        var ENTITY_TAG_MAP = NameMap.of("",
                BuiltInRegistries.ENTITY_TYPE.getTags()
                        .map(p -> p.getFirst().location().toString())
                        .sorted()
                        .toArray(String[]::new)).create();

        config.addEnum("entity", entityTypeId, v -> entityTypeId = v, ENTITY_NAME_MAP, ResourceLocation.withDefaultNamespace("zombie"));
        config.addEnum("entity_type_tag", getTypeTagStr(), v -> entityTypeTag = parseTypeTag(v), ENTITY_TAG_MAP);
        config.addString("custom_name", customName, v -> customName = v, "");
        config.addList("scoreboard_tags", scoreboardTags, new StringConfig(), "")
                .setNameKey("morequesttypes.task.tags_csv");
        config.addInt("min_tags_required", minTagsRequired, v -> minTagsRequired = Math.max(0, v), 0, 0, 64)
                .setNameKey("morequesttypes.task.min_tags");
        config.addString("nbt_filter_snbt", nbtFilterSnbt, v -> { nbtFilterSnbt = v; parseNbtFilter(); }, "")
                .setNameKey("morequesttypes.task.nbt");

        var MODES = NameMap.of(Mode.TOTAL, Mode.values()).create();
        config.addEnum("mode", mode, v -> mode = v, MODES).setNameKey("morequesttypes.task.damage.mode");

        config.addLong("value", value, v -> value = Math.max(1L, v), 100L, 1L, Long.MAX_VALUE)
                .setNameKey("morequesttypes.task.damage.value");

        config.add(
                "held_item",
                new dev.ftb.mods.ftbquests.client.ConfigIconItemStack(),
                heldItemFilter,
                v -> { heldItemFilter = v.copy(); if (!heldItemFilter.isEmpty()) heldItemFilter.setCount(1); },
                ItemStack.EMPTY
        ).setNameKey("morequesttypes.task.damage.held_item");

        var ITEM_TAG_MAP = NameMap.of("",
                BuiltInRegistries.ITEM.getTags()
                        .map(p -> p.getFirst().location().toString())
                        .sorted()
                        .toArray(String[]::new)).create();

        config.addEnum("held_item_tag", heldItemTagStr, v -> {
            heldItemTagStr = v;
            resolveHeldItemTag();
        }, ITEM_TAG_MAP).setNameKey("morequesttypes.task.damage.held_item_tag");

        maybeRequestStructureSync();

        List<String> choices = new ArrayList<>();
        choices.add("");
        if (KNOWN_STRUCTURES.isEmpty()) {
            choices.add(DEFAULT_STRUCTURE.toString());
        } else {
            choices.addAll(KNOWN_STRUCTURES);
        }

        var STRUCTURE_MAP = NameMap
                .of(getStructure(), choices)
                .name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "None" : s))
                .create();

        config.addEnum("structure", getStructure(), this::setStructure, STRUCTURE_MAP)
                .setNameKey("morequesttypes.task.structure");

        maybeRequestWorldSync();
        List<String> dimChoices = new ArrayList<>();
        dimChoices.add("");
        if (KNOWN_DIMENSIONS.isEmpty()) {
            dimChoices.add("minecraft:overworld");
            dimChoices.add("minecraft:the_nether");
            dimChoices.add("minecraft:the_end");
        } else {
            dimChoices.addAll(KNOWN_DIMENSIONS);
        }
        var DIMENSION_MAP = NameMap.of(dimension, dimChoices)
                .name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "Any" : s))
                .create();
        config.addEnum("dimension", dimension, v -> dimension = v, DIMENSION_MAP)
                .setNameKey("morequesttypes.task.dimension");

        maybeRequestBiomeSync();
        List<String> biomeChoices = new ArrayList<>();
        biomeChoices.add("");
        if (KNOWN_BIOMES.isEmpty()) {
            biomeChoices.add("minecraft:plains");
            biomeChoices.add("minecraft:forest");
            biomeChoices.add("minecraft:desert");
        } else {
            biomeChoices.addAll(KNOWN_BIOMES);
        }
        var BIOME_MAP = NameMap.of(biome, biomeChoices)
                .name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "Any" : s))
                .create();
        config.addEnum("biome", biome, v -> biome = v, BIOME_MAP)
                .setNameKey("morequesttypes.task.biome");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("entity", entityTypeId.toString());
        if (entityTypeTag != null) nbt.putString("entityTypeTag", entityTypeTag.location().toString());
        if (!customName.isEmpty()) nbt.putString("custom_name", customName);
        if (!scoreboardTags.isEmpty()) {
            ListTag tagList = new ListTag();
            for (String s : scoreboardTags) tagList.add(StringTag.valueOf(s));
            nbt.put("scoreboard_tags", tagList);
        }
        if (minTagsRequired > 0) nbt.putInt("min_tags_required", minTagsRequired);
        if (!nbtFilterSnbt.isEmpty()) nbt.putString("nbt_filter_snbt", nbtFilterSnbt);

        nbt.putString("mode", mode.name());
        nbt.putLong("value", value);

        if (!heldItemFilter.isEmpty()) nbt.put("held_item", saveItemSingleLine(heldItemFilter.copyWithCount(1)));
        if (!heldItemTagStr.isEmpty()) nbt.putString("held_item_tag", heldItemTagStr);

        String s = getStructure();
        if (!s.isEmpty()) nbt.putString("structure", s);

        if (!dimension.isEmpty()) nbt.putString("dimension", dimension);
        if (!biome.isEmpty()) nbt.putString("biome", biome);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        entityTypeId = ResourceLocation.tryParse(nbt.getString("entity"));
        entityTypeTag = parseTypeTag(nbt.getString("entityTypeTag"));
        customName = nbt.getString("custom_name");
        scoreboardTags.clear();
        if (nbt.contains("scoreboard_tags")) {
            var list = nbt.getList("scoreboard_tags", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String s = list.getString(i);
                if (!s.isBlank()) scoreboardTags.add(s.trim());
            }
        } else if (nbt.contains("scoreboard_tags_csv")) {
            String csv = nbt.getString("scoreboard_tags_csv");
            if (!csv.isBlank()) scoreboardTags.addAll(parseCsv(csv));
        }
        minTagsRequired = nbt.contains("min_tags_required") ? nbt.getInt("min_tags_required") : 0;
        nbtFilterSnbt = nbt.getString("nbt_filter_snbt");
        parseNbtFilter();

        try { mode = Mode.valueOf(nbt.getString("mode")); } catch (Throwable ignored) { mode = Mode.TOTAL; }
        value = Math.max(1L, nbt.getLong("value"));

        heldItemFilter = nbt.contains("held_item") ? itemOrMissingFromNBT(nbt.get("held_item"), provider) : ItemStack.EMPTY;
        if (!heldItemFilter.isEmpty()) heldItemFilter.setCount(1);
        heldItemTagStr = nbt.getString("held_item_tag");
        resolveHeldItemTag();

        String s = nbt.getString("structure");
        if (!s.isEmpty()) setStructure(s); else structure = null;

        String d = nbt.getString("dimension");
        dimension = d.trim();
        String b = nbt.getString("biome");
        biome = b.trim();
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(entityTypeId.toString());
        buf.writeUtf(entityTypeTag == null ? "" : entityTypeTag.location().toString());
        buf.writeUtf(customName);
        buf.writeVarInt(scoreboardTags.size());
        for (String s : scoreboardTags) buf.writeUtf(s == null ? "" : s);
        buf.writeVarInt(minTagsRequired);
        buf.writeUtf(nbtFilterSnbt);

        buf.writeEnum(mode);
        buf.writeVarLong(value);

        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, heldItemFilter);
        buf.writeUtf(heldItemTagStr);

        buf.writeUtf(getStructure());

        buf.writeUtf(dimension);
        buf.writeUtf(biome);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        entityTypeId = ResourceLocation.tryParse(buf.readUtf());
        entityTypeTag = parseTypeTag(buf.readUtf());
        customName = buf.readUtf();
        scoreboardTags.clear();
        int nTags = buf.readVarInt();
        for (int i = 0; i < nTags; i++) {
            String s = buf.readUtf();
            if (!s.isBlank()) scoreboardTags.add(s.trim());
        }
        minTagsRequired = buf.readVarInt();
        nbtFilterSnbt = buf.readUtf();
        parseNbtFilter();

        mode = buf.readEnum(Mode.class);
        value = Math.max(1L, buf.readVarLong());

        heldItemFilter = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        if (!heldItemFilter.isEmpty()) heldItemFilter.setCount(1);
        heldItemTagStr = buf.readUtf();
        resolveHeldItemTag();

        String s = buf.readUtf();
        if (!s.isEmpty()) setStructure(s); else structure = null;

        dimension = buf.readUtf();
        biome = buf.readUtf();
    }

    private void parseNbtFilter() {
        if (nbtFilterSnbt == null || nbtFilterSnbt.isBlank()) { nbtFilterParsed = null; return; }
        try {
            nbtFilterParsed = TagParser.parseTag(nbtFilterSnbt);
        } catch (Exception ignored) { nbtFilterParsed = null; }
    }

    private void resolveHeldItemTag() {
        if (heldItemTagStr == null || heldItemTagStr.isBlank()) { heldItemTag = null; return; }
        String s = heldItemTagStr.startsWith("#") ? heldItemTagStr.substring(1) : heldItemTagStr;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        heldItemTag = (rl != null) ? TagKey.create(Registries.ITEM, rl) : null;
    }

    private static TagKey<EntityType<?>> parseTypeTag(String str) {
        if (str == null || str.isEmpty()) return null;
        String s = str.startsWith("#") ? str.substring(1) : str;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        return (rl != null && !rl.getPath().isEmpty()) ? TagKey.create(Registries.ENTITY_TYPE, rl) : null;
    }

    private String getTypeTagStr() {
        return entityTypeTag == null ? "" : entityTypeTag.location().toString();
    }

    private void setStructure(String resLoc) {
        if (resLoc == null || resLoc.isEmpty()) { structure = null; return; }
        structure = resLoc.startsWith("#")
                ? Either.right(TagKey.create(Registries.STRUCTURE, safeStructure(resLoc.substring(1))))
                : Either.left(ResourceKey.create(Registries.STRUCTURE, safeStructure(resLoc)));
    }

    private String getStructure() {
        if (structure == null) return "";
        return structure.map(k -> k.location().toString(), t -> "#" + t.location());
    }

    private ResourceLocation safeStructure(String s) {
        ResourceLocation rl = ResourceLocation.tryParse(s);
        return rl != null ? rl : DamageTask.DEFAULT_STRUCTURE;
    }

    private static void maybeRequestStructureSync() {
        if (KNOWN_STRUCTURES.isEmpty()) {
            NetworkManager.sendToServer(
                    new MQTStructuresRequest()
            );
        }
    }
    private static void maybeRequestWorldSync() {
        if (KNOWN_DIMENSIONS.isEmpty()) {
            NetworkManager.sendToServer(
                    new MQTWorldsRequest()
            );
        }
    }
    private static void maybeRequestBiomeSync() {
        if (KNOWN_BIOMES.isEmpty()) {
            NetworkManager.sendToServer(
                    new MQTBiomesRequest()
            );
        }
    }
    public static void syncKnownDimensionList(java.util.List<String> data) {
        KNOWN_DIMENSIONS.clear();
        KNOWN_DIMENSIONS.addAll(data);
    }
    public static void syncKnownBiomeList(java.util.List<String> data) {
        KNOWN_BIOMES.clear();
        KNOWN_BIOMES.addAll(data);
    }

    public static void syncKnownStructureList(java.util.List<String> data) {
        KNOWN_STRUCTURES.clear();
        KNOWN_STRUCTURES.addAll(data);
    }
}
