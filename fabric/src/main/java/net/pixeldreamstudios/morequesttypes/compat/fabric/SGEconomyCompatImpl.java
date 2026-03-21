package net.pixeldreamstudios.morequesttypes.compat.fabric;

import net.minecraft.server.level.ServerPlayer;

public final class SGEconomyCompatImpl {
    private SGEconomyCompatImpl() {
    }

    public static boolean isLoaded() {
        // SG-Economy is NeoForge only
        return false;
    }

    public static double getBalance(ServerPlayer player) {
        return 0.0;
    }

    public static boolean hasBalance(ServerPlayer player, double amount) {
        return false;
    }

    public static boolean withdrawBalance(ServerPlayer player, double amount) {
        return false;
    }

    public static boolean depositBalance(ServerPlayer player, double amount) {
        return false;
    }

    public static boolean setBalance(ServerPlayer player, double amount) {
        return false;
    }
}
