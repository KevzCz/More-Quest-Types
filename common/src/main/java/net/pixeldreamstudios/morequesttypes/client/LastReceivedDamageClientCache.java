package net.pixeldreamstudios.morequesttypes.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class LastReceivedDamageClientCache {
    private static ResourceLocation damageType;
    private static ResourceLocation sourceEntityType;
    private static long amount;

    private LastReceivedDamageClientCache() {}

    public static void update(@Nullable ResourceLocation type, @Nullable ResourceLocation source, long damageAmount) {
        if (type == null) {
            return;
        }
        damageType = type;
        sourceEntityType = source;
        amount = Math.max(0L, damageAmount);
    }

    public static boolean hasData() {
        return damageType != null;
    }

    @Nullable
    public static ResourceLocation damageType() {
        return damageType;
    }

    @Nullable
    public static ResourceLocation sourceEntityType() {
        return sourceEntityType;
    }

    public static long amount() {
        return amount;
    }
}
