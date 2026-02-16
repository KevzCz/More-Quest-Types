package net.pixeldreamstudios.morequesttypes.fabric;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.pixeldreamstudios.morequesttypes.event.UseBlockEventBuffer;

public final class UseBlockHooksFabric {
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayer sp) {
                long gt = world.getGameTime();
                var pos = hitResult.getBlockPos();
                var state = world.getBlockState(pos);
                UseBlockEventBuffer.push(sp.getUUID(), pos, state, gt);
            }
            return InteractionResult.PASS;
        });
    }

    private UseBlockHooksFabric() {}
}