// net/pixeldreamstudios/morequesttypes/net/MQTStructuresRequest.java
package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;

public record MQTStructuresRequest() implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "structures_req");
    public static final Type<MQTStructuresRequest> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, MQTStructuresRequest> STREAM_CODEC = StreamCodec.unit(new MQTStructuresRequest());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MQTStructuresRequest self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            if (ctx.getPlayer() instanceof ServerPlayer sp && sp.getServer() != null) {
                NetworkManager.sendToPlayer(sp, MQTStructuresResponse.create(sp.getServer()));
            }
        });
    }
}
