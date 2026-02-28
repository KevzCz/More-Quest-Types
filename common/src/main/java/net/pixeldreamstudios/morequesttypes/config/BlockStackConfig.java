package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import dev.ftb.mods.ftblibrary.config.ResourceConfigValue;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.config.ui.resource.SelectableResource;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalLong;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class BlockStackConfig extends ResourceConfigValue<ItemStack> {

    private SelectableResource<ItemStack> resource = SelectableResource.item(ItemStack.EMPTY);
    private Consumer<String> blockIdSetter = null;

    public BlockStackConfig() {
    }

    public BlockStackConfig(Consumer<String> blockIdSetter) {
        this.blockIdSetter = blockIdSetter;
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

        if (button == MouseButton.LEFT) {
            new SelectBlockItemStackScreen(this, callback).openGui();
        } else if (button == MouseButton.MIDDLE) {
            openManualInput(callback);
        }
    }

    private void openManualInput(ConfigCallback callback) {
        String currentId = "";
        if (!getValue().isEmpty()) {
            ResourceLocation resLoc = BuiltInRegistries.ITEM.getKey(getValue().getItem());
            currentId = resLoc.toString();
        }

        ConfigGroup manualGroup = new ConfigGroup("manual_block_input", accepted -> {
            callback.save(accepted);
        });

        manualGroup.addString("block_id", currentId, this::trySetBlockFromId, currentId)
                .setNameKey("morequesttypes.config.block_stack.manual_id");

        new EditConfigScreen(manualGroup).openGui();
    }

    private void trySetBlockFromId(String inputBlockId) {
        if (inputBlockId == null || inputBlockId.trim().isEmpty()) {
            setValue(ItemStack.EMPTY);
            setResource(SelectableResource.item(ItemStack.EMPTY));
            if (blockIdSetter != null) {
                blockIdSetter.accept("");
            }
            return;
        }

        ResourceLocation resourceLocation = ResourceLocation.tryParse(inputBlockId.trim());
        if (resourceLocation == null) {
            return;
        }

        var item = BuiltInRegistries.ITEM.get(resourceLocation);
        if (item != Items.AIR && item instanceof BlockItem) {
            ItemStack stack = new ItemStack(item);
            setValue(stack);
            setResource(SelectableResource.item(stack));
            if (blockIdSetter != null) {
                blockIdSetter.accept("");
            }
            return;
        }

        var block = BuiltInRegistries.BLOCK.get(resourceLocation);
        if (block != Blocks.AIR) {
            if (blockIdSetter != null) {
                blockIdSetter.accept(resourceLocation.toString());
                setValue(ItemStack.EMPTY);
                setResource(SelectableResource.item(ItemStack.EMPTY));
            }
        }
    }

    @Override
    public ItemStack copy(ItemStack value) {
        if (value == null || value.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return value.copy();
    }

    @Override
    public void addInfo(TooltipList list) {
        super.addInfo(list);
        list.add(Component.translatable("morequesttypes.config.block_stack.info"));
        list.add(Component.translatable("morequesttypes.config.block_stack.manual_hint")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ConfigValue<ItemStack> init(@Nullable ConfigGroup group, String id, @Nullable ItemStack currentValue, Consumer<ItemStack> setter, @Nullable ItemStack defaultValue) {
        super.init(group, id, currentValue, setter, defaultValue);
        this.resource = SelectableResource.item(currentValue != null ? currentValue : ItemStack.EMPTY);
        return this;
    }
}