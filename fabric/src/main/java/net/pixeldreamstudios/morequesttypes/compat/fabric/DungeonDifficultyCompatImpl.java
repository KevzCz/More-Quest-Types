package net.pixeldreamstudios.morequesttypes.compat.fabric;

import dev.architectury.platform.Platform;
import net.dungeon_difficulty.logic.EntityDifficultyScalable;
import net.minecraft.world.entity.LivingEntity;

public final class DungeonDifficultyCompatImpl {
    private static final String MOD_ID = "dungeon_difficulty";

    public static boolean isLoaded() {
        return Platform.isModLoaded(MOD_ID);
    }

    public static int getLevel(LivingEntity entity) {
        if (! isLoaded()) return 0;
        try {
            if (entity instanceof EntityDifficultyScalable scalable) {
                return scalable.getScalingLevel();
            }
            return 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static boolean canHaveLevel(LivingEntity entity) {
        if (!isLoaded()) return false;
        return entity instanceof EntityDifficultyScalable;
    }
}