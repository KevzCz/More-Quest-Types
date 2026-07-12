package net.pixeldreamstudios.morequesttypes.fabric;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionResultHolder;

public final class UseItemHooksFabric {
    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) ->
                InteractionResultHolder.pass(player.getItemInHand(hand)));
    }

    private UseItemHooksFabric() {}
}
