package net.pixeldreamstudios.morequesttypes.tasks;

import com.mojang.datafixers.util.Either;
import dev.architectury.registry.registries.RegistrarManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.client.ConfigIconItemStack;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.biome.Biome;
import net.pixeldreamstudios.morequesttypes.interact.InteractEventBuffer;
import net.pixeldreamstudios.morequesttypes.interact.InteractEventBuffer.Interaction;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InteractEntityTask extends dev.ftb.mods.ftbquests.quest.task.Task {

    public enum HandMode { ANY, MAIN_HAND, OFF_HAND }

    private static final ResourceLocation ZOMBIE = ResourceLocation.withDefaultNamespace("zombie");

    private static NameMap<ResourceLocation> ENTITY_NAME_MAP;
    private static NameMap<String> ENTITY_TAG_MAP;
    private static NameMap<String> ITEM_TAG_MAP;
    private static final Map<ResourceLocation, Icon> ENTITY_ICONS = new HashMap<>();

    private ResourceLocation entityTypeId = ZOMBIE;
    private TagKey<EntityType<?>> entityTypeTag = null;
    private String customName = "";
    private String scoreboardTagsCsv = "";
    private int minTagsRequired = 0;
    private String nbtFilterSnbt = "";
    private transient Tag nbtFilterParsed = null;
    private long value = 1;
    private HandMode handMode = HandMode.ANY;
    private ItemStack heldItemFilter = ItemStack.EMPTY;
    private String heldItemTagStr = "";
    private transient TagKey<Item> heldItemTag;
    private transient long lastProcessedTick = Long.MIN_VALUE;
    private static final ResourceLocation DEFAULT_STRUCTURE = ResourceLocation.withDefaultNamespace("mineshaft");
    private static final List<String> KNOWN_STRUCTURES = new ArrayList<>();
    private Either<ResourceKey<Structure>, TagKey<Structure>> structure = null;
    private static final List<String> KNOWN_DIMENSIONS = new ArrayList<>();
    private String dimension = "";
    private static final List<String> KNOWN_BIOMES = new ArrayList<>();
    private String biome = "";

    public InteractEntityTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.INTERACT_ENTITY;
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
        if (!customName.isEmpty()) nbt.putString("custom_name", customName);
        if (!scoreboardTagsCsv.isEmpty()) nbt.putString("scoreboard_tags_csv", scoreboardTagsCsv);
        nbt.putInt("min_tags_required", minTagsRequired);
        if (!nbtFilterSnbt.isEmpty()) nbt.putString("nbt_filter_snbt", nbtFilterSnbt);
        nbt.putLong("value", value);
        nbt.putString("hand_mode", handMode.name());
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
        entityTypeTag = parseEntityTypeTag(nbt.getString("entityTypeTag"));
        customName = nbt.getString("custom_name");
        scoreboardTagsCsv = nbt.getString("scoreboard_tags_csv");
        minTagsRequired = nbt.contains("min_tags_required") ? nbt.getInt("min_tags_required") : 0;
        nbtFilterSnbt = nbt.getString("nbt_filter_snbt");
        parseNbtFilter();
        value = Math.max(1L, nbt.getLong("value"));
        try {
            handMode = HandMode.valueOf(nbt.getString("hand_mode"));
        } catch (Throwable ignored) {
            handMode = HandMode.ANY;
        }
        heldItemFilter = nbt.contains("held_item") ? itemOrMissingFromNBT(nbt.get("held_item"), provider) : ItemStack.EMPTY;
        if (!heldItemFilter.isEmpty()) heldItemFilter.setCount(1);
        heldItemTagStr = nbt.getString("held_item_tag");
        resolveHeldItemTag();
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
        buf.writeVarLong(value);
        buf.writeEnum(handMode);
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
        entityTypeTag = parseEntityTypeTag(buf.readUtf());
        customName = buf.readUtf();
        scoreboardTagsCsv = buf.readUtf();
        minTagsRequired = buf.readVarInt();
        nbtFilterSnbt = buf.readUtf();
        parseNbtFilter();
        value = buf.readVarLong();
        handMode = buf.readEnum(HandMode.class);
        heldItemFilter = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        if (!heldItemFilter.isEmpty()) heldItemFilter.setCount(1);
        heldItemTagStr = buf.readUtf();
        resolveHeldItemTag();
        String s = buf.readUtf();
        if (s != null && !s.isEmpty()) setStructure(s); else structure = null;
        dimension = buf.readUtf();
        biome = buf.readUtf();
    }

    private void parseNbtFilter() {
        if (nbtFilterSnbt == null || nbtFilterSnbt.isBlank()) {
            nbtFilterParsed = null;
            return;
        }
        try {
            Tag parsed = TagParser.parseTag(nbtFilterSnbt);
            nbtFilterParsed = (parsed instanceof CompoundTag) ? parsed : null;
        } catch (Exception ignored) {
            nbtFilterParsed = null;
        }
    }

    private void resolveHeldItemTag() {
        if (heldItemTagStr == null || heldItemTagStr.isBlank()) { heldItemTag = null; return; }
        String s = heldItemTagStr.startsWith("#") ? heldItemTagStr.substring(1) : heldItemTagStr;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        heldItemTag = (rl != null) ? TagKey.create(Registries.ITEM, rl) : null;
    }

    private static @Nullable TagKey<EntityType<?>> parseEntityTypeTag(String tag) {
        if (tag == null || tag.isEmpty()) return null;
        String s = tag.startsWith("#") ? tag.substring(1) : tag;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        return (rl != null && !rl.getPath().isEmpty()) ? TagKey.create(Registries.ENTITY_TYPE, rl) : null;
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
                    .icon(InteractEntityTask::iconForEntityType)
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
        if (ITEM_TAG_MAP == null) {
            var list = new ArrayList<String>(List.of(""));
            list.addAll(BuiltInRegistries.ITEM.getTags()
                    .map(p -> p.getFirst().location().toString())
                    .sorted()
                    .toList());
            ITEM_TAG_MAP = NameMap.of("", list).create();
        }
        config.addEnum("entity", entityTypeId, v -> entityTypeId = v, ENTITY_NAME_MAP, ZOMBIE);
        config.addEnum("entity_type_tag", getEntityTypeTagStr(), v -> entityTypeTag = parseEntityTypeTag(v), ENTITY_TAG_MAP);
        config.addString("custom_name", customName, v -> customName = v, "");
        config.addString("scoreboard_tags_csv", scoreboardTagsCsv, v -> scoreboardTagsCsv = v.trim(), "")
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.tags_csv");
        config.addInt("min_tags_required", minTagsRequired, v -> minTagsRequired = Math.max(0, v), 0, 0, 64)
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.min_tags");
        config.addString("nbt_filter_snbt", nbtFilterSnbt, v -> { nbtFilterSnbt = v; parseNbtFilter(); }, "")
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.nbt");
        config.addLong("value", value, v -> value = Math.max(1L, v), 1L, 1L, Long.MAX_VALUE);
        var HANDS = NameMap.of(HandMode.ANY, HandMode.values()).create();
        config.addEnum("hand_mode", handMode, v -> handMode = v, HANDS)
                .setNameKey("ftbquests.task.interact_entity.hand");
        ((ConfigIconItemStack) config.add(
                "held_item",
                new ConfigIconItemStack(),
                heldItemFilter,
                v -> { heldItemFilter = v.copy(); if (!heldItemFilter.isEmpty()) heldItemFilter.setCount(1); },
                ItemStack.EMPTY
        )).setNameKey("ftbquests.task.interact_entity.held_item");
        config.addEnum("held_item_tag", heldItemTagStr, v -> {
            heldItemTagStr = v;
            resolveHeldItemTag();
        }, ITEM_TAG_MAP).setNameKey("ftbquests.task.interact_entity.held_item_tag");

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
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.structure");

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
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.dimension");

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
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.biome");
    }

    private String getEntityTypeTagStr() {
        return entityTypeTag == null ? "" : entityTypeTag.location().toString();
    }

    private static Icon iconForEntityType(ResourceLocation typeId) {
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
        ITEM_TAG_MAP = null;
        ENTITY_ICONS.clear();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        MutableComponent entityName = (entityTypeTag == null)
                ? Component.translatable("entity." + entityTypeId.toLanguageKey())
                : Component.literal("#" + getEntityTypeTagStr());
        String max = formatMaxProgress();
        Component itemDesc;
        if (heldItemTag != null) {
            itemDesc = Component.literal("#" + heldItemTag.location());
        } else if (heldItemFilter.isEmpty()) {
            itemDesc = Component.translatable("ftbquests.task.interact_entity.empty_hand");
        } else {
            itemDesc = heldItemFilter.getHoverName();
        }
        if (handMode == HandMode.ANY) {
            return Component.translatable("ftbquests.task.interact_entity.title", max, entityName)
                    .append(Component.literal(" "))
                    .append(Component.translatable("ftbquests.task.interact_entity.with_item", itemDesc));
        } else {
            String handKey = (handMode == HandMode.MAIN_HAND)
                    ? "ftbquests.task.interact_entity.main_hand"
                    : "ftbquests.task.interact_entity.off_hand";
            return Component.translatable("ftbquests.task.interact_entity.title_with_hand",
                            max, entityName, Component.translatable(handKey))
                    .append(Component.literal(" "))
                    .append(Component.translatable("ftbquests.task.interact_entity.with_item", itemDesc));
        }
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

    @Override
    public int autoSubmitOnPlayerTick() {
        return 1;
    }

    @Override
    public void submitTask(TeamData teamData, net.minecraft.server.level.ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;
        long current = teamData.getProgress(this);
        if (current >= getMaxProgress()) return;
        List<Interaction> events = InteractEventBuffer.snapshotLatest(player.getUUID());
        if (events.isEmpty()) return;
        long tick = events.get(events.size() - 1).gameTime();
        if (tick == lastProcessedTick) return;
        int increments = (handMode == HandMode.ANY)
                ? countAnyHand(events)
                : countExactHand(events);
        if (increments > 0) {
            long next = current + increments;
            teamData.setProgress(this, Math.min(next, getMaxProgress()));
        }
        lastProcessedTick = tick;
    }

    private int countExactHand(List<Interaction> events) {
        int inc = 0;
        for (Interaction ev : events) {
            if (handMode == HandMode.MAIN_HAND && ev.hand() != InteractionHand.MAIN_HAND) continue;
            if (handMode == HandMode.OFF_HAND  && ev.hand() != InteractionHand.OFF_HAND)  continue;
            if (matches(ev)) inc++;
        }
        return inc;
    }

    private int countAnyHand(List<Interaction> events) {
        int inc = 0;
        Set<Integer> counted = new HashSet<>();
        for (Interaction ev : events) {
            Entity ent = ev.entity();
            int id = ent.getId();
            if (counted.contains(id)) continue;
            if (!(ent instanceof LivingEntity)) continue;
            if (!entityMatches((LivingEntity) ent)) continue;
            if (!heldItemMatches(ev.stack())) continue;
            counted.add(id);
            inc++;
        }
        return inc;
    }

    private static void maybeRequestStructureSync() {
        if (KNOWN_STRUCTURES.isEmpty()) {
            dev.architectury.networking.NetworkManager.sendToServer(
                    new net.pixeldreamstudios.morequesttypes.network.MQTStructuresRequest()
            );
        }
    }

    private static void maybeRequestWorldSync() {
        if (KNOWN_DIMENSIONS.isEmpty()) {
            dev.architectury.networking.NetworkManager.sendToServer(
                    new net.pixeldreamstudios.morequesttypes.network.MQTWorldsRequest()
            );
        }
    }
    private static void maybeRequestBiomeSync() {
        if (KNOWN_BIOMES.isEmpty()) {
            dev.architectury.networking.NetworkManager.sendToServer(
                    new net.pixeldreamstudios.morequesttypes.network.MQTBiomesRequest()
            );
        }
    }

    private boolean matches(Interaction ev) {
        if (handMode == HandMode.MAIN_HAND && ev.hand() != InteractionHand.MAIN_HAND) return false;
        if (handMode == HandMode.OFF_HAND  && ev.hand() != InteractionHand.OFF_HAND)  return false;
        Entity ent = ev.entity();
        if (!(ent instanceof LivingEntity le)) return false;
        if (!entityMatches(le)) return false;
        return heldItemMatches(ev.stack());
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
            if (e instanceof Player p) {
                if (!p.getGameProfile().getName().equals(customName)) return false;
            } else if (!e.getName().getString().equals(customName)) {
                return false;
            }
        }
        return true;
    }

    private boolean heldItemMatches(ItemStack stack) {
        if (heldItemTag != null) return !stack.isEmpty() && stack.is(heldItemTag);
        if (heldItemFilter.isEmpty()) return stack.isEmpty();
        return !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, heldItemFilter);
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

    public static void syncKnownStructureList(List<String> data) {
        KNOWN_STRUCTURES.clear();
        KNOWN_STRUCTURES.addAll(data);
    }
    public static void syncKnownDimensionList(List<String> data) {
        KNOWN_DIMENSIONS.clear();
        KNOWN_DIMENSIONS.addAll(data);
    }
    public static void syncKnownBiomeList(List<String> data) {
        KNOWN_BIOMES.clear();
        KNOWN_BIOMES.addAll(data);
    }

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
}
