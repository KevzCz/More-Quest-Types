package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.LivingEntity;

public final class DynamicDifficultyCompat {
    private DynamicDifficultyCompat() {}

    @ExpectPlatform
    public static boolean isLoaded() { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean hasLevel(LivingEntity entity) { throw new AssertionError(); }

    @ExpectPlatform
    public static int getLevel(LivingEntity entity) { throw new AssertionError(); }

    @ExpectPlatform
    public static void setAndUpdateLevel(LivingEntity entity, int newLevel) { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean canHaveLevel(LivingEntity entity) { throw new AssertionError(); }
}