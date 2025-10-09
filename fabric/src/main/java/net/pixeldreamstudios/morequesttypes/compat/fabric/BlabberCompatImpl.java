package net.pixeldreamstudios.morequesttypes.compat.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.blabber.Blabber;
import org.ladysnake.blabber.impl.common.PlayerDialogueTracker;

public final class BlabberCompatImpl {
    private static final String MOD_ID = "blabber";

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static @Nullable ResourceLocation getCurrentDialogueId(ServerPlayer player) {
        if (!isLoaded()) return null;
        var opt = PlayerDialogueTracker.get(player).getCurrentDialogue();
        if (opt.isEmpty()) return null;
        var id = opt.get().getId();
        return ResourceLocation.tryParse(id.toString());
    }

    public static @Nullable String getCurrentDialogueStateKey(ServerPlayer player) {
        if (!isLoaded()) return null;
        var opt = PlayerDialogueTracker.get(player).getCurrentDialogue();
        if (opt.isEmpty()) return null;
        return opt.get().getCurrentStateKey();
    }

    public static void startDialogue(ServerPlayer player, ResourceLocation id, @Nullable Entity interlocutor) {
        if (!isLoaded()) return;
        Blabber.startDialogue(player, id, interlocutor);
    }

    private BlabberCompatImpl() {}
}
