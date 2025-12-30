package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.morequesttypes.api.ITaskOriginExtension;
import net.pixeldreamstudios.morequesttypes.compat.OriginsCompat;
import net.pixeldreamstudios.morequesttypes.tasks.OriginTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = Task.class, remap = false)
public abstract class TaskExtensionsOrigins implements ITaskOriginExtension {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("MoreQuestTypes/OriginExtension");

    @Shadow
    public abstract long getMaxProgress();

    @Unique
    private boolean mqt$checkOrigin = false;
    @Unique
    private ResourceLocation mqt$originLayer = null;
    @Unique
    private ResourceLocation mqt$origin = null;

    @Unique
    private void mqt$ensureInitialized() {
        if (mqt$originLayer == null) {
            mqt$originLayer = ResourceLocation.withDefaultNamespace("empty");
        }
        if (mqt$origin == null) {
            mqt$origin = ResourceLocation.withDefaultNamespace("empty");
        }
    }

    @Unique
    private boolean mqt$shouldApplyOriginExtension() {
        return ! ((Task)(Object)this instanceof OriginTask);
    }

    @Override
    public boolean shouldCheckOrigin() {
        return mqt$checkOrigin;
    }

    @Override
    public void setShouldCheckOrigin(boolean check) {
        this.mqt$checkOrigin = check;
    }

    @Override
    public ResourceLocation getRequiredOriginLayer() {
        mqt$ensureInitialized();
        return mqt$originLayer;
    }

    @Override
    public void setRequiredOriginLayer(ResourceLocation layerId) {
        this.mqt$originLayer = layerId;
    }

    @Override
    public ResourceLocation getRequiredOrigin() {
        mqt$ensureInitialized();
        return mqt$origin;
    }

    @Override
    public void setRequiredOrigin(ResourceLocation originId) {
        this.mqt$origin = originId;
    }

    @Inject(method = "writeData", at = @At("TAIL"), remap = false)
    private void mqt$writeOriginData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (!mqt$shouldApplyOriginExtension()) return;

        if (OriginsCompat.isLoaded()) {
            mqt$ensureInitialized();
            CompoundTag originTag = new CompoundTag();
            originTag.putBoolean("check", mqt$checkOrigin);
            originTag.putString("layer", mqt$originLayer.toString());
            originTag.putString("origin", mqt$origin.toString());
            nbt.put("Origins", originTag);
        }
    }

    @Inject(method = "readData", at = @At("TAIL"), remap = false)
    private void mqt$readOriginData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (!mqt$shouldApplyOriginExtension()) return;

        if (OriginsCompat.isLoaded() && nbt.contains("Origins")) {
            CompoundTag originTag = nbt.getCompound("Origins");
            mqt$checkOrigin = originTag.getBoolean("check");

            ResourceLocation parsedLayer = ResourceLocation.tryParse(originTag.getString("layer"));
            ResourceLocation parsedOrigin = ResourceLocation.tryParse(originTag.getString("origin"));

            mqt$originLayer = parsedLayer != null ? parsedLayer : ResourceLocation.withDefaultNamespace("empty");
            mqt$origin = parsedOrigin != null ? parsedOrigin : ResourceLocation.withDefaultNamespace("empty");
        } else {
            mqt$ensureInitialized();
        }
    }

    @Inject(method = "writeNetData", at = @At("TAIL"), remap = false)
    private void mqt$writeNetOriginData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        if (!mqt$shouldApplyOriginExtension()) {
            buffer.writeBoolean(false);
            return;
        }

        boolean hasOrigins = OriginsCompat.isLoaded();
        buffer.writeBoolean(hasOrigins);
        if (hasOrigins) {
            mqt$ensureInitialized();
            buffer.writeBoolean(mqt$checkOrigin);
            buffer.writeResourceLocation(mqt$originLayer);
            buffer.writeResourceLocation(mqt$origin);
        }
    }

    @Inject(method = "readNetData", at = @At("TAIL"), remap = false)
    private void mqt$readNetOriginData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        if (!mqt$shouldApplyOriginExtension()) {
            buffer.readBoolean();
            return;
        }

        boolean hasOrigins = buffer.readBoolean();
        if (hasOrigins) {
            mqt$checkOrigin = buffer.readBoolean();
            mqt$originLayer = buffer.readResourceLocation();
            mqt$origin = buffer.readResourceLocation();
        } else {
            mqt$ensureInitialized();
        }
    }

    @Environment(EnvType.CLIENT)
    @Inject(method = "fillConfigGroup", at = @At("TAIL"), remap = false)
    private void mqt$addOriginConfig(ConfigGroup config, CallbackInfo ci) {
        if (!mqt$shouldApplyOriginExtension()) return;
        if (!  OriginsCompat.isLoaded()) return;

        mqt$ensureInitialized();

        ConfigGroup originGroup = config.getOrCreateSubgroup("origins");
        originGroup.setNameKey("morequesttypes.config.group.origins");

        originGroup.addBool("check_origin", mqt$checkOrigin,
                        v -> mqt$checkOrigin = v, false)
                .setNameKey("morequesttypes.task.origins.check_origin");

        List<ResourceLocation> layers = new ArrayList<>();
        layers.add(ResourceLocation.withDefaultNamespace("empty"));
        layers.addAll(OriginsCompat.getAvailableLayers());

        if (! layers.contains(mqt$originLayer)) {
            mqt$originLayer = ResourceLocation.withDefaultNamespace("empty");
        }

        var LAYER_MAP = NameMap.of(mqt$originLayer, layers.toArray(ResourceLocation[]::new))
                .name(layer -> {
                    if (layer.equals(ResourceLocation.withDefaultNamespace("empty"))) {
                        return Component.literal("None");
                    }
                    return Component.literal(layer.toString());
                })
                .create();

        originGroup.addEnum("origin_layer", mqt$originLayer, v -> {
            mqt$originLayer = v;
            if (! v.equals(ResourceLocation.withDefaultNamespace("empty"))) {
                List<ResourceLocation> availableOrigins = OriginsCompat.getOriginsForLayer(v);
                if (! availableOrigins.contains(mqt$origin)) {
                    mqt$origin = ResourceLocation.withDefaultNamespace("empty");
                }
            }
        }, LAYER_MAP).setNameKey("morequesttypes.task.origins.layer");

        List<ResourceLocation> origins = new ArrayList<>();
        origins.add(ResourceLocation.withDefaultNamespace("empty"));
        if (! mqt$originLayer.equals(ResourceLocation.withDefaultNamespace("empty"))) {
            origins.addAll(OriginsCompat.getOriginsForLayer(mqt$originLayer));
        }

        if (!origins.contains(mqt$origin)) {
            mqt$origin = ResourceLocation.withDefaultNamespace("empty");
        }

        var ORIGIN_MAP = NameMap.of(mqt$origin, origins.toArray(ResourceLocation[]::new))
                .name(origin -> {
                    if (origin.equals(ResourceLocation.withDefaultNamespace("empty"))) {
                        return Component.literal("None");
                    }
                    return Component.literal(origin.toString());
                })
                .create();

        originGroup.addEnum("origin", mqt$origin, v -> {
            mqt$origin = v;
        }, ORIGIN_MAP).setNameKey("morequesttypes.task.origins.origin");
    }
}