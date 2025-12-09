package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import dev.architectury.platform.Platform;
import net.minecraft.world.entity.LivingEntity;

public final class DynamicDifficultyCompatImpl {
    private static final String MOD_ID = "dynamic_difficulty";

    public static boolean isLoaded() {
        return Platform.isModLoaded(MOD_ID);
    }

    public static boolean hasLevel(LivingEntity entity) {
        if (!isLoaded()) return false;
        try {
            return dev.muon.dynamic_difficulty.api.LevelingAPI.hasLevel(entity);
        } catch (Throwable t) {
            return false;
        }
    }

    public static int getLevel(LivingEntity entity) {
        if (!isLoaded()) return 1;
        try {
            return dev.muon.dynamic_difficulty.api.LevelingAPI.getLevel(entity);
        } catch (Throwable t) {
            return 1;
        }
    }

    public static void setAndUpdateLevel(LivingEntity entity, int newLevel) {
        if (!isLoaded()) return;
        try {
            dev.muon.dynamic_difficulty.api.LevelingAPI.setAndUpdateLevel(entity, newLevel);
        } catch (Throwable t) {

        }
    }

    public static boolean canHaveLevel(LivingEntity entity) {
        if (!isLoaded()) return false;
        try {
            return dev.muon.dynamic_difficulty.api.LevelingAPI.canHaveLevel(entity);
        } catch (Throwable t) {
            return false;
        }
    }
}