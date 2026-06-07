package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.client.LastReceivedDamageClientCache;

public record MQTLastReceivedDamagePacket(String damageType, String sourceEntityType, long amount)
        implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "last_received_damage");
    public static final Type<MQTLastReceivedDamagePacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, MQTLastReceivedDamagePacket> STREAM_CODEC =
            StreamCodec.of(MQTLastReceivedDamagePacket::write, MQTLastReceivedDamagePacket::read);

    public static void write(FriendlyByteBuf buf, MQTLastReceivedDamagePacket packet) {
        buf.writeUtf(packet.damageType());
        buf.writeUtf(packet.sourceEntityType());
        buf.writeVarInt((int) Math.min(packet.amount(), Integer.MAX_VALUE));
    }

    public static MQTLastReceivedDamagePacket read(FriendlyByteBuf buf) {
        return new MQTLastReceivedDamagePacket(buf.readUtf(), buf.readUtf(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MQTLastReceivedDamagePacket self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> LastReceivedDamageClientCache.update(
                ResourceLocation.tryParse(self.damageType()),
                self.sourceEntityType().isBlank() ? null : ResourceLocation.tryParse(self.sourceEntityType()),
                self.amount()
        ));
    }
}
