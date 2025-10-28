package net.pixeldreamstudios.morequesttypes.mixin.client;

import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.morequesttypes.tasks.FindEntityTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(QuestObjectBase.class)
public abstract class TitleDistanceMixin {

    @Inject(method = "getTitle", at = @At("RETURN"), cancellable = true)
    private void morequesttypes$appendDistance(CallbackInfoReturnable<Component> cir) {
        Object self = this;
        if (!(self instanceof Task t) || !(t instanceof FindEntityTask fe)) {
            return;
        }

        Component base = cir.getReturnValue();
        String meters = FindEntityTask.uiDistanceSuffix(fe);
        Component decorated = base.copy().append(Component.literal(" [" + meters + "]"));
        cir.setReturnValue(decorated);
    }
}
