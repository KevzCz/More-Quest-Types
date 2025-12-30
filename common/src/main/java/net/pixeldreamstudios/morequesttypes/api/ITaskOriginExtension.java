package net.pixeldreamstudios.morequesttypes.api;

import net.minecraft.resources.ResourceLocation;

public interface ITaskOriginExtension {
    boolean shouldCheckOrigin();
    void setShouldCheckOrigin(boolean check);

    ResourceLocation getRequiredOriginLayer();
    void setRequiredOriginLayer(ResourceLocation layerId);

    ResourceLocation getRequiredOrigin();
    void setRequiredOrigin(ResourceLocation originId);
}