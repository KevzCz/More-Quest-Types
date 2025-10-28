package net.pixeldreamstudios.morequesttypes.tasks;

import com.mojang.datafixers.util.Either;
import dev.architectury.registry.registries.RegistrarManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
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
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class FindEntityTask extends dev.ftb.mods.ftbquests.quest.task.Task {
    private static final ResourceLocation DEFAULT_STRUCTURE = ResourceLocation.withDefaultNamespace("mineshaft");
    private static final ResourceLocation ZOMBIE = ResourceLocation.withDefaultNamespace("zombie");

    private static NameMap<ResourceLocation> ENTITY_NAME_MAP;
    private static NameMap<String> ENTITY_TAG_MAP;

    private ResourceLocation entityTypeId = ZOMBIE;
    private TagKey<EntityType<?>> entityTypeTag = null;
    private String customName = "";
    private String scoreboardTagsCsv = "";
    private int minTagsRequired = 0;
    private String nbtFilterSnbt = "";
    private transient Tag nbtFilterParsed = null;
    private int targetRadiusBlocks = 5;
    private int searchRadiusBlocks = 64;

    private static final Map<ResourceLocation, dev.ftb.mods.ftblibrary.icon.Icon> ENTITY_ICONS = new HashMap<>();

    private static final List<String> KNOWN_STRUCTURES = new ArrayList<>();
    private Either<ResourceKey<Structure>, TagKey<Structure>> structure = null;
    private static final List<String> KNOWN_DIMENSIONS = new ArrayList<>();
    private String dimension = "";
    private static final List<String> KNOWN_BIOMES = new ArrayList<>();
    private String biome = "";
    @Environment(EnvType.CLIENT)
    private static final java.util.Map<Long, Double> CLIENT_NEAREST = new java.util.HashMap<>();
    @Environment(EnvType.CLIENT)
    private static long CLIENT_LAST_REQ_TICK = 0L;

    @Environment(EnvType.CLIENT)
    public static void updateClientNearest(long taskId, double meters, long gameTime) {
        CLIENT_NEAREST.put(taskId, meters);
        CLIENT_LAST_REQ_TICK = gameTime;

        try {
            ClientQuestFile.INSTANCE.refreshGui();
        } catch (Throwable ignored) {}
    }


    public FindEntityTask(long id, Quest quest) { super(id, quest); }

    @Override public TaskType getType() { return MoreTasksTypes.FIND_ENTITY; }
    @Override public long getMaxProgress() { return 1L; }
    @Override public boolean hideProgressNumbers() { return true; }
    @Override public int autoSubmitOnPlayerTick() { return 10; }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, net.minecraft.world.item.ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        double nearest = nearestDistanceServer(player);
        if (nearest <= targetRadiusBlocks) {
            teamData.setProgress(this, 1L);
        } else {
        }
    }

    public double nearestDistanceServer(ServerPlayer sp) {
        var level = sp.serverLevel();
        var aabb = sp.getBoundingBox().inflate(searchRadiusBlocks);
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, aabb, this::entityMatches);
        if (list.isEmpty()) return Double.POSITIVE_INFINITY;

        double best = Double.POSITIVE_INFINITY;
        for (Entity e : list) {
            double d = Math.sqrt(e.distanceToSqr(sp));
            if (d < best) best = d;
        }
        return best;
    }
    private boolean entityMatches(LivingEntity e) {
        boolean baseOk = (entityTypeTag == null)
                ? entityTypeId.equals(RegistrarManager.getId(e.getType(), Registries.ENTITY_TYPE)) && nameMatchOK(e)
                : e.getType().is(entityTypeTag) && nameMatchOK(e);
        if (!baseOk) return false;

        if (!scoreboardTagsCsv.isBlank()) {
            var required = parseCsv(scoreboardTagsCsv);
            var present = e.getTags();
            int count = 0;
            for (var r : required) if (present.contains(r)) count++;
            int need = (minTagsRequired <= 0) ? required.size() : minTagsRequired;
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

        return true;
    }

    private boolean nameMatchOK(LivingEntity e) {
        if (!customName.isEmpty()) {
            if (e instanceof net.minecraft.world.entity.player.Player p) return p.getGameProfile().getName().equals(customName);
            return e.getName().getString().equals(customName);
        }
        return true;
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
            List<Tag> remaining = new ArrayList<>();
            for (int i = 0; i < aList.size(); i++) remaining.add(aList.get(i));
            outer:
            for (int i = 0; i < fList.size(); i++) {
                Tag fEl = fList.get(i);
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

    private boolean insideLocationFilters(ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (structure != null && !isInsideStructureOrTag(level, pos)) return false;
        if (!isInsideDimension(level)) return false;
        if (!isInsideBiome(level, pos)) return false;
        return true;
    }

    private boolean isInsideStructureOrTag(ServerLevel level, net.minecraft.core.BlockPos pos) {
        StructureManager mgr = level.structureManager();
        return structure == null ? true : structure.map(
                key -> mgr.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(key)
                        .map(h -> mgr.getStructureWithPieceAt(pos, h.value()).isValid()).orElse(false),
                tag -> mgr.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(tag)
                        .map(hs -> {
                            for (var h : hs) if (mgr.getStructureWithPieceAt(pos, h.value()).isValid()) return true;
                            return false;
                        }).orElse(false)
        );
    }
    private boolean isInsideDimension(ServerLevel level) {
        if (dimension == null || dimension.isEmpty()) return true;
        return dimension.equals(level.dimension().location().toString());
    }
    private boolean isInsideBiome(ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (biome == null || biome.isEmpty()) return true;
        Holder<Biome> h = level.getBiome(pos);
        if (biome.startsWith("#")) {
            ResourceLocation rl = ResourceLocation.tryParse(biome.substring(1));
            if (rl == null) return false;
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, rl);
            return h.is(tag);
        } else {
            return h.unwrapKey().map(k -> k.location().toString().equals(biome)).orElse(false);
        }
    }

    private static @Nullable TagKey<EntityType<?>> parseTypeTag(String tag) {
        if (tag == null || tag.isEmpty()) return null;
        String s = tag.startsWith("#") ? tag.substring(1) : tag;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        return (rl != null && !rl.getPath().isEmpty()) ? TagKey.create(Registries.ENTITY_TYPE, rl) : null;
    }
    private String getTypeTagStr() { return entityTypeTag == null ? "" : entityTypeTag.location().toString(); }

    public static void syncKnownStructureList(List<String> data) { KNOWN_STRUCTURES.clear(); KNOWN_STRUCTURES.addAll(data); }
    public static void syncKnownDimensionList(List<String> data) { KNOWN_DIMENSIONS.clear(); KNOWN_DIMENSIONS.addAll(data); }
    public static void syncKnownBiomeList(List<String> data)     { KNOWN_BIOMES.clear();     KNOWN_BIOMES.addAll(data); }

    private void setStructure(String resLoc) {
        if (resLoc == null || resLoc.isEmpty()) { structure = null; return; }
        structure = resLoc.startsWith("#")
                ? Either.right(TagKey.create(Registries.STRUCTURE, safeStructure(resLoc.substring(1), DEFAULT_STRUCTURE)))
                : Either.left(ResourceKey.create(Registries.STRUCTURE, safeStructure(resLoc, DEFAULT_STRUCTURE)));
    }
    private String getStructure() {
        if (structure == null) return "";
        return structure.map(k -> k.location().toString(), t -> "#" + String.valueOf(t.location()));
    }
    private ResourceLocation safeStructure(String s, ResourceLocation fallback) {
        ResourceLocation rl = ResourceLocation.tryParse(s);
        return rl != null ? rl : fallback;
    }
    private static void maybeRequestStructureSync() {
        if (KNOWN_STRUCTURES.isEmpty()) {
            dev.architectury.networking.NetworkManager.sendToServer(new net.pixeldreamstudios.morequesttypes.network.MQTStructuresRequest());
        }
    }
    private static void maybeRequestWorldSync() {
        if (KNOWN_DIMENSIONS.isEmpty()) {
            dev.architectury.networking.NetworkManager.sendToServer(new net.pixeldreamstudios.morequesttypes.network.MQTWorldsRequest());
        }
    }
    private static void maybeRequestBiomeSync() {
        if (KNOWN_BIOMES.isEmpty()) {
            dev.architectury.networking.NetworkManager.sendToServer(new net.pixeldreamstudios.morequesttypes.network.MQTBiomesRequest());
        }
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
            var list = new ArrayList<String>(List.of(""));
            list.addAll(BuiltInRegistries.ENTITY_TYPE.getTags()
                    .map(p -> p.getFirst().location().toString())
                    .sorted()
                    .toList());
            ENTITY_TAG_MAP = NameMap.of("", list).create();
        }

        config.addEnum("entity", entityTypeId, v -> entityTypeId = v, ENTITY_NAME_MAP, ZOMBIE);
        config.addEnum("entity_type_tag", getTypeTagStr(), v -> entityTypeTag = parseTypeTag(v), ENTITY_TAG_MAP);
        config.addString("custom_name", customName, v -> customName = v, "");
        config.addString("scoreboard_tags_csv", scoreboardTagsCsv, v -> scoreboardTagsCsv = v.trim(), "")
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.tags_csv");
        config.addInt("min_tags_required", minTagsRequired, v -> minTagsRequired = Math.max(0, v), 0, 0, 64)
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.min_tags");
        config.addString("nbt_filter_snbt", nbtFilterSnbt, v -> { nbtFilterSnbt = v; parseNbtFilter(); }, "")
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.nbt");

        config.addInt("target_radius_blocks", targetRadiusBlocks, v -> targetRadiusBlocks = Math.max(0, v), 5, 0, 256)
                .setNameKey("ftbquests.task.find_entity.target");
        config.addInt("search_radius_blocks", searchRadiusBlocks, v -> searchRadiusBlocks = Math.max(1, v), 64, 1, 512)
                .setNameKey("ftbquests.task.find_entity.radius");

        maybeRequestStructureSync();
        List<String> structureChoices = new ArrayList<>();
        structureChoices.add("");
        if (KNOWN_STRUCTURES.isEmpty()) structureChoices.add(DEFAULT_STRUCTURE.toString()); else structureChoices.addAll(KNOWN_STRUCTURES);
        var STRUCTURE_MAP = NameMap.of(getStructure(), structureChoices)
                .name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "None" : s))
                .create();
        config.addEnum("structure", getStructure(), this::setStructure, STRUCTURE_MAP)
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.structure");

        maybeRequestWorldSync();
        List<String> dimChoices = new ArrayList<>();
        dimChoices.add("");
        if (KNOWN_DIMENSIONS.isEmpty()) {
            dimChoices.add("minecraft:overworld");
            dimChoices.add("minecraft:the_nether");
            dimChoices.add("minecraft:the_end");
        } else dimChoices.addAll(KNOWN_DIMENSIONS);
        var DIMENSION_MAP = NameMap.of(dimension, dimChoices)
                .name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "Any" : s))
                .create();
        config.addEnum("dimension", dimension, v -> dimension = v, DIMENSION_MAP)
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.dimension");

        maybeRequestBiomeSync();
        List<String> biomeChoices = new ArrayList<>();
        biomeChoices.add("");
        if (KNOWN_BIOMES.isEmpty()) {
            biomeChoices.add("minecraft:plains");
            biomeChoices.add("minecraft:forest");
            biomeChoices.add("minecraft:desert");
        } else biomeChoices.addAll(KNOWN_BIOMES);
        var BIOME_MAP = NameMap.of(biome, biomeChoices)
                .name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "Any" : s))
                .create();
        config.addEnum("biome", biome, v -> biome = v, BIOME_MAP)
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.biome");
    }

    private void parseNbtFilter() {
        if (nbtFilterSnbt == null || nbtFilterSnbt.isBlank()) { nbtFilterParsed = null; return; }
        try {
            Tag parsed = TagParser.parseTag(nbtFilterSnbt);
            nbtFilterParsed = (parsed instanceof CompoundTag) ? parsed : null;
        } catch (Exception ignored) { nbtFilterParsed = null; }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        MutableComponent entityName = (entityTypeTag == null)
                ? Component.translatable("entity." + entityTypeId.toLanguageKey())
                : Component.literal("#" + getTypeTagStr());

        return Component.translatable("ftbquests.task.find_entity.title", entityName);
    }

    @Environment(EnvType.CLIENT)
    private double lastNearestClientMeters() {
        try {
            var client = FTBQuestsClient.getClientPlayer();
            var level  = net.minecraft.client.Minecraft.getInstance().level;
            if (client == null || level == null) return Double.POSITIVE_INFINITY;

            boolean serverOnlyFilters =
                    (structure != null) || (dimension != null && !dimension.isEmpty()) || (biome != null && !biome.isEmpty());

            if (!serverOnlyFilters) {
                var aabb = client.getBoundingBox().inflate(searchRadiusBlocks);
                List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, aabb, this::entityMatches);
                if (list.isEmpty()) return Double.POSITIVE_INFINITY;

                double best = Double.POSITIVE_INFINITY;
                for (Entity e : list) {
                    double d = Math.sqrt(e.distanceToSqr(client));
                    if (d < best) best = d;
                }
                return best;
            }

            long now = level.getGameTime();
            if (now - CLIENT_LAST_REQ_TICK > 20) {
                CLIENT_LAST_REQ_TICK = now;
                dev.architectury.networking.NetworkManager.sendToServer(
                        new net.pixeldreamstudios.morequesttypes.network.MQTNearestEntityRequest(this.getId())
                );
            }
            return CLIENT_NEAREST.getOrDefault(this.getId(), Double.POSITIVE_INFINITY);
        } catch (Throwable t) {
            return Double.POSITIVE_INFINITY;
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public dev.ftb.mods.ftblibrary.icon.Icon getAltIcon() {
        if (entityTypeTag == null) return AdvancedKillTask.iconForEntityType(entityTypeId);
        List<dev.ftb.mods.ftblibrary.icon.Icon> icons = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.getTag(entityTypeTag).ifPresent(set -> set.forEach(holder ->
                holder.unwrapKey().map(k -> icons.add(AdvancedKillTask.iconForEntityType(k.location())))));
        return icons.isEmpty() ? dev.ftb.mods.ftblibrary.icon.Icons.BARRIER : dev.ftb.mods.ftblibrary.icon.IconAnimation.fromList(icons, false);
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("entity", entityTypeId.toString());
        if (entityTypeTag != null) nbt.putString("entityTypeTag", entityTypeTag.location().toString());
        if (!customName.isEmpty()) nbt.putString("custom_name", customName);
        if (!scoreboardTagsCsv.isEmpty()) nbt.putString("scoreboard_tags_csv", scoreboardTagsCsv);
        if (minTagsRequired > 0) nbt.putInt("min_tags_required", minTagsRequired);
        if (!nbtFilterSnbt.isEmpty()) nbt.putString("nbt_filter_snbt", nbtFilterSnbt);

        nbt.putInt("target_radius_blocks", targetRadiusBlocks);
        nbt.putInt("search_radius_blocks", searchRadiusBlocks);

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
        scoreboardTagsCsv = nbt.getString("scoreboard_tags_csv");
        minTagsRequired = nbt.contains("min_tags_required") ? nbt.getInt("min_tags_required") : 0;
        nbtFilterSnbt = nbt.getString("nbt_filter_snbt");
        parseNbtFilter();

        targetRadiusBlocks = nbt.contains("target_radius_blocks") ? Math.max(0, nbt.getInt("target_radius_blocks")) : 5;
        searchRadiusBlocks = nbt.contains("search_radius_blocks") ? Math.max(1, nbt.getInt("search_radius_blocks")) : 64;

        String s = nbt.getString("structure");
        if (s != null && !s.isEmpty()) setStructure(s); else structure = null;
        String d = nbt.getString("dimension");
        dimension = (d == null) ? "" : d.trim();
        String b = nbt.getString("biome");
        biome = (b == null) ? "" : b.trim();
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(entityTypeId.toString());
        buf.writeUtf(entityTypeTag == null ? "" : entityTypeTag.location().toString());
        buf.writeUtf(customName);
        buf.writeUtf(scoreboardTagsCsv);
        buf.writeVarInt(minTagsRequired);
        buf.writeUtf(nbtFilterSnbt);
        buf.writeVarInt(targetRadiusBlocks);
        buf.writeVarInt(searchRadiusBlocks);
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
        scoreboardTagsCsv = buf.readUtf();
        minTagsRequired = buf.readVarInt();
        nbtFilterSnbt = buf.readUtf();
        targetRadiusBlocks = buf.readVarInt();
        searchRadiusBlocks = buf.readVarInt();
        String s = buf.readUtf();
        if (s != null && !s.isEmpty()) setStructure(s); else structure = null;
        dimension = buf.readUtf();
        biome = buf.readUtf();
    }
    @Environment(EnvType.CLIENT)
    public static String uiDistanceSuffix(FindEntityTask t) {
        double d = t.lastNearestClientMeters();
        return (d == Double.POSITIVE_INFINITY) ? "?" :
                String.format(java.util.Locale.ROOT, "%.1fm", d);
    }
}
