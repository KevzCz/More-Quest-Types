package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.StringUtils;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.compat.SGEconomyCompat;

public final class SGEconomyReward extends Reward {
    public enum Mode {
        ADD,
        REMOVE,
        SET
    }

    private Mode mode = Mode.ADD;
    private double amount = 100.0;

    public SGEconomyReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.SGECONOMY;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        if (!SGEconomyCompat.isLoaded()) return;

        boolean success = false;
        String messageKey = null;
        double displayAmount = amount;

        switch (mode) {
            case ADD -> {
                success = SGEconomyCompat.depositBalance(player, amount);
                messageKey = amount >= 0 ? "morequesttypes.reward.sgeconomy.added" : "morequesttypes.reward.sgeconomy.removed";
                displayAmount = Math.abs(amount);
            }
            case REMOVE -> {
                success = SGEconomyCompat.withdrawBalance(player, Math.abs(amount));
                messageKey = "morequesttypes.reward.sgeconomy.removed";
                displayAmount = Math.abs(amount);
            }
            case SET -> {
                success = SGEconomyCompat.setBalance(player, amount);
                messageKey = "morequesttypes.reward.sgeconomy.set";
                displayAmount = amount;
            }
        }

        if (success && notify && messageKey != null) {
            player.displayClientMessage(
                    Component.translatable(messageKey, StringUtils.formatDouble(displayAmount, true)),
                    false
            );
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        return switch (mode) {
            case ADD -> Component.translatable("morequesttypes.reward.sgeconomy.title.add",
                    StringUtils.formatDouble(amount, true));
            case REMOVE -> Component.translatable("morequesttypes.reward.sgeconomy.title.remove",
                    StringUtils.formatDouble(Math.abs(amount), true));
            case SET -> Component.translatable("morequesttypes.reward.sgeconomy.title.set",
                    StringUtils.formatDouble(amount, true));
        };
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return Icon.getIcon("minecraft:item/gold_ingot");
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var MODES = NameMap.of(Mode.ADD, Mode.values()).create();
        config.addEnum("mode", mode, v -> mode = v, MODES)
                .setNameKey("morequesttypes.reward.sgeconomy.mode");

        config.addDouble("amount", amount, v -> amount = v, 100.0, -Double.MAX_VALUE, Double.MAX_VALUE)
                .setNameKey("morequesttypes.reward.sgeconomy.amount");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("mode", mode.name());
        nbt.putDouble("amount", amount);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        if (nbt.contains("mode")) {
            try {
                mode = Mode.valueOf(nbt.getString("mode"));
            } catch (IllegalArgumentException e) {
                mode = Mode.ADD;
            }
        }
        if (nbt.contains("amount")) {
            amount = nbt.getDouble("amount");
        }
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeEnum(mode);
        buffer.writeDouble(amount);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        mode = buffer.readEnum(Mode.class);
        amount = buffer.readDouble();
    }
}
