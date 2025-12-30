package net.pixeldreamstudios.morequesttypes.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.pixeldreamstudios.morequesttypes.mixin.client.accessor.QuestScreenAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = QuestPanel.class, remap = false)
public abstract class QuestPanelCrosshairMixin {

    @Shadow @Final private QuestScreen questScreen;
    @Shadow protected double questX;
    @Shadow protected double questY;
    @Shadow private double questMinX;
    @Shadow private double questMinY;
    @Shadow private double questMaxX;
    @Shadow private double questMaxY;

    @Inject(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void mqt$drawCrosshairWhenMoving(GuiGraphics graphics, dev.ftb.mods.ftblibrary.ui.Theme theme,
                                             int x, int y, int w, int h, CallbackInfo ci) {
        QuestScreenAccessor accessor = (QuestScreenAccessor) questScreen;
        QuestPanel panel = (QuestPanel) (Object) this;

        if (! QuestScreenAccessor.mqt$isGridEnabled() || questScreen.isViewingQuest()) return;
        if (!accessor.mqt$isMovingObjects() || accessor.mqt$getSelectedObjects().isEmpty()) return;

        double dx = questMaxX - questMinX;
        double dy = questMaxY - questMinY;

        double px = panel.getX() - panel.getScrollX();
        double py = panel.getY() - panel.getScrollY();

        double scrollWidth = accessor.mqt$getScrollWidth();
        double scrollHeight = accessor.mqt$getScrollHeight();

        double ominX = (questX - questMinX) / dx * scrollWidth + px;
        double ominY = (questY - questMinY) / dy * scrollHeight + py;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 1000.0F);

        Color4I.WHITE.draw(graphics, (int)Math.round(ominX), (int)Math.round(ominY), 1, 1);
        Color4I.WHITE.withAlpha(30).draw(graphics, panel.getX(), (int)ominY, panel.getWidth(), 1);
        Color4I.WHITE.withAlpha(30).draw(graphics, (int)Math.round(ominX), panel.getY(), 1, panel.getHeight());

        poseStack.popPose();
    }
}