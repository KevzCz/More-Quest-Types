package net.pixeldreamstudios.morequesttypes.mixin.client;

import dev.ftb.mods.ftblibrary.ui.BlankPanel;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.client.gui.quests.RewardButton;
import dev.ftb.mods.ftbquests.client.gui.quests.TaskButton;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import dev.ftb.mods.ftbquests.net.ChangeProgressMessage;
import dev.ftb.mods.ftbquests.quest.Quest;
import net.pixeldreamstudios.morequesttypes.mixin.client.accessor.QuestScreenAccessor;
import net.pixeldreamstudios.morequesttypes.mixin.client.accessor.RewardButtonAccessor;
import net.pixeldreamstudios.morequesttypes.mixin.client.accessor.TaskButtonAccessor;
import net.pixeldreamstudios.morequesttypes.mixin.client.accessor.ViewQuestPanelAccessor;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ViewQuestPanel.class, remap = false)
public abstract class ViewQuestPanelMixin {

    @Shadow
    @Final
    private QuestScreen questScreen;

    @Shadow
    private Quest quest;

    @Inject(method = "keyReleased", at = @At("HEAD"), cancellable = true)
    private void onKeyReleased(Key key, CallbackInfo ci) {
        if (quest == null) return;

        if (key.is(GLFW.GLFW_KEY_R) && key.modifiers.shift() && key.modifiers.control()) {
            QuestScreenAccessor accessor = (QuestScreenAccessor) questScreen;
            ViewQuestPanelAccessor panelAccessor = (ViewQuestPanelAccessor) this;

            BlankPanel panelTasks = panelAccessor.mqt$getPanelTasks();
            BlankPanel panelRewards = panelAccessor.mqt$getPanelRewards();

            // Check if hovering over a task
            if (panelTasks != null) {
                for (Widget w : panelTasks.getWidgets()) {
                    if (w instanceof TaskButton && w.isMouseOver()) {
                        TaskButtonAccessor taskAccessor = (TaskButtonAccessor) w;
                        ChangeProgressMessage.sendToServer(
                                accessor.mqt$getFile().selfTeamData,
                                taskAccessor.mqt$getTask(),
                                progressChange -> progressChange.setReset(true)
                        );
                        ci.cancel();
                        return;
                    }
                }
            }

            // Check if hovering over a reward
            if (panelRewards != null) {
                for (Widget w : panelRewards.getWidgets()) {
                    if (w instanceof RewardButton && w.isMouseOver()) {
                        RewardButtonAccessor rewardAccessor = (RewardButtonAccessor) w;
                        ChangeProgressMessage.sendToServer(
                                accessor.mqt$getFile().selfTeamData,
                                rewardAccessor.mqt$getReward(),
                                progressChange -> progressChange.setReset(true)
                        );
                        ci.cancel();
                        return;
                    }
                }
            }

            ChangeProgressMessage.sendToServer(
                    accessor.mqt$getFile().selfTeamData,
                    quest,
                    progressChange -> progressChange.setReset(true)
            );
            ci.cancel();
        }
    }
}