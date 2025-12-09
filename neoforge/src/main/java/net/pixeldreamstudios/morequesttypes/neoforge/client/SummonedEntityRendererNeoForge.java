package net.pixeldreamstudios.morequesttypes.neoforge.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.pixeldreamstudios.morequesttypes.client.summon.SummonedEntityRenderer;

public final class SummonedEntityRendererNeoForge {
    private SummonedEntityRendererNeoForge() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        try {
            SummonedEntityRenderer.render(
                    event.getPoseStack(),
                    event.getCamera(),
                    mc.level,
                    event.getPartialTick().getGameTimeDeltaPartialTick(false)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void init() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(SummonedEntityRendererNeoForge::onRenderLevelStage);
    }
}