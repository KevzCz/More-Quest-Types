package net.pixeldreamstudios.morequesttypes.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.event.FishingCatchEventBuffer;

@EventBusSubscriber(modid = MoreQuestTypes.MOD_ID)
public final class FishingCatchHooksNeoForge {
    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            long gt = sp.level().getGameTime();

            for (ItemStack stack : e.getDrops()) {
                if (!stack.isEmpty()) {
                    FishingCatchEventBuffer.push(sp.getUUID(), stack, "", gt);
                }
            }
        }
    }

    private FishingCatchHooksNeoForge() {}
}