package net.pixeldreamstudios.morequesttypes.mixin.client.accessor;

import dev.ftb.mods.ftblibrary.ui.BlankPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ViewQuestPanel.class, remap = false)
public interface ViewQuestPanelAccessor {

    @Accessor("panelTasks")
    BlankPanel mqt$getPanelTasks();

    @Accessor("panelRewards")
    BlankPanel mqt$getPanelRewards();

    @Accessor("panelContent")
    BlankPanel mqt$getPanelContent();
}