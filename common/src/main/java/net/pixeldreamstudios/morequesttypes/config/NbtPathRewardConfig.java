package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.rewards.NbtPathReward;
import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class NbtPathRewardConfig extends ConfigValue<NbtPathRewardConfig.NbtPathData> {

    private static ItemStack cachedPreviewItem = ItemStack.EMPTY;

    public static class NbtPathData {
        public NbtPathReward.Operation operation = NbtPathReward.Operation.ADD;
        public String path = "";
        public boolean checkExists = false;
        public List<String> nbtEntries = new ArrayList<>();
        public NbtPathReward.TargetSlot targetSlot = NbtPathReward.TargetSlot.MAINHAND;
        public ItemStack targetItem = ItemStack.EMPTY;
        public boolean isValue = false;

        public NbtPathReward.ConditionType conditionType = NbtPathReward.ConditionType.NONE;
        public String conditionPath = "";
        public List<String> conditionEntries = new ArrayList<>();
        public int conditionsMatchNumber = -1;
        public ComparisonMode comparisonMode = ComparisonMode.EQUALS;
        public int comparisonFirst = 0;
        public int comparisonSecond = 0;

        public NbtPathData copy() {
            NbtPathData data = new NbtPathData();
            data.operation = this.operation;
            data.path = this.path;
            data.checkExists = this.checkExists;
            data.nbtEntries = new ArrayList<>(this.nbtEntries);
            data.targetSlot = this.targetSlot;
            data.targetItem = this.targetItem.copy();
            data.isValue = this.isValue;
            data.conditionType = this.conditionType;
            data.conditionPath = this.conditionPath;
            data.conditionEntries = new ArrayList<>(this.conditionEntries);
            data.conditionsMatchNumber = this.conditionsMatchNumber;
            data.comparisonMode = this.comparisonMode;
            data.comparisonFirst = this.comparisonFirst;
            data.comparisonSecond = this.comparisonSecond;
            return data;
        }
    }

    public NbtPathRewardConfig() {
    }

    @Override
    public Color4I getColor(@Nullable NbtPathData v) {
        return Color4I.WHITE;
    }

    @Override
    public Component getStringForGUI(@Nullable NbtPathData v) {
        if (v == null || v.path.isEmpty()) {
            return Component.literal("Not configured").withStyle(ChatFormatting.GRAY);
        }

        return Component.literal(v.operation.name().toLowerCase() + " → " + v.path)
                .withStyle(ChatFormatting.YELLOW);
    }

    @Override
    public Icon getIcon(@Nullable NbtPathData v) {
        return Icons.SETTINGS;
    }

    @Override
    public void onClicked(Widget clicked, MouseButton button, ConfigCallback callback) {
        if (!getCanEdit()) {
            return;
        }

        NbtPathData current = getValue() != null ? getValue().copy() : new NbtPathData();

        ConfigGroup itemSelectionGroup = new ConfigGroup("item_selection", accepted -> {
            if (accepted) {
                openMainConfig(current, callback);
            } else {
                callback.save(false);
            }
        });

        dev.ftb.mods.ftbquests.client.ConfigIconItemStack itemConfig = new dev.ftb.mods.ftbquests.client.ConfigIconItemStack();
        itemSelectionGroup.add("preview_item", itemConfig, cachedPreviewItem, v -> {
            cachedPreviewItem = v.copy();
            if (!cachedPreviewItem.isEmpty()) {
                cachedPreviewItem.setCount(1);
            }
        }, ItemStack.EMPTY).setNameKey("morequesttypes.config.nbt_path.preview_item");

        new EditConfigScreen(itemSelectionGroup).openGui();
    }

    private void openMainConfig(NbtPathData current, ConfigCallback callback) {
        ConfigGroup mainGroup = new ConfigGroup("nbt_path_config", accepted -> {
            if (accepted) {
                setValue(current);
                callback.save(true);
            } else {
                callback.save(false);
            }
        });

        ConfigGroup basicGroup = mainGroup.getOrCreateSubgroup("basic");
        basicGroup.setNameKey("morequesttypes.config.nbt_path.basic");

        var OPS = NameMap.of(current.operation, NbtPathReward.Operation.values()).create();
        basicGroup.addEnum("operation", current.operation, v -> current.operation = v, OPS)
                .setNameKey("morequesttypes.config.nbt_path.operation");

        var SLOTS = NameMap.of(current.targetSlot, NbtPathReward.TargetSlot.values()).create();
        basicGroup.addEnum("target_slot", current.targetSlot, v -> current.targetSlot = v, SLOTS)
                .setNameKey("morequesttypes.config.nbt_path.target_slot");

        dev.ftb.mods.ftbquests.client.ConfigIconItemStack itemConfig = new dev.ftb.mods.ftbquests.client.ConfigIconItemStack();
        basicGroup.add("target_item", itemConfig, current.targetItem, v -> {
            current.targetItem = v.copy();
            if (!current.targetItem.isEmpty()) {
                current.targetItem.setCount(1);
            }
        }, ItemStack.EMPTY).setNameKey("morequesttypes.config.nbt_path.target_item");

        ItemStack previewStack = getPreviewStack(current);
        List<String> availablePaths = extractAvailablePaths(previewStack);

        if (!availablePaths.isEmpty()) {
            PathSelectorConfig pathSelector = new PathSelectorConfig(previewStack);
            basicGroup.add("path", pathSelector, current.path, v -> current.path = v, "")
                    .setNameKey("morequesttypes.config.nbt_path.path");
        } else {
            basicGroup.addString("path", current.path, v -> current.path = v, "")
                    .setNameKey("morequesttypes.config.nbt_path.path");
        }

        basicGroup.addBool("check_exists", current.checkExists, v -> current.checkExists = v, false)
                .setNameKey("morequesttypes.config.nbt_path.check_exists");

        basicGroup.addBool("is_value", current.isValue, v -> current.isValue = v, false)
                .setNameKey("morequesttypes.config.nbt_path.is_value");

        basicGroup.addList("nbt_entries", current.nbtEntries, new dev.ftb.mods.ftblibrary.config.StringConfig(), "")
                .setNameKey("morequesttypes.config.nbt_path.nbt_entries");

        ConfigGroup condGroup = mainGroup.getOrCreateSubgroup("conditions");
        condGroup.setNameKey("morequesttypes.config.nbt_path.conditions");

        var COND_TYPES = NameMap.of(current.conditionType, NbtPathReward.ConditionType.values()).create();
        condGroup.addEnum("condition_type", current.conditionType, v -> current.conditionType = v, COND_TYPES)
                .setNameKey("morequesttypes.config.nbt_path.condition_type");

        if (!availablePaths.isEmpty()) {
            PathSelectorConfig condPathSelector = new PathSelectorConfig(previewStack);
            condGroup.add("condition_path", condPathSelector, current.conditionPath, v -> current.conditionPath = v, "")
                    .setNameKey("morequesttypes.config.nbt_path.condition_path");
        } else {
            condGroup.addString("condition_path", current.conditionPath, v -> current.conditionPath = v, "")
                    .setNameKey("morequesttypes.config.nbt_path.condition_path");
        }

        condGroup.addList("condition_entries", current.conditionEntries, new dev.ftb.mods.ftblibrary.config.StringConfig(), "")
                .setNameKey("morequesttypes.config.nbt_path.condition_entries");

        condGroup.addInt("conditions_match_number", current.conditionsMatchNumber,
                        v -> current.conditionsMatchNumber = v, -1, -1, 999)
                .setNameKey("morequesttypes.config.nbt_path.conditions_match_number");

        var COMP_MODES = NameMap.of(current.comparisonMode, ComparisonMode.values())
                .name(mode -> Component.translatable(mode.getTranslationKey()))
                .create();
        condGroup.addEnum("comparison_mode", current.comparisonMode, v -> current.comparisonMode = v, COMP_MODES)
                .setNameKey("morequesttypes.config.nbt_path.comparison_mode");

        condGroup.addInt("comparison_first", current.comparisonFirst, v -> current.comparisonFirst = v,
                        0, Integer.MIN_VALUE, Integer.MAX_VALUE)
                .setNameKey("morequesttypes.config.nbt_path.comparison_first");

        condGroup.addInt("comparison_second", current.comparisonSecond, v -> current.comparisonSecond = v,
                        0, Integer.MIN_VALUE, Integer.MAX_VALUE)
                .setNameKey("morequesttypes.config.nbt_path.comparison_second");

        new EditConfigScreen(mainGroup).openGui();
    }

    private ItemStack getPreviewStack(NbtPathData data) {
        if (!cachedPreviewItem.isEmpty()) {
            return cachedPreviewItem;
        }

        if (!data.targetItem.isEmpty()) {
            return data.targetItem;
        }

        var player = Minecraft.getInstance().player;
        if (player == null) return ItemStack.EMPTY;

        return switch (data.targetSlot) {
            case MAINHAND -> player.getMainHandItem();
            case OFFHAND -> player.getOffhandItem();
            case HEAD -> player.getItemBySlot(EquipmentSlot.HEAD);
            case CHEST -> player.getItemBySlot(EquipmentSlot.CHEST);
            case LEGS -> player.getItemBySlot(EquipmentSlot.LEGS);
            case FEET -> player.getItemBySlot(EquipmentSlot.FEET);
        };
    }

    private List<String> extractAvailablePaths(ItemStack stack) {
        List<String> paths = new ArrayList<>();

        if (stack.isEmpty()) return paths;

        try {
            var player = Minecraft.getInstance().player;
            if (player == null) return paths;

            CompoundTag fullTag = (CompoundTag) stack.save(player.level().registryAccess());

            collectPaths(fullTag, "root", paths);

            CompoundTag components = fullTag.getCompound("components");
            collectPaths(components, "", paths);
        } catch (Exception e) {
        }

        return paths;
    }

    private void collectPaths(CompoundTag tag, String prefix, List<String> paths) {
        for (String key : tag.getAllKeys()) {
            String currentPath = prefix.isEmpty() ? key : prefix + "." + key;
            paths.add(currentPath);

            Tag value = tag.get(key);
            if (value instanceof CompoundTag compound) {
                collectPaths(compound, currentPath, paths);
            }
        }
    }

    private Tag navigateToPath(CompoundTag root, String path) {
        if (path.isEmpty()) return root;

        String[] parts = path.split("\\.");
        Tag current = root;

        for (String part : parts) {
            if (current instanceof CompoundTag compound) {
                current = compound.get(part);
                if (current == null) return null;
            } else {
                return null;
            }
        }

        return current;
    }

    @Override
    public NbtPathData copy(NbtPathData value) {
        if (value == null) return new NbtPathData();
        return value.copy();
    }

    @Override
    public void addInfo(dev.ftb.mods.ftblibrary.util.TooltipList list) {
        super.addInfo(list);
        list.add(Component.translatable("morequesttypes.config.nbt_path.info"));
    }

    @Environment(EnvType.CLIENT)
    public static class PathSelectorConfig extends ConfigValue<String> {
        private final ItemStack previewStack;
        private final List<String> allPaths;

        public PathSelectorConfig(ItemStack previewStack) {
            this.previewStack = previewStack;
            this.allPaths = new ArrayList<>();
            extractAllPaths();
        }

        private void extractAllPaths() {
            if (previewStack.isEmpty()) return;

            try {
                var player = Minecraft.getInstance().player;
                if (player == null) return;

                CompoundTag fullTag = (CompoundTag) previewStack.save(player.level().registryAccess());

                collectAllPaths(fullTag, "root", allPaths);

                CompoundTag components = fullTag.getCompound("components");
                collectAllPaths(components, "", allPaths);
            } catch (Exception e) {
            }
        }

        private void collectAllPaths(CompoundTag tag, String prefix, List<String> paths) {
            for (String key : tag.getAllKeys()) {
                String currentPath = prefix.isEmpty() ? key : prefix + "." + key;
                paths.add(currentPath);

                Tag value = tag.get(key);
                if (value instanceof CompoundTag compound) {
                    collectAllPaths(compound, currentPath, paths);
                }
            }
        }

        @Override
        public Color4I getColor(@Nullable String v) {
            return Color4I.WHITE;
        }

        @Override
        public Component getStringForGUI(@Nullable String v) {
            if (v == null || v.isEmpty()) {
                return Component.literal("(None)").withStyle(ChatFormatting.GRAY);
            }
            return Component.literal(v).withStyle(ChatFormatting.YELLOW);
        }

        @Override
        public Icon getIcon(@Nullable String v) {
            return Icons.COMPASS;
        }

        @Override
        public void onClicked(Widget clicked, MouseButton button, ConfigCallback callback) {
            if (!getCanEdit()) {
                return;
            }

            String currentPath = getValue() != null ? getValue() : "";

            if (button == MouseButton.LEFT) {
                if (allPaths.isEmpty()) {
                    callback.save(false);
                    return;
                }

                int currentIndex = allPaths.indexOf(currentPath);
                int nextIndex = (currentIndex + 1) % allPaths.size();
                String nextPath = allPaths.get(nextIndex);

                setValue(nextPath);
                callback.save(true);

            } else if (button == MouseButton.RIGHT) {
                showPathContents(currentPath, callback);
            } else if (button == MouseButton.MIDDLE) {
                ConfigGroup editGroup = new ConfigGroup("edit_path", accepted -> {
                    if (accepted) {
                        callback.save(true);
                    } else {
                        callback.save(false);
                    }
                });

                editGroup.addString("path_manual", currentPath, v -> setValue(v), currentPath)
                        .setNameKey("morequesttypes.config.nbt_path.path_manual");

                new EditConfigScreen(editGroup).openGui();
            }
        }

        private void showPathContents(String path, ConfigCallback callback) {
            ConfigGroup contentsGroup = new ConfigGroup("path_contents", accepted -> {
                callback.save(false);
            });

            contentsGroup.addString("current_path", path, v -> {}, path)
                    .setNameKey("morequesttypes.config.nbt_path.viewing_path")
                    .setCanEdit(false);

            try {
                var player = Minecraft.getInstance().player;
                if (player == null) {
                    new EditConfigScreen(contentsGroup).openGui();
                    return;
                }

                CompoundTag fullTag = (CompoundTag) previewStack.save(player.level().registryAccess());

                boolean isRoot = path.startsWith("root.");
                String actualPath = isRoot ? path.substring(5) : path;
                CompoundTag targetTag = isRoot ? fullTag : fullTag.getCompound("components");

                Tag result = navigateToPath(targetTag, actualPath);

                if (result instanceof CompoundTag compound) {
                    ConfigGroup compoundGroup = contentsGroup.getOrCreateSubgroup("compound_contents");
                    compoundGroup.setNameKey("morequesttypes.config.nbt_path.compound_contents");

                    ConfigGroup howToGroup = compoundGroup.getOrCreateSubgroup("how_to");
                    howToGroup.setNameKey("morequesttypes.config.nbt_path.how_to_modify");

                    howToGroup.addString("add_example", generateAddExample(compound), v -> {}, "")
                            .setNameKey("morequesttypes.config.nbt_path.how_to.add_to_compound")
                            .setCanEdit(false);

                    howToGroup.addString("set_example", generateSetExample(compound), v -> {}, "")
                            .setNameKey("morequesttypes.config.nbt_path.how_to.set_compound")
                            .setCanEdit(false);

                    int count = 0;
                    for (String key : compound.getAllKeys()) {
                        if (count >= 50) {
                            compoundGroup.addString("more", "... +" + (compound.size() - 50) + " more entries", v -> {}, "")
                                    .setCanEdit(false);
                            break;
                        }

                        Tag value = compound.get(key);
                        String display = key + ": " + value.toString();
                        compoundGroup.addString("key_" + count, display, v -> {}, display)
                                .setCanEdit(false);
                        count++;
                    }
                } else if (result instanceof ListTag list) {
                    ConfigGroup listGroup = contentsGroup.getOrCreateSubgroup("list_contents");
                    listGroup.setNameKey("morequesttypes.config.nbt_path.list_contents");

                    ConfigGroup howToGroup = listGroup.getOrCreateSubgroup("how_to");
                    howToGroup.setNameKey("morequesttypes.config.nbt_path.how_to_modify");

                    if (!list.isEmpty()) {
                        howToGroup.addString("add_example", generateAddToListExample(list), v -> {}, "")
                                .setNameKey("morequesttypes.config.nbt_path.how_to.add_to_list")
                                .setCanEdit(false);

                        howToGroup.addString("remove_example", generateRemoveFromListExample(list), v -> {}, "")
                                .setNameKey("morequesttypes.config.nbt_path.how_to.remove_from_list")
                                .setCanEdit(false);
                    }

                    howToGroup.addString("set_example", generateSetListExample(list), v -> {}, "")
                            .setNameKey("morequesttypes.config.nbt_path.how_to.set_list")
                            .setCanEdit(false);

                    listGroup.addString("size", "Size: " + list.size(), v -> {}, "")
                            .setCanEdit(false);

                    for (int i = 0; i < Math.min(list.size(), 50); i++) {
                        Tag element = list.get(i);
                        String display = "[" + i + "]: " + element.toString();
                        listGroup.addString("element_" + i, display, v -> {}, display)
                                .setCanEdit(false);
                    }

                    if (list.size() > 50) {
                        listGroup.addString("more", "... +" + (list.size() - 50) + " more elements", v -> {}, "")
                                .setCanEdit(false);
                    }
                } else if (result != null) {
                    ConfigGroup valueGroup = contentsGroup.getOrCreateSubgroup("value_contents");
                    valueGroup.setNameKey("morequesttypes.config.nbt_path.value_contents");

                    ConfigGroup howToGroup = valueGroup.getOrCreateSubgroup("how_to");
                    howToGroup.setNameKey("morequesttypes.config.nbt_path.how_to_modify");

                    howToGroup.addString("set_example", generateSetValueExample(result), v -> {}, "")
                            .setNameKey("morequesttypes.config.nbt_path.how_to.set_value")
                            .setCanEdit(false);

                    if (result instanceof net.minecraft.nbt.NumericTag) {
                        howToGroup.addString("add_example", generateAddValueExample(result), v -> {}, "")
                                .setNameKey("morequesttypes.config.nbt_path.how_to.add_to_value")
                                .setCanEdit(false);

                        howToGroup.addString("subtract_example", generateSubtractValueExample(result), v -> {}, "")
                                .setNameKey("morequesttypes.config.nbt_path.how_to.subtract_from_value")
                                .setCanEdit(false);
                    }

                    valueGroup.addString("value", result.toString(), v -> {}, result.toString())
                            .setNameKey("morequesttypes.config.nbt_path.value")
                            .setCanEdit(false);
                } else {
                    contentsGroup.addString("not_found", "Path not found", v -> {}, "")
                            .setCanEdit(false);
                }
            } catch (Exception e) {
                contentsGroup.addString("error", "Error reading path: " + e.getMessage(), v -> {}, "")
                        .setCanEdit(false);
            }

            new EditConfigScreen(contentsGroup).openGui();
        }

        private String generateAddExample(CompoundTag compound) {
            if (compound.isEmpty()) {
                return "{new_key: \"new_value\"}";
            }
            String firstKey = compound.getAllKeys().iterator().next();
            Tag firstValue = compound.get(firstKey);
            return "{" + firstKey + ": " + firstValue.toString() + "}";
        }

        private String generateSetExample(CompoundTag compound) {
            if (compound.isEmpty()) {
                return "{key: \"value\"}";
            }
            StringBuilder example = new StringBuilder("{");
            int count = 0;
            for (String key : compound.getAllKeys()) {
                if (count > 0) example.append(", ");
                example.append(key).append(": ").append(compound.get(key).toString());
                if (++count >= 3) break;
            }
            example.append("}");
            return example.toString();
        }

        private String generateAddToListExample(ListTag list) {
            if (list.isEmpty()) return "\"new_entry\"";
            return list.get(0).toString();
        }

        private String generateRemoveFromListExample(ListTag list) {
            if (list.isEmpty()) return "\"entry_to_remove\"";
            return list.get(0).toString();
        }

        private String generateSetListExample(ListTag list) {
            if (list.isEmpty()) return "[\"entry1\", \"entry2\"]";
            StringBuilder example = new StringBuilder("[");
            for (int i = 0; i < Math.min(list.size(), 3); i++) {
                if (i > 0) example.append(", ");
                example.append(list.get(i).toString());
            }
            example.append("]");
            return example.toString();
        }

        private String generateSetValueExample(Tag tag) {
            if (tag instanceof net.minecraft.nbt.StringTag stringTag) {
                return stringTag.getAsString();
            }
            return tag.toString();
        }

        private String generateAddValueExample(Tag tag) {
            if (tag instanceof net.minecraft.nbt.IntTag intTag) {
                return String.valueOf(Math.max(1, intTag.getAsInt() / 10));
            } else if (tag instanceof net.minecraft.nbt.DoubleTag doubleTag) {
                return String.valueOf(Math.max(0.1, doubleTag.getAsDouble() / 10));
            }
            return "1";
        }

        private String generateSubtractValueExample(Tag tag) {
            if (tag instanceof net.minecraft.nbt.IntTag intTag) {
                return String.valueOf(Math.max(1, intTag.getAsInt() / 10));
            } else if (tag instanceof net.minecraft.nbt.DoubleTag doubleTag) {
                return String.valueOf(Math.max(0.1, doubleTag.getAsDouble() / 10));
            }
            return "1";
        }

        private Tag navigateToPath(CompoundTag root, String path) {
            if (path.isEmpty()) return root;

            String[] parts = path.split("\\.");
            Tag current = root;

            for (String part : parts) {
                if (current instanceof CompoundTag compound) {
                    current = compound.get(part);
                    if (current == null) return null;
                } else {
                    return null;
                }
            }

            return current;
        }

        @Override
        public void addInfo(dev.ftb.mods.ftblibrary.util.TooltipList list) {
            super.addInfo(list);
            list.add(Component.translatable("morequesttypes.config.nbt_path.path.cycle_hint"));
            list.add(Component.translatable("morequesttypes.config.nbt_path.path.view_hint"));
            list.add(Component.translatable("morequesttypes.config.nbt_path.path.edit_hint"));
        }
    }
}