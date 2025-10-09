package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class SkillsCompat {
    private SkillsCompat() {}

    @ExpectPlatform
    public static boolean isLoaded() { throw new AssertionError(); }

    @ExpectPlatform
    public static int getTotalLevel(ServerPlayer player) { throw new AssertionError(); }

    @ExpectPlatform
    public static int getCategoryLevel(ServerPlayer player, ResourceLocation categoryId) { throw new AssertionError(); }

    @ExpectPlatform
    public static Collection<ResourceLocation> getCategories(boolean onlyWithExperience) { throw new AssertionError(); }
}
