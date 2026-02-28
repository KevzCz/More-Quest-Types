package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.*;
import dev.ftb.mods.ftblibrary.util.*;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.task.*;
import net.fabricmc.api.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.pixeldreamstudios.morequesttypes.compat.*;

public final class EasyNPCDialogueTask extends Task {
    private String dialogueLabel = "";

    public EasyNPCDialogueTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.EASY_NPC_DIALOGUE;
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
        list.add(Component.translatable("morequesttypes.task.easynpc_dialogue.label_tooltip", dialogueLabel.isBlank() ? "?" : dialogueLabel));
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 20;
    }

    @Override
    public boolean checkOnLogin() {
        return true;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;
        if (!EasyNPCCompat.isLoaded()) return;
        if (dialogueLabel.isBlank()) return;

        if (EasyNPCCompat.hasCompletedDialogue(player, dialogueLabel)) {
            teamData.setProgress(this, 1L);
            EasyNPCCompat.clearDialogueCompletion(player, dialogueLabel);
        }
    }

    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addString("dialogue_label", dialogueLabel, v -> dialogueLabel = v.trim(), "")
                .setNameKey("morequesttypes.task.easynpc_dialogue.dialogue_label");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (!dialogueLabel.isBlank()) nbt.putString("dialogue_label", dialogueLabel);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        dialogueLabel = nbt.getString("dialogue_label");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(dialogueLabel);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        dialogueLabel = buffer.readUtf();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        String s = dialogueLabel.isBlank() ? "?" : dialogueLabel;
        return Component.translatable("morequesttypes.task.easynpc_dialogue.title", s);
    }
}