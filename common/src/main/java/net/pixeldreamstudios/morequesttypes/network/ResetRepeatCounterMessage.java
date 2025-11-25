package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbquests.net.SyncTeamDataMessage;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.util.ProgressChange;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.api.ITeamDataExtension;

public record ResetRepeatCounterMessage(long questId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ResetRepeatCounterMessage> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "reset_repeat_counter"));

    public static final StreamCodec<FriendlyByteBuf, ResetRepeatCounterMessage> STREAM_CODEC = StreamCodec.of(
            ResetRepeatCounterMessage::write,
            ResetRepeatCounterMessage::read
    );

    public static void write(FriendlyByteBuf buf, ResetRepeatCounterMessage message) {
        buf.writeLong(message.questId);
    }

    public static ResetRepeatCounterMessage read(FriendlyByteBuf buf) {
        return new ResetRepeatCounterMessage(buf.readLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResetRepeatCounterMessage message, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                Quest quest = ServerQuestFile.INSTANCE.getQuest(message.questId);
                if (quest != null && quest.canBeRepeated()) {
                    ServerQuestFile.INSTANCE.getTeamData(player).ifPresent(teamData -> {
                        ((ITeamDataExtension) teamData).resetQuestCompletionCount(quest.id, player.getUUID());

                        ProgressChange progressChange = new ProgressChange(quest, player.getUUID());
                        progressChange.setReset(true);
                        quest.forceProgress(teamData, progressChange);

                        teamData.markDirty();

                        NetworkManager.sendToPlayer(player, new SyncTeamDataMessage(teamData));
                    });
                }
            }
        });
    }
}