package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import net.pixeldreamstudios.morequesttypes.compat.BlabberCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DialogueTask extends Task {
    private String dialogueId = "";
    private final List<String> endStates = new ArrayList<>();
    private transient final Set<UUID> watching = new HashSet<>();
    private transient final Map<UUID, String> lastState = new HashMap<>();

    public DialogueTask(long id, dev.ftb.mods.ftbquests.quest.Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.DIALOGUE;
    }

    @Override
    public long getMaxProgress() {
        return 1L;
    }

    @Override
    public boolean hideProgressNumbers() {
        return true;
    }

    @Override
    public String formatMaxProgress() {
        return "1";
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getButtonText() {
        return Component.empty();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        if (!getQuestFile().canEdit()) return;
        list.add(Component.translatable("morequesttypes.task.dialogue.id_tooltip", dialogueId.isBlank() ? "?" : dialogueId));
        if (!endStates.isEmpty()) {
            list.add(Component.translatable("morequesttypes.task.dialogue.ends", String.join(", ", endStates)));
        }
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 1;
    }

    @Override
    public boolean checkOnLogin() {
        return true;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, net.minecraft.world.item.ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;
        if (!BlabberCompat.isLoaded()) return;
        if (dialogueId.isBlank()) return;

        UUID key = player.getUUID();
        ResourceLocation target = parse(dialogueId);
        if (target == null) return;

        @Nullable ResourceLocation currentId = BlabberCompat.getCurrentDialogueId(player);
        if (currentId != null && currentId.equals(target)) {
            watching.add(key);
            String sk = BlabberCompat.getCurrentDialogueStateKey(player);
            if (sk != null) lastState.put(key, sk);
            return;
        }

        if (currentId == null && watching.remove(key)) {
            String endedAt = lastState.remove(key);
            if (isAllowedEnd(endedAt)) {
                teamData.setProgress(this, 1L);
            }
        }
    }

    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addString("dialogue_id", dialogueId, v -> dialogueId = v.trim(), "")
                .setNameKey("morequesttypes.task.dialogue.dialogue_id");
        config.addList("end_states", endStates, new dev.ftb.mods.ftblibrary.config.StringConfig(), "")
                .setNameKey("morequesttypes.task.dialogue.end_states");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (!dialogueId.isBlank()) nbt.putString("dialogue_id", dialogueId);
        if (!endStates.isEmpty()) {
            ListTag list = new ListTag();
            for (String s : endStates) list.add(StringTag.valueOf(s));
            nbt.put("end_states", list);
        }
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        dialogueId = nbt.getString("dialogue_id");

        endStates.clear();
        ListTag list = nbt.getList("end_states", net.minecraft.nbt.Tag.TAG_STRING);
        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) endStates.add(list.getString(i));
        } else {
            String csv = nbt.getString("end_states_csv");
            if (!csv.isBlank()) {
                for (String part : csv.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) endStates.add(trimmed);
                }
            }
        }
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(dialogueId);
        buffer.writeVarInt(endStates.size());
        for (String s : endStates) buffer.writeUtf(s);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        dialogueId = buffer.readUtf();
        endStates.clear();
        int n = buffer.readVarInt();
        for (int i = 0; i < n; i++) endStates.add(buffer.readUtf());
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        String s = dialogueId.isBlank() ? "?" : dialogueId;
        return Component.translatable("morequesttypes.task.dialogue.title", s);
    }

    private static @Nullable ResourceLocation parse(String s) {
        if (s == null || s.isBlank()) return null;
        return ResourceLocation.tryParse(s);
    }

    private boolean isAllowedEnd(@Nullable String stateKey) {
        if (stateKey == null) return endStates.isEmpty();
        if (endStates.isEmpty()) return true;
        return endStates.contains(stateKey);
    }
}