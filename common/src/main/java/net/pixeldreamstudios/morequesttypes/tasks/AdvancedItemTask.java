package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import net.pixeldreamstudios.morequesttypes.config.ItemNbtConfigPanels;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.util.ItemNbtMatcher;

import java.util.ArrayList;
import java.util.List;

public final class AdvancedItemTask extends ItemTask {
    private final List<String> nbtFilters = new ArrayList<>();
    private final List<String> nbtIgnorePaths = new ArrayList<>();

    public AdvancedItemTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.ITEM_ADVANCED;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        ItemNbtMatcher.bindSubmittingPlayer(player.getUUID(), player.getGameProfile().getName(), player.registryAccess());
        try {
            super.submitTask(teamData, player, craftedItem);
        } finally {
            ItemNbtMatcher.clearSubmittingPlayer();
        }
    }

    @Override
    public boolean test(ItemStack stack) {
        if (!super.test(stack)) {
            return false;
        }
        if (nbtFilters.isEmpty()) {
            return true;
        }

        ItemNbtMatcher.SubmittingPlayer player = ItemNbtMatcher.submittingPlayer();
        if (player != null) {
            return ItemNbtMatcher.matches(stack, nbtFilters, nbtIgnorePaths,
                    player.uuid(), player.name(), player.provider());
        }
        return ItemNbtMatcher.matches(stack, nbtFilters, nbtIgnorePaths, getQuestFile().holderLookup());
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (!nbtFilters.isEmpty()) {
            ListTag list = new ListTag();
            for (String s : nbtFilters) list.add(StringTag.valueOf(s));
            nbt.put("nbt_filters", list);
        }
        if (!nbtIgnorePaths.isEmpty()) {
            ListTag list = new ListTag();
            for (String s : nbtIgnorePaths) list.add(StringTag.valueOf(s));
            nbt.put("nbt_ignore_paths", list);
        }
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        nbtFilters.clear();
        ListTag filters = nbt.getList("nbt_filters", Tag.TAG_STRING);
        for (int i = 0; i < filters.size(); i++) nbtFilters.add(filters.getString(i));
        nbtIgnorePaths.clear();
        ListTag ignore = nbt.getList("nbt_ignore_paths", Tag.TAG_STRING);
        for (int i = 0; i < ignore.size(); i++) nbtIgnorePaths.add(ignore.getString(i));
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeVarInt(nbtFilters.size());
        for (String s : nbtFilters) buf.writeUtf(s);
        buf.writeVarInt(nbtIgnorePaths.size());
        for (String s : nbtIgnorePaths) buf.writeUtf(s);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        nbtFilters.clear();
        int filterCount = buf.readVarInt();
        for (int i = 0; i < filterCount; i++) nbtFilters.add(buf.readUtf());
        nbtIgnorePaths.clear();
        int ignoreCount = buf.readVarInt();
        for (int i = 0; i < ignoreCount; i++) nbtIgnorePaths.add(buf.readUtf());
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        ItemNbtConfigPanels.addNbtMatching(config, nbtFilters, nbtIgnorePaths, getItemStack());
    }

}
