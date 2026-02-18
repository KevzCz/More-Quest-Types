package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;

public record LookAtMessage(CompoundTag data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LookAtMessage> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "look_at"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LookAtMessage> STREAM_CODEC =
            StreamCodec.of(LookAtMessage::write, LookAtMessage::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, LookAtMessage msg) {
        buf.writeNbt(msg.data);
    }

    private static LookAtMessage read(RegistryFriendlyByteBuf buf) {
        return new LookAtMessage(buf.readNbt());
    }

    public static void handle(LookAtMessage msg, NetworkManager.PacketContext context) {
        if (context.getEnv() == EnvType.CLIENT) {
            context.queue(() -> LookAtMessageClient.handleClient(msg));
        }
    }
}