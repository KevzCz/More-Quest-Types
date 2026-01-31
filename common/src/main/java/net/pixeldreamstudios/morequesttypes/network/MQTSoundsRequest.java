package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;

public record MQTSoundsRequest() implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "sounds_req");
    public static final Type<MQTSoundsRequest> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, MQTSoundsRequest> STREAM_CODEC = StreamCodec.unit(new MQTSoundsRequest());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MQTSoundsRequest self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            if (ctx.getPlayer() instanceof ServerPlayer sp && sp.getServer() != null) {
                NetworkHelper.sendToPlayer(sp, MQTSoundsResponse.create(sp.getServer()));
            }
        });
    }

}
