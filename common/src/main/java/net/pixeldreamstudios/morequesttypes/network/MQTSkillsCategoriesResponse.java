package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;
import net.pixeldreamstudios.morequesttypes.tasks.SkillsLevelTask;
import net.pixeldreamstudios.morequesttypes.rewards.SkillsLevelReward;

import java.util.HashMap;
import java.util.Map;

public record MQTSkillsCategoriesResponse(Map<String, String> categoryIcons) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "skills_categories_rsp");
    public static final Type<MQTSkillsCategoriesResponse> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, MQTSkillsCategoriesResponse> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(
                    HashMap::new,
                    ByteBufCodecs.STRING_UTF8,
                    ByteBufCodecs.STRING_UTF8
            ),
            MQTSkillsCategoriesResponse::categoryIcons,
            MQTSkillsCategoriesResponse::new
    );

    @Override
    public Type<?  extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static MQTSkillsCategoriesResponse create(ServerPlayer player) {
        if (!SkillsCompat.isLoaded()) {
            return new MQTSkillsCategoriesResponse(new HashMap<>());
        }
        return new MQTSkillsCategoriesResponse(SkillsCompat.getCategoryIconData(player));
    }

    public static void handle(MQTSkillsCategoriesResponse self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            if (! SkillsCompat.isLoaded()) {
                return;
            }

            SkillsLevelTask.syncCategoryIcons(self.categoryIcons());
            SkillsLevelReward.syncCategoryIcons(self.categoryIcons());
        });
    }
}