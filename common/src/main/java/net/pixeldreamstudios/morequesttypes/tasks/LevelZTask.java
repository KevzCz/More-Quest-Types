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
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.compat.LevelZCompat;
import net.pixeldreamstudios.morequesttypes.util.ComparisonManager;
import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;

import java.util.*;

public class LevelZTask extends Task {
    public enum Mode { LEVEL, TOTAL_LEVEL }

    private Mode mode = Mode.LEVEL;
    private ComparisonMode comparisonMode = ComparisonMode.GREATER_OR_EQUAL;
    private int firstNumber = 10;
    private int secondNumber = 20;
    private int skillId = -1;

    public LevelZTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.LEVELZ;
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
        var mc = net.minecraft.client.Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return Component.literal("? / " + getMaxProgress());

        long p = TeamData.get(player).getProgress(this);

        if (comparisonMode.isRange()) {
            return Component.literal(p >= 1 ? "✓" : "✗");
        }

        long shown = Math.max(0, Math.min(p, getMaxProgress()));
        return Component.literal(shown + " / " + getMaxProgress());
    }

    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var MODE_MAP = NameMap.of(Mode.LEVEL, Mode.values()).create();
        config.addEnum("mode", mode, v -> mode = v, MODE_MAP)
                .setNameKey("morequesttypes.task.levelz.mode");

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

        Map<Integer, String> skills = new LinkedHashMap<>();
        skills.put(-1, "player_level");
        if (LevelZCompat.isLoaded()) {
            skills.putAll(LevelZCompat.getAvailableSkills());
        }

        List<Integer> skillIds = new ArrayList<>(skills.keySet());

        Integer currentSkillId = skillIds.contains(skillId) ? skillId : (skillIds.isEmpty() ? -1 : skillIds.get(0));

        var SKILL_MAP = NameMap.of(currentSkillId, skillIds.toArray(Integer[]::new))
                .name(id -> {
                    if (id == null) return Component.literal("Unknown");
                    if (id == -1) return Component.literal("Player Level");
                    return Component.literal(skills.getOrDefault(id, "Unknown (" + id + ")"));
                })
                .create();

        config.addEnum("skill", currentSkillId, v -> skillId = v, SKILL_MAP)
                .setNameKey("morequesttypes.task.levelz.skill");
    }

    @Override
    public void writeData(net.minecraft.nbt.CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("mode", mode.name());
        nbt.putString("comparison_mode", comparisonMode.name());
        nbt.putInt("first_number", firstNumber);
        nbt.putInt("second_number", secondNumber);
        nbt.putInt("skill_id", skillId);
    }

    @Override
    public void readData(net.minecraft.nbt.CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        if (nbt.contains("mode")) {
            try {
                mode = Mode.valueOf(nbt.getString("mode"));
            } catch (IllegalArgumentException ignored) {
                mode = Mode.LEVEL;
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
        skillId = nbt.getInt("skill_id");

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
        buf.writeVarInt(skillId);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        mode = buf.readEnum(Mode.class);
        comparisonMode = buf.readEnum(ComparisonMode.class);
        firstNumber = Math.max(0, buf.readVarInt());
        secondNumber = Math.max(0, buf.readVarInt());
        skillId = buf.readVarInt();

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
        if (! LevelZCompat.isLoaded()) return;

        Collection<ServerPlayer> online = teamData.getOnlineMembers();
        if (online == null || online.isEmpty()) return;
        if (! online.iterator().next().getUUID().equals(player.getUUID())) return;

        int best = 0;
        for (ServerPlayer p : online) {
            int value = 0;
            if (mode == Mode.TOTAL_LEVEL) {
                value = LevelZCompat.getTotalSkillLevels(p);
            } else {
                if (skillId == -1) {
                    value = LevelZCompat.getLevel(p);
                } else {
                    value = LevelZCompat.getSkillLevel(p, skillId);
                }
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
            list.add(Component.translatable("morequesttypes.task.levelz.tooltip.total_comparison", compDesc));
        } else {
            Map<Integer, String> skills = new LinkedHashMap<>();
            skills.put(-1, "player_level");
            skills.putAll(LevelZCompat.getAvailableSkills());

            String skillName = skillId == -1 ? "Player Level" : skills.getOrDefault(skillId, "Unknown");
            list.add(Component.translatable("morequesttypes.task.levelz.tooltip.skill_comparison", skillName, compDesc));
        }
    }
    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        if (mode == Mode.TOTAL_LEVEL) {
            return getType().getIconSupplier();
        }

        if (skillId == -1) {
            return Icon.getIcon(ResourceLocation.fromNamespaceAndPath("levelz", "icon.png"));
        }

        Map<Integer, String> skills = LevelZCompat.getAvailableSkills();
        String skillKey = skills.get(skillId);

        if (skillKey != null) {
            try {
                ResourceLocation iconLocation = ResourceLocation.fromNamespaceAndPath("levelz",
                        "textures/gui/sprites/" + skillKey + ".png");
                return Icon.getIcon(iconLocation);
            } catch (Exception e) {
                return getType().getIconSupplier();
            }
        }

        return getType().getIconSupplier();
    }
}