package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.tasks.*;

import java.util.ArrayList;
import java.util.List;

public record MQTBiomesResponse(List<String> data) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "biomes_rsp");
    public static final Type<MQTBiomesResponse> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, MQTBiomesResponse> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    MQTBiomesResponse::data,
                    MQTBiomesResponse::new
            );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static MQTBiomesResponse create(MinecraftServer server) {
        var list = new ArrayList<String>();
        server.registryAccess().registryOrThrow(Registries.BIOME)
                .registryKeySet().stream().map(k -> k.location().toString()).sorted().forEach(list::add);
        server.registryAccess().registryOrThrow(Registries.BIOME)
                .getTagNames().map(t -> "#"+t.location()).sorted().forEach(list::add);
        return new MQTBiomesResponse(list);
    }
    public static void handle(MQTBiomesResponse self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            var data = self.data();
                AdvancedKillTask.syncKnownBiomeList(data);
                DamageTask.syncKnownBiomeList(data);
                InteractEntityTask.syncKnownBiomeList(data);
                UseItemTask.syncKnownBiomeList(data);
                HoldItemTask.syncKnownBiomeList(data);
                FindEntityTask.syncKnownBiomeList(data);
        });
    }
}
