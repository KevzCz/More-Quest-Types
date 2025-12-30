package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.api.ITaskOriginExtension;
import net.pixeldreamstudios.morequesttypes.compat.OriginsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TeamData.class, remap = false)
public abstract class TeamDataOriginMixin {

    @Inject(method = "setProgress", at = @At("HEAD"), cancellable = true, remap = false)
    private void mqt$checkOriginBeforeSetProgress(Task task, long progress, CallbackInfo ci) {
        if (! OriginsCompat.isLoaded()) return;

        if (progress <= 0) return;

        if (!(task instanceof ITaskOriginExtension extension)) return;

        if (!extension.shouldCheckOrigin()) return;

        TeamData teamData = (TeamData)(Object)this;

        boolean hasValidOrigin = false;
        for (ServerPlayer player : teamData.getOnlineMembers()) {
            if (OriginsCompat.hasOrigin(player, extension.getRequiredOriginLayer(), extension.getRequiredOrigin())) {
                hasValidOrigin = true;
                break;
            }
        }

        if (!hasValidOrigin) {
            ci.cancel();
        }
    }
}