package net.pixeldreamstudios.morequesttypes.compat.fabric;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;
import net.pixeldreamstudios.morequesttypes.network.*;
import net.pixeldreamstudios.morequesttypes.rewards.summon.SummonedEntityTracker;

public final class ServerLoginHooksImpl {
    private static boolean firstJoin = true;

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

            if (firstJoin) {
                firstJoin = false;
                for (ServerLevel level : server.getAllLevels()) {
                    SummonedEntityTracker.restoreFromWorld(level);
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (server.getPlayerList().getPlayerCount() == 0) {
                firstJoin = true;
                SummonedEntityTracker.clearRestoredLevels();
            }
        });
    }
}