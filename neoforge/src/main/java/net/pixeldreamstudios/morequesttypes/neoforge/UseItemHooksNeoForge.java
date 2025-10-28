package net.pixeldreamstudios.morequesttypes.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.event.UseItemEventBuffer;

@EventBusSubscriber(modid = MoreQuestTypes.MOD_ID)
public final class UseItemHooksNeoForge {
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            long gt = sp.level().getGameTime();
            var hand = e.getHand();
            var stack = e.getItemStack();
            UseItemEventBuffer.push(sp.getUUID(), hand, stack.copy(), gt);
        }
    }
    private UseItemHooksNeoForge() {}
}
