package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;
import net.pixeldreamstudios.morequesttypes.network.*;
import net.pixeldreamstudios.morequesttypes.rewards.summon.SummonedEntityTracker;

@EventBusSubscriber(modid = MoreQuestTypes.MOD_ID)
public final class ServerLoginHooksImpl {
    private static boolean firstJoin = true;

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

        if (SkillsCompat.isLoaded()) {
            NetworkManager.sendToPlayer(sp, MQTSkillsCategoriesResponse.create(sp));
        }

        if (firstJoin) {
            firstJoin = false;
            for (ServerLevel level : server.getAllLevels()) {
                SummonedEntityTracker.restoreFromWorld(level);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        var server = sp.getServer();
        if (server == null) return;

        if (server.getPlayerList().getPlayerCount() == 0) {
            firstJoin = true;
            SummonedEntityTracker.clearRestoredLevels();
        }
    }
}