package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class EasyNPCCompat {
    private EasyNPCCompat() {}

    @ExpectPlatform
    public static boolean isLoaded() { throw new AssertionError(); }

    @ExpectPlatform
    public static void startDialogueByLabel(ServerPlayer player, Entity npc, String dialogLabel) { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean hasDialogueByLabel(Entity entity, String dialogLabel) { throw new AssertionError(); }

    @ExpectPlatform
    public static void markDialogueCompleted(ServerPlayer player, String dialogLabel) { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean hasCompletedDialogue(ServerPlayer player, String dialogLabel) { throw new AssertionError(); }

    @ExpectPlatform
    public static void clearDialogueCompletion(ServerPlayer player, String dialogLabel) { throw new AssertionError(); }
}