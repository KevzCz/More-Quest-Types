package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;

public record MQTLoottablesRequest() implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "loot_tables_req");
    public static final Type<MQTLoottablesRequest> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, MQTLoottablesRequest> STREAM_CODEC =
            StreamCodec.unit(new MQTLoottablesRequest());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MQTLoottablesRequest self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            if (ctx.getPlayer() instanceof ServerPlayer sp && sp.getServer() != null) {
                var batches = MQTLoottablesResponse.createBatches(sp.getServer());
                for (var batch : batches) {
                    NetworkManager.sendToPlayer(sp, batch);
                }
            }
        });
    }
}