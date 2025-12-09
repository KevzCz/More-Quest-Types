package net.pixeldreamstudios.morequesttypes.fabric.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.pixeldreamstudios.morequesttypes.client.summon.SummonedEntityRenderer;

public final class SummonedEntityRendererFabric {
    private SummonedEntityRendererFabric() {}

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(SummonedEntityRendererFabric::onRender);
    }

    private static void onRender(WorldRenderContext context) {
        SummonedEntityRenderer.render(
                context.matrixStack(),
                context.camera(),
                context.world(),
                context.tickCounter().getGameTimeDeltaPartialTick(false)
        );
    }
}