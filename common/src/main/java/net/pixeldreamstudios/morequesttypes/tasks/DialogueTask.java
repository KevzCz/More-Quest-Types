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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import net.pixeldreamstudios.morequesttypes.compat.BlabberCompat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DialogueTask extends Task {
    private String dialogueId = "";
    private String endStatesCsv = "";
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
        list.add(Component.translatable("ftbquests.morequesttypes.task.dialogue.id", dialogueId.isBlank() ? "?" : dialogueId));
        if (!endStatesCsv.isBlank()) {
            list.add(Component.translatable("ftbquests.morequesttypes.task.dialogue.ends", endStatesCsv));
        }
        if (!BlabberCompat.isLoaded()) {
            list.add(Component.translatable("ftbquests.morequesttypes.task.dialogue.missing_api"));
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
                .setNameKey("ftbquests.task.dialogue.dialogue_id");
        config.addString("end_states_csv", endStatesCsv, v -> endStatesCsv = v.trim(), "")
                .setNameKey("ftbquests.task.dialogue.end_states_csv");
    }


    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (!dialogueId.isBlank()) nbt.putString("dialogue_id", dialogueId);
        if (!endStatesCsv.isBlank()) nbt.putString("end_states_csv", endStatesCsv);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        dialogueId = nbt.getString("dialogue_id");
        endStatesCsv = nbt.getString("end_states_csv");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(dialogueId);
        buffer.writeUtf(endStatesCsv);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        dialogueId = buffer.readUtf();
        endStatesCsv = buffer.readUtf();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        String s = dialogueId.isBlank() ? "?" : dialogueId;
        return Component.translatable("ftbquests.morequesttypes.task.dialogue.title", s);
    }

    private static @Nullable ResourceLocation parse(String s) {
        if (s == null || s.isBlank()) return null;
        return ResourceLocation.tryParse(s);
    }

    private boolean isAllowedEnd(@Nullable String stateKey) {
        if (stateKey == null) return endStatesCsv.isBlank();
        if (endStatesCsv.isBlank()) return true;
        for (String p : endStatesCsv.split(",")) {
            String t = p.trim();
            if (!t.isEmpty() && t.equals(stateKey)) return true;
        }
        return false;
    }
}
