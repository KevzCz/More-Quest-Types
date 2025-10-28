package net.pixeldreamstudios.morequesttypes.fabric;

import dev.architectury.event.EventResult;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.event.InteractEventBuffer;

public final class InteractEntityHooksFabric {

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (player instanceof ServerPlayer sp) {
                long gt = world.getGameTime();
                ItemStack held = player.getItemInHand(hand);
                InteractEventBuffer.push(sp.getUUID(), entity, hand, held.isEmpty() ? ItemStack.EMPTY : held, gt);
            }
            return EventResult.pass().asMinecraft();
        });
    }

    private InteractEntityHooksFabric() {}
}
