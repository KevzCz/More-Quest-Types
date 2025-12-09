package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.LivingEntity;

public final class DungeonDifficultyCompat {
    private DungeonDifficultyCompat() {}

    @ExpectPlatform
    public static boolean isLoaded() { throw new AssertionError(); }

    @ExpectPlatform
    public static int getLevel(LivingEntity entity) { throw new AssertionError(); }

    @ExpectPlatform
    public static boolean canHaveLevel(LivingEntity entity) { throw new AssertionError(); }
}