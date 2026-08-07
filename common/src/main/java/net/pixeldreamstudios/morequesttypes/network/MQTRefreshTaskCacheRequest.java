package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;

public record MQTRefreshTaskCacheRequest() implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "refresh_task_cache_req");
    public static final Type<MQTRefreshTaskCacheRequest> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, MQTRefreshTaskCacheRequest> STREAM_CODEC =
            StreamCodec.unit(new MQTRefreshTaskCacheRequest());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MQTRefreshTaskCacheRequest self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            if (!(ctx.getPlayer() instanceof ServerPlayer sp)) return;
            if (!sp.hasPermissions(2)) return;

            ServerQuestFile file = ServerQuestFile.INSTANCE;
            if (file != null) {
                file.clearCachedData();
            }
        });
    }
}
