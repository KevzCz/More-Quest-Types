package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class BlabberCompatImpl {
    private BlabberCompatImpl() {}

    public static boolean isLoaded() {
        return false;
    }

    public static @Nullable ResourceLocation getCurrentDialogueId(ServerPlayer player) {
        return null;
    }

    public static @Nullable String getCurrentDialogueStateKey(ServerPlayer player) {
        return null;
    }

    public static void startDialogue(ServerPlayer player, ResourceLocation id, @Nullable Entity interlocutor) {
    }
}
