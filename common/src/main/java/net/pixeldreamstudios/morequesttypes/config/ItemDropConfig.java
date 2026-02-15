package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public class ItemDropConfig extends StringConfig {
    private ItemStack cachedStack = ItemStack.EMPTY;
    private String lastValue = "";

    public ItemDropConfig() {
        super();
    }

    @Override
    public Component getStringForGUI(@Nullable String v) {
        if (v == null || v.isEmpty()) {
            return Component.literal("None").withStyle(ChatFormatting.GRAY);
        }

        parseValue(v);

        if (!cachedStack.isEmpty()) {
            Component itemName = cachedStack.getHoverName();
            int count = getCountFromString(v);

            if (count > 1) {
                return Component.literal(count + "x ").append(itemName);
            }
            return itemName;
        }

        return Component.literal(v).withStyle(ChatFormatting.RED);
    }

    @Override
    public Optional<Icon> getIcon(@Nullable String v) {
        if (v == null || v.isEmpty()) {
            return Optional.of(ItemIcon.getItemIcon(Items.BARRIER));
        }

        parseValue(v);

        if (!cachedStack.isEmpty()) {
            return Optional.of(ItemIcon.getItemIcon(cachedStack));
        }

        return Optional.of(ItemIcon.getItemIcon(Items.BARRIER));
    }

    @Override
    public void onClicked(Panel panel, ConfigGroup group, ConfigCallback callback, MouseButton button) {
        panel.getGui().openYesNo(Component.translatable("ftbquests.gui.select_item"), Component.empty(), () -> {
            panel.openItemSelector(this::selectItemCallback);
        });
    }

    private void selectItemCallback(ItemStack selectedStack) {
        if (selectedStack.isEmpty()) {
            setCurrentValue("");
            return;
        }

        StringBuilder result = new StringBuilder();

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(selectedStack.getItem());
        result.append(itemId.toString());

        if (selectedStack.getCount() > 1) {
            result.append(" ").append(selectedStack.getCount());
        }

        CompoundTag components = selectedStack.getComponentsPatch().toTag();
        if (!components.isEmpty()) {
            result.append(" ").append(components.toString());
        }

        setCurrentValue(result.toString());
    }

    private void parseValue(String value) {
        if (value.equals(lastValue) && !cachedStack.isEmpty()) {
            return;
        }

        lastValue = value;
        cachedStack = ItemStack.EMPTY;

        if (value == null || value.isEmpty()) {
            return;
        }

        try {
            String[] parts = value.split("\\s+", 2);
            ResourceLocation itemId = ResourceLocation.tryParse(parts[0]);
            if (itemId == null) return;

            var item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null || item == Items.AIR) return;

            int count = 1;
            CompoundTag nbt = null;

            if (parts.length > 1) {
                String[] countAndNbt = parts[1].split("\\s+", 2);
                try {
                    count = Integer.parseInt(countAndNbt[0]);
                } catch (NumberFormatException ignored) {
                }

                if (countAndNbt.length > 1) {
                    try {
                        nbt = TagParser.parseTag(countAndNbt[1]);
                    } catch (Exception ignored) {
                    }
                }
            }

            cachedStack = new ItemStack(item, count);

            if (nbt != null && !nbt.isEmpty()) {
                try {
                    net.minecraft.core.component.DataComponentPatch patch =
                            net.minecraft.core.component.DataComponentPatch.CODEC
                                    .parse(net.minecraft.nbt.NbtOps.INSTANCE, nbt)
                                    .result()
                                    .orElse(net.minecraft.core.component.DataComponentPatch.EMPTY);
                    cachedStack.applyComponents(patch);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            cachedStack = ItemStack.EMPTY;
        }
    }

    private int getCountFromString(String value) {
        if (value == null || value.isEmpty()) return 1;

        try {
            String[] parts = value.split("\\s+", 2);
            if (parts.length > 1) {
                String[] countAndNbt = parts[1].split("\\s+", 2);
                return Integer.parseInt(countAndNbt[0]);
            }
        } catch (Exception ignored) {
        }
        return 1;
    }

    @Override
    public void draw(GuiGraphics graphics, int x, int y, int w, int h) {
        String currentValue = getCurrentValue();
        parseValue(currentValue);

        if (!cachedStack.isEmpty()) {
            graphics.renderItem(cachedStack, x + 2, y + 2);

            int count = cachedStack.getCount();
            if (count > 1) {
                graphics.drawString(graphics.pose().last().pose(),
                        String.valueOf(count),
                        x + 17 - graphics.pose().last().pose().toString().length() * 6,
                        y + 11,
                        0xFFFFFF,
                        true);
            }
        }

        Component text = getStringForGUI(currentValue);
        graphics.drawString(graphics.pose().last().pose(),
                text,
                x + 22,
                y + (h - 8) / 2,
                0xFFFFFFFF,
                true);
    }
}