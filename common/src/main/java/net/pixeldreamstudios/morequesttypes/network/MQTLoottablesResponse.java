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

public record MQTLoottablesResponse(List<String> data) implements CustomPacketPayload {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "loot_tables_rsp");
    public static final Type<MQTLoottablesResponse> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, MQTLoottablesResponse> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    MQTLoottablesResponse::data,
                    MQTLoottablesResponse::new
            );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static MQTLoottablesResponse create(MinecraftServer server) {
        var list = new ArrayList<String>();
        server.reloadableRegistries()
                .getKeys(LootDataType.TABLE.registryKey())
                .stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(list::add);

        server.registryAccess().registry(Registries.LOOT_TABLE).ifPresent(reg -> {
            reg.registryKeySet().stream().map(k -> k.location().toString()).sorted().forEach(s -> {
                if (!list.contains(s)) list.add(s);
            });
        });

        return new MQTLoottablesResponse(list);
    }

    public static void handle(MQTLoottablesResponse self, NetworkManager.PacketContext ctx) {
        ctx.queue(() -> LootTableReward.syncKnownLootTableList(self.data()));
    }
}
