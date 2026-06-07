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
import net.pixeldreamstudios.morequesttypes.util.ItemNbtFilterRepair;

import org.jetbrains.annotations.Nullable;



import java.util.function.Supplier;



@Environment(EnvType.CLIENT)

public class ItemNbtFilterEntryConfig extends ConfigValue<ItemNbtFilterData.FilterEntry> {

    private final Supplier<ItemStack> previewStackSupplier;



    public ItemNbtFilterEntryConfig(Supplier<ItemStack> previewStackSupplier) {

        this.previewStackSupplier = previewStackSupplier;

    }



    @Override

    public Color4I getColor(@Nullable ItemNbtFilterData.FilterEntry v) {

        if (v == null) {

            return Color4I.GRAY;

        }

        return switch (v.kind) {

            case EXCLUDE -> Color4I.RED;

            case OR -> Color4I.BLUE;

            case REQUIRE -> Color4I.GREEN;

        };

    }



    @Override

    public Component getStringForGUI(@Nullable ItemNbtFilterData.FilterEntry v) {

        if (v == null || v.encode().isBlank()) {

            return Component.literal("(Empty filter)").withStyle(ChatFormatting.GRAY);

        }



        String prefix = switch (v.kind) {

            case EXCLUDE -> "NOT ";

            case OR -> "OR ";

            case REQUIRE -> "";

        };

        return Component.literal(prefix + abbreviate(v.encode(), 56)).withStyle(ChatFormatting.YELLOW);

    }



    private static String abbreviate(String value, int max) {

        String compact = value.replace('\n', ' ').trim();

        if (compact.length() <= max) {

            return compact;

        }

        return compact.substring(0, max - 3) + "...";

    }



    @Override

    public Icon getIcon(@Nullable ItemNbtFilterData.FilterEntry v) {

        return Icons.COMPASS;

    }



    @Override

    public void onClicked(Widget clicked, MouseButton button, ConfigCallback callback) {

        if (!getCanEdit()) {

            return;

        }



        ItemStack previewStack = previewStackSupplier.get();

        ItemNbtFilterData.FilterEntry current = getValue() != null ? getValue().copy() : new ItemNbtFilterData.FilterEntry();
        ItemNbtFilterRepair.repair(current, previewStack);

        ItemNbtFilterEditScreens.FilterEditState state = new ItemNbtFilterEditScreens.FilterEditState(current);



        ItemNbtFilterEditScreens.open(previewStack, state, accepted -> {

            if (accepted) {

                ItemNbtFilterEditScreens.finalizeState(previewStack, state);
                ItemNbtFilterRepair.repair(state.entry, previewStack);

                setValue(state.entry.copy());

            }

            callback.save(accepted);

        });

    }



    @Override

    public ItemNbtFilterData.FilterEntry copy(ItemNbtFilterData.FilterEntry value) {

        return value == null ? new ItemNbtFilterData.FilterEntry() : value.copy();

    }



    @Override

    public void addInfo(TooltipList list) {

        super.addInfo(list);

        list.add(Component.translatable("morequesttypes.config.item_nbt_matching.filter_entry.info"));

        list.add(Component.translatable("morequesttypes.config.item_nbt_matching.placeholders_hint"));

    }

}

