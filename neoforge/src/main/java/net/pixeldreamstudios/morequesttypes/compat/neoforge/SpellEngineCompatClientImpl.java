package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.spell_engine.client.util.SpellRender;

public final class SpellEngineCompatClientImpl {
    private SpellEngineCompatClientImpl() {
    }

    public static ResourceLocation getSpellIconTexture(ResourceLocation spellId) {
        if (spellId == null || FMLEnvironment.dist != Dist.CLIENT) return null;
        try {
            return SpellRender.iconTexture(spellId);
        } catch (Throwable t) {
            return null;
        }
    }
}
