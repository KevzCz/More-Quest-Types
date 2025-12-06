package net.pixeldreamstudios.morequesttypes.compat.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.levelz.access.LevelManagerAccess;
import net.levelz.init.ConfigInit;
import net.levelz.level.LevelManager;
import net.levelz.level.Skill;
import net.levelz.util.PacketHelper;
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
        if (!isLoaded() || player == null) return 0;
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

    public static int getTotalExperience(ServerPlayer player) {
        if (!isLoaded() || player == null) return 0;
        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    return manager.getTotalLevelExperience();
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
        if (! isLoaded() || player == null) return 0;
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

    /**
     * Calculate total XP required to reach a given level
     */
    private static int calculateTotalXpForLevel(int targetLevel) {
        if (targetLevel <= 0) return 0;

        int totalXp = 0;
        for (int level = 0; level < targetLevel; level++) {
            int xpCost = (int)((double) ConfigInit.CONFIG.xpBaseCost +
                    (double) ConfigInit.CONFIG.xpCostMultiplicator *
                            Math.pow((double)level, (double) ConfigInit.CONFIG.xpExponent));

            if (ConfigInit.CONFIG.xpMaxCost != 0 && xpCost > ConfigInit.CONFIG.xpMaxCost) {
                xpCost = ConfigInit.CONFIG.xpMaxCost;
            }
            totalXp += xpCost;
        }
        return totalXp;
    }

    /**
     * Calculate level from total XP
     */
    private static int calculateLevelFromXp(int totalXp) {
        int level = 0;
        int accumulatedXp = 0;

        while (true) {
            int xpForNextLevel = (int)((double) ConfigInit.CONFIG.xpBaseCost +
                    (double) ConfigInit.CONFIG.xpCostMultiplicator *
                            Math.pow((double)level, (double) ConfigInit.CONFIG.xpExponent));

            if (ConfigInit.CONFIG.xpMaxCost != 0 && xpForNextLevel > ConfigInit.CONFIG.xpMaxCost) {
                xpForNextLevel = ConfigInit.CONFIG.xpMaxCost;
            }

            if (accumulatedXp + xpForNextLevel > totalXp) {
                break;
            }

            accumulatedXp += xpForNextLevel;
            level++;

            // Check max level
            if (ConfigInit.CONFIG.overallMaxLevel > 0 && level >= ConfigInit.CONFIG.overallMaxLevel) {
                break;
            }
        }

        return level;
    }

    public static void setLevel(ServerPlayer player, int level) {
        if (!isLoaded() || player == null) return;
        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    // Calculate the total XP needed for this level
                    int totalXp = calculateTotalXpForLevel(level);

                    // Set both level and XP
                    manager.setOverallLevel(level);
                    manager.setTotalLevelExperience(totalXp);
                    manager.setLevelProgress(0.0F);

                    // Sync to client
                    PacketHelper.updatePlayerSkills(player, null);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void addExperience(ServerPlayer player, int xpAmount) {
        if (!isLoaded() || player == null) return;
        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    int currentXp = manager.getTotalLevelExperience();
                    setExperience(player, currentXp + xpAmount);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void setExperience(ServerPlayer player, int xpAmount) {
        if (!isLoaded() || player == null) return;
        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    // Clamp to 0
                    xpAmount = Math.max(0, xpAmount);

                    // Calculate what level this XP amount corresponds to
                    int newLevel = calculateLevelFromXp(xpAmount);
                    int xpForCurrentLevel = calculateTotalXpForLevel(newLevel);
                    int xpInCurrentLevel = xpAmount - xpForCurrentLevel;

                    // Calculate next level XP requirement
                    int xpForNextLevel = (int)((double) ConfigInit.CONFIG.xpBaseCost +
                            (double) ConfigInit.CONFIG.xpCostMultiplicator *
                                    Math.pow((double)newLevel, (double) ConfigInit.CONFIG.xpExponent));

                    if (ConfigInit.CONFIG.xpMaxCost != 0 && xpForNextLevel > ConfigInit.CONFIG.xpMaxCost) {
                        xpForNextLevel = ConfigInit.CONFIG.xpMaxCost;
                    }

                    // Calculate progress
                    float progress = xpForNextLevel > 0 ? (float)xpInCurrentLevel / (float)xpForNextLevel : 0.0F;

                    // Update all values
                    manager.setOverallLevel(newLevel);
                    manager.setTotalLevelExperience(xpAmount);
                    manager.setLevelProgress(progress);

                    // Sync to client
                    PacketHelper.updatePlayerSkills(player, null);
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
                    PacketHelper.updatePlayerSkills(player, null);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void setSkillPoints(ServerPlayer player, int points) {
        if (!isLoaded() || player == null) return;
        try {
            if (player instanceof LevelManagerAccess access) {
                LevelManager manager = access.getLevelManager();
                if (manager != null) {
                    manager.setSkillPoints(points);
                    PacketHelper.updatePlayerSkills(player, null);
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