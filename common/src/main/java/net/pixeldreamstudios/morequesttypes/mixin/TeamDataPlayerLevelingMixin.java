package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.api.ITaskLevelZExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskReskillableExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskSkillsExtension;
import net.pixeldreamstudios.morequesttypes.compat.LevelZCompat;
import net.pixeldreamstudios.morequesttypes.compat.ReskillableCompat;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TeamData.class, remap = false)
public abstract class TeamDataPlayerLevelingMixin {

    @Inject(method = "setProgress", at = @At("HEAD"), cancellable = true, remap = false)
    private void mqt$checkPlayerLevelingBeforeSetProgress(Task task, long progress, CallbackInfo ci) {
        if (progress <= 0) return;

        TeamData teamData = (TeamData)(Object)this;

        if (! mqt$checkSkillsRequirement(task, teamData)) {
            ci.cancel();
            return;
        }

        if (!mqt$checkLevelZRequirement(task, teamData)) {
            ci.cancel();
            return;
        }

        if (!mqt$checkReskillableRequirement(task, teamData)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean mqt$checkSkillsRequirement(Task task, TeamData teamData) {
        if (!SkillsCompat.isLoaded()) return true;
        if (!(task instanceof ITaskSkillsExtension extension)) return true;
        if (!extension.shouldCheckSkillsLevel()) return true;

        for (ServerPlayer player : teamData.getOnlineMembers()) {
            int playerValue;
            if (extension.getSkillsMode() == ITaskSkillsExtension.SkillsMode.TOTAL_LEVEL) {
                playerValue = SkillsCompat.getTotalLevel(player);
            } else {
                ResourceLocation catId = ResourceLocation.tryParse(extension.getSkillsCategoryId());
                if (catId == null) continue;
                playerValue = SkillsCompat.getCategoryLevel(player, catId);
            }

            if (extension.getSkillsComparison().compare(
                    playerValue,
                    extension.getSkillsFirstNumber(),
                    extension.getSkillsSecondNumber())) {
                return true;
            }
        }

        return false;
    }

    @Unique
    private boolean mqt$checkLevelZRequirement(Task task, TeamData teamData) {
        if (!LevelZCompat.isLoaded()) return true;
        if (!(task instanceof ITaskLevelZExtension extension)) return true;
        if (!extension.shouldCheckLevelZ()) return true;

        for (ServerPlayer player : teamData.getOnlineMembers()) {
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

            if (extension.getLevelZComparison().compare(
                    playerValue,
                    extension.getLevelZFirstNumber(),
                    extension.getLevelZSecondNumber())) {
                return true;
            }
        }

        return false;
    }

    @Unique
    private boolean mqt$checkReskillableRequirement(Task task, TeamData teamData) {
        if (!ReskillableCompat.isLoaded()) return true;
        if (!(task instanceof ITaskReskillableExtension extension)) return true;
        if (!extension.shouldCheckReskillable()) return true;

        for (ServerPlayer player : teamData.getOnlineMembers()) {
            int playerValue;
            if (extension.getReskillableMode() == ITaskReskillableExtension.ReskillableMode.TOTAL_LEVEL) {
                playerValue = ReskillableCompat.getTotalSkillLevels(player);
            } else {
                playerValue = ReskillableCompat.getSkillLevel(player, extension.getReskillableSkillIndex());
            }

            if (extension.getReskillableComparison().compare(
                    playerValue,
                    extension.getReskillableFirstNumber(),
                    extension.getReskillableSecondNumber())) {
                return true;
            }
        }

        return false;
    }
}