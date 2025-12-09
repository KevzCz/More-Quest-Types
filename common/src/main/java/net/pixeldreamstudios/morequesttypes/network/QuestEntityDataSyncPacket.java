package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.api.IQuestSummonedEntity;

import java.util.UUID;

public record QuestEntityDataSyncPacket(
        int entityId,
        boolean questSummoned,
        UUID ownerUuid,
        String texture,
        float textureScale,
        long rewardId,
        boolean shouldFollow,
        boolean shouldDespawn,
        double textureOffsetX,
        double textureOffsetY,
        double textureOffsetZ
) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            MoreQuestTypes.MOD_ID, "quest_entity_sync"
    );
    public static final Type<QuestEntityDataSyncPacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, QuestEntityDataSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public QuestEntityDataSyncPacket decode(FriendlyByteBuf buf) {
            int entityId = buf.readVarInt();
            boolean questSummoned = buf.readBoolean();
            UUID ownerUuid = buf.readBoolean() ? buf.readUUID() : null;
            String texture = buf.readUtf();
            float textureScale = buf.readFloat();
            long rewardId = buf.readLong();
            boolean shouldFollow = buf.readBoolean();
            boolean shouldDespawn = buf.readBoolean();
            double textureOffsetX = buf.readDouble();
            double textureOffsetY = buf.readDouble();
            double textureOffsetZ = buf.readDouble();
            return new QuestEntityDataSyncPacket(entityId, questSummoned, ownerUuid, texture, textureScale, rewardId, shouldFollow, shouldDespawn, textureOffsetX, textureOffsetY, textureOffsetZ);
        }

        @Override
        public void encode(FriendlyByteBuf buf, QuestEntityDataSyncPacket packet) {
            buf.writeVarInt(packet.entityId);
            buf.writeBoolean(packet.questSummoned);
            buf.writeBoolean(packet.ownerUuid != null);
            if (packet.ownerUuid != null) {
                buf.writeUUID(packet.ownerUuid);
            }
            buf.writeUtf(packet.texture);
            buf.writeFloat(packet.textureScale);
            buf.writeLong(packet.rewardId);
            buf.writeBoolean(packet.shouldFollow);
            buf.writeBoolean(packet.shouldDespawn);
            buf.writeDouble(packet.textureOffsetX);
            buf.writeDouble(packet.textureOffsetY);
            buf.writeDouble(packet.textureOffsetZ);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestEntityDataSyncPacket packet, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Entity entity = mc.level.getEntity(packet.entityId);
            if (entity instanceof IQuestSummonedEntity questEntity) {
                if (packet.shouldDespawn) {
                    entity.discard();
                    return;
                }

                questEntity.setQuestSummoned(packet.questSummoned);
                questEntity.setQuestOwnerUuid(packet.ownerUuid);
                questEntity.setQuestTexture(packet.texture);
                questEntity.setQuestTextureScale(packet.textureScale);
                questEntity.setQuestRewardId(packet.rewardId);
                questEntity.setQuestTextureOffsetX(packet.textureOffsetX);
                questEntity.setQuestTextureOffsetY(packet.textureOffsetY);
                questEntity.setQuestTextureOffsetZ(packet.textureOffsetZ);

                if (questEntity instanceof net.minecraft.world.entity.Mob mob && packet.shouldFollow && packet.ownerUuid != null) {
                    mob.getNavigation().stop();
                }
            }
        });
    }
}