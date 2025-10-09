package net.pixeldreamstudios.morequesttypes.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.interact.InteractEventBuffer;

@EventBusSubscriber(modid = MoreQuestTypes.MOD_ID)
public final class InteractEntityHooksNeoForge {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            long gt = sp.level().getGameTime();
            var hand = event.getHand();
            var stack = event.getItemStack();
            InteractEventBuffer.push(sp.getUUID(), event.getTarget(), hand, stack.copy(), gt);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            long gt = sp.level().getGameTime();
            var hand = event.getHand();
            var stack = event.getItemStack();
            InteractEventBuffer.push(sp.getUUID(), event.getTarget(), hand, stack.copy(), gt);
        }
    }

    private InteractEntityHooksNeoForge() {}
}
