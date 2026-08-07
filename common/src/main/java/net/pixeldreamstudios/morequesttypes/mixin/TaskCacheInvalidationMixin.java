package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.pixeldreamstudios.morequesttypes.api.IQuestFileExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Task.class)
public abstract class TaskCacheInvalidationMixin {

    @Inject(method = "onCreated", at = @At("TAIL"))
    private void morequesttypes$invalidateOnCreate(CallbackInfo ci) {
        morequesttypes$invalidateTaskCaches();
    }

    @Inject(method = "deleteSelf", at = @At("TAIL"))
    private void morequesttypes$invalidateOnDelete(CallbackInfo ci) {
        morequesttypes$invalidateTaskCaches();
    }

    private void morequesttypes$invalidateTaskCaches() {
        BaseQuestFile file = ((Task) (Object) this).getQuestFile();
        if (file == null || !file.isServerSide()) return;
        if (file instanceof IQuestFileExtension ext && !ext.mqt$isAutoRefreshTaskCache()) return;

        file.clearCachedData();
    }
}
