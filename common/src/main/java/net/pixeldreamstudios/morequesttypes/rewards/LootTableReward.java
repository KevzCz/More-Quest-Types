package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.pixeldreamstudios.morequesttypes.network.MQTLoottablesRequest;
import net.pixeldreamstudios.morequesttypes.network.NetworkHelper;

import java.util.ArrayList;
import java.util.List;

public final class LootTableReward extends Reward {
    private static final List<String> KNOWN_LOOT_TABLES = new ArrayList<>();
    public static void syncKnownLootTableList(List<String> data) {
        KNOWN_LOOT_TABLES.clear();
        KNOWN_LOOT_TABLES.addAll(data);
    }
    private String tableId = "minecraft:chests/simple_dungeon";
    private int rolls = 1;
    private boolean respectLuck = true;
    private boolean dropIfFull = true;
    public LootTableReward(long id, dev.ftb.mods.ftbquests.quest.Quest q) { super(id, q); }
    @Override public RewardType getType() { return MoreRewardTypes.LOOT_TABLE; }
    @Override
    public void claim(ServerPlayer player, boolean notify) {
        var server = player.getServer();
        if (server == null) return;

        ResourceLocation rl = ResourceLocation.tryParse(tableId);
        if (rl == null) return;

        var holder = server.reloadableRegistries();
        LootTable table = holder.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, rl));
        if (table == LootTable.EMPTY) return;

        LootParams params = new LootParams.Builder(player.serverLevel())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withLuck(respectLuck ? player.getLuck() : 0.0F)
                .create(LootContextParamSets.CHEST);

        int r = Math.max(1, rolls);
        for (int i = 0; i < r; i++) {
            for (var stack : table.getRandomItems(params)) {
                if (stack.isEmpty()) continue;
                boolean ok = player.getInventory().add(stack.copy());
                if (!ok && dropIfFull) {
                    Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
                }
            }
        }
    }
    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        if (KNOWN_LOOT_TABLES.isEmpty()) {
            NetworkHelper.sendToServer(
                    new MQTLoottablesRequest()
            );
        }

        var choices = new ArrayList<String>();
        if (KNOWN_LOOT_TABLES.isEmpty()) {
            choices.add("minecraft:chests/simple_dungeon");
            choices.add("minecraft:chests/village/village_armorer");
        } else {
            choices.addAll(KNOWN_LOOT_TABLES);
        }

        var TABLES = NameMap.of(tableId, choices)
                .name(s -> Component.literal((s == null || s.isEmpty()) ? "?" : s))
                .create();

        config.addEnum("loot_table", tableId, v -> tableId = v, TABLES)
                .setNameKey("morequesttypes.reward.loot_table.table");

        config.addInt("rolls", rolls, v -> rolls = Math.max(1, v), 1, 1, 64)
                .setNameKey("morequesttypes.reward.loot_table.rolls");

        config.addBool("respect_luck", respectLuck, v -> respectLuck = v, true)
                .setNameKey("morequesttypes.reward.loot_table.respect_luck");

        config.addBool("drop_if_full", dropIfFull, v -> dropIfFull = v, true)
                .setNameKey("morequesttypes.reward.loot_table.drop_if_full");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("table", tableId);
        nbt.putInt("rolls", rolls);
        nbt.putBoolean("respect_luck", respectLuck);
        nbt.putBoolean("drop_if_full", dropIfFull);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        tableId = nbt.getString("table");
        rolls = Math.max(1, nbt.getInt("rolls"));
        respectLuck = nbt.contains("respect_luck") && nbt.getBoolean("respect_luck");
        dropIfFull = !nbt.contains("drop_if_full") || nbt.getBoolean("drop_if_full");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(tableId);
        buf.writeVarInt(rolls);
        buf.writeBoolean(respectLuck);
        buf.writeBoolean(dropIfFull);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        tableId = buf.readUtf();
        rolls = Math.max(1, buf.readVarInt());
        respectLuck = buf.readBoolean();
        dropIfFull = buf.readBoolean();
    }

}
