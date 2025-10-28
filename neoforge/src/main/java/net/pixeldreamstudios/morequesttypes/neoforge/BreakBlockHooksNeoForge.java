package net.pixeldreamstudios.morequesttypes.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.event.BreakBlockEventBuffer;

@EventBusSubscriber(modid = MoreQuestTypes.MOD_ID)
public final class BreakBlockHooksNeoForge {
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.getPlayer() instanceof ServerPlayer sp) {
            long gt = sp.level().getGameTime();
            var tool = sp.getMainHandItem();
            BreakBlockEventBuffer.push(sp.getUUID(), e.getState(), tool, gt);
        }
    }
    private BreakBlockHooksNeoForge() {}
}
