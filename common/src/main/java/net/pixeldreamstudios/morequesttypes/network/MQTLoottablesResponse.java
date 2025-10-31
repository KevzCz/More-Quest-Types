package net.pixeldreamstudios.morequesttypes.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.rewards.LootTableReward;

import java.util.ArrayList;
import java.util.List;

public record MQTLoottablesResponse(List<String> data, int batchIndex, int totalBatches) implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "loot_tables_rsp");
    public static final Type<MQTLoottablesResponse> TYPE = new Type<>(ID);

    // Limit each batch to 2000 entries to stay well under packet size limits
    private static final int BATCH_SIZE = 2000;

    public static final StreamCodec<FriendlyByteBuf, MQTLoottablesResponse> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    MQTLoottablesResponse::data,
                    ByteBufCodecs.VAR_INT, MQTLoottablesResponse::batchIndex,
                    ByteBufCodecs.VAR_INT, MQTLoottablesResponse::totalBatches,
                    MQTLoottablesResponse::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static List<MQTLoottablesResponse> createBatches(MinecraftServer server) {
        // Collect all loot tables
        var allTables = new ArrayList<String>();
        server.reloadableRegistries()
                .getKeys(LootDataType.TABLE.registryKey())
                .stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(allTables::add);

        server.registryAccess().registry(Registries.LOOT_TABLE).ifPresent(reg -> {
            reg.registryKeySet().stream().map(k -> k.location().toString()).sorted().forEach(s -> {
                if (!allTables.contains(s)) allTables.add(s);
            });
        });

        // Split into batches
        List<MQTLoottablesResponse> batches = new ArrayList<>();
        int totalBatches = (int) Math.ceil((double) allTables.size() / BATCH_SIZE);

        for (int i = 0; i < allTables.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, allTables.size());
            List<String> batch = allTables.subList(i, end);
            int batchIndex = i / BATCH_SIZE;
            batches.add(new MQTLoottablesResponse(new ArrayList<>(batch), batchIndex, totalBatches));
        }

        return batches.isEmpty() ? List.of(new MQTLoottablesResponse(List.of(), 0, 1)) : batches;
    }

    public static void handle(MQTLoottablesResponse self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> LootTableReward.syncKnownLootTableList(self.data(), self.batchIndex(), self.totalBatches()));
    }
}