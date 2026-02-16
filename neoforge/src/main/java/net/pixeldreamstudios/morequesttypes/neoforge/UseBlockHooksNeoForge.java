package net.pixeldreamstudios.morequesttypes.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.event.UseBlockEventBuffer;

@EventBusSubscriber(modid = MoreQuestTypes.MOD_ID)
public final class UseBlockHooksNeoForge {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            long gt = sp.level().getGameTime();
            var pos = e.getPos();
            var state = sp.level().getBlockState(pos);
            UseBlockEventBuffer.push(sp.getUUID(), pos, state, gt);
        }
    }

    private UseBlockHooksNeoForge() {}
}