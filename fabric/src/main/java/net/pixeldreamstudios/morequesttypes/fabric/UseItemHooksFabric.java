package net.pixeldreamstudios.morequesttypes.fabric;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.event.UseItemEventBuffer;

public final class UseItemHooksFabric {
    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayer sp) {
                long gt = world.getGameTime();
                ItemStack stack = player.getItemInHand(hand);
                UseItemEventBuffer.push(sp.getUUID(), hand, stack.isEmpty() ? ItemStack.EMPTY : stack, gt);
            }
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        });
    }

    private UseItemHooksFabric() {}
}
