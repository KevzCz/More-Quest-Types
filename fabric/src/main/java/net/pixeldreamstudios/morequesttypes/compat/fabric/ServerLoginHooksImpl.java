package net.pixeldreamstudios.morequesttypes.compat.fabric;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;
import net.pixeldreamstudios.morequesttypes.network.*;

public final class ServerLoginHooksImpl {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            NetworkManager.sendToPlayer(handler.player, MQTStructuresResponse.create(server));
            NetworkManager.sendToPlayer(handler.player, MQTWorldsResponse.create(server));
            NetworkManager.sendToPlayer(handler.player, MQTBiomesResponse.create(server));
            NetworkManager.sendToPlayer(handler.player, MQTSoundsResponse.create(server));
            NetworkManager.sendToPlayer(handler.player, MQTLoottablesResponse.create(server));

            if (SkillsCompat.isLoaded()) {
                NetworkManager.sendToPlayer(handler.player, MQTSkillsCategoriesResponse.create(handler.player));
            }
        });
    }
}