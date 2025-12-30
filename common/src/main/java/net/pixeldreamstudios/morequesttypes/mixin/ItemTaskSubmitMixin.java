package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.util.TaskRequirementChecker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemTask.class, remap = false)
public class ItemTaskSubmitMixin {

    @Inject(method = "submitTask", at = @At("HEAD"), cancellable = true, remap = false)
    private void mqt$checkRequirementsBeforeSubmit(TeamData teamData, ServerPlayer player, ItemStack craftedItem, CallbackInfo ci) {
        ItemTask task = (ItemTask) (Object) this;

        if (! TaskRequirementChecker.meetsAllRequirements(task, player)) {
            ci.cancel();
        }
    }
}