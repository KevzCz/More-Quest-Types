package net.pixeldreamstudios.morequesttypes.accessor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public interface LivingEntityLastDamageAccess {
    UUID mqt$getLastDamageAttacker();

    float mqt$getLastDamageBaselineAmount();

    float mqt$getLastDamageFinalAmount();

    float mqt$getLastDamagePrevHealth();

    float mqt$getLastDamageNewHealth();

    int mqt$getLastDamageSeq();

    ResourceLocation mqt$getLastDamageTypeId();

    Entity mqt$getLastDamageSourceEntity();

    ItemStack mqt$getLastDamageWeapon();
}
