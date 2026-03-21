package net.pixeldreamstudios.morequesttypes.util;

import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.api.ITaskLevelZExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskOriginExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskReskillableExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskSkillsExtension;
import net.pixeldreamstudios.morequesttypes.compat.LevelZCompat;
import net.pixeldreamstudios.morequesttypes.compat.OriginsCompat;
import net.pixeldreamstudios.morequesttypes.compat.ReskillableCompat;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;

public class TaskRequirementChecker {

    public static boolean meetsAllRequirements(Task task, ServerPlayer player) {
        if (!TaskRequirementChecker.checkOriginRequirement(task, player)) {
            return false;
        }

        if (!TaskRequirementChecker.checkSkillsRequirement(task, player)) {
            return false;
        }

        if (!TaskRequirementChecker.checkLevelZRequirement(task, player)) {
            return false;
        }

        if (!TaskRequirementChecker.checkReskillableRequirement(task, player)) {
            return false;
        }

        return true;
    }

    private static boolean checkOriginRequirement(Task task, ServerPlayer player) {
        if (!OriginsCompat.isLoaded()) return true;
        if (!(task instanceof ITaskOriginExtension extension)) return true;
        if (!extension.shouldCheckOrigin()) return true;

        ResourceLocation layer = extension.getRequiredOriginLayer();
        ResourceLocation origin = extension.getRequiredOrigin();

        if (layer == null || layer.equals(ResourceLocation.withDefaultNamespace("empty")) ||
                origin == null || origin.equals(ResourceLocation.withDefaultNamespace("empty"))) {
            return true;
        }

        return OriginsCompat.hasOrigin(player, layer, origin);
    }

    private static boolean checkSkillsRequirement(Task task, ServerPlayer player) {
        if (!SkillsCompat.isLoaded()) return true;
        if (!(task instanceof ITaskSkillsExtension extension)) return true;
        if (!extension.shouldCheckSkillsLevel()) return true;

        int playerValue;
        if (extension.getSkillsMode() == ITaskSkillsExtension.SkillsMode.TOTAL_LEVEL) {
            playerValue = SkillsCompat.getTotalLevel(player);
        } else {
            ResourceLocation catId = ResourceLocation.tryParse(extension.getSkillsCategoryId());
            if (catId == null) return true;
            playerValue = SkillsCompat.getCategoryLevel(player, catId);
        }

        return extension.getSkillsComparison().compare(
                playerValue,
                extension.getSkillsFirstNumber(),
                extension.getSkillsSecondNumber()
        );
    }

    private static boolean checkLevelZRequirement(Task task, ServerPlayer player) {
        if (!LevelZCompat.isLoaded()) return true;
        if (!(task instanceof ITaskLevelZExtension extension)) return true;
        if (!extension.shouldCheckLevelZ()) return true;

        int playerValue;
        if (extension.getLevelZMode() == ITaskLevelZExtension.LevelZMode.TOTAL_LEVEL) {
            playerValue = LevelZCompat.getTotalSkillLevels(player);
        } else {
            int skillId = extension.getLevelZSkillId();
            if (skillId == -1) {
                playerValue = LevelZCompat.getLevel(player);
            } else {
                playerValue = LevelZCompat.getSkillLevel(player, skillId);
            }
        }

        return extension.getLevelZComparison().compare(
                playerValue,
                extension.getLevelZFirstNumber(),
                extension.getLevelZSecondNumber()
        );
    }

    private static boolean checkReskillableRequirement(Task task, ServerPlayer player) {
        if (!ReskillableCompat.isLoaded()) return true;
        if (!(task instanceof ITaskReskillableExtension extension)) return true;
        if (!extension.shouldCheckReskillable()) return true;

        int playerValue;
        if (extension.getReskillableMode() == ITaskReskillableExtension.ReskillableMode.TOTAL_LEVEL) {
            playerValue = ReskillableCompat.getTotalSkillLevels(player);
        } else {
            playerValue = ReskillableCompat.getSkillLevel(player, extension.getReskillableSkillId());
        }

        return extension.getReskillableComparison().compare(
                playerValue,
                extension.getReskillableFirstNumber(),
                extension.getReskillableSecondNumber()
        );
    }
}