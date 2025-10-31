package net.pixeldreamstudios.morequesttypes.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.commands.MoreQuestTypesCommands;
import net.pixeldreamstudios.morequesttypes.rewards.manager.AttributeRewardManager;
import net.pixeldreamstudios.morequesttypes.rewards.manager.SpellRewardManager;

public final class MoreQuestTypesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MoreQuestTypes.init();
        InteractEntityHooksFabric.register();
        UseItemHooksFabric.register();
        BreakBlockHooksFabric.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MoreQuestTypesCommands.register(dispatcher);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            server.execute(() -> {
                AttributeRewardManager.syncForPlayer(player);
                SpellRewardManager.syncForPlayer(player);
            });
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 100 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                AttributeRewardManager.syncForPlayer(player);
                SpellRewardManager.syncForPlayer(player);
            }
        });
    }

}
