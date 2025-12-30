package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class OriginsCompatImpl {
    private OriginsCompatImpl() {}

    public static boolean isLoaded() {
        return false;
    }

    public static boolean hasOrigin(ServerPlayer player, ResourceLocation layerId, ResourceLocation originId) {
        return false;
    }

    public static ResourceLocation getOrigin(ServerPlayer player, ResourceLocation layerId) {
        return ResourceLocation.withDefaultNamespace("empty");
    }

    public static List<ResourceLocation> getAllOrigins(ServerPlayer player) {
        return new ArrayList<>();
    }

    public static List<ResourceLocation> getAvailableLayers() {
        return new ArrayList<>();
    }

    public static List<ResourceLocation> getOriginsForLayer(ResourceLocation layerId) {
        return new ArrayList<>();
    }
}