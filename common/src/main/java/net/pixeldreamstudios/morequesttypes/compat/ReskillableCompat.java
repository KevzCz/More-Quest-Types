package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class ReskillableCompat {
    private ReskillableCompat() {
    }

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int getSkillLevel(ServerPlayer player, String skillId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int getTotalSkillLevels(ServerPlayer player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setSkillLevel(ServerPlayer player, String skillId, int level) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Map<String, String> getAllSkills() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static String getSkillIcon(String skillId) {
        throw new AssertionError();
    }
}