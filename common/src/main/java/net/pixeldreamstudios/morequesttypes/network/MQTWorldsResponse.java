package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.tasks.AdvancedKillTask;
import net.pixeldreamstudios.morequesttypes.tasks.DamageTask;
import net.pixeldreamstudios.morequesttypes.tasks.InteractEntityTask;

import java.util.ArrayList;
import java.util.List;

public record MQTWorldsResponse(List<String> data) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "worlds_rsp");
    public static final Type<MQTWorldsResponse> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, MQTWorldsResponse> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    MQTWorldsResponse::data,
                    MQTWorldsResponse::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static MQTWorldsResponse create(MinecraftServer server) {
        var list = new ArrayList<String>();
        // Enumerate actual Level keys (e.g. minecraft:overworld, minecraft:the_nether, ...)
        for (var key : server.levelKeys()) { // Set<ResourceKey<Level>>
            list.add(key.location().toString());
        }
        list.sort(String::compareTo);
        return new MQTWorldsResponse(list);
    }

    public static void handle(MQTWorldsResponse self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            var data = self.data();
            AdvancedKillTask.syncKnownDimensionList(data);
            DamageTask.syncKnownDimensionList(data);
            InteractEntityTask.syncKnownDimensionList(data);
        });
    }
}
