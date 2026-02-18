package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import dev.ftb.mods.ftblibrary.config.ResourceConfigValue;
import dev.ftb.mods.ftblibrary.config.ui.resource.SelectableResource;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalLong;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class BlockStackConfig extends ResourceConfigValue<ItemStack> {

    private SelectableResource<ItemStack> resource = SelectableResource.item(ItemStack.EMPTY);

    public BlockStackConfig() {
    }

    @Override
    public SelectableResource<ItemStack> getResource() {
        return resource;
    }

    @Override
    public boolean setResource(SelectableResource<ItemStack> selectedStack) {
        if (selectedStack != null && !selectedStack.isEmpty()) {
            ItemStack stack = selectedStack.resource();
            if (!(stack.getItem() instanceof BlockItem)) {
                return false;
            }
        }

        SelectableResource<ItemStack> old = this.resource;
        this.resource = selectedStack != null ? selectedStack : SelectableResource.item(ItemStack.EMPTY);

        if (this.resource != null) {
            this.value = this.resource.resource();
        }

        return !old.equals(this.resource);
    }

    @Override
    public boolean allowEmptyResource() {
        return true;
    }

    @Override
    public OptionalLong fixedResourceSize() {
        return OptionalLong.empty();
    }

    @Override
    public boolean isEmpty() {
        return resource == null || resource.isEmpty();
    }

    @Override
    public boolean canHaveNBT() {
        return false;
    }

    @Override
    public boolean allowResource(ItemStack itemStack) {
        return itemStack.isEmpty() || itemStack.getItem() instanceof BlockItem;
    }

    @Override
    public Color4I getColor(@Nullable ItemStack v) {
        return Color4I.WHITE;
    }

    @Override
    public Component getStringForGUI(@Nullable ItemStack v) {
        if (v == null || v.isEmpty()) {
            return Component.literal("None");
        }
        return v.getHoverName();
    }

    @Override
    public Icon getIcon(@Nullable ItemStack v) {
        if (v == null || v.isEmpty()) {
            return ItemIcon.getItemIcon(Items.BARRIER);
        }
        return ItemIcon.getItemIcon(v);
    }

    @Override
    public void onClicked(Widget clickedWidget, MouseButton button, ConfigCallback callback) {
        if (!getCanEdit()) {
            return;
        }

        new SelectBlockItemStackScreen(this, callback).openGui();
    }

    @Override
    public ItemStack copy(ItemStack value) {
        if (value == null || value.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return value.copy();
    }

    @Override
    public void addInfo(dev.ftb.mods.ftblibrary.util.TooltipList list) {
        super.addInfo(list);
        list.add(Component.translatable("morequesttypes.config.block_stack.info"));
    }

    @Override
    public ConfigValue<ItemStack> init(@Nullable dev.ftb.mods.ftblibrary.config.ConfigGroup group, String id, @Nullable ItemStack currentValue, Consumer<ItemStack> setter, @Nullable ItemStack defaultValue) {
        super.init(group, id, currentValue, setter, defaultValue);
        this.resource = SelectableResource.item(currentValue != null ? currentValue : ItemStack.EMPTY);
        return this;
    }
}