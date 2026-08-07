package net.pixeldreamstudios.morequesttypes.mixin.client;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.OtherButtonsPanelBottom;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.morequesttypes.network.MQTRefreshTaskCacheRequest;
import net.pixeldreamstudios.morequesttypes.network.NetworkHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = OtherButtonsPanelBottom.class, remap = false)
public abstract class OtherButtonsPanelBottomMixin extends Panel {
    @Unique
    private static final int MQT$TAB_BUTTON_WIDTH = 20;

    @Unique
    private static final int MQT$TAB_BUTTON_HEIGHT = 18;

    protected OtherButtonsPanelBottomMixin(Panel panel) {
        super(panel);
    }

    @Inject(method = "addWidgets", at = @At("HEAD"))
    private void morequesttypes$addRefreshButton(CallbackInfo ci) {
        if (!ClientQuestFile.canClientPlayerEdit()) return;

        SimpleButton refreshButton = new SimpleButton(this,
                List.of(Component.translatable("morequesttypes.gui.refresh_task_cache"),
                        Component.translatable("morequesttypes.gui.refresh_task_cache.tooltip")
                                .withStyle(ChatFormatting.GRAY)),
                Icon.getIcon("minecraft:item/clock_00"),
                (widget, button) -> NetworkHelper.sendToServer(new MQTRefreshTaskCacheRequest()));
        refreshButton.setSize(MQT$TAB_BUTTON_WIDTH, MQT$TAB_BUTTON_HEIGHT);
        add(refreshButton);
    }
}
