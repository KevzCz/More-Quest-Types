package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class LevelZCompatImpl {
    private LevelZCompatImpl() {}

    public static boolean isLoaded() {
        return false;
    }

    public static int getLevel(ServerPlayer player) {
        return 0;
    }

    public static int getTotalExperience(ServerPlayer player) {
        return 0;
    }

    public static int getSkillPoints(ServerPlayer player) {
        return 0;
    }

    public static int getSkillLevel(ServerPlayer player, int skillId) {
        return 0;
    }

    public static int getTotalSkillLevels(ServerPlayer player) {
        return 0;
    }

    public static void setLevel(ServerPlayer player, int level) {
        // No-op
    }

    public static void addExperience(ServerPlayer player, int xpAmount) {
        // No-op
    }

    public static void setExperience(ServerPlayer player, int xpAmount) {
        // No-op
    }

    public static void setSkillLevel(ServerPlayer player, int skillId, int level) {
        // No-op
    }

    public static void setSkillPoints(ServerPlayer player, int points) {
        // No-op
    }

    public static Map<Integer, String> getAvailableSkills() {
        return Map.of();
    }
}