package net.pixeldreamstudios.morequesttypes.fabric;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;

public final class UseBlockHooksFabric {
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> InteractionResult.PASS);
    }

    private UseBlockHooksFabric() {}
}