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

import java.util.ArrayList;
import java.util.List;

public record MQTStructuresResponse(List<String> data) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "structures_rsp");
    public static final Type<MQTStructuresResponse> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, MQTStructuresResponse> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    MQTStructuresResponse::data,
                    MQTStructuresResponse::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static MQTStructuresResponse create(MinecraftServer server) {
        var list = new ArrayList<String>();

        server.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .registryKeySet().stream().map(k -> k.location().toString()).sorted().forEach(list::add);

        server.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getTagNames().map(t -> "#"+t.location()).sorted().forEach(list::add);
        return new MQTStructuresResponse(list);
    }

    public static void handle(MQTStructuresResponse self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            var list = self.data();
            net.pixeldreamstudios.morequesttypes.tasks.AdvancedKillTask.syncKnownStructureList(list);
            net.pixeldreamstudios.morequesttypes.tasks.DamageTask.syncKnownStructureList(list);
            net.pixeldreamstudios.morequesttypes.tasks.InteractEntityTask.syncKnownStructureList(list);
        });
    }
}
