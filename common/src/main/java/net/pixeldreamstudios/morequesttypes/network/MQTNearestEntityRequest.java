package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.tasks.FindEntityTask;

public record MQTNearestEntityRequest(long taskId) implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "nearest_req");
    public static final Type<MQTNearestEntityRequest> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, MQTNearestEntityRequest> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, MQTNearestEntityRequest::taskId,
                    MQTNearestEntityRequest::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MQTNearestEntityRequest self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer sp)) return;

            var file = dev.ftb.mods.ftbquests.quest.ServerQuestFile.INSTANCE;
            var obj  = file.get(self.taskId());
            if (obj instanceof FindEntityTask t) {
                var teamOpt = file.getTeamData(sp);
                if (teamOpt.isEmpty()) return;
                var team = teamOpt.get();
                if (team.isCompleted(t)) {
                    return;
                }

                double meters = t.nearestDistanceServer(sp);
                NetworkManager.sendToPlayer(sp, new MQTNearestEntityResponse(self.taskId(), meters));
            }
        });
    }

}
