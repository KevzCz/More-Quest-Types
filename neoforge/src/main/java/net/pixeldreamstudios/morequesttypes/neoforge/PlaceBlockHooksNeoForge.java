package net.pixeldreamstudios.morequesttypes.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.event.PlaceBlockEventBuffer;

@EventBusSubscriber(modid = MoreQuestTypes.MOD_ID)
public final class PlaceBlockHooksNeoForge {
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            long gt = sp.level().getGameTime();
            PlaceBlockEventBuffer.push(sp.getUUID(), e.getPos(), e.getPlacedBlock(), gt);
        }
    }

    private PlaceBlockHooksNeoForge() {}
}