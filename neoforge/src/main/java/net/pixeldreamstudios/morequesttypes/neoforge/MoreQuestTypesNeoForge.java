package net.pixeldreamstudios.morequesttypes.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.client.MoreQuestTypesClient;
import net.pixeldreamstudios.morequesttypes.commands.MoreQuestTypesCommands;
import net.pixeldreamstudios.morequesttypes.network.MQTNetwork;
import net.pixeldreamstudios.morequesttypes.rewards.manager.AttributeRewardManager;
import net.pixeldreamstudios.morequesttypes.rewards.manager.EquipmentBonusManager;
import net.pixeldreamstudios.morequesttypes.rewards.manager.SpellRewardManager;

@Mod(MoreQuestTypes.MOD_ID)
public final class MoreQuestTypesNeoForge {
    public MoreQuestTypesNeoForge(IEventBus modBus) {
        MQTDataComponents.DATA_COMPONENTS.register(modBus);
        EquipmentBonusManager.init(MQTDataComponents.EQUIPMENT_BONUSES);

        MoreQuestTypes.init();
        MQTNetwork.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            initClient();
        }
    }

    private void initClient() {
        MoreQuestTypesClient.init();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MoreQuestTypesCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getServer().execute(() -> {
                AttributeRewardManager.syncForPlayer(player);
                SpellRewardManager.syncForPlayer(player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getServer().execute(() -> {
                AttributeRewardManager.syncForPlayer(player);
                SpellRewardManager.syncForPlayer(player);
            });
        }
    }
}