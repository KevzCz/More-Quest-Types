package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

import java.util.*;

public final class EasyNPCCompatImpl {
    private static final String MOD_ID = "easy_npc";
    private static final Map<UUID, Set<String>> COMPLETED_DIALOGUES = new HashMap<>();

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static void startDialogueByLabel(ServerPlayer player, Entity npc, String dialogLabel) {
        if (!isLoaded()) return;
        if (npc instanceof de.markusbordihn.easynpc.entity.easynpc.EasyNPC<?> easyNPC
                && easyNPC instanceof de.markusbordihn.easynpc.entity.easynpc.data.DialogDataCapable<?> dialogCapable) {

            UUID dialogId = dialogCapable.getDialogId(dialogLabel);
            if (dialogId != null) {
                dialogCapable.openDialog(player, dialogId);
            }
        }
    }

    public static boolean hasDialogueByLabel(Entity entity, String dialogLabel) {
        if (!isLoaded()) return false;
        if (entity instanceof de.markusbordihn.easynpc.entity.easynpc.EasyNPC<?> easyNPC
                && easyNPC instanceof de.markusbordihn.easynpc.entity.easynpc.data.DialogDataCapable<?> dialogCapable) {
            return dialogCapable.hasDialog(dialogLabel);
        }
        return false;
    }

    public static void markDialogueCompleted(ServerPlayer player, String dialogLabel) {
        COMPLETED_DIALOGUES.computeIfAbsent(player.getUUID(), k -> new HashSet<>()).add(dialogLabel);
    }

    public static boolean hasCompletedDialogue(ServerPlayer player, String dialogLabel) {
        Set<String> completed = COMPLETED_DIALOGUES.get(player.getUUID());
        return completed != null && completed.contains(dialogLabel);
    }

    public static void clearDialogueCompletion(ServerPlayer player, String dialogLabel) {
        Set<String> completed = COMPLETED_DIALOGUES.get(player.getUUID());
        if (completed != null) {
            completed.remove(dialogLabel);
            if (completed.isEmpty()) {
                COMPLETED_DIALOGUES.remove(player.getUUID());
            }
        }
    }

    private EasyNPCCompatImpl() {}
}