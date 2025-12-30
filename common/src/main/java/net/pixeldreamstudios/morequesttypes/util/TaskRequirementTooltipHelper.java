package net.pixeldreamstudios.morequesttypes.util;

import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.morequesttypes.api.ITaskOriginExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskSkillsExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskLevelZExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskReskillableExtension;
import net.pixeldreamstudios.morequesttypes.compat.OriginsCompat;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;
import net.pixeldreamstudios.morequesttypes.compat.LevelZCompat;
import net.pixeldreamstudios.morequesttypes.compat.ReskillableCompat;

import java.util.LinkedHashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class TaskRequirementTooltipHelper {

    public static void addRequirementTooltips(TooltipList list, Task task) {
        boolean hasAnyRequirement = false;

        hasAnyRequirement |= addOriginRequirement(list, task, hasAnyRequirement);
        hasAnyRequirement |= addSkillsRequirement(list, task, hasAnyRequirement);
        hasAnyRequirement |= addLevelZRequirement(list, task, hasAnyRequirement);
        hasAnyRequirement |= addReskillableRequirement(list, task, hasAnyRequirement);
    }

    private static boolean addOriginRequirement(TooltipList list, Task task, boolean alreadyHasRequirement) {
        if (!OriginsCompat.isLoaded()) return false;
        if (!(task instanceof ITaskOriginExtension extension)) return false;
        if (! extension.shouldCheckOrigin()) return false;

        ResourceLocation layer = extension.getRequiredOriginLayer();
        ResourceLocation origin = extension.getRequiredOrigin();

        if (layer != null && ! layer.equals(ResourceLocation.withDefaultNamespace("empty")) &&
                origin != null && !origin.equals(ResourceLocation.withDefaultNamespace("empty"))) {

            if (! alreadyHasRequirement) {
                list.blankLine();
            }

            String originName = origin.getPath().replace("_", " ");
            originName = originName.substring(0, 1).toUpperCase() + originName.substring(1);

            list.add(Component.translatable("morequesttypes.quest.requirement.origin", originName)
                    .withStyle(ChatFormatting.YELLOW));
            return true;
        }
        return false;
    }

    private static boolean addSkillsRequirement(TooltipList list, Task task, boolean alreadyHasRequirement) {
        if (!SkillsCompat.isLoaded()) return false;
        if (!(task instanceof ITaskSkillsExtension extension)) return false;
        if (!extension.shouldCheckSkillsLevel()) return false;

        if (!alreadyHasRequirement) {
            list.blankLine();
        }

        String requirement = formatRequirement(
                extension.getSkillsComparison(),
                extension.getSkillsFirstNumber(),
                extension.getSkillsSecondNumber()
        );

        if (extension.getSkillsMode() == ITaskSkillsExtension.SkillsMode.TOTAL_LEVEL) {
            list.add(Component.translatable("morequesttypes.quest.requirement.skills.total", requirement)
                    .withStyle(ChatFormatting.AQUA));
        } else {
            String categoryId = extension.getSkillsCategoryId();
            if (categoryId != null && !categoryId.isEmpty()) {
                ResourceLocation catId = ResourceLocation.tryParse(categoryId);
                String catName = catId != null ? catId.getPath().replace("_", " ") : categoryId;
                catName = catName.substring(0, 1).toUpperCase() + catName.substring(1);

                list.add(Component.translatable("morequesttypes.quest.requirement.skills.category",
                        catName, requirement).withStyle(ChatFormatting.AQUA));
            }
        }
        return true;
    }

    private static boolean addLevelZRequirement(TooltipList list, Task task, boolean alreadyHasRequirement) {
        if (!LevelZCompat.isLoaded()) return false;
        if (!(task instanceof ITaskLevelZExtension extension)) return false;
        if (!extension.shouldCheckLevelZ()) return false;

        if (!alreadyHasRequirement) {
            list.blankLine();
        }

        String requirement = formatRequirement(
                extension.getLevelZComparison(),
                extension.getLevelZFirstNumber(),
                extension.getLevelZSecondNumber()
        );

        if (extension.getLevelZMode() == ITaskLevelZExtension.LevelZMode.TOTAL_LEVEL) {
            list.add(Component.translatable("morequesttypes.quest.requirement.levelz.total", requirement)
                    .withStyle(ChatFormatting.GREEN));
        } else {
            int skillId = extension.getLevelZSkillId();
            if (skillId == -1) {
                list.add(Component.translatable("morequesttypes.quest.requirement.levelz.player", requirement)
                        .withStyle(ChatFormatting.GREEN));
            } else {
                Map<Integer, String> skills = new LinkedHashMap<>();
                skills.putAll(LevelZCompat.getAvailableSkills());
                String skillName = skills.getOrDefault(skillId, "Unknown Skill");

                list.add(Component.translatable("morequesttypes.quest.requirement.levelz.skill",
                        skillName, requirement).withStyle(ChatFormatting.GREEN));
            }
        }
        return true;
    }

    private static boolean addReskillableRequirement(TooltipList list, Task task, boolean alreadyHasRequirement) {
        if (!ReskillableCompat.isLoaded()) return false;
        if (!(task instanceof ITaskReskillableExtension extension)) return false;
        if (!extension.shouldCheckReskillable()) return false;

        if (!alreadyHasRequirement) {
            list.blankLine();
        }

        String requirement = formatRequirement(
                extension.getReskillableComparison(),
                extension.getReskillableFirstNumber(),
                extension.getReskillableSecondNumber()
        );

        if (extension.getReskillableMode() == ITaskReskillableExtension.ReskillableMode.TOTAL_LEVEL) {
            list.add(Component.translatable("morequesttypes.quest.requirement.reskillable.total", requirement)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            Map<Integer, String> skills = new LinkedHashMap<>();
            skills.putAll(ReskillableCompat.getAvailableSkills());
            String skillName = skills.getOrDefault(extension.getReskillableSkillIndex(), "Unknown Skill");

            list.add(Component.translatable("morequesttypes.quest.requirement.reskillable.skill",
                    skillName, requirement).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    private static String formatRequirement(ComparisonMode mode, int first, int second) {
        return switch (mode) {
            case EQUALS -> "= " + first;
            case GREATER_THAN -> "> " + first;
            case LESS_THAN -> "< " + first;
            case GREATER_OR_EQUAL -> "≥ " + first;
            case LESS_OR_EQUAL -> "≤ " + first;
            case RANGE -> first + " > x > " + second;
            case RANGE_EQUAL -> first + " ≥ x ≥ " + second;
            case RANGE_EQUAL_FIRST -> first + " ≥ x > " + second;
            case RANGE_EQUAL_SECOND -> first + " > x ≥ " + second;
        };
    }
}