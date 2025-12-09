package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.rewards.PlaySoundReward;

import java.util.ArrayList;
import java.util.List;

public record MQTSoundsResponse(List<String> data) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "sounds_rsp");
    public static final Type<MQTSoundsResponse> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, MQTSoundsResponse> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    MQTSoundsResponse::data,
                    MQTSoundsResponse::new
            );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static MQTSoundsResponse create(MinecraftServer server) {
        var list = new ArrayList<String>();

        server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.SOUND_EVENT)
                .registryKeySet().stream()
                .map(k -> k.location().toString())
                .sorted()
                .forEach(list::add);

        return new MQTSoundsResponse(list);
    }

    public static void handle(MQTSoundsResponse self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> PlaySoundReward.syncKnownSoundList(self.data()));
    }
}