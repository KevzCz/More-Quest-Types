package net.pixeldreamstudios.morequesttypes.neoforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;

@EventBusSubscriber(modid = MoreQuestTypes.MOD_ID)
public final class UseBlockHooksNeoForge {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
    }

    private UseBlockHooksNeoForge() {}
}