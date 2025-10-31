package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Collection;

public final class SpellEngineCompat {
    private SpellEngineCompat() {}

    @ExpectPlatform
    public static boolean isLoaded() { throw new AssertionError(); }

    @ExpectPlatform
    public static Collection<ResourceLocation> getAllSpells(Level level) { throw new AssertionError(); }
    @ExpectPlatform
    public static ResourceLocation getSpellIconTexture(ResourceLocation spellId) { throw new AssertionError(); }

    @ExpectPlatform
    public static void installSpells(ServerPlayer player, String baseKey, Collection<ResourceLocation> spells) { throw new AssertionError(); }

    @ExpectPlatform
    public static void uninstallSpells(ServerPlayer player, String baseKey) { throw new AssertionError(); }
    @ExpectPlatform
    public static Collection<String> getInstalledSpellKeys(ServerPlayer player) { throw new AssertionError(); }

    @ExpectPlatform
    public static void removeInstalledSpellKeys(ServerPlayer player, Collection<String> keys) { throw new AssertionError(); }

}
