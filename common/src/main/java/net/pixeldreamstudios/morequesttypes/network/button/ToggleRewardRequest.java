package net.pixeldreamstudios.morequesttypes.network.button;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;

public record ToggleRewardRequest(long rewardId) implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "toggle_reward_req");

    public static final Type<ToggleRewardRequest> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, ToggleRewardRequest> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ToggleRewardRequest decode(FriendlyByteBuf buf) {
                    long id = buf.readVarLong();
                    return new ToggleRewardRequest(id);
                }

                @Override
                public void encode(FriendlyByteBuf buf, ToggleRewardRequest value) {
                    buf.writeVarLong(value.rewardId());
                }
            };


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleRewardRequest self, PacketContext ctx) {
        ctx.queue(() -> {
            if (ctx.getPlayer() instanceof ServerPlayer player) {
                ToggleRewardHandler.handleToggle(player, self.rewardId());
            }
        });
    }
}
