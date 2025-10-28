package net.pixeldreamstudios.morequesttypes.fabric;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.event.BreakBlockEventBuffer;

public final class BreakBlockHooksFabric {
    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (player instanceof ServerPlayer sp) {
                long gt = world.getGameTime();
                var tool = sp.getMainHandItem();
                BreakBlockEventBuffer.push(sp.getUUID(), state, tool, gt);
            }
        });
    }
    private BreakBlockHooksFabric() {}
}
