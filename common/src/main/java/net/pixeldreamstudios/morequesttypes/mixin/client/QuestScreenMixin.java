package net.pixeldreamstudios.morequesttypes.mixin.client;

import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.net.EditObjectMessage;
import dev.ftb.mods.ftbquests.quest.Movable;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.morequesttypes.api.IQuestExtension;
import net.pixeldreamstudios.morequesttypes.network.NetworkHelper;
import net.pixeldreamstudios.morequesttypes.network.ResetRepeatCounterMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = QuestScreen.class, remap = false)
public class QuestScreenMixin {

    @Inject(method = "addObjectMenuItems", at = @At("TAIL"), remap = false)
    @SuppressWarnings("unchecked")
    private void addCustomMenuItems(List<ContextMenuItem> contextMenu, Runnable gui,
                                    QuestObjectBase object, Movable deletionFocus, CallbackInfo ci) {
        if (object instanceof Quest quest) {
            IQuestExtension ext = (IQuestExtension) (Object) quest;

            boolean currentlyInvisible = ext.isAlwaysInvisible();
            contextMenu.add(new ContextMenuItem(
                    Component.translatable("morequesttypes.quest.always_invisible.toggle",
                            Component.translatable(currentlyInvisible ? "gui.yes" : "gui.no")),
                    Icons.COLOR_BLANK,
                    (button) -> {
                        ext.setAlwaysInvisible(!currentlyInvisible);
                        NetworkHelper.sendToServer(EditObjectMessage.forQuestObject(quest));
                        if (gui != null) {
                            gui.run();
                        }
                    }
            ));

            if (quest.canBeRepeated() && ext.getMaxRepeats() > 0) {
                contextMenu.add(new ContextMenuItem(
                        Component.translatable("morequesttypes.quest.reset_repeat_counter"),
                        Icons.REFRESH,
                        (button) -> {
                            NetworkHelper.sendToServer(new ResetRepeatCounterMessage(quest.id));
                        }
                ).setYesNoText(Component.translatable("morequesttypes.quest.reset_repeat_counter.confirm")));
            }
        }
    }
}