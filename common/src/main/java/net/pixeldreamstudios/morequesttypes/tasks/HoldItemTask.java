package net.pixeldreamstudios.morequesttypes.tasks;

import com.mojang.datafixers.util.*;
import dev.ftb.mods.ftblibrary.config.*;
import dev.ftb.mods.ftblibrary.icon.*;
import dev.ftb.mods.ftblibrary.util.*;
import dev.ftb.mods.ftbquests.client.*;
import dev.ftb.mods.ftbquests.integration.item_filtering.*;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.task.*;
import dev.ftb.mods.ftbquests.registry.*;
import net.fabricmc.api.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.pixeldreamstudios.morequesttypes.network.NetworkHelper;
import net.pixeldreamstudios.morequesttypes.network.*;

import java.util.*;

public final class HoldItemTask extends Task {
    public enum HandMode {ANY, MAIN_HAND, OFF_HAND}

    private double durationSeconds = 5.0D;
    private HandMode handMode = HandMode.ANY;
    private boolean anyItem = false;
    private ItemStack itemFilter = ItemStack.EMPTY;
    private String itemTagStr = "";
    private transient TagKey<Item> itemTag;
    private ItemMatchingSystem.ComponentMatchType matchComponents = ItemMatchingSystem.ComponentMatchType.NONE;
    private final List<String> nbtFilters = new ArrayList<>();
    private static final ResourceLocation DEFAULT_STRUCTURE = ResourceLocation.withDefaultNamespace("mineshaft");
    private static final List<String> KNOWN_STRUCTURES = new ArrayList<>();
    private Either<ResourceKey<Structure>, TagKey<Structure>> structure = null;
    private static final List<String> KNOWN_DIMENSIONS = new ArrayList<>();
    private String dimension = "";
    private static final List<String> KNOWN_BIOMES = new ArrayList<>();
    private String biome = "";

    public HoldItemTask(long id, Quest quest) {
        super(id, quest);
    }

    private long maxTicks() {
        return Math.max(1L, Math.round(durationSeconds * 20.0D));
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.HOLD_ITEM;
    }

    @Override
    public long getMaxProgress() {
        return maxTicks();
    }

    @Override
    public boolean hideProgressNumbers() {
        return false;
    }

    @Override
    public String formatMaxProgress() {
        return StringUtils.formatDouble(durationSeconds, true) + "s";
    }

    @Override
    public String formatProgress(TeamData teamData, long progress) {
        long remainingTicks = Math.max(0L, maxTicks() - progress);
        double remainingSeconds = remainingTicks / 20.0D;
        return StringUtils.formatDouble(remainingSeconds, true) + "s";
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 1;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        var level = player.serverLevel();
        if (!insideLocationFilters(level, player.blockPosition())) return;

        boolean ok = switch (handMode) {
            case MAIN_HAND -> matches(player, player.getMainHandItem());
            case OFF_HAND -> matches(player, player.getOffhandItem());
            default -> matches(player, player.getMainHandItem()) || matches(player, player.getOffhandItem());
        };
        if (!ok) return;

        long cur = teamData.getProgress(this);
        long next = Math.min(getMaxProgress(), cur + 1L);
        if (next != cur) teamData.setProgress(this, next);
    }

    private boolean matches(ServerPlayer player, ItemStack stack) {
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
            List<String> processed = processPlaceholders(nbtFilters, player.getUUID(), player.getGameProfile().getName());
            return checkNbtFilters(stack, processed, player.registryAccess());
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
        if (a instanceof CompoundTag ca && b instanceof CompoundTag cb) {
            if (cb.isEmpty()) return true;
            return containsPartialNbt(ca, cb);
        }
        return NbtUtils.compareNbt(a, b, true);
    }

    private boolean insideLocationFilters(ServerLevel level, BlockPos pos) {
        if (structure != null && !isInsideStructureOrTag(level, pos)) return false;
        if (!isInsideDimension(level)) return false;
        if (!isInsideBiome(level, pos)) return false;
        return true;
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
        if (itemTagStr == null || itemTagStr.isBlank()) {
            itemTag = null;
            return;
        }
        String s = itemTagStr.startsWith("#") ? itemTagStr.substring(1) : itemTagStr;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        itemTag = (rl != null) ? TagKey.create(Registries.ITEM, rl) : null;
    }

    private void setStructure(String resLoc) {
        if (resLoc == null || resLoc.isEmpty()) {
            structure = null;
            return;
        }
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
        return rl != null ? rl : HoldItemTask.DEFAULT_STRUCTURE;
    }

    private static void maybeRequestStructureSync() {
        if (HoldItemTask.KNOWN_STRUCTURES.isEmpty()) {
            NetworkHelper.sendToServer(new MQTStructuresRequest());
        }
    }

    private static void maybeRequestWorldSync() {
        if (HoldItemTask.KNOWN_DIMENSIONS.isEmpty()) {
            NetworkHelper.sendToServer(new MQTWorldsRequest());
        }
    }

    private static void maybeRequestBiomeSync() {
        if (HoldItemTask.KNOWN_BIOMES.isEmpty()) {
            NetworkHelper.sendToServer(new MQTBiomesRequest());
        }
    }

    public static void syncKnownStructureList(List<String> data) {
        HoldItemTask.KNOWN_STRUCTURES.clear();
        HoldItemTask.KNOWN_STRUCTURES.addAll(data);
    }

    public static void syncKnownDimensionList(List<String> data) {
        HoldItemTask.KNOWN_DIMENSIONS.clear();
        HoldItemTask.KNOWN_DIMENSIONS.addAll(data);
    }

    public static void syncKnownBiomeList(List<String> data) {
        HoldItemTask.KNOWN_BIOMES.clear();
        HoldItemTask.KNOWN_BIOMES.addAll(data);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        config.addDouble("duration_seconds", durationSeconds, v -> durationSeconds = v, 5.0D, 0.05D, 86400.0D)
                .setNameKey("morequesttypes.task.hold_item.duration");

        var HANDS = NameMap.of(HandMode.ANY, HandMode.values()).create();
        config.addEnum("hand_mode", handMode, v -> handMode = v, HANDS)
                .setNameKey("morequesttypes.task.hold_item.hand");

        config.addBool("any_item", anyItem, v -> anyItem = v, false)
                .setNameKey("morequesttypes.task.hold_item.any_item");

        ConfigIconItemStack cis = new ConfigIconItemStack();
        config.add(
                        "item", cis, itemFilter, v -> {
                            itemFilter = v.copy();
                            if (!itemFilter.isEmpty()) itemFilter.setCount(1);
                        }, ItemStack.EMPTY
                ).setNameKey("morequesttypes.task.hold_item.item")
                .setCanEdit(!anyItem);

        var ITEM_TAGS = NameMap.of("", BuiltInRegistries.ITEM.getTags()
                .map(p -> p.getFirst().location().toString()).sorted().toArray(String[]::new)).create();
        config.addEnum("item_tag", itemTagStr, v -> {
                    itemTagStr = v;
                    resolveItemTag();
                }, ITEM_TAGS)
                .setNameKey("morequesttypes.task.hold_item.item_tag")
                .setCanEdit(!anyItem);

        var COMP_MATCH = NameMap.of(ItemMatchingSystem.ComponentMatchType.NONE, ItemMatchingSystem.ComponentMatchType.values()).create();
        config.addEnum("match_components", matchComponents, v -> matchComponents = v, COMP_MATCH)
                .setNameKey("morequesttypes.task.match_components")
                .setCanEdit(!anyItem);

        config.addList("nbt_filters", nbtFilters, new StringConfig(), "")
                .setNameKey("morequesttypes.task.nbt_filters");

        HoldItemTask.maybeRequestStructureSync();
        List<String> structureChoices = new ArrayList<>();
        structureChoices.add("");
        if (HoldItemTask.KNOWN_STRUCTURES.isEmpty()) structureChoices.add(HoldItemTask.DEFAULT_STRUCTURE.toString());
        else structureChoices.addAll(HoldItemTask.KNOWN_STRUCTURES);
        var STRUCTURE_MAP = NameMap.of(getStructure(), structureChoices)
                .name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "None" : s))
                .create();
        config.addEnum("structure", getStructure(), this::setStructure, STRUCTURE_MAP)
                .setNameKey("morequesttypes.task.structure");

        HoldItemTask.maybeRequestWorldSync();
        List<String> dimChoices = new ArrayList<>();
        dimChoices.add("");
        if (HoldItemTask.KNOWN_DIMENSIONS.isEmpty()) {
            dimChoices.add("minecraft:overworld");
            dimChoices.add("minecraft:the_nether");
            dimChoices.add("minecraft:the_end");
        } else dimChoices.addAll(HoldItemTask.KNOWN_DIMENSIONS);
        var DIMENSION_MAP = NameMap.of(dimension, dimChoices)
                .name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "Any" : s))
                .create();
        config.addEnum("dimension", dimension, v -> dimension = v, DIMENSION_MAP)
                .setNameKey("morequesttypes.task.dimension");

        HoldItemTask.maybeRequestBiomeSync();
        List<String> biomeChoices = new ArrayList<>();
        biomeChoices.add("");
        if (HoldItemTask.KNOWN_BIOMES.isEmpty()) {
            biomeChoices.add("minecraft:plains");
            biomeChoices.add("minecraft:forest");
            biomeChoices.add("minecraft:desert");
        } else biomeChoices.addAll(HoldItemTask.KNOWN_BIOMES);
        var BIOME_MAP = NameMap.of(biome, biomeChoices)
                .name(s -> Component.nullToEmpty((s == null || s.isEmpty()) ? "Any" : s))
                .create();
        config.addEnum("biome", biome, v -> biome = v, BIOME_MAP)
                .setNameKey("morequesttypes.task.biome");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putDouble("duration_seconds", durationSeconds);
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
        durationSeconds = Math.max(0.05D, nbt.contains("duration_seconds") ? nbt.getDouble("duration_seconds") : 5.0D);
        try {
            handMode = HandMode.valueOf(nbt.getString("hand_mode"));
        } catch (Throwable ignored) {
            handMode = HandMode.ANY;
        }
        anyItem = nbt.getBoolean("any_item");
        itemFilter = nbt.contains("item") ? QuestObjectBase.itemOrMissingFromNBT(nbt.get("item"), provider) : ItemStack.EMPTY;
        if (!itemFilter.isEmpty()) itemFilter.setCount(1);
        itemTagStr = nbt.getString("item_tag");
        resolveItemTag();
        matchComponents = ItemMatchingSystem.ComponentMatchType.NAME_MAP.get(nbt.getString("match_components"));
        nbtFilters.clear();
        ListTag list = nbt.getList("nbt_filters", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) nbtFilters.add(list.getString(i));
        String s = nbt.getString("structure");
        if (!s.isEmpty()) setStructure(s);
        else structure = null;
        String d = nbt.getString("dimension");
        dimension = d.trim();
        String b = nbt.getString("biome");
        biome = b.trim();
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeDouble(durationSeconds);
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
        durationSeconds = Math.max(0.05D, buf.readDouble());
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
        if (!s.isEmpty()) setStructure(s);
        else structure = null;
        dimension = buf.readUtf();
        biome = buf.readUtf();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        var item = anyItem ? Component.literal("Any Item")
                : itemFilter.isEmpty() ? Component.literal(itemTagStr.isBlank() ? "Any" : itemTagStr)
                : itemFilter.getHoverName();
        return Component.translatable("morequesttypes.task.hold_item.title", formatMaxProgress(), item);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        if (anyItem) {
            return Icon.getIcon("minecraft:item/barrier");
        }

        List<Icon> icons = new ArrayList<>();
        for (ItemStack stack : getValidDisplayItems()) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            Icon icon = ItemIcon.getItemIcon(copy);
            if (!icon.isEmpty()) {
                icons.add(icon);
            }
        }
        if (icons.isEmpty()) {
            return ItemIcon.getItemIcon(
                    ModItems.MISSING_ITEM.get()
            );
        }

        return IconAnimation.fromList(icons, false);
    }

    @Environment(EnvType.CLIENT)
    private List<ItemStack> getValidDisplayItems() {
        List<ItemStack> out = new ArrayList<>();

        if (!itemFilter.isEmpty()) {
            ItemStack one = itemFilter.copy();
            one.setCount(1);
            out.add(one);
            return out;
        }

        if (itemTag != null) {
            BuiltInRegistries.ITEM.getTag(itemTag).ifPresent(tagSet -> {
                final int MAX = 16;
                int i = 0;
                for (var holder : tagSet) {
                    out.add(new ItemStack(holder.value()));
                    if (++i >= MAX) break;
                }
            });
        }

        return out;
    }
}