package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import dev.architectury.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import net.sirgrantd.sg_economy.api.SGEconomyApi;

public final class SGEconomyCompatImpl {
    private static final String MOD_ID = "sg_economy";

    private SGEconomyCompatImpl() {
    }

    public static boolean isLoaded() {
        return Platform.isModLoaded(SGEconomyCompatImpl.MOD_ID);
    }

    public static double getBalance(ServerPlayer player) {
        if (!SGEconomyCompatImpl.isLoaded()) return 0.0;
        try {
            var economy = SGEconomyApi.get();
            return economy.getBalance(player);
        } catch (Throwable t) {
            return 0.0;
        }
    }

    public static boolean hasBalance(ServerPlayer player, double amount) {
        if (!SGEconomyCompatImpl.isLoaded()) return false;
        try {
            var economy = SGEconomyApi.get();
            return economy.hasBalance(player, amount);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean withdrawBalance(ServerPlayer player, double amount) {
        if (!SGEconomyCompatImpl.isLoaded()) return false;
        try {
            var economy = SGEconomyApi.get();
            return economy.withdrawBalance(player, amount);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean depositBalance(ServerPlayer player, double amount) {
        if (!SGEconomyCompatImpl.isLoaded()) return false;
        try {
            var economy = SGEconomyApi.get();
            return economy.depositBalance(player, amount);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean setBalance(ServerPlayer player, double amount) {
        if (!SGEconomyCompatImpl.isLoaded()) return false;
        try {
            var economy = SGEconomyApi.get();
            return economy.setBalance(player, amount);
        } catch (Throwable t) {
            return false;
        }
    }
}
