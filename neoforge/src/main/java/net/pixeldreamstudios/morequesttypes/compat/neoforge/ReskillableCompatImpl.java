package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ReskillableCompatImpl {
    private static final String MOD_ID = "reskillable";

    private ReskillableCompatImpl() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static int getSkillLevel(ServerPlayer player, int skillIndex) {
        if (! isLoaded() || player == null) return 0;
        try {
            SkillModel model = SkillModel.get(player);
            if (model != null && skillIndex >= 0 && skillIndex < Skill.values().length) {
                Skill skill = Skill.values()[skillIndex];
                return model.getSkillLevel(skill);
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public static int getTotalSkillLevels(ServerPlayer player) {
        if (!isLoaded() || player == null) return 0;
        try {
            SkillModel model = SkillModel.get(player);
            if (model != null) {
                int total = 0;
                for (Skill skill : Skill.values()) {
                    total += model.getSkillLevel(skill);
                }
                return total;
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public static void setSkillLevel(ServerPlayer player, int skillIndex, int level) {
        if (!isLoaded() || player == null) return;
        try {
            SkillModel model = SkillModel.get(player);
            if (model != null && skillIndex >= 0 && skillIndex < Skill.values().length) {
                Skill skill = Skill.values()[skillIndex];
                model.setSkillLevel(skill, level);
                model.syncSkills(player);
            }
        } catch (Throwable ignored) {}
    }

    public static Map<Integer, String> getAvailableSkills() {
        Map<Integer, String> skills = new LinkedHashMap<>();
        if (!isLoaded()) return skills;

        try {
            Skill[] allSkills = Skill.values();
            for (int i = 0; i < allSkills.length; i++) {
                skills.put(i, allSkills[i].name().toLowerCase());
            }
        } catch (Throwable ignored) {}

        return skills;
    }
}