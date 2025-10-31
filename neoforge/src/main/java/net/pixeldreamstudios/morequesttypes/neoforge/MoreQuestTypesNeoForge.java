package net.pixeldreamstudios.morequesttypes.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.commands.MoreQuestTypesCommands;
import net.pixeldreamstudios.morequesttypes.rewards.manager.AttributeRewardManager;

@Mod(MoreQuestTypes.MOD_ID)
public final class MoreQuestTypesNeoForge {
    public MoreQuestTypesNeoForge() {
        MoreQuestTypes.init();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MoreQuestTypesCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getServer().execute(() -> AttributeRewardManager.syncForPlayer(player));
        }
    }
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        var server = event.getServer();
        if (server.getTickCount() % 100 != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            AttributeRewardManager.syncForPlayer(player);
        }
    }
}
