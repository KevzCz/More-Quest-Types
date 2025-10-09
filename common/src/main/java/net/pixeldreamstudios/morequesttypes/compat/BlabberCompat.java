package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class BlabberCompat {
    private BlabberCompat() {}

    @ExpectPlatform
    public static boolean isLoaded() { throw new AssertionError(); }

    @ExpectPlatform
    public static @Nullable ResourceLocation getCurrentDialogueId(ServerPlayer player) { throw new AssertionError(); }

    @ExpectPlatform
    public static @Nullable String getCurrentDialogueStateKey(ServerPlayer player) { throw new AssertionError(); }

    @ExpectPlatform
    public static void startDialogue(ServerPlayer player, ResourceLocation id, @Nullable Entity interlocutor) { throw new AssertionError(); }
}
