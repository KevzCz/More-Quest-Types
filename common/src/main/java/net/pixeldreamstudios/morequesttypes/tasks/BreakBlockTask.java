package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.client.ConfigIconItemStack;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.pixeldreamstudios.morequesttypes.event.BreakBlockEventBuffer;

import java.util.ArrayList;

public final class BreakBlockTask extends Task {
    private long value = 1;
    private ItemStack blockItemFilter = ItemStack.EMPTY;
    private String blockTagStr = "";
    private transient TagKey<Block> blockTag;
    private String toolTagStr = "";
    private transient TagKey<Item> toolTag;
    private ItemStack toolItemFilter = ItemStack.EMPTY;
    public BreakBlockTask(long id, Quest quest) { super(id, quest); }
    @Override public TaskType getType() { return MoreTasksTypes.BREAK_BLOCK; }
    @Override public long getMaxProgress() { return Math.max(1L, value); }
    @Override public int autoSubmitOnPlayerTick() { return 1; }
    private final transient java.util.Map<java.util.UUID, Long> lastProcessedTick = new java.util.WeakHashMap<>();
    @Override
    public void submitTask(TeamData teamData, net.minecraft.server.level.ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        long cur = teamData.getProgress(this);
        if (cur >= getMaxProgress()) return;

        var events = BreakBlockEventBuffer.snapshotLatest(player.getUUID());
        if (events.isEmpty()) return;

        long latestTick = events.get(0).gameTime();
        Long seen = lastProcessedTick.get(player.getUUID());
        if (seen != null && seen == latestTick) return;

        int inc = 0;
        for (var ev : events) {
            if (!matchesBlock(ev.state())) continue;
            if (!matchesTool(ev.tool())) continue;
            inc++;
        }
        if (inc <= 0) { lastProcessedTick.put(player.getUUID(), latestTick); return; }

        long next = Math.min(getMaxProgress(), cur + inc);
        if (next != cur) teamData.setProgress(this, next);

        lastProcessedTick.put(player.getUUID(), latestTick);
    }
    private boolean matchesBlock(BlockState state) {
        if (state == null) return false;
        if (blockTag != null) return state.is(blockTag);
        if (!blockItemFilter.isEmpty()) {
            var fromBlock = state.getBlock().asItem();
            return ItemStack.isSameItemSameComponents(new ItemStack(fromBlock), blockItemFilter);
        }
        return true;
    }

    private boolean matchesTool(ItemStack tool) {
        if (toolTag != null) return !tool.isEmpty() && tool.is(toolTag);
        if (!toolItemFilter.isEmpty()) return !tool.isEmpty() && ItemStack.isSameItemSameComponents(tool, toolItemFilter);
        return true;
    }

    private void resolveTags() {

        if (blockTagStr == null || blockTagStr.isBlank()) {
            blockTag = null;
        } else {
            String s = blockTagStr.startsWith("#") ? blockTagStr.substring(1) : blockTagStr;
            var rl = ResourceLocation.tryParse(s);
            blockTag = (rl == null) ? null : TagKey.create(Registries.BLOCK, rl);
        }

        if (toolTagStr == null || toolTagStr.isBlank()) {
            toolTag = null;
        } else {
            String s = toolTagStr.startsWith("#") ? toolTagStr.substring(1) : toolTagStr;
            var rl = ResourceLocation.tryParse(s);
            toolTag = (rl == null) ? null : TagKey.create(Registries.ITEM, rl);
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        Component blockName;
        if (!blockItemFilter.isEmpty()) blockName = blockItemFilter.getHoverName();
        else if (!blockTagStr.isBlank()) blockName = Component.literal(blockTagStr);
        else blockName = Component.literal("Any Block");
        return Component.translatable("morequesttypes.task.break_block.title", formatMaxProgress(), blockName);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        var icons = new ArrayList<Icon>();

        if (!blockItemFilter.isEmpty()) {
            var copy = blockItemFilter.copy(); copy.setCount(1);
            var icon = ItemIcon.getItemIcon(copy);
            if (!icon.isEmpty()) icons.add(icon);
        } else if (blockTag != null) {
            BuiltInRegistries.BLOCK.getTag(blockTag).ifPresent(tagSet -> {
                int i = 0;
                for (var holder : tagSet) {
                    var item = holder.value().asItem();
                    var icon = ItemIcon.getItemIcon(new ItemStack(item));
                    if (!icon.isEmpty()) icons.add(icon);
                    if (++i >= 16) break;
                }
            });
        }

        if (icons.isEmpty()) return ItemIcon.getItemIcon(net.minecraft.world.item.Items.IRON_PICKAXE);
        return IconAnimation.fromList(icons, false);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        config.addLong("value", value, v -> value = Math.max(1L, v), 1L, 1L, Long.MAX_VALUE)
                .setNameKey("morequesttypes.task.break_block.value");
        var cisBlock = new ConfigIconItemStack();
        config.add(
                "block_item", cisBlock, blockItemFilter, v -> {
                    blockItemFilter = v.copy();
                    if (!blockItemFilter.isEmpty()) blockItemFilter.setCount(1);
                }, ItemStack.EMPTY
        ).setNameKey("morequesttypes.task.break_block.block_item");

        var BLOCK_TAGS = NameMap.of("",
                BuiltInRegistries.BLOCK.getTags()
                        .map(p -> p.getFirst().location().toString())
                        .sorted().toArray(String[]::new)
        ).create();
        config.addEnum("block_tag", blockTagStr, v -> { blockTagStr = v; resolveTags(); }, BLOCK_TAGS)
                .setNameKey("morequesttypes.task.break_block.block_tag");

        var cisTool = new ConfigIconItemStack();
        config.add(
                "tool_item", cisTool, toolItemFilter, v -> {
                    toolItemFilter = v.copy();
                    if (!toolItemFilter.isEmpty()) toolItemFilter.setCount(1);
                }, ItemStack.EMPTY
        ).setNameKey("morequesttypes.task.break_block.tool_item");

        var ITEM_TAGS = NameMap.of("",
                BuiltInRegistries.ITEM.getTags()
                        .map(p -> p.getFirst().location().toString())
                        .sorted().toArray(String[]::new)
        ).create();
        config.addEnum("tool_tag", toolTagStr, v -> { toolTagStr = v; resolveTags(); }, ITEM_TAGS)
                .setNameKey("morequesttypes.task.break_block.tool_tag");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putLong("value", value);
        if (!blockItemFilter.isEmpty()) nbt.put("block_item", saveItemSingleLine(blockItemFilter.copyWithCount(1)));
        if (!blockTagStr.isEmpty()) nbt.putString("block_tag", blockTagStr);
        if (!toolItemFilter.isEmpty()) nbt.put("tool_item", saveItemSingleLine(toolItemFilter.copyWithCount(1)));
        if (!toolTagStr.isEmpty()) nbt.putString("tool_tag", toolTagStr);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        value = Math.max(1L, nbt.getLong("value"));
        blockItemFilter = nbt.contains("block_item") ? itemOrMissingFromNBT(nbt.get("block_item"), provider) : ItemStack.EMPTY;
        if (!blockItemFilter.isEmpty()) blockItemFilter.setCount(1);
        blockTagStr = nbt.getString("block_tag");

        toolItemFilter = nbt.contains("tool_item") ? itemOrMissingFromNBT(nbt.get("tool_item"), provider) : ItemStack.EMPTY;
        if (!toolItemFilter.isEmpty()) toolItemFilter.setCount(1);
        toolTagStr = nbt.getString("tool_tag");

        resolveTags();
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeVarLong(value);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, blockItemFilter);
        buf.writeUtf(blockTagStr);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, toolItemFilter);
        buf.writeUtf(toolTagStr);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        value = buf.readVarLong();

        blockItemFilter = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        if (!blockItemFilter.isEmpty()) blockItemFilter.setCount(1);
        blockTagStr = buf.readUtf();

        toolItemFilter = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        if (!toolItemFilter.isEmpty()) toolItemFilter.setCount(1);
        toolTagStr = buf.readUtf();

        resolveTags();
    }
}
