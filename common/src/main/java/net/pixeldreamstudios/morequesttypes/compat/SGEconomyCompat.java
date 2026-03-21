package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;

public final class SGEconomyCompat {
    private SGEconomyCompat() {}

    @ExpectPlatform
    public static boolean isLoaded() { throw new AssertionError(); }

    @ExpectPlatform
    public static double getBalance(ServerPlayer player) { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean hasBalance(ServerPlayer player, double amount) { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean withdrawBalance(ServerPlayer player, double amount) { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean depositBalance(ServerPlayer player, double amount) { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean setBalance(ServerPlayer player, double amount) { throw new AssertionError(); }
}
