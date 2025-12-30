package net.pixeldreamstudios.morequesttypes.compat.fabric;

import dev.architectury.platform.Platform;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayerManager;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class OriginsCompatImpl {
    private static final String MOD_ID = "origins";

    private OriginsCompatImpl() {}

    public static boolean isLoaded() {
        return Platform.isModLoaded(MOD_ID);
    }

    public static boolean hasOrigin(ServerPlayer player, ResourceLocation layerId, ResourceLocation originId) {
        if (! isLoaded()) return false;

        try {
            OriginComponent component = ModComponents.ORIGIN.get(player);
            OriginLayer layer = OriginLayerManager.getNullable(layerId);

            if (layer == null) return false;

            Origin origin = component.getOrigin(layer);
            if (origin == null) return false;

            return origin.getId().equals(originId);
        } catch (Exception e) {
            return false;
        }
    }

    public static ResourceLocation getOrigin(ServerPlayer player, ResourceLocation layerId) {
        if (!isLoaded()) return ResourceLocation.withDefaultNamespace("empty");

        try {
            OriginComponent component = ModComponents.ORIGIN.get(player);
            OriginLayer layer = OriginLayerManager.getNullable(layerId);

            if (layer == null) return ResourceLocation.withDefaultNamespace("empty");

            Origin origin = component.getOrigin(layer);
            if (origin == null) return ResourceLocation.withDefaultNamespace("empty");

            return origin.getId();
        } catch (Exception e) {
            return ResourceLocation.withDefaultNamespace("empty");
        }
    }

    public static List<ResourceLocation> getAllOrigins(ServerPlayer player) {
        List<ResourceLocation> origins = new ArrayList<>();
        if (!isLoaded()) return origins;

        try {
            OriginComponent component = ModComponents.ORIGIN.get(player);
            component.getOrigins().forEach((layer, origin) -> {
                if (origin != null && ! origin.getId().equals(ResourceLocation.withDefaultNamespace("empty"))) {
                    origins.add(origin.getId());
                }
            });
        } catch (Exception e) {
            // Ignore
        }

        return origins;
    }

    public static List<ResourceLocation> getAvailableLayers() {
        List<ResourceLocation> layers = new ArrayList<>();
        if (!isLoaded()) return layers;

        try {
            OriginLayerManager.values().stream()
                    .filter(OriginLayer::isEnabled)
                    .forEach(layer -> layers.add(layer.getId()));
        } catch (Exception e) {
            // Ignore
        }

        return layers;
    }

    public static List<ResourceLocation> getOriginsForLayer(ResourceLocation layerId) {
        List<ResourceLocation> origins = new ArrayList<>();
        if (!isLoaded()) return origins;

        try {
            OriginLayer layer = OriginLayerManager.getNullable(layerId);
            if (layer != null) {
                origins.addAll(layer.getOrigins());
            }
        } catch (Exception e) {
            // Ignore
        }

        return origins;
    }
}