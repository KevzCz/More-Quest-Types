package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;

import java.util.*;

public final class SpellEngineCompat {
    private SpellEngineCompat() {
    }

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Collection<ResourceLocation> getAllSpells(Level level) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ResourceLocation getSpellIconTexture(ResourceLocation spellId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void installSpells(ServerPlayer player, String baseKey, Collection<ResourceLocation> spells) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void uninstallSpells(ServerPlayer player, String baseKey) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Collection<String> getInstalledSpellKeys(ServerPlayer player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void removeInstalledSpellKeys(ServerPlayer player, Collection<String> keys) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<ResourceLocation> getItemSpells(ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addItemSpell(ItemStack stack, ResourceLocation spellId, String contentType) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void removeItemSpell(ItemStack stack, ResourceLocation spellId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setItemSpells(ItemStack stack, List<ResourceLocation> spellIds, String contentType) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean hasItemSpell(ItemStack stack, ResourceLocation spellId) {
        throw new AssertionError();
    }

}
