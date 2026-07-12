package net.pixeldreamstudios.morequesttypes.compat.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.spell_engine.client.util.SpellRender;

@Environment(EnvType.CLIENT)
public final class SpellEngineCompatClientImpl {
    private SpellEngineCompatClientImpl() {
    }

    public static ResourceLocation getSpellIconTexture(ResourceLocation spellId) {
        if (spellId == null) return null;
        try {
            return SpellRender.iconTexture(spellId);
        } catch (Throwable t) {
            return null;
        }
    }
}
