package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.network.*;

@EventBusSubscriber(modid = MoreQuestTypes.MOD_ID)
public final class ServerLoginHooksImpl {
    public static void register() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        var server = sp.getServer();
        if (server == null) return;
        NetworkManager.sendToPlayer(sp, MQTStructuresResponse.create(server));
        NetworkManager.sendToPlayer(sp, MQTWorldsResponse.create(server));
        NetworkManager.sendToPlayer(sp, MQTBiomesResponse.create(server));
        NetworkManager.sendToPlayer(sp, MQTSoundsResponse.create(server));
        NetworkManager.sendToPlayer(sp, MQTLoottablesResponse.create(server));

    }
}
