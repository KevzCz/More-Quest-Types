package net.pixeldreamstudios.morequesttypes.tasks;

import com.mojang.datafixers.util.Either;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.client.ConfigIconItemStack;
import dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.pixeldreamstudios.morequesttypes.event.UseItemEventBuffer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class UseItemTask extends dev.ftb.mods.ftbquests.quest.task.Task {
    public enum HandMode { ANY, MAIN_HAND, OFF_HAND }
    private long value = 1;
    private HandMode handMode = HandMode.ANY;
    private boolean anyItem = false;
    private ItemStack itemFilter = ItemStack.EMPTY;
    private String itemTagStr = "";
    private transient TagKey<net.minecraft.world.item.Item> itemTag;
    private ItemMatchingSystem.ComponentMatchType matchComponents = ItemMatchingSystem.ComponentMatchType.NONE;
    private final List<String> nbtFilters = new ArrayList<>();
    private static final ResourceLocation DEFAULT_STRUCTURE = ResourceLocation.withDefaultNamespace("mineshaft");
    private static final List<String> KNOWN_STRUCTURES = new ArrayList<>();
    private Either<ResourceKey<Structure>, TagKey<Structure>> structure = null;
    private static final List<String> KNOWN_DIMENSIONS = new ArrayList<>();
    private String dimension = "";
    private static final List<String> KNOWN_BIOMES = new ArrayList<>();
    private String biome = "";

    public UseItemTask(long id, Quest quest) { super(id, quest); }
    @Override public TaskType getType() { return MoreTasksTypes.USE_ITEM; }
    @Override public long getMaxProgress() { return Math.max(1L, value); }
    @Override public int autoSubmitOnPlayerTick() { return 1; }

    @Override
    public void submitTask(TeamData teamData, net.minecraft.server.level.ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        long cur = teamData.getProgress(this);
        if (cur >= getMaxProgress()) return;

        var events = UseItemEventBuffer.snapshotLatest(player.getUUID());
        if (events.isEmpty()) return;

        int inc = (handMode == HandMode.ANY) ? countAny(events, player) : countExact(events, player);
        if (inc <= 0) return;

        long next = Math.min(getMaxProgress(), cur + inc);
        teamData.setProgress(this, next);
    }

    private int countExact(List<UseItemEventBuffer.Use> events, net.minecraft.server.level.ServerPlayer player) {
        int inc = 0;
        for (var ev : events) {
            if (handMode == HandMode.MAIN_HAND && ev.hand() != InteractionHand.MAIN_HAND) continue;
            if (handMode == HandMode.OFF_HAND  && ev.hand() != InteractionHand.OFF_HAND)  continue;
            if (matches(player, ev.stack())) inc++;
        }
        return inc;
    }

    private int countAny(List<UseItemEventBuffer.Use> events, net.minecraft.server.level.ServerPlayer player) {
        int inc = 0;
        EnumSet<InteractionHand> counted = EnumSet.noneOf(InteractionHand.class);
        for (var ev : events) {
            if (counted.contains(ev.hand())) continue;
            if (matches(player, ev.stack())) {
                counted.add(ev.hand());
                inc++;
            }
        }
        return inc;
    }

    private boolean matches(net.minecraft.server.level.ServerPlayer who, ItemStack stack) {
        if (!insideLocationFilters(who.serverLevel(), who.blockPosition())) return false;

        if (anyItem) {
            if (stack.isEmpty()) return false;
        } else {
            if (itemTag != null) {
                if (!stack.is(itemTag)) return false;
            } else if (itemFilter.isEmpty()) {
                if (stack.isEmpty()) return false;
            } else {
                if (!ItemMatchingSystem.INSTANCE.doesItemMatch(itemFilter, stack, matchComponents, getQuestFile().holderLookup())) {
                    return false;
                }
            }
        }

        if (!nbtFilters.isEmpty() && !stack.isEmpty()) {
            List<String> processed = processPlaceholders(nbtFilters, who.getUUID(), who.getGameProfile().getName());
            if (!checkNbtFilters(stack, processed, who.registryAccess())) {
                return false;
            }
        }

        return true;
    }

    private List<String> processPlaceholders(List<String> entries, UUID playerUuid, String playerName) {
        List<String> processed = new ArrayList<>();
        for (String entry : entries) {
            String result = entry
                    .replace("{player_uuid}", playerUuid.toString())
                    .replace("{player_name}", playerName)
                    .replace("{player_uuid_array}", uuidToIntArray(playerUuid));
            processed.add(result);
        }
        return processed;
    }

    private String uuidToIntArray(UUID uuid) {
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();

        int[] ints = new int[4];
        ints[0] = (int) (mostSigBits >> 32);
        ints[1] = (int) mostSigBits;
        ints[2] = (int) (leastSigBits >> 32);
        ints[3] = (int) leastSigBits;

        return "[I;" + ints[0] + "," + ints[1] + "," + ints[2] + "," + ints[3] + "]";
    }

    private boolean checkNbtFilters(ItemStack stack, List<String> filters, HolderLookup.Provider provider) {
        try {
            CompoundTag fullTag = (CompoundTag) stack.save(provider);

            for (String filterSnbt : filters) {
                if (filterSnbt == null || filterSnbt.isBlank()) continue;

                String cleanFilter = filterSnbt.trim();
                if (cleanFilter.startsWith("\"") && cleanFilter.endsWith("\"")) {
                    cleanFilter = cleanFilter.substring(1, cleanFilter.length() - 1);
                }

                try {
                    Tag parsedFilter = TagParser.parseTag(cleanFilter);
                    if (parsedFilter instanceof CompoundTag filterCompound) {
                        if (!containsPartialNbt(fullTag, filterCompound)) {
                            return false;
                        }
                    }
                } catch (Exception e) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean containsPartialNbt(CompoundTag itemTag, CompoundTag filter) {
        for (String key : filter.getAllKeys()) {
            Tag filterValue = filter.get(key);
            Tag itemValue = itemTag.get(key);

            if (itemValue == null) return false;

            if (filterValue instanceof CompoundTag filterCompound && itemValue instanceof CompoundTag itemCompound) {
                if (!containsPartialNbt(itemCompound, filterCompound)) {
                    return false;
                }
            } else if (filterValue instanceof CompoundTag && itemValue instanceof CompoundTag) {
                if (!containsPartialNbt((CompoundTag) itemValue, (CompoundTag) filterValue)) {
                    return false;
                }
            } else if (filterValue instanceof CompoundTag) {
                return false;
            } else if (!tagsEqual(itemValue, filterValue)) {
                return false;
            }
        }
        return true;
    }

    private boolean tagsEqual(Tag a, Tag b) {
        if (a instanceof CompoundTag && b instanceof CompoundTag) {
            CompoundTag ca = (CompoundTag) a;
            CompoundTag cb = (CompoundTag) b;
            if (cb.isEmpty()) return true;
            return containsPartialNbt(ca, cb);
        }
        return net.minecraft.nbt.NbtUtils.compareNbt(a, b, true);
    }



    private boolean insideLocationFilters(ServerLevel level, BlockPos pos) {
        if (structure != null && !isInsideStructureOrTag(level, pos)) return false;
        if (!isInsideDimension(level)) return false;
        return isInsideBiome(level, pos);
    }

    private boolean isInsideStructureOrTag(ServerLevel level, BlockPos pos) {
        StructureManager mgr = level.structureManager();
        return structure == null || structure.map(
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

    private boolean isInsideBiome(ServerLevel level, BlockPos pos) {
        if (biome == null || biome.isEmpty()) return true;
        var h = level.getBiome(pos);
        if (biome.startsWith("#")) {
            var rl = ResourceLocation.tryParse(biome.substring(1));
            if (rl == null) return false;
            var tag = TagKey.create(Registries.BIOME, rl);
            return h.is(tag);
        } else {
            return h.unwrapKey().map(k -> k.location().toString().equals(biome)).orElse(false);
        }
    }

    private void resolveItemTag() {
        if (itemTagStr == null || itemTagStr.isBlank()) { itemTag = null; return; }
        String s = itemTagStr.startsWith("#") ? itemTagStr.substring(1) : itemTagStr;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        itemTag = (rl != null) ? TagKey.create(Registries.ITEM, rl) : null;
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
        return rl != null ? rl : UseItemTask.DEFAULT_STRUCTURE;
    }

    private static void maybeRequestStructureSync() {
        if (KNOWN_STRUCTURES.isEmpty()) {
            net.pixeldreamstudios.morequesttypes.network.NetworkHelper.sendToServer(new net.pixeldreamstudios.morequesttypes.network.MQTStructuresRequest());
        }
    }

    private static void maybeRequestWorldSync() {
        if (KNOWN_DIMENSIONS.isEmpty()) {
            net.pixeldreamstudios.morequesttypes.network.NetworkHelper.sendToServer(new net.pixeldreamstudios.morequesttypes.network.MQTWorldsRequest());
        }
    }

    private static void maybeRequestBiomeSync() {
        if (KNOWN_BIOMES.isEmpty()) {
            net.pixeldreamstudios.morequesttypes.network.NetworkHelper.sendToServer(new net.pixeldreamstudios.morequesttypes.network.MQTBiomesRequest());
        }
    }

    public static void syncKnownStructureList(List<String> data) { KNOWN_STRUCTURES.clear(); KNOWN_STRUCTURES.addAll(data); }
    public static void syncKnownDimensionList(List<String> data) { KNOWN_DIMENSIONS.clear(); KNOWN_DIMENSIONS.addAll(data); }
    public static void syncKnownBiomeList(List<String> data)     { KNOWN_BIOMES.clear();     KNOWN_BIOMES.addAll(data); }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        config.addLong("value", value, v -> value = Math.max(1L, v), 1L, 1L, Long.MAX_VALUE);
        var HANDS = NameMap.of(HandMode.ANY, HandMode.values()).create();
        config.addEnum("hand_mode", handMode, v -> handMode = v, HANDS)
                .setNameKey("morequesttypes.task.use_item.hand");

        config.addBool("any_item", anyItem, v -> anyItem = v, false)
                .setNameKey("morequesttypes.task.use_item.any_item");

        ConfigIconItemStack cis = new ConfigIconItemStack();
        config.add("item", cis, itemFilter, v -> {
                    itemFilter = v.copy();
                    if (!itemFilter.isEmpty()) itemFilter.setCount(1);
                }, ItemStack.EMPTY).setNameKey("morequesttypes.task.use_item.item")
                .setCanEdit(!anyItem);

        var ITEM_TAGS = NameMap.of("", BuiltInRegistries.ITEM.getTags()
                .map(p -> p.getFirst().location().toString()).sorted().toArray(String[]::new)).create();
        config.addEnum("item_tag", itemTagStr, v -> { itemTagStr = v; resolveItemTag(); }, ITEM_TAGS)
                .setNameKey("morequesttypes.task.use_item.item_tag")
                .setCanEdit(!anyItem);

        var COMP_MATCH = NameMap.of(ItemMatchingSystem.ComponentMatchType.NONE, ItemMatchingSystem.ComponentMatchType.values()).create();
        config.addEnum("match_components", matchComponents, v -> matchComponents = v, COMP_MATCH)
                .setNameKey("morequesttypes.task.match_components")
                .setCanEdit(!anyItem);

        config.addList("nbt_filters", nbtFilters, new dev.ftb.mods.ftblibrary.config.StringConfig(), "")
                .setNameKey("morequesttypes.task.nbt_filters");

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
        } else dimChoices.addAll(KNOWN_DIMENSIONS);
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
        } else biomeChoices.addAll(KNOWN_BIOMES);
        var BIOME_MAP = NameMap.of(biome, biomeChoices)
                .name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "Any" : s))
                .create();
        config.addEnum("biome", biome, v -> biome = v, BIOME_MAP)
                .setNameKey("morequesttypes.task.biome");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putLong("value", value);
        nbt.putString("hand_mode", handMode.name());
        if (anyItem) nbt.putBoolean("any_item", true);
        if (!itemFilter.isEmpty()) nbt.put("item", saveItemSingleLine(itemFilter.copyWithCount(1)));
        if (!itemTagStr.isEmpty()) nbt.putString("item_tag", itemTagStr);
        if (matchComponents != ItemMatchingSystem.ComponentMatchType.NONE) {
            nbt.putString("match_components", ItemMatchingSystem.ComponentMatchType.NAME_MAP.getName(matchComponents));
        }
        if (!nbtFilters.isEmpty()) {
            ListTag list = new ListTag();
            for (String s : nbtFilters) list.add(StringTag.valueOf(s));
            nbt.put("nbt_filters", list);
        }
        String s = getStructure();
        if (!s.isEmpty()) nbt.putString("structure", s);
        if (!dimension.isEmpty()) nbt.putString("dimension", dimension);
        if (!biome.isEmpty()) nbt.putString("biome", biome);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        value = Math.max(1L, nbt.getLong("value"));
        try { handMode = HandMode.valueOf(nbt.getString("hand_mode")); } catch (Throwable ignored) { handMode = HandMode.ANY; }
        anyItem = nbt.getBoolean("any_item");
        itemFilter = nbt.contains("item") ? itemOrMissingFromNBT(nbt.get("item"), provider) : ItemStack.EMPTY;
        if (!itemFilter.isEmpty()) itemFilter.setCount(1);
        itemTagStr = nbt.getString("item_tag");
        resolveItemTag();
        matchComponents = (ItemMatchingSystem.ComponentMatchType) ItemMatchingSystem.ComponentMatchType.NAME_MAP.get(nbt.getString("match_components"));
        nbtFilters.clear();
        ListTag list = nbt.getList("nbt_filters", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) nbtFilters.add(list.getString(i));
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
        buf.writeVarLong(value);
        buf.writeEnum(handMode);
        buf.writeBoolean(anyItem);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, itemFilter);
        buf.writeUtf(itemTagStr);
        buf.writeEnum(matchComponents);
        buf.writeVarInt(nbtFilters.size());
        for (String s : nbtFilters) buf.writeUtf(s);
        buf.writeUtf(getStructure());
        buf.writeUtf(dimension);
        buf.writeUtf(biome);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        value = buf.readVarLong();
        handMode = buf.readEnum(HandMode.class);
        anyItem = buf.readBoolean();
        itemFilter = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        if (!itemFilter.isEmpty()) itemFilter.setCount(1);
        itemTagStr = buf.readUtf();
        resolveItemTag();
        matchComponents = buf.readEnum(ItemMatchingSystem.ComponentMatchType.class);
        nbtFilters.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) nbtFilters.add(buf.readUtf());
        String s = buf.readUtf();
        if (!s.isEmpty()) setStructure(s); else structure = null;
        dimension = buf.readUtf();
        biome = buf.readUtf();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public net.minecraft.network.chat.MutableComponent getAltTitle() {
        var item = anyItem ? Component.literal("Any Item")
                : itemFilter.isEmpty() ? Component.literal(itemTagStr.isBlank() ? "Any" : itemTagStr)
                : itemFilter.getHoverName();
        return Component.translatable("morequesttypes.task.use_item.title", formatMaxProgress(), item);
    }

    @Environment(net.fabricmc.api.EnvType.CLIENT)
    @Override
    public dev.ftb.mods.ftblibrary.icon.Icon getAltIcon() {
        if (anyItem) {
            return dev.ftb.mods.ftblibrary.icon.Icon.getIcon("minecraft:item/barrier");
        }

        java.util.List<dev.ftb.mods.ftblibrary.icon.Icon> icons = new java.util.ArrayList<>();

        for (net.minecraft.world.item.ItemStack stack : getValidDisplayItems()) {
            net.minecraft.world.item.ItemStack copy = stack.copy();
            copy.setCount(1);
            dev.ftb.mods.ftblibrary.icon.Icon icon = dev.ftb.mods.ftblibrary.icon.ItemIcon.getItemIcon(copy);
            if (!icon.isEmpty()) {
                icons.add(icon);
            }
        }

        if (icons.isEmpty()) {
            return dev.ftb.mods.ftblibrary.icon.ItemIcon.getItemIcon(
                    dev.ftb.mods.ftbquests.registry.ModItems.MISSING_ITEM.get()
            );
        }

        return dev.ftb.mods.ftblibrary.icon.IconAnimation.fromList(icons, false);
    }

    @Environment(EnvType.CLIENT)
    private java.util.List<net.minecraft.world.item.ItemStack> getValidDisplayItems() {
        java.util.List<net.minecraft.world.item.ItemStack> out = new java.util.ArrayList<>();

        if (!itemFilter.isEmpty()) {
            net.minecraft.world.item.ItemStack one = itemFilter.copy();
            one.setCount(1);
            out.add(one);
            return out;
        }

        if (itemTag != null) {
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(itemTag).ifPresent(tagSet -> {
                final int MAX = 16;
                int i = 0;
                for (var holder : tagSet) {
                    out.add(new net.minecraft.world.item.ItemStack(holder.value()));
                    if (++i >= MAX) break;
                }
            });
        }

        return out;
    }
}