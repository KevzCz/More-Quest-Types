package net.pixeldreamstudios.morequesttypes.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class DamageTypeHelper {
    private DamageTypeHelper() {}

    @Environment(EnvType.CLIENT)
    public static List<String> clientDamageTypeChoices(Level level) {
        if (level == null) return List.of();
        List<String> choices = new ArrayList<>();
        level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).holders().forEach(holder ->
                holder.unwrapKey()
                        .map(ResourceKey::location)
                        .map(ResourceLocation::toString)
                        .ifPresent(choices::add)
        );
        choices.sort(String::compareTo);
        return choices;
    }
}
