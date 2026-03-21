package net.pixeldreamstudios.morequesttypes.compat.fabric;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class ReskillableCompatImpl {
    private ReskillableCompatImpl() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static int getSkillLevel(ServerPlayer player, String skillId) {
        return 0;
    }

    public static int getTotalSkillLevels(ServerPlayer player) {
        return 0;
    }

    public static void setSkillLevel(ServerPlayer player, String skillId, int level) {
        // No-op
    }

    public static Map<String, String> getAllSkills() {
        return Map.of();
    }

    public static String getSkillIcon(String skillId) {
        return "";
    }
}