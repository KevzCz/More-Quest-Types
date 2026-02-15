package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import dev.ftb.mods.ftblibrary.config.DoubleConfig;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.config.ui.resource.SelectItemStackScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ItemWithDropRateConfig extends ConfigValue<ItemWithDropRateConfig.ItemDrop> {

    public static class ItemDrop {
        public ItemStack stack;
        public double dropRate;

        public ItemDrop(ItemStack stack, double dropRate) {
            this.stack = stack;
            this.dropRate = Math.max(0.0, Math.min(1.0, dropRate));
        }

        public ItemDrop() {
            this(ItemStack.EMPTY, 1.0);
        }

        public String serialize() {
            if (stack.isEmpty()) return "";

            try {
                var level = Minecraft.getInstance().level;
                if (level != null) {
                    Tag fullTag = stack.save(level.registryAccess());
                    return fullTag.toString() + "|" + dropRate;
                }
            } catch (Exception e) {
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            StringBuilder result = new StringBuilder(itemId.toString());
            if (stack.getCount() > 1) {
                result.append(" ").append(stack.getCount());
            }
            result.append("|").append(dropRate);
            return result.toString();
        }

        public static ItemDrop deserialize(String value) {
            if (value == null || value.isEmpty()) {
                return new ItemDrop();
            }

            String[] parts = value.split("\\|");
            String itemData = parts[0];
            double dropRate = parts.length > 1 ? parseDropRate(parts[1]) : 1.0;

            ItemStack stack = parseItemStack(itemData);
            return new ItemDrop(stack, dropRate);
        }

        private static double parseDropRate(String value) {
            try {
                return Math.max(0.0, Math.min(1.0, Double.parseDouble(value)));
            } catch (Exception e) {
                return 1.0;
            }
        }

        private static ItemStack parseItemStack(String value) {
            try {
                CompoundTag fullTag = TagParser.parseTag(value);
                if (fullTag.contains("id")) {
                    var level = Minecraft.getInstance().level;
                    if (level != null) {
                        return ItemStack.parseOptional(level.registryAccess(), fullTag);
                    }
                }
            } catch (Exception ignored) {
            }

            try {
                String[] itemParts = value.split("\\s+", 2);
                ResourceLocation itemId = ResourceLocation.tryParse(itemParts[0]);
                if (itemId == null) return ItemStack.EMPTY;

                var item = BuiltInRegistries.ITEM.get(itemId);
                if (item == null || item == Items.AIR) return ItemStack.EMPTY;

                int count = 1;
                CompoundTag nbt = null;

                if (itemParts.length > 1) {
                    String[] countAndNbt = itemParts[1].split("\\s+", 2);
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

                ItemStack stack = new ItemStack(item, count);

                if (nbt != null && !nbt.isEmpty()) {
                    try {
                        DataComponentPatch patch = DataComponentPatch.CODEC
                                .parse(NbtOps.INSTANCE, nbt)
                                .result()
                                .orElse(DataComponentPatch.EMPTY);
                        stack.applyComponents(patch);
                    } catch (Exception ignored) {
                    }
                }

                return stack;
            } catch (Exception e) {
                return ItemStack.EMPTY;
            }
        }
    }

    public ItemWithDropRateConfig() {
    }

    @Override
    public Color4I getColor(@Nullable ItemDrop v) {
        return Color4I.WHITE;
    }

    @Override
    public Component getStringForGUI(@Nullable ItemDrop v) {
        if (v == null || v.stack.isEmpty()) {
            return Component.literal("None").withStyle(ChatFormatting.GRAY);
        }

        Component itemName = v.stack.getHoverName();
        int count = v.stack.getCount();
        String chanceStr = String.format("%.0f%%", v.dropRate * 100);

        if (count > 1) {
            return Component.literal(count + "x ").append(itemName)
                    .append(Component.literal(" (" + chanceStr + ")").withStyle(ChatFormatting.GOLD));
        }
        return itemName.copy().append(Component.literal(" (" + chanceStr + ")").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public Icon getIcon(@Nullable ItemDrop v) {
        if (v == null || v.stack.isEmpty()) {
            return ItemIcon.getItemIcon(Items.BARRIER);
        }
        return ItemIcon.getItemIcon(v.stack);
    }

    @Override
    public void onClicked(Widget clicked, MouseButton button, ConfigCallback callback) {
        if (!getCanEdit()) {
            return;
        }

        ItemDrop current = getValue();

        if (button == MouseButton.LEFT) {
            var tempConfig = new dev.ftb.mods.ftblibrary.config.ItemStackConfig(false, true);
            tempConfig.setValue(current != null ? current.stack : ItemStack.EMPTY);

            new SelectItemStackScreen(tempConfig, accepted -> {
                if (accepted) {
                    ItemStack selectedStack = tempConfig.getValue();

                    if (selectedStack.isEmpty()) {
                        setCurrentValue(new ItemDrop());
                        callback.save(true);
                        return;
                    }

                    double currentRate = (current != null) ? current.dropRate : 1.0;

                    ConfigGroup tempGroup = new ConfigGroup("drop_rate_config", accepted2 -> {
                        if (accepted2) {
                            callback.save(true);
                        } else {
                            callback.save(false);
                        }
                    });

                    DoubleConfig dropRateConfig = tempGroup.addDouble("drop_rate", currentRate,
                            newRate -> {
                                setCurrentValue(new ItemDrop(selectedStack, newRate));
                            }, currentRate, 0.0, 1.0);
                    dropRateConfig.setNameKey("morequesttypes.config.drop_rate");

                    new EditConfigScreen(tempGroup).openGui();
                } else {
                    callback.save(false);
                }
            }).openGui();
        } else if (button == MouseButton.RIGHT && current != null && !current.stack.isEmpty()) {
            ConfigGroup tempGroup = new ConfigGroup("drop_rate_config", accepted -> {
                callback.save(accepted);
            });

            DoubleConfig dropRateConfig = tempGroup.addDouble("drop_rate", current.dropRate,
                    newRate -> {
                        setCurrentValue(new ItemDrop(current.stack.copy(), newRate));
                    }, current.dropRate, 0.0, 1.0);
            dropRateConfig.setNameKey("morequesttypes.config.drop_rate");

            new EditConfigScreen(tempGroup).openGui();
        }
    }

    @Override
    public ItemDrop copy(ItemDrop value) {
        if (value == null) return new ItemDrop();
        return new ItemDrop(value.stack.copy(), value.dropRate);
    }

    @Override
    public void addInfo(dev.ftb.mods.ftblibrary.util.TooltipList list) {
        super.addInfo(list);
        list.add(Component.translatable("morequesttypes.config.item_with_drop_rate.left_click"));
        list.add(Component.translatable("morequesttypes.config.item_with_drop_rate.right_click"));
    }
}