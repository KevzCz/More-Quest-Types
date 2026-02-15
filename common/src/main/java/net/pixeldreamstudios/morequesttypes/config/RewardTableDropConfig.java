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
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class RewardTableDropConfig extends ConfigValue<RewardTableDropConfig.RewardTableDrop> {

    public enum TriggerMode {
        RANDOM("random"),
        ALL("all");

        private final String id;

        TriggerMode(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    public static class RewardTableDrop {
        public String rewardTableId;
        public double dropRate;
        public TriggerMode mode;

        public RewardTableDrop(String rewardTableId, double dropRate, TriggerMode mode) {
            this.rewardTableId = rewardTableId == null ? "" : rewardTableId;
            this.dropRate = Math.max(0.0, Math.min(1.0, dropRate));
            this.mode = mode == null ? TriggerMode.RANDOM : mode;
        }

        public RewardTableDrop() {
            this("", 1.0, TriggerMode.RANDOM);
        }

        public String serialize() {
            if (rewardTableId.isEmpty()) return "";
            return rewardTableId + "|" + dropRate + "|" + mode.getId();
        }

        public static RewardTableDrop deserialize(String value) {
            if (value == null || value.isEmpty()) {
                return new RewardTableDrop();
            }

            String[] parts = value.split("\\|");
            String tableId = parts[0];
            double dropRate = parts.length > 1 ? parseDropRate(parts[1]) : 1.0;
            TriggerMode mode = parts.length > 2 ? parseMode(parts[2]) : TriggerMode.RANDOM;

            return new RewardTableDrop(tableId, dropRate, mode);
        }

        private static double parseDropRate(String value) {
            try {
                return Math.max(0.0, Math.min(1.0, Double.parseDouble(value)));
            } catch (Exception e) {
                return 1.0;
            }
        }

        private static TriggerMode parseMode(String value) {
            try {
                return TriggerMode.valueOf(value.toUpperCase());
            } catch (Exception e) {
                return TriggerMode.RANDOM;
            }
        }
    }

    public RewardTableDropConfig() {
    }

    @Override
    public Color4I getColor(@Nullable RewardTableDrop v) {
        return Color4I.WHITE;
    }

    @Override
    public Component getStringForGUI(@Nullable RewardTableDrop v) {
        if (v == null || v.rewardTableId.isEmpty()) {
            return Component.literal("None").withStyle(ChatFormatting.GRAY);
        }

        RewardTable table = ClientQuestFile.INSTANCE.getRewardTable(ClientQuestFile.INSTANCE.getID(v.rewardTableId));
        Component tableName = table != null ? table.getTitle() : Component.literal(v.rewardTableId);
        String chanceStr = String.format("%.0f%%", v.dropRate * 100);
        String modeStr = v.mode == TriggerMode.ALL ? "All" : "Random";

        return tableName.copy()
                .append(Component.literal(" [" + modeStr + ", " + chanceStr + "]").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public Icon getIcon(@Nullable RewardTableDrop v) {
        if (v == null || v.rewardTableId.isEmpty()) {
            return Icons.DICE;
        }

        RewardTable table = ClientQuestFile.INSTANCE.getRewardTable(ClientQuestFile.INSTANCE.getID(v.rewardTableId));
        return table != null ? table.getIcon() : Icons.DICE;
    }

    @Override
    public void onClicked(Widget clicked, MouseButton button, ConfigCallback callback) {
        if (!getCanEdit()) {
            return;
        }

        RewardTableDrop current = getValue();

        if (button == MouseButton.LEFT) {
            List<String> tableIds = new ArrayList<>();
            ClientQuestFile.INSTANCE.getRewardTables().forEach(table -> {
                tableIds.add(table.getCodeString());
            });

            if (tableIds.isEmpty()) {
                callback.save(false);
                return;
            }

            String currentTableId = current != null && !current.rewardTableId.isEmpty() ? current.rewardTableId : tableIds.get(0);

            NameMap<String> nameMap = NameMap.of(currentTableId, tableIds)
                    .name(id -> {
                        RewardTable table = ClientQuestFile.INSTANCE.getRewardTable(ClientQuestFile.INSTANCE.getID(id));
                        return table != null ? table.getTitle() : Component.literal(id);
                    })
                    .icon(id -> {
                        RewardTable table = ClientQuestFile.INSTANCE.getRewardTable(ClientQuestFile.INSTANCE.getID(id));
                        return table != null ? table.getIcon() : Icons.DICE;
                    })
                    .create();

            ConfigGroup tempGroup = new ConfigGroup("reward_table_drop_config", accepted -> {
                if (accepted) {
                    callback.save(true);
                } else {
                    callback.save(false);
                }
            });

            tempGroup.addEnum("reward_table", currentTableId, selectedId -> {
                double rate = current != null ? current.dropRate : 1.0;
                TriggerMode mode = current != null ? current.mode : TriggerMode.RANDOM;
                setValue(new RewardTableDrop(selectedId, rate, mode));
            }, nameMap).setNameKey("morequesttypes.config.reward_table");

            double currentRate = current != null ? current.dropRate : 1.0;
            tempGroup.addDouble("drop_rate", currentRate, newRate -> {
                RewardTableDrop val = getValue();
                if (val != null) {
                    setValue(new RewardTableDrop(val.rewardTableId, newRate, val.mode));
                }
            }, currentRate, 0.0, 1.0).setNameKey("morequesttypes.config.drop_rate");

            TriggerMode currentMode = current != null ? current.mode : TriggerMode.RANDOM;
            NameMap<TriggerMode> modeMap = NameMap.of(currentMode, TriggerMode.values())
                    .name(mode -> Component.translatable("morequesttypes.config.trigger_mode." + mode.getId()))
                    .create();

            tempGroup.addEnum("trigger_mode", currentMode, newMode -> {
                RewardTableDrop val = getValue();
                if (val != null) {
                    setValue(new RewardTableDrop(val.rewardTableId, val.dropRate, newMode));
                }
            }, modeMap).setNameKey("morequesttypes.config.trigger_mode");

            new EditConfigScreen(tempGroup).openGui();

        } else if (button == MouseButton.RIGHT && current != null && !current.rewardTableId.isEmpty()) {
            ConfigGroup tempGroup = new ConfigGroup("reward_table_drop_config", accepted -> {
                callback.save(accepted);
            });

            tempGroup.addDouble("drop_rate", current.dropRate, newRate -> {
                setValue(new RewardTableDrop(current.rewardTableId, newRate, current.mode));
            }, current.dropRate, 0.0, 1.0).setNameKey("morequesttypes.config.drop_rate");

            NameMap<TriggerMode> modeMap = NameMap.of(current.mode, TriggerMode.values())
                    .name(mode -> Component.translatable("morequesttypes.config.trigger_mode." + mode.getId()))
                    .create();

            tempGroup.addEnum("trigger_mode", current.mode, newMode -> {
                setValue(new RewardTableDrop(current.rewardTableId, current.dropRate, newMode));
            }, modeMap).setNameKey("morequesttypes.config.trigger_mode");

            new EditConfigScreen(tempGroup).openGui();
        }
    }

    @Override
    public RewardTableDrop copy(RewardTableDrop value) {
        if (value == null) return new RewardTableDrop();
        return new RewardTableDrop(value.rewardTableId, value.dropRate, value.mode);
    }

    @Override
    public void addInfo(dev.ftb.mods.ftblibrary.util.TooltipList list) {
        super.addInfo(list);
        list.add(Component.translatable("morequesttypes.config.reward_table_drop.left_click"));
        list.add(Component.translatable("morequesttypes.config.reward_table_drop.right_click"));
    }
}