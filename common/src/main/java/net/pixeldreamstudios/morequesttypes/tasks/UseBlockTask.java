package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.pixeldreamstudios.morequesttypes.config.BlockStackConfig;
import net.pixeldreamstudios.morequesttypes.event.UseBlockEventBuffer;

import java.util.ArrayList;
import java.util.List;

public final class UseBlockTask extends dev.ftb.mods.ftbquests.quest.task.Task {
    private long value = 1;
    private ItemStack blockFilter = ItemStack.EMPTY;
    private String blockTagStr = "";
    private transient TagKey<Block> blockTag;
    private String dimension = "";
    private String biome = "";
    private static final List<String> KNOWN_DIMENSIONS = new ArrayList<>();
    private static final List<String> KNOWN_BIOMES = new ArrayList<>();

    public UseBlockTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.USE_BLOCK;
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
    public void submitTask(TeamData teamData, net.minecraft.server.level.ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        long cur = teamData.getProgress(this);
        if (cur >= getMaxProgress()) return;

        var events = UseBlockEventBuffer.snapshotLatest(player.getUUID());
        if (events.isEmpty()) return;

        int inc = 0;
        for (var ev : events) {
            if (matches(player.serverLevel(), ev.pos(), ev.state())) {
                inc++;
            }
        }

        if (inc <= 0) return;

        long next = Math.min(getMaxProgress(), cur + inc);
        teamData.setProgress(this, next);
    }

    private boolean matches(ServerLevel level, BlockPos pos, BlockState state) {
        if (!insideLocationFilters(level, pos)) return false;

        if (blockTag != null) {
            return state.is(blockTag);
        }

        if (blockFilter.isEmpty()) {
            return true;
        }

        Block filterBlock = Block.byItem(blockFilter.getItem());
        return state.is(filterBlock);
    }

    private boolean insideLocationFilters(ServerLevel level, BlockPos pos) {
        if (!isInsideDimension(level)) return false;
        return isInsideBiome(level, pos);
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

    private void resolveBlockTag() {
        if (blockTagStr == null || blockTagStr.isBlank()) {
            blockTag = null;
            return;
        }
        String s = blockTagStr.startsWith("#") ? blockTagStr.substring(1) : blockTagStr;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        blockTag = (rl != null) ? TagKey.create(Registries.BLOCK, rl) : null;
    }

    private static void maybeRequestWorldSync() {
        if (KNOWN_DIMENSIONS.isEmpty()) {
            net.pixeldreamstudios.morequesttypes.network.NetworkHelper.sendToServer(
                    new net.pixeldreamstudios.morequesttypes.network.MQTWorldsRequest()
            );
        }
    }

    private static void maybeRequestBiomeSync() {
        if (KNOWN_BIOMES.isEmpty()) {
            net.pixeldreamstudios.morequesttypes.network.NetworkHelper.sendToServer(
                    new net.pixeldreamstudios.morequesttypes.network.MQTBiomesRequest()
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

        config.addLong("value", value, v -> value = Math.max(1L, v), 1L, 1L, Long.MAX_VALUE)
                .setNameKey("morequesttypes.task.use_block.value");

        config.add("block", new BlockStackConfig(), blockFilter, v -> {
            blockFilter = v.copy();
            if (!blockFilter.isEmpty()) blockFilter.setCount(1);
        }, ItemStack.EMPTY).setNameKey("morequesttypes.task.use_block.block");

        var BLOCK_TAGS = NameMap.of("", BuiltInRegistries.BLOCK.getTags()
                .map(p -> p.getFirst().location().toString()).sorted().toArray(String[]::new)).create();
        config.addEnum("block_tag", blockTagStr, v -> {
            blockTagStr = v;
            resolveBlockTag();
        }, BLOCK_TAGS).setNameKey("morequesttypes.task.use_block.block_tag");

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
        nbt.putLong("value", value);
        if (!blockFilter.isEmpty()) nbt.put("block", saveItemSingleLine(blockFilter.copyWithCount(1)));
        if (!blockTagStr.isEmpty()) nbt.putString("block_tag", blockTagStr);
        if (!dimension.isEmpty()) nbt.putString("dimension", dimension);
        if (!biome.isEmpty()) nbt.putString("biome", biome);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        value = Math.max(1L, nbt.getLong("value"));
        blockFilter = nbt.contains("block") ? itemOrMissingFromNBT(nbt.get("block"), provider) : ItemStack.EMPTY;
        if (!blockFilter.isEmpty()) blockFilter.setCount(1);
        blockTagStr = nbt.getString("block_tag");
        resolveBlockTag();
        dimension = nbt.getString("dimension");
        biome = nbt.getString("biome");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeVarLong(value);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, blockFilter);
        buf.writeUtf(blockTagStr);
        buf.writeUtf(dimension);
        buf.writeUtf(biome);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        value = buf.readVarLong();
        blockFilter = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        if (!blockFilter.isEmpty()) blockFilter.setCount(1);
        blockTagStr = buf.readUtf();
        resolveBlockTag();
        dimension = buf.readUtf();
        biome = buf.readUtf();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public net.minecraft.network.chat.MutableComponent getAltTitle() {
        var block = blockFilter.isEmpty()
                ? Component.literal(blockTagStr.isBlank() ? "Any" : blockTagStr)
                : blockFilter.getHoverName();
        return Component.translatable("morequesttypes.task.use_block.title", formatMaxProgress(), block);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public dev.ftb.mods.ftblibrary.icon.Icon getAltIcon() {
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

        if (!blockFilter.isEmpty()) {
            net.minecraft.world.item.ItemStack one = blockFilter.copy();
            one.setCount(1);
            out.add(one);
            return out;
        }

        if (blockTag != null) {
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getTag(blockTag).ifPresent(tagSet -> {
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