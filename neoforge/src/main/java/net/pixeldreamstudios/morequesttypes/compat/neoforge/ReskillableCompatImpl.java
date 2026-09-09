package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.skills.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ReskillableCompatImpl {
    private static final String MOD_ID = "reskillable";

    private ReskillableCompatImpl() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static int getSkillLevel(ServerPlayer player, String skillId) {
        if (!isLoaded() || player == null || skillId == null) return 0;
        try {
            SkillModel model = SkillModel.get(player);
            if (model != null) {
                return model.getSkillLevel(Configuration.canonicalSkillId(skillId.trim()));
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    public static int getTotalSkillLevels(ServerPlayer player) {
        if (!isLoaded() || player == null) return 0;
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
        if (!isLoaded() || player == null || skillId == null) return;
        try {
            SkillModel model = SkillModel.get(player);
            if (model != null) {
                String canonical = Configuration.canonicalSkillId(skillId.trim());
                model.ensureSkillExists(canonical);
                model.setSkillLevel(canonical, level);
                model.updateSkillAttributeBonuses(player);
                model.syncSkills(player);
            }
        } catch (Throwable ignored) {
        }
    }

    public static Map<String, String> getAllSkills() {
        Map<String, String> skills = new LinkedHashMap<>();
        if (!isLoaded()) return skills;

        try {
            for (Skill skill : Configuration.getEnabledBuiltInSkills()) {
                skills.put(Configuration.getBuiltInSkillId(skill),
                        resolveName(Configuration.getBuiltInSkillDisplayName(skill), skill.getDisplayName()));
            }
            for (Configuration.CustomSkillSlot customSkill : Configuration.getEnabledCustomSkills()) {
                if (customSkill != null) {
                    skills.put(customSkill.getId(), resolveName(customSkill.getDisplayName(), customSkill.getId()));
                }
            }
        } catch (Throwable ignored) {
        }

        if (skills.isEmpty()) {
            for (Skill skill : Skill.values()) {
                skills.put(skill.getSerializedName(), resolveName(skill.getDisplayName(), skill.getSerializedName()));
            }
        }

        return skills;
    }

    private static String resolveName(String displayName, String fallbackKey) {
        String key = displayName == null || displayName.isBlank() ? fallbackKey : displayName;
        if (key == null || key.isBlank()) return "";
        String resolved = Component.translatable(key).getString();
        return resolved.isBlank() ? key : resolved;
    }

    public static String getSkillIcon(String skillId) {
        if (!isLoaded() || skillId == null) return null;

        try {
            String canonical = Configuration.canonicalSkillId(skillId.trim());

            Configuration.CustomSkillSlot customSkill = Configuration.getCustomSkill(canonical);
            if (customSkill != null && customSkill.isEnabled()) {
                return customSkill.getIcon();
            }

            Skill builtIn = Configuration.resolveBuiltInSkill(canonical);
            if (builtIn == null) {
                builtIn = Skill.fromString(canonical);
            }
            if (builtIn != null) {
                Configuration.BuiltInSkillSlot slot = Configuration.getBuiltInSkill(builtIn);
                if (slot != null) {
                    return slot.getIcon();
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }
}
