package net.pixeldreamstudios.morequesttypes.fabric.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.morequesttypes.network.*;
import net.pixeldreamstudios.morequesttypes.network.button.ToggleRewardRequest;

public class FabricMQTNetwork {
    private FabricMQTNetwork() {}

    public static void init() {
        // Register payload types
        PayloadTypeRegistry.playC2S().register(MQTStructuresRequest.TYPE, MQTStructuresRequest.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MQTStructuresResponse.TYPE, MQTStructuresResponse.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(MQTWorldsRequest.TYPE, MQTWorldsRequest.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MQTWorldsResponse.TYPE, MQTWorldsResponse.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(MQTBiomesRequest.TYPE, MQTBiomesRequest.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MQTBiomesResponse.TYPE, MQTBiomesResponse.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(MQTSoundsRequest.TYPE, MQTSoundsRequest.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MQTSoundsResponse.TYPE, MQTSoundsResponse.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(MQTNearestEntityRequest.TYPE, MQTNearestEntityRequest.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MQTNearestEntityResponse.TYPE, MQTNearestEntityResponse.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(MQTLoottablesRequest.TYPE, MQTLoottablesRequest.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MQTLoottablesResponse.TYPE, MQTLoottablesResponse.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(ToggleRewardRequest.TYPE, ToggleRewardRequest.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ResetRepeatCounterMessage.TYPE, ResetRepeatCounterMessage.STREAM_CODEC);

        PayloadTypeRegistry.playC2S().register(MQTSkillsCategoriesRequest.TYPE, MQTSkillsCategoriesRequest.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MQTSkillsCategoriesResponse.TYPE, MQTSkillsCategoriesResponse.STREAM_CODEC);

        PayloadTypeRegistry.playS2C().register(QuestEntityDataSyncPacket.TYPE, QuestEntityDataSyncPacket.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(MQTStructuresRequest.TYPE,
                (payload, fabricContext) -> MQTStructuresRequest.handle(payload, wrapServer(fabricContext)));

        ServerPlayNetworking.registerGlobalReceiver(MQTWorldsRequest.TYPE,
                (payload, fabricContext) -> MQTWorldsRequest.handle(payload, wrapServer(fabricContext)));

        ServerPlayNetworking.registerGlobalReceiver(MQTBiomesRequest.TYPE,
                (payload, fabricContext) -> MQTBiomesRequest.handle(payload, wrapServer(fabricContext)));

        ServerPlayNetworking.registerGlobalReceiver(MQTSoundsRequest.TYPE,
                (payload, fabricContext) -> MQTSoundsRequest.handle(payload, wrapServer(fabricContext)));

        ServerPlayNetworking.registerGlobalReceiver(MQTNearestEntityRequest.TYPE,
                (payload, fabricContext) -> MQTNearestEntityRequest.handle(payload, wrapServer(fabricContext)));

        ServerPlayNetworking.registerGlobalReceiver(MQTLoottablesRequest.TYPE,
                (payload, fabricContext) -> MQTLoottablesRequest.handle(payload, wrapServer(fabricContext)));

        ServerPlayNetworking.registerGlobalReceiver(ToggleRewardRequest.TYPE,
                (payload, fabricContext) -> ToggleRewardRequest.handle(payload, wrapServer(fabricContext)));

        ServerPlayNetworking.registerGlobalReceiver(ResetRepeatCounterMessage.TYPE,
                (payload, fabricContext) -> ResetRepeatCounterMessage.handle(payload, wrapServer(fabricContext)));

        ServerPlayNetworking.registerGlobalReceiver(MQTSkillsCategoriesRequest.TYPE,
                (payload, fabricContext) -> MQTSkillsCategoriesRequest.handle(payload, wrapServer(fabricContext)));

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            registerClientReceivers();
        }
    }

    private static NetworkManager.PacketContext wrapServer(ServerPlayNetworking.Context fabricContext) {
        return new NetworkManager.PacketContext() {
            @Override
            public Player getPlayer() {
                return fabricContext.player();
            }

            @Override
            public void queue(Runnable runnable) {
                fabricContext.server().execute(runnable);
            }

            @Override
            public Env getEnvironment() {
                return Env.SERVER;
            }

            @Override
            public RegistryAccess registryAccess() {
                return fabricContext.player().registryAccess();
            }
        };
    }

    @Environment(EnvType.CLIENT)
    private static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(MQTStructuresResponse.TYPE,
                (payload, context) -> MQTStructuresResponse.handle(payload, wrapClient(context)));

        ClientPlayNetworking.registerGlobalReceiver(MQTWorldsResponse.TYPE,
                (payload, context) -> MQTWorldsResponse.handle(payload, wrapClient(context)));

        ClientPlayNetworking.registerGlobalReceiver(MQTBiomesResponse.TYPE,
                (payload, context) -> MQTBiomesResponse.handle(payload, wrapClient(context)));

        ClientPlayNetworking.registerGlobalReceiver(MQTSoundsResponse.TYPE,
                (payload, context) -> MQTSoundsResponse.handle(payload, wrapClient(context)));

        ClientPlayNetworking.registerGlobalReceiver(MQTNearestEntityResponse.TYPE,
                (payload, context) -> MQTNearestEntityResponse.handle(payload, wrapClient(context)));

        ClientPlayNetworking.registerGlobalReceiver(MQTLoottablesResponse.TYPE,
                (payload, context) -> MQTLoottablesResponse.handle(payload, wrapClient(context)));

        ClientPlayNetworking.registerGlobalReceiver(MQTSkillsCategoriesResponse.TYPE,
                (payload, context) -> MQTSkillsCategoriesResponse.handle(payload, wrapClient(context)));

        ClientPlayNetworking.registerGlobalReceiver(QuestEntityDataSyncPacket.TYPE,
                (payload, context) -> QuestEntityDataSyncPacket.handle(payload, wrapClient(context)));
    }

    @Environment(EnvType.CLIENT)
    private static NetworkManager.PacketContext wrapClient(ClientPlayNetworking.Context context) {
        return new NetworkManager.PacketContext() {
            @Override
            public Player getPlayer() {
                return context.player();
            }

            @Override
            public void queue(Runnable runnable) {
                context.client().execute(runnable);
            }

            @Override
            public Env getEnvironment() {
                return Env.CLIENT;
            }

            @Override
            public RegistryAccess registryAccess() {
                return Minecraft.getInstance().level.registryAccess();
            }
        };
    }
}