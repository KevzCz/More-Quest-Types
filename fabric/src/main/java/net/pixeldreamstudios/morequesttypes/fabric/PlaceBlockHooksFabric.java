package net.pixeldreamstudios.morequesttypes.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.pixeldreamstudios.morequesttypes.event.PlaceBlockEventBuffer;

public final class PlaceBlockHooksFabric {
    public static void register() {
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayer sp) {
                BlockPos pos = hitResult.getBlockPos().relative(hitResult.getDirection());
                world.getServer().execute(() -> {
                    BlockState state = world.getBlockState(pos);
                    if (!state.isAir()) {
                        PlaceBlockEventBuffer.push(sp.getUUID(), pos, state, world.getGameTime());
                    }
                });
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
    }

    private PlaceBlockHooksFabric() {}
}