package net.pixeldreamstudios.morequesttypes.compat.neoforge;

import dev.architectury.platform.Platform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

public final class DungeonDifficultyCompatImpl {
    private static final String MOD_ID = "dungeon_difficulty";
    private static final String NBT_KEY = "dd_scaled";

    public static boolean isLoaded() {
        return Platform.isModLoaded(MOD_ID);
    }

    public static int getLevel(LivingEntity entity) {
        if (!isLoaded()) return 0;
        try {
            CompoundTag nbt = new CompoundTag();
            entity.saveWithoutId(nbt);
            if (nbt.getBoolean(NBT_KEY)) {
                return 1;
            }
            return nbt.getInt(NBT_KEY);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static boolean canHaveLevel(LivingEntity entity) {
        if (!isLoaded()) return false;
        return true;
    }
}