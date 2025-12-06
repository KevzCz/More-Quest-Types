package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class LevelZCompat {
    private LevelZCompat() {}

    @ExpectPlatform
    public static boolean isLoaded() { throw new AssertionError(); }

    @ExpectPlatform
    public static int getLevel(ServerPlayer player) { throw new AssertionError(); }

    @ExpectPlatform
    public static int getTotalExperience(ServerPlayer player) { throw new AssertionError(); }

    @ExpectPlatform
    public static int getSkillPoints(ServerPlayer player) { throw new AssertionError(); }

    @ExpectPlatform
    public static int getSkillLevel(ServerPlayer player, int skillId) { throw new AssertionError(); }

    @ExpectPlatform
    public static int getTotalSkillLevels(ServerPlayer player) { throw new AssertionError(); }

    @ExpectPlatform
    public static void setLevel(ServerPlayer player, int level) { throw new AssertionError(); }

    @ExpectPlatform
    public static void addExperience(ServerPlayer player, int xpAmount) { throw new AssertionError(); }

    @ExpectPlatform
    public static void setExperience(ServerPlayer player, int xpAmount) { throw new AssertionError(); }

    @ExpectPlatform
    public static void setSkillLevel(ServerPlayer player, int skillId, int level) { throw new AssertionError(); }

    @ExpectPlatform
    public static void setSkillPoints(ServerPlayer player, int points) { throw new AssertionError(); }

    @ExpectPlatform
    public static Map<Integer, String> getAvailableSkills() { throw new AssertionError(); }
}