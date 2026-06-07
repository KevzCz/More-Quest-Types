package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.ConfigIconItemStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.util.ItemNbtFilterData;
import net.pixeldreamstudios.morequesttypes.util.ItemNbtFilterRepair;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ItemNbtMatcherConfig extends ConfigValue<ItemNbtFilterData> {
    private static ItemStack cachedPreviewItem = ItemStack.EMPTY;

    public static void setDefaultPreview(ItemStack stack) {
        cachedPreviewItem = stack != null ? stack.copy() : ItemStack.EMPTY;
        if (!cachedPreviewItem.isEmpty()) {
            cachedPreviewItem.setCount(1);
        }
    }

    public ItemNbtMatcherConfig() {
    }

    @Override
    public Color4I getColor(@Nullable ItemNbtFilterData v) {
        return Color4I.WHITE;
    }

    @Override
    public Component getStringForGUI(@Nullable ItemNbtFilterData v) {
        if (v == null || (v.filters.isEmpty() && v.ignorePaths.isEmpty())) {
            return Component.literal("Not configured").withStyle(ChatFormatting.GRAY);
        }

        int require = 0;
        int exclude = 0;
        int or = 0;
        for (ItemNbtFilterData.FilterEntry entry : v.filters) {
            switch (entry.kind) {
                case EXCLUDE -> exclude++;
                case OR -> or++;
                case REQUIRE -> require++;
            }
        }

        return Component.translatable(
                "morequesttypes.config.item_nbt_matching.summary",
                require,
                exclude,
                or,
                v.ignorePaths.size()
        );
    }

    @Override
    public Icon getIcon(@Nullable ItemNbtFilterData v) {
        return Icons.SETTINGS;
    }

    @Override
    public void onClicked(Widget clicked, MouseButton button, ConfigCallback callback) {
        if (!getCanEdit()) {
            return;
        }

        ItemNbtFilterData current = getValue() != null ? getValue().copy() : new ItemNbtFilterData();
        if (current.previewItem.isEmpty() && !cachedPreviewItem.isEmpty()) {
            current.previewItem = cachedPreviewItem.copy();
        }

        ConfigGroup mainGroup = new ConfigGroup("item_nbt_matching", accepted -> {
            if (accepted) {
                setValue(current);
                callback.save(true);
            } else {
                callback.save(false);
            }
        });

        ConfigGroup previewGroup = mainGroup.getOrCreateSubgroup("preview");
        previewGroup.setNameKey("morequesttypes.config.item_nbt_matching.preview");

        ConfigIconItemStack previewConfig = new ConfigIconItemStack();
        previewGroup.add("preview_item", previewConfig, current.previewItem, stack -> {
            current.previewItem = stack.copy();
            if (!current.previewItem.isEmpty()) {
                current.previewItem.setCount(1);
            }
            cachedPreviewItem = current.previewItem.copy();
        }, ItemStack.EMPTY).setNameKey("morequesttypes.config.item_nbt_matching.preview_item");

        ItemStack previewStack = resolvePreviewStack(current);
        restorePersistedFilters(current, previewStack);

        ConfigGroup filtersGroup = mainGroup.getOrCreateSubgroup("filters");
        filtersGroup.setNameKey("morequesttypes.config.item_nbt_matching.filters");
        filtersGroup.addList("filter_entries", current.filters, new ItemNbtFilterEntryConfig(() -> resolvePreviewStack(current)), new ItemNbtFilterData.FilterEntry())
                .setNameKey("morequesttypes.config.item_nbt_matching.filter_entries");

        ConfigGroup ignoreGroup = mainGroup.getOrCreateSubgroup("ignore");
        ignoreGroup.setNameKey("morequesttypes.config.item_nbt_matching.ignore");

        if (!previewStack.isEmpty()) {
            ignoreGroup.addList("ignore_paths", current.ignorePaths, new NbtPathRewardConfig.PathSelectorConfig(previewStack), "")
                    .setNameKey("morequesttypes.config.item_nbt_matching.ignore_paths");
        } else {
            ignoreGroup.addList("ignore_paths", current.ignorePaths, new StringConfig(), "")
                    .setNameKey("morequesttypes.config.item_nbt_matching.ignore_paths");
        }

        new EditConfigScreen(mainGroup).openGui();
    }

    private static void restorePersistedFilters(ItemNbtFilterData data, ItemStack previewStack) {
        for (ItemNbtFilterData.FilterEntry entry : data.filters) {
            ItemNbtFilterRepair.repair(entry, previewStack);
        }
    }

    private ItemStack resolvePreviewStack(ItemNbtFilterData data) {
        if (!data.previewItem.isEmpty()) {
            return data.previewItem;
        }
        if (!cachedPreviewItem.isEmpty()) {
            return cachedPreviewItem;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack main = player.getMainHandItem();
        return main.isEmpty() ? player.getOffhandItem() : main;
    }

    @Override
    public ItemNbtFilterData copy(ItemNbtFilterData value) {
        return value == null ? new ItemNbtFilterData() : value.copy();
    }

    @Override
    public void addInfo(TooltipList list) {
        super.addInfo(list);
        list.add(Component.translatable("morequesttypes.config.item_nbt_matching.info"));
    }
}
