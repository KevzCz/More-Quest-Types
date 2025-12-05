package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class ReskillableCompat {
    private ReskillableCompat() {}

    @ExpectPlatform
    public static boolean isLoaded() { throw new AssertionError(); }

    @ExpectPlatform
    public static int getSkillLevel(ServerPlayer player, int skillIndex) { throw new AssertionError(); }

    @ExpectPlatform
    public static int getTotalSkillLevels(ServerPlayer player) { throw new AssertionError(); }

    @ExpectPlatform
    public static void setSkillLevel(ServerPlayer player, int skillIndex, int level) { throw new AssertionError(); }
    @ExpectPlatform
    public static Map<Integer, String> getAvailableSkills() { throw new AssertionError(); }
}