package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.util.ItemNbtFilterData;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ItemNbtMatcherConfig extends ConfigValue<ItemNbtFilterData> {
    private ItemStack initialPreview = ItemStack.EMPTY;

    public ItemNbtMatcherConfig() {
    }

    public void setInitialPreview(ItemStack stack) {
        initialPreview = stack != null ? stack.copy() : ItemStack.EMPTY;
        if (!initialPreview.isEmpty()) {
            initialPreview.setCount(1);
        }
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
        if (!initialPreview.isEmpty()
                && (current.previewItem.isEmpty() || current.previewItem.getItem() != initialPreview.getItem())) {
            current.previewItem = initialPreview.copy();
        }

        new ItemNbtMatcherEditScreen(current, initialPreview, accepted -> {
            if (accepted) {
                setValue(current);
                callback.save(true);
            } else {
                callback.save(false);
            }
        }).openGui();
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
