package net.pixeldreamstudios.morequesttypes.compat.fabric;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import dev.architectury.networking.NetworkManager;
import net.pixeldreamstudios.morequesttypes.network.*;

public final class ServerLoginHooksImpl {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            NetworkManager.sendToPlayer(handler.player, MQTStructuresResponse.create(server));
            NetworkManager.sendToPlayer(handler.player, MQTWorldsResponse.create(server));
            NetworkManager.sendToPlayer(handler.player, MQTBiomesResponse.create(server));
            NetworkManager.sendToPlayer(handler.player, MQTSoundsResponse.create(server));
            NetworkManager.sendToPlayer(handler.player, MQTLoottablesResponse.create(server));
        });
    }
}
