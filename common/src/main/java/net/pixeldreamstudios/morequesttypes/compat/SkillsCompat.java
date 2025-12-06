package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Map;

public final class SkillsCompat {
    private SkillsCompat() {}

    @ExpectPlatform
    public static boolean isLoaded() { throw new AssertionError(); }

    @ExpectPlatform
    public static int getTotalLevel(ServerPlayer player) { throw new AssertionError(); }

    @ExpectPlatform
    public static int getCategoryLevel(ServerPlayer player, ResourceLocation categoryId) { throw new AssertionError(); }

    @ExpectPlatform
    public static int getCategoryExperience(ServerPlayer player, ResourceLocation categoryId) { throw new AssertionError(); }

    @ExpectPlatform
    public static int getCategoryPoints(ServerPlayer player, ResourceLocation categoryId, ResourceLocation source) { throw new AssertionError(); }

    @ExpectPlatform
    public static void addCategoryExperience(ServerPlayer player, ResourceLocation categoryId, int amount) { throw new AssertionError(); }

    @ExpectPlatform
    public static void setCategoryExperience(ServerPlayer player, ResourceLocation categoryId, int amount) { throw new AssertionError(); }

    @ExpectPlatform
    public static void addCategoryPoints(ServerPlayer player, ResourceLocation categoryId, ResourceLocation source, int amount) { throw new AssertionError(); }

    @ExpectPlatform
    public static void setCategoryPoints(ServerPlayer player, ResourceLocation categoryId, ResourceLocation source, int amount) { throw new AssertionError(); }

    @ExpectPlatform
    public static Collection<ResourceLocation> getCategories(boolean onlyWithExperience) { throw new AssertionError(); }

    @ExpectPlatform
    public static Map<String, String> getCategoryIconData(ServerPlayer player) { throw new AssertionError(); }
}