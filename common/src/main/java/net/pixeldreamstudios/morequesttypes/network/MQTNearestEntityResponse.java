package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.tasks.FindEntityTask;

public record MQTNearestEntityResponse(long taskId, double meters) implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "nearest_resp");
    public static final Type<MQTNearestEntityResponse> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, MQTNearestEntityResponse> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, MQTNearestEntityResponse::taskId,
                    ByteBufCodecs.DOUBLE,  MQTNearestEntityResponse::meters,
                    MQTNearestEntityResponse::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MQTNearestEntityResponse self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            var mc = Minecraft.getInstance();
            if (mc.level == null) return;

            FindEntityTask.updateClientNearest(
                    self.taskId(), self.meters(), mc.level.getGameTime()
            );
            var cfile = dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE;
            var obj   = cfile.get(self.taskId());
            if (obj instanceof dev.ftb.mods.ftbquests.quest.task.Task t) {
                t.clearCachedData();
            }

            dev.ftb.mods.ftbquests.client.PinnedQuestsTracker.INSTANCE.refresh();

            cfile.refreshGui();
        });
    }

}
