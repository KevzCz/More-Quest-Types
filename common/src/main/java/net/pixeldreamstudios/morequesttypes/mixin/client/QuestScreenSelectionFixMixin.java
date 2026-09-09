package net.pixeldreamstudios.morequesttypes.mixin.client;

import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Movable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

@Mixin(value = QuestScreen.class, remap = false)
public abstract class QuestScreenSelectionFixMixin {

    @Shadow @Final private List<Movable> selectedObjects;

    @Inject(method = "restorePersistedScreenData", at = @At("TAIL"))
    private void mqt$dropUnresolvedSelection(CallbackInfo ci) {
        selectedObjects.removeIf(Objects::isNull);
    }
}
