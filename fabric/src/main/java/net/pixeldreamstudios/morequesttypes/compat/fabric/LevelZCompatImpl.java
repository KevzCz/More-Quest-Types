package net.pixeldreamstudios.morequesttypes.compat.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.levelz.access.LevelManagerAccess;
import net.levelz.level.LevelManager;
import net.levelz.level.Skill;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public final class LevelZCompatImpl {
    private static final String MOD_ID = "levelz";

    private LevelZCompatImpl() {}

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static int getLevel(ServerPlayer player) {
        if (! isLoaded() || player == null) return 0;
        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    return manager.getOverallLevel();
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public static int getSkillPoints(ServerPlayer player) {
        if (!isLoaded() || player == null) return 0;
        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    return manager.getSkillPoints();
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public static int getSkillLevel(ServerPlayer player, int skillId) {
        if (!isLoaded() || player == null) return 0;
        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    return manager.getSkillLevel(skillId);
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public static int getTotalSkillLevels(ServerPlayer player) {
        if (!isLoaded() || player == null) return 0;
        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    int total = 0;
                    for (Integer skillId : LevelManager.SKILLS.keySet()) {
                        total += manager.getSkillLevel(skillId);
                    }
                    return total;
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public static void addExperience(ServerPlayer player, int amount) {
        if (!isLoaded() || player == null) return;
        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    manager.addExperienceLevels(amount);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void addSkillPoints(ServerPlayer player, int amount) {
        if (!isLoaded() || player == null) return;
        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    int current = manager.getSkillPoints();
                    manager.setSkillPoints(current + amount);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void setLevel(ServerPlayer player, int level) {
        if (! isLoaded() || player == null) return;
        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    manager.setOverallLevel(level);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void setSkillLevel(ServerPlayer player, int skillId, int level) {
        if (!isLoaded() || player == null) return;
        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    manager.setSkillLevel(skillId, level);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static Map<Integer, String> getAvailableSkills() {
        Map<Integer, String> skills = new HashMap<>();
        if (!isLoaded()) return skills;

        try {
            for (Map.Entry<Integer, Skill> entry : LevelManager.SKILLS.entrySet()) {
                skills.put(entry.getKey(), entry.getValue().getKey());
            }
        } catch (Throwable ignored) {}

        return skills;
    }
}