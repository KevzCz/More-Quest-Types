package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class OriginsCompat {
    private OriginsCompat() {}

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean hasOrigin(ServerPlayer player, ResourceLocation layerId, ResourceLocation originId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ResourceLocation getOrigin(ServerPlayer player, ResourceLocation layerId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<ResourceLocation> getAllOrigins(ServerPlayer player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<ResourceLocation> getAvailableLayers() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<ResourceLocation> getOriginsForLayer(ResourceLocation layerId) {
        throw new AssertionError();
    }
}