package net.pixeldreamstudios. morequesttypes.mixin.client;

import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb. mods.ftbquests. quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task. ItemTask;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.pixeldreamstudios.morequesttypes.util.TaskRequirementTooltipHelper;
import org.spongepowered.asm.mixin. Mixin;
import org.spongepowered.asm. mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org. spongepowered.asm.mixin.injection.callback. CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = ItemTask.class, remap = false)
public class ItemTaskMouseOverTextMixin {

    @Inject(method = "addMouseOverText", at = @At("TAIL"), remap = false)
    private void mqt$addRequirementTooltips(TooltipList list, TeamData teamData, CallbackInfo ci) {
        ItemTask task = (ItemTask) (Object) this;
        TaskRequirementTooltipHelper.addRequirementTooltips(list, task);
    }
}