package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;

public record MQTSkillsCategoriesRequest() implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "skills_categories_req");
    public static final Type<MQTSkillsCategoriesRequest> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, MQTSkillsCategoriesRequest> STREAM_CODEC = StreamCodec.unit(new MQTSkillsCategoriesRequest());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MQTSkillsCategoriesRequest self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            if (! SkillsCompat.isLoaded()) {
                return;
            }

            if (ctx.getPlayer() instanceof ServerPlayer sp) {
                NetworkHelper.sendToPlayer(sp, MQTSkillsCategoriesResponse.create(sp));
            }
        });
    }
}