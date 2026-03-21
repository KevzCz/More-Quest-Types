package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.compat.ReskillableCompat;
import net.pixeldreamstudios.morequesttypes.util.ComparisonManager;
import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReskillableTask extends Task {
    public enum Mode {SKILL_LEVEL, TOTAL_LEVEL}

    private Mode mode = Mode.SKILL_LEVEL;
    private ComparisonMode comparisonMode = ComparisonMode.GREATER_OR_EQUAL;
    private int firstNumber = 10;
    private int secondNumber = 20;
    private String skillId = "mining";

    public ReskillableTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.RESKILLABLE;
    }

    @Override
    public long getMaxProgress() {
        return ComparisonManager.getMaxProgress(comparisonMode, firstNumber, secondNumber);
    }

    @Override
    public boolean hideProgressNumbers() {
        return comparisonMode.isRange();
    }

    @Override
    public String formatMaxProgress() {
        if (comparisonMode.isRange()) {
            return "1";
        }
        return Long.toString(getMaxProgress());
    }

    @Environment(EnvType.CLIENT)
    @Override
    public String formatProgress(TeamData teamData, long progress) {
        if (comparisonMode.isRange()) {
            return progress >= 1 ? "1" : "0";
        }
        long shown = Math.max(0, Math.min(progress, getMaxProgress()));
        return Long.toString(shown);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getButtonText() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return Component.literal("?  / " + getMaxProgress());

        long p = TeamData.get(player).getProgress(this);

        if (comparisonMode.isRange()) {
            return Component.literal(p >= 1 ? "✓" : "✗");
        }

        long shown = Math.max(0, Math.min(p, getMaxProgress()));
        return Component.literal(shown + " / " + getMaxProgress());
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return ReskillableTask.getSkillIcon(skillId, getType());
    }

    @Environment(EnvType.CLIENT)
    private static Icon getSkillIcon(String skillId, TaskType taskType) {
        if (skillId == null || skillId.isEmpty()) {
            return taskType.getIconSupplier();
        }

        String normalized = skillId.toLowerCase(Locale.ROOT);
        String iconName = switch (normalized) {
            case "mining" -> "mining";
            case "gathering" -> "gathering";
            case "attack" -> "attack";
            case "defense" -> "defense";
            case "building" -> "building";
            case "farming" -> "farming";
            case "agility" -> "agility";
            case "magic" -> "magic";
            default -> null;
        };

        if (iconName != null) {
            try {
                ResourceLocation iconLocation = ResourceLocation.fromNamespaceAndPath("more_quest_types",
                        "textures/reskillable_compat/" + iconName + ".png");
                return Icon.getIcon(iconLocation);
            } catch (Exception e) {
                return taskType.getIconSupplier();
            }
        }

        if (ReskillableCompat.isLoaded()) {
            try {
                String customIconPath = ReskillableCompat.getSkillIcon(skillId);
                if (customIconPath != null && !customIconPath.isEmpty()) {
                    if (customIconPath.contains(":")) {
                        String[] parts = customIconPath.split(":", 2);
                        String namespace = parts[0];
                        String path = parts[1];
                        if (path.startsWith("textures/") && path.endsWith(".png")) {
                            ResourceLocation iconLocation = ResourceLocation.fromNamespaceAndPath(namespace, path);
                            return Icon.getIcon(iconLocation);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return taskType.getIconSupplier();
    }

    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var MODE_MAP = NameMap.of(Mode.SKILL_LEVEL, Mode.values()).create();
        config.addEnum("mode", mode, v -> mode = v, MODE_MAP)
                .setNameKey("morequesttypes.task.reskillable.mode");

        var COMPARISON_MAP = NameMap.of(ComparisonMode.GREATER_OR_EQUAL, ComparisonMode.values())
                .name(cm -> Component.translatable(cm.getTranslationKey()))
                .create();
        config.addEnum("comparison_mode", comparisonMode, v -> {
            comparisonMode = v;
            if (v.isRange() && secondNumber <= firstNumber) {
                secondNumber = firstNumber + 10;
            }
        }, COMPARISON_MAP).setNameKey("morequesttypes.task.comparison_mode");

        config.addInt("first_number", firstNumber, v -> {
            firstNumber = Math.max(0, v);
            if (comparisonMode.isRange() && secondNumber <= firstNumber) {
                secondNumber = firstNumber + 10;
            }
        }, 10, 0, 100000).setNameKey("morequesttypes.task.first_number");

        config.addInt("second_number", secondNumber, v -> {
            if (comparisonMode.isRange() && v <= firstNumber) {
                secondNumber = firstNumber + 10;
            } else {
                secondNumber = Math.max(0, v);
            }
        }, 20, 0, 100000).setNameKey("morequesttypes.task.second_number");

        Map<String, String> skills = new LinkedHashMap<>();
        if (ReskillableCompat.isLoaded()) {
            skills.putAll(ReskillableCompat.getAllSkills());
        }

        if (skills.isEmpty()) {
            skills.put("none", "No Skills Available");
        }

        List<String> skillIds = new ArrayList<>(skills.keySet());
        String currentSkillId = skillIds.contains(skillId) ? skillId : skillIds.get(0);

        TaskType taskType = getType();
        var SKILL_MAP = NameMap.of(currentSkillId, skillIds.toArray(String[]::new))
                .name(id -> {
                    if (id == null) return Component.literal("Unknown");
                    return Component.literal(skills.getOrDefault(id, "Unknown"));
                })
                .icon(id -> {
                    if (id == null) return taskType.getIconSupplier();
                    return ReskillableTask.getSkillIcon(id, taskType);
                })
                .create();

        config.addEnum("skill", currentSkillId, v -> skillId = v, SKILL_MAP)
                .setNameKey("morequesttypes.task.reskillable.skill");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("mode", mode.name());
        nbt.putString("comparison_mode", comparisonMode.name());
        nbt.putInt("first_number", firstNumber);
        nbt.putInt("second_number", secondNumber);
        nbt.putString("skill_id", skillId);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        if (nbt.contains("mode")) {
            try {
                mode = Mode.valueOf(nbt.getString("mode"));
            } catch (IllegalArgumentException ignored) {
                mode = Mode.SKILL_LEVEL;
            }
        }
        if (nbt.contains("comparison_mode")) {
            try {
                comparisonMode = ComparisonMode.valueOf(nbt.getString("comparison_mode"));
            } catch (IllegalArgumentException ignored) {
                comparisonMode = ComparisonMode.GREATER_OR_EQUAL;
            }
        }
        firstNumber = Math.max(0, nbt.getInt("first_number"));
        secondNumber = Math.max(0, nbt.getInt("second_number"));

        if (nbt.contains("skill_id")) {
            skillId = nbt.getString("skill_id");
        } else if (nbt.contains("skill_index")) {
            int oldIndex = nbt.getInt("skill_index");
            skillId = switch (oldIndex) {
                case 0 -> "mining";
                case 1 -> "gathering";
                case 2 -> "attack";
                case 3 -> "defense";
                case 4 -> "building";
                case 5 -> "farming";
                case 6 -> "agility";
                case 7 -> "magic";
                default -> "mining";
            };
        } else {
            skillId = "mining";
        }

        if (comparisonMode.isRange() && secondNumber <= firstNumber) {
            secondNumber = firstNumber + 10;
        }

        if (nbt.contains("required_amount") && !nbt.contains("first_number")) {
            firstNumber = Math.max(1, nbt.getInt("required_amount"));
            comparisonMode = ComparisonMode.GREATER_OR_EQUAL;
        }
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(mode);
        buf.writeEnum(comparisonMode);
        buf.writeVarInt(firstNumber);
        buf.writeVarInt(secondNumber);
        buf.writeUtf(skillId);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        mode = buf.readEnum(Mode.class);
        comparisonMode = buf.readEnum(ComparisonMode.class);
        firstNumber = Math.max(0, buf.readVarInt());
        secondNumber = Math.max(0, buf.readVarInt());
        skillId = buf.readUtf();

        if (comparisonMode.isRange() && secondNumber <= firstNumber) {
            secondNumber = firstNumber + 10;
        }
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 20;
    }

    @Override
    public boolean checkOnLogin() {
        return true;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;
        if (!ReskillableCompat.isLoaded()) return;

        Collection<ServerPlayer> online = teamData.getOnlineMembers();
        if (online == null || online.isEmpty()) return;
        if (!online.iterator().next().getUUID().equals(player.getUUID())) return;

        int best = 0;
        for (ServerPlayer p : online) {
            int value = 0;
            if (mode == Mode.TOTAL_LEVEL) {
                value = ReskillableCompat.getTotalSkillLevels(p);
            } else {
                value = ReskillableCompat.getSkillLevel(p, skillId);
            }
            best = Math.max(best, value);
        }

        long current = teamData.getProgress(this);
        long target;

        if (comparisonMode.isRange()) {
            target = ComparisonManager.compare(best, comparisonMode, firstNumber, secondNumber) ? 1 : 0;
        } else {
            target = Math.max(0, Math.min(getMaxProgress(), best));
        }

        if (target != current) {
            teamData.setProgress(this, target);
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        String compDesc = ComparisonManager.getDescription(comparisonMode, firstNumber, secondNumber);
        if (mode == Mode.TOTAL_LEVEL) {
            list.add(Component.translatable("morequesttypes.task.reskillable.tooltip.total_comparison", compDesc));
        } else {
            Map<String, String> skills = ReskillableCompat.getAllSkills();
            String skillName = skills.getOrDefault(skillId, skillId);
            list.add(Component.translatable("morequesttypes.task.reskillable.tooltip.skill_comparison", skillName, compDesc));
        }
    }
}