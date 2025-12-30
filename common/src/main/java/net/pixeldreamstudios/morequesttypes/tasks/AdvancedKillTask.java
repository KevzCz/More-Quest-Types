package net.pixeldreamstudios.morequesttypes.tasks;

import com.mojang.datafixers.util.Either;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.registries.RegistrarManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.pixeldreamstudios.morequesttypes.api.ITaskDungeonDifficultyExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskDynamicDifficultyExtension;
import net.pixeldreamstudios.morequesttypes.compat.DungeonDifficultyCompat;
import net.pixeldreamstudios.morequesttypes.compat.DynamicDifficultyCompat;
import net.pixeldreamstudios.morequesttypes.network.MQTBiomesRequest;
import net.pixeldreamstudios.morequesttypes.network.MQTStructuresRequest;
import net.pixeldreamstudios.morequesttypes.network.MQTWorldsRequest;
import net.pixeldreamstudios.morequesttypes.util.ComparisonManager;
import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancedKillTask extends KillTask {
    private static final ResourceLocation ZOMBIE = ResourceLocation.withDefaultNamespace("zombie");
    private static NameMap<ResourceLocation> ENTITY_NAME_MAP;
    private static NameMap<String> ENTITY_TAG_MAP;
    private static final Map<ResourceLocation, Icon> ENTITY_ICONS = new HashMap<>();
    private ResourceLocation entityTypeId = ZOMBIE;
    private TagKey<EntityType<?>> entityTypeTag = null;
    private long value = 1;
    private String customName = "";
    private final List<String> scoreboardTags = new ArrayList<>();
    private int minTagsRequired = 0;
    private String nbtFilterSnbt = "";
    private transient Tag nbtFilterParsed = null;
    private static final ResourceLocation DEFAULT_STRUCTURE = ResourceLocation.withDefaultNamespace("mineshaft");
    private static final List<String> KNOWN_STRUCTURES = new ArrayList<>();
    private Either<ResourceKey<Structure>, TagKey<Structure>> structure = null;
    private static final List<String> KNOWN_DIMENSIONS = new ArrayList<>();
    private String dimension = "";
    private static final List<String> KNOWN_BIOMES = new ArrayList<>();
    private String biome = "";
    public AdvancedKillTask(long id, Quest quest) {
        super(id, quest);
    }
    @Override
    public TaskType getType() {
        return MoreTasksTypes.KILL_ADVANCED;
    }
    @Override
    public long getMaxProgress() {
        return value;
    }
    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("entity", entityTypeId.toString());
        if (entityTypeTag != null) nbt.putString("entityTypeTag", entityTypeTag.location().toString());
        nbt.putLong("value", value);
        if (!customName.isEmpty()) nbt.putString("custom_name", customName);
        if (!scoreboardTags.isEmpty()) {
            ListTag tagList = new ListTag();
            for (String s : scoreboardTags) tagList.add(StringTag.valueOf(s));
            nbt.put("scoreboard_tags", tagList);
        }
        nbt.putInt("min_tags_required", minTagsRequired);
        if (!nbtFilterSnbt.isEmpty()) nbt.putString("nbt_filter_snbt", nbtFilterSnbt);
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
        value = nbt.getLong("value");
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
        buf.writeVarLong(value);
        buf.writeUtf(customName);
        buf.writeVarInt(scoreboardTags.size());
        for (String s : scoreboardTags) buf.writeUtf(s == null ? "" : s);
        buf.writeVarInt(minTagsRequired);
        buf.writeUtf(nbtFilterSnbt);
        buf.writeUtf(getStructure());
        buf.writeUtf(dimension);
        buf.writeUtf(biome);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        entityTypeId = ResourceLocation.tryParse(buf.readUtf());
        entityTypeTag = parseTypeTag(buf.readUtf());
        value = buf.readVarLong();
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

    private static @Nullable TagKey<EntityType<?>> parseTypeTag(String tag) {
        if (tag == null || tag.isEmpty()) return null;
        if (tag.startsWith("#")) tag = tag.substring(1);
        var rl = ResourceLocation.tryParse(tag);
        return (rl != null && !rl.getPath().isEmpty()) ? TagKey.create(Registries.ENTITY_TYPE, rl) : null;
    }

    public static void syncKnownStructureList(List<String> data) {
        KNOWN_STRUCTURES.clear();
        KNOWN_STRUCTURES.addAll(data);
    }

    private void setStructure(String resLoc) {
        if (resLoc == null || resLoc.isEmpty()) { this.structure = null; return; }
        this.structure = resLoc.startsWith("#")
                ? Either.right(TagKey.create(Registries.STRUCTURE, safeStructure(resLoc.substring(1))))
                : Either.left(ResourceKey.create(Registries.STRUCTURE, safeStructure(resLoc)));
    }

    private String getStructure() {
        if (this.structure == null) return "";
        return this.structure.map(
                key -> key.location().toString(),
                tag -> "#" + tag.location()
        );
    }

    private ResourceLocation safeStructure(String str) {
        ResourceLocation rl = ResourceLocation.tryParse(str);
        return rl != null ? rl : AdvancedKillTask.DEFAULT_STRUCTURE;
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

    public static void syncKnownDimensionList(List<String> data) {
        KNOWN_DIMENSIONS.clear();
        KNOWN_DIMENSIONS.addAll(data);
    }

    public static void syncKnownBiomeList(List<String> data) {
        KNOWN_BIOMES.clear();
        KNOWN_BIOMES.addAll(data);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        if (ENTITY_NAME_MAP == null) {
            var ids = new ArrayList<ResourceLocation>();
            BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
                if (type.create(FTBQuestsClient.getClientLevel()) instanceof LivingEntity) {
                    ids.add(type.arch$registryName());
                }
            });
            ids.sort((a, b) -> {
                var c1 = Component.translatable("entity." + a.toLanguageKey());
                var c2 = Component.translatable("entity." + b.toLanguageKey());
                return c1.getString().compareTo(c2.getString());
            });
            ENTITY_NAME_MAP = NameMap.of(ZOMBIE, ids)
                    .nameKey(id -> "entity." + id.toLanguageKey())
                    .icon(AdvancedKillTask::iconForEntityType)
                    .create();
        }

        if (ENTITY_TAG_MAP == null) {
            var list = new ArrayList<>(List.of(""));
            list.addAll(BuiltInRegistries.ENTITY_TYPE.getTags()
                    .map(p -> p.getFirst().location().toString())
                    .sorted()
                    .toList());
            ENTITY_TAG_MAP = NameMap.of("minecraft:zombies", list).create();
        }

        config.addEnum("entity", entityTypeId, v -> entityTypeId = v, ENTITY_NAME_MAP, ZOMBIE);
        config.addEnum("entity_type_tag", getTypeTagStr(), v -> entityTypeTag = parseTypeTag(v), ENTITY_TAG_MAP);
        config.addLong("value", value, v -> value = v, 1L, 1L, Long.MAX_VALUE);
        config.addString("custom_name", customName, v -> customName = v, "");
        config.addList("scoreboard_tags", scoreboardTags, new StringConfig(), "")
                .setNameKey("morequesttypes.task.tags_csv");
        config.addInt("min_tags_required", minTagsRequired, v -> minTagsRequired = Math.max(0, v), 0, 0, 64)
                .setNameKey("morequesttypes.task.min_tags");
        config.addString("nbt_filter_snbt", nbtFilterSnbt, v -> { nbtFilterSnbt = v; parseNbtFilter(); }, "")
                .setNameKey("morequesttypes.task.nbt");

        maybeRequestStructureSync();
        List<String> structureChoices = new ArrayList<>();
        structureChoices.add("");
        if (KNOWN_STRUCTURES.isEmpty()) structureChoices.add(DEFAULT_STRUCTURE.toString()); else structureChoices.addAll(KNOWN_STRUCTURES);
        var STRUCTURE_MAP = NameMap.of(getStructure(), structureChoices)
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

    private String getTypeTagStr() {
        return entityTypeTag == null ? "" : entityTypeTag.location().toString();
    }

    public static Icon iconForEntityType(ResourceLocation typeId) {
        return ENTITY_ICONS.computeIfAbsent(typeId, k -> {
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(typeId);
            if (entityType.equals(EntityType.PLAYER)) return Icons.PLAYER;
            Item item = SpawnEggItem.byId(entityType);
            if (item == null) {
                Entity e = entityType.create(FTBQuestsClient.getClientLevel());
                if (e != null) {
                    ItemStack stack = e.getPickResult();
                    if (stack != null) item = stack.getItem();
                }
            }
            return ItemIcon.getItemIcon(item != null ? item : Items.SPAWNER);
        });
    }

    @Override
    public void clearCachedData() {
        super.clearCachedData();
        ENTITY_NAME_MAP = null;
        ENTITY_TAG_MAP = null;
        ENTITY_ICONS.clear();
    }

    @Override
    public void kill(TeamData teamData, LivingEntity e) {
        if (!teamData.isCompleted(this) && match(e)) {
            teamData.addProgress(this, 1L);
        }
    }

    private boolean match(LivingEntity e) {
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
            if (!(nbtFilterParsed instanceof CompoundTag filter) || !nbtSubsetMatches(actual, filter)) return false;
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
                                if (mgr.getStructureWithPieceAt(pos, structure).isValid()) return true;
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
    private boolean nameMatchOK(LivingEntity e) {
        if (!customName.isEmpty()) {
            if (e instanceof Player p) return p.getGameProfile().getName().equals(customName);
            return e.getName().getString().equals(customName);
        }
        return true;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        MutableComponent name = (entityTypeTag == null)
                ? Component.translatable("entity." + entityTypeId.toLanguageKey())
                : Component.literal("#" + getTypeTagStr());

        MutableComponent baseTitle;
        if (! customName.isEmpty()) {
            baseTitle = Component.translatable("ftbquests.task.ftbquests.kill.title_named", formatMaxProgress(), name, Component.literal(customName));
        } else {
            baseTitle = Component.translatable("ftbquests.task.ftbquests.kill.title", formatMaxProgress(), name);
        }

        if (DynamicDifficultyCompat.isLoaded()) {
            ITaskDynamicDifficultyExtension ext = (ITaskDynamicDifficultyExtension)(Object) this;
            if (ext.shouldCheckDynamicDifficultyLevel()) {
                String levelReq = mqt$formatLevelRequirement(
                        ext.getDynamicDifficultyComparison(),
                        ext.getDynamicDifficultyFirst(),
                        ext.getDynamicDifficultySecond()
                );
                baseTitle = Component.translatable("morequesttypes.task.kill_advanced.title_with_dynamic_difficulty",
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
                baseTitle = Component.translatable("morequesttypes.task.kill_advanced.title_with_dungeon_difficulty",
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
        if (entityTypeTag == null) return iconForEntityType(entityTypeId);
        List<Icon> icons = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.getTag(entityTypeTag).ifPresent(set -> set.forEach(holder ->
                holder.unwrapKey().map(k -> icons.add(iconForEntityType(k.location())))));
        return icons.isEmpty() ? Icons.BARRIER : IconAnimation.fromList(icons, false);
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
}
