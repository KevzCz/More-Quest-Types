package net.pixeldreamstudios.morequesttypes.neoforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.event.TradingEventBuffer;

@EventBusSubscriber(modid = MoreQuestTypes.MOD_ID)
public final class TradingHooksNeoForge {
    private TradingHooksNeoForge() {}

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var merchant = event.getAbstractVillager();
            var offer = event.getMerchantOffer();
            if (offer != null && merchant != null) {
                TradingEventBuffer.record(
                        player.getUUID(),
                        merchant,
                        offer.getCostA().copy(),
                        offer.getResult().copy(),
                        player.level().getGameTime()
                );
            }
        }
    }
}