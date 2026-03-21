package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.skills.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ReskillableCompatImpl {
    private static final String MOD_ID = "reskillable";

    private ReskillableCompatImpl() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(ReskillableCompatImpl.MOD_ID);
    }

    public static int getSkillLevel(ServerPlayer player, String skillId) {
        if (!ReskillableCompatImpl.isLoaded() || player == null || skillId == null) return 0;
        try {
            SkillModel model = SkillModel.get(player);
            if (model != null) {
                String normalized = skillId.trim().toLowerCase(Locale.ROOT);
                return model.getSkillLevel(normalized);
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    public static int getTotalSkillLevels(ServerPlayer player) {
        if (!ReskillableCompatImpl.isLoaded() || player == null) return 0;
        try {
            SkillModel model = SkillModel.get(player);
            if (model != null) {
                return model.getAllSkillLevels().values().stream()
                        .mapToInt(Integer::intValue)
                        .sum();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    public static void setSkillLevel(ServerPlayer player, String skillId, int level) {
        if (!ReskillableCompatImpl.isLoaded() || player == null || skillId == null) return;
        try {
            SkillModel model = SkillModel.get(player);
            if (model != null) {
                String normalized = skillId.trim().toLowerCase(Locale.ROOT);
                model.setSkillLevel(normalized, level);
                model.syncSkills(player);
            }
        } catch (Throwable ignored) {
        }
    }

    public static Map<String, String> getAllSkills() {
        Map<String, String> skills = new LinkedHashMap<>();
        if (!ReskillableCompatImpl.isLoaded()) return skills;

        try {
            for (Skill skill : Skill.values()) {
                String skillId = skill.name().toLowerCase(Locale.ROOT);
                skills.put(skillId, skill.name());
            }
            for (Configuration.CustomSkillSlot customSkill : Configuration.getCustomSkills()) {
                if (customSkill != null && customSkill.isEnabled()) {
                    String skillId = customSkill.getId();
                    String displayName = customSkill.getDisplayName();
                    skills.put(skillId, displayName);
                }
            }
        } catch (Throwable ignored) {
        }

        return skills;
    }

    public static String getSkillIcon(String skillId) {
        if (!ReskillableCompatImpl.isLoaded() || skillId == null) return null;

        try {
            String normalized = skillId.trim().toLowerCase(Locale.ROOT);

            for (Configuration.CustomSkillSlot customSkill : Configuration.getCustomSkills()) {
                if (customSkill != null && customSkill.isEnabled()) {
                    if (normalized.equals(customSkill.getId().toLowerCase(Locale.ROOT))) {
                        return customSkill.getIcon();
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }
}