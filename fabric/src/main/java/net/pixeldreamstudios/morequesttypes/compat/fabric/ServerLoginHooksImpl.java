package net.pixeldreamstudios.morequesttypes.compat.fabric;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.pixeldreamstudios.morequesttypes.network.MQTStructuresResponse;
import net.pixeldreamstudios.morequesttypes.network.MQTWorldsResponse;
import net.pixeldreamstudios.morequesttypes.network.MQTBiomesResponse;
import dev.architectury.networking.NetworkManager;

public final class ServerLoginHooksImpl {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            NetworkManager.sendToPlayer(handler.player, MQTStructuresResponse.create(server));
            NetworkManager.sendToPlayer(handler.player, MQTWorldsResponse.create(server));
            NetworkManager.sendToPlayer(handler.player, MQTBiomesResponse.create(server));
        });
    }
}
