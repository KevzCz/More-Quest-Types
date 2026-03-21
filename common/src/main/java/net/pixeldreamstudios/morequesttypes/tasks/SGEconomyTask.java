package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.StringUtils;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.compat.SGEconomyCompat;
import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;

public final class SGEconomyTask extends Task {
    public enum Mode {
        CHECK,
        PAY
    }

    private Mode mode = Mode.PAY;
    private double requiredAmount = 100.0;
    private ComparisonMode comparisonMode = ComparisonMode.GREATER_OR_EQUAL;

    public SGEconomyTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.PAY;
    }

    @Override
    public long getMaxProgress() {
        return 1L;
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return mode == Mode.CHECK ? 20 : 0;
    }

    @Override
    public boolean checkOnLogin() {
        return mode == Mode.CHECK;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        if (!SGEconomyCompat.isLoaded()) {
            return;
        }

        double currentBalance = SGEconomyCompat.getBalance(player);

        if (mode == Mode.CHECK) {
            boolean satisfied = compareBalance(currentBalance, requiredAmount, comparisonMode);
            if (satisfied) {
                teamData.setProgress(this, 1L);
            }
        } else {
            boolean canAfford = currentBalance >= requiredAmount;
            if (canAfford) {
                if (SGEconomyCompat.withdrawBalance(player, requiredAmount)) {
                    teamData.setProgress(this, 1L);
                    player.sendSystemMessage(
                            Component.translatable("morequesttypes.task.pay.deducted",
                                            StringUtils.formatDouble(requiredAmount, true))
                                    .withStyle(ChatFormatting.GOLD)
                    );
                }
            }
        }
    }

    private boolean compareBalance(double balance, double required, ComparisonMode mode) {
        double epsilon = 0.01;
        return switch (mode) {
            case EQUALS -> Math.abs(balance - required) < epsilon;
            case GREATER_THAN -> balance > required + epsilon;
            case LESS_THAN -> balance < required - epsilon;
            case GREATER_OR_EQUAL -> balance >= required - epsilon;
            case LESS_OR_EQUAL -> balance <= required + epsilon;
            case RANGE -> balance > required + epsilon;
            case RANGE_EQUAL -> balance >= required - epsilon;
            case RANGE_EQUAL_FIRST -> balance >= required - epsilon;
            case RANGE_EQUAL_SECOND -> balance > required + epsilon;
        };
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var MODES = NameMap.of(Mode.PAY, Mode.values()).create();
        config.addEnum("mode", mode, v -> mode = v, MODES)
                .setNameKey("morequesttypes.task.pay.mode");

        config.addDouble("required_amount", requiredAmount, v -> requiredAmount = Math.max(0.0, v), 100.0, 0.0, Double.MAX_VALUE)
                .setNameKey("morequesttypes.task.pay.required_amount");
        if (mode == Mode.CHECK) {
            var COMPARISON_MAP = NameMap.of(ComparisonMode.GREATER_OR_EQUAL, ComparisonMode.values())
                    .name(cm -> Component.translatable(cm.getTranslationKey()))
                    .create();
            config.addEnum("comparison_mode", comparisonMode, v -> comparisonMode = v, COMPARISON_MAP)
                    .setNameKey("morequesttypes.task.pay.comparison_mode");
        }
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("mode", mode.name());
        nbt.putDouble("required_amount", requiredAmount);
        nbt.putString("comparison_mode", comparisonMode.name());
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);

        if (nbt.contains("mode")) {
            try {
                mode = Mode.valueOf(nbt.getString("mode"));
            } catch (IllegalArgumentException e) {
                mode = Mode.PAY;
            }
        }

        if (nbt.contains("required_amount")) {
            requiredAmount = nbt.getDouble("required_amount");
        }

        if (nbt.contains("comparison_mode")) {
            try {
                comparisonMode = ComparisonMode.valueOf(nbt.getString("comparison_mode"));
            } catch (IllegalArgumentException e) {
                comparisonMode = ComparisonMode.GREATER_OR_EQUAL;
            }
        }
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeEnum(mode);
        buffer.writeDouble(requiredAmount);
        buffer.writeEnum(comparisonMode);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        mode = buffer.readEnum(Mode.class);
        requiredAmount = buffer.readDouble();
        comparisonMode = buffer.readEnum(ComparisonMode.class);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        if (mode == Mode.CHECK) {
            return Component.translatable("morequesttypes.task.pay.title.check",
                    StringUtils.formatDouble(requiredAmount, true));
        } else {
            return Component.translatable("morequesttypes.task.pay.title.pay",
                    StringUtils.formatDouble(requiredAmount, true));
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return Icon.getIcon("minecraft:item/gold_ingot");
    }
}