package net.pixeldreamstudios.morequesttypes.mixin.client;

import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.net.ChangeProgressMessage;
import dev.ftb.mods.ftbquests.quest.Quest;
import net.pixeldreamstudios.morequesttypes.mixin.client.accessor.QuestButtonAccessor;
import net.pixeldreamstudios.morequesttypes.mixin.client.accessor.QuestScreenAccessor;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = QuestPanel.class, remap = false)
public abstract class QuestPanelMixin {

    @Shadow
    private QuestScreen questScreen;

    @Shadow
    QuestButton mouseOverQuest;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(Key key, CallbackInfoReturnable<Boolean> cir) {
        if (key.is(GLFW.GLFW_KEY_R) && key.modifiers.shift() && key.modifiers.control()) {
            QuestScreenAccessor accessor = (QuestScreenAccessor) questScreen;
            if (accessor.mqt$getSelectedChapter() != null && ! questScreen.isViewingQuest()) {
                if (mouseOverQuest != null) {
                    Quest hoveredQuest = ((QuestButtonAccessor) mouseOverQuest).mqt$getQuest();
                    ChangeProgressMessage.sendToServer(
                            accessor.mqt$getFile().selfTeamData,
                            hoveredQuest,
                            progressChange -> progressChange.setReset(true)
                    );
                    cir.setReturnValue(true);
                }
            }
        }
    }
}