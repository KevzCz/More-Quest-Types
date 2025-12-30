package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.AbstractBooleanTask;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.api.ITaskLevelZExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskOriginExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskReskillableExtension;
import net.pixeldreamstudios.morequesttypes.api.ITaskSkillsExtension;
import net.pixeldreamstudios.morequesttypes.compat.LevelZCompat;
import net.pixeldreamstudios.morequesttypes.compat.OriginsCompat;
import net.pixeldreamstudios.morequesttypes.compat.ReskillableCompat;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractBooleanTask.class, remap = false)
public abstract class AbstractBooleanTaskExtensionMixin {

    @Inject(method = "submitTask", at = @At("HEAD"), cancellable = true, remap = false)
    private void mqt$checkAllRequirements(TeamData teamData, ServerPlayer player, ItemStack craftedItem, CallbackInfo ci) {
        AbstractBooleanTask task = (AbstractBooleanTask)(Object)this;

        if (! mqt$checkOriginRequirement(task, player)) {
            ci.cancel();
            return;
        }

        if (!mqt$checkSkillsRequirement(task, player)) {
            ci.cancel();
            return;
        }

        if (!mqt$checkLevelZRequirement(task, player)) {
            ci.cancel();
            return;
        }

        if (!mqt$checkReskillableRequirement(task, player)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean mqt$checkOriginRequirement(AbstractBooleanTask task, ServerPlayer player) {
        if (! OriginsCompat.isLoaded()) return true;
        if (!(task instanceof ITaskOriginExtension extension)) return true;
        if (! extension.shouldCheckOrigin()) return true;

        return OriginsCompat.hasOrigin(player, extension.getRequiredOriginLayer(), extension.getRequiredOrigin());
    }

    @Unique
    private boolean mqt$checkSkillsRequirement(AbstractBooleanTask task, ServerPlayer player) {
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

    @Unique
    private boolean mqt$checkLevelZRequirement(AbstractBooleanTask task, ServerPlayer player) {
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

    @Unique
    private boolean mqt$checkReskillableRequirement(AbstractBooleanTask task, ServerPlayer player) {
        if (!ReskillableCompat.isLoaded()) return true;
        if (!(task instanceof ITaskReskillableExtension extension)) return true;
        if (!extension.shouldCheckReskillable()) return true;

        int playerValue;
        if (extension.getReskillableMode() == ITaskReskillableExtension.ReskillableMode.TOTAL_LEVEL) {
            playerValue = ReskillableCompat.getTotalSkillLevels(player);
        } else {
            playerValue = ReskillableCompat.getSkillLevel(player, extension.getReskillableSkillIndex());
        }

        return extension.getReskillableComparison().compare(
                playerValue,
                extension.getReskillableFirstNumber(),
                extension.getReskillableSecondNumber()
        );
    }
}