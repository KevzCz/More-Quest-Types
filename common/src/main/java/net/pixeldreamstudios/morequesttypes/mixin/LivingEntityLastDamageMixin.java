package net.pixeldreamstudios.morequesttypes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.pixeldreamstudios.morequesttypes.accessor.LivingEntityLastDamageAccess;
import net.pixeldreamstudios.morequesttypes.damage.DamageEventBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLastDamageMixin implements LivingEntityLastDamageAccess {

    @Unique private UUID  mqt$lastAttacker;
    @Unique private float mqt$lastBaseline;
    @Unique private float mqt$lastFinal;
    @Unique private float mqt$prevHealth;
    @Unique private float mqt$newHealth;
    @Unique private int   mqt$damageSeqCounter = 0;
    @Unique private int   mqt$lastDamageSeq    = 0;

    @Inject(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"))
    private void mqt$atHead(DamageSource source, float passed, CallbackInfo ci) {
        mqt$lastDamageSeq = ++mqt$damageSeqCounter;

        Entity origin = mqt$resolveOrigin(source);
        if (origin == null) origin = (Entity)(Object)this;
        mqt$lastAttacker = origin.getUUID();

        mqt$prevHealth = ((LivingEntity)(Object)this).getHealth();
        mqt$lastBaseline = 0.0F;
        mqt$lastFinal = 0.0F;
        mqt$newHealth = mqt$prevHealth;
    }

    @ModifyArg(
            method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"),
            index = 0
    )
    private float mqt$captureSetHealth(float newHealth) {
        mqt$newHealth = newHealth;
        float applied = Math.max(0.0F, mqt$prevHealth - newHealth);
        mqt$lastFinal = applied;

        mqt$lastBaseline = applied;
        return newHealth;
    }

    @Inject(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("TAIL"))
    private void mqt$pushAtTail(DamageSource source, float passed, CallbackInfo ci) {
        Level lvl = ((LivingEntity)(Object)this).level();
        if (lvl.isClientSide()) return;

        if (mqt$lastAttacker == null || (mqt$lastBaseline <= 0F && mqt$lastFinal <= 0F)) return;

        var server = lvl.getServer();
        if (server == null) return;

        ServerPlayer sp = server.getPlayerList().getPlayer(mqt$lastAttacker);
        if (sp == null) return;

        long gt = sp.level().getGameTime();
        var used = sp.getMainHandItem();

        DamageEventBuffer.push(
                sp.getUUID(),
                (LivingEntity)(Object)this,
                used,
                gt,
                Math.round(Math.max(0F, mqt$lastBaseline)),
                Math.round(Math.max(0F, mqt$lastFinal)),
                mqt$prevHealth,
                mqt$newHealth,
                mqt$lastDamageSeq
        );
    }

    @Unique
    private static Entity mqt$resolveOrigin(DamageSource src) {
        if (src == null) return null;
        Entity origin = src.getEntity();
        if (origin == null) origin = src.getDirectEntity();
        if (origin instanceof Projectile proj && proj.getOwner() != null) return proj.getOwner();
        if (origin instanceof AreaEffectCloud cloud && cloud.getOwner() != null) return cloud.getOwner();
        if (origin instanceof LightningBolt bolt && bolt.getCause() != null) return bolt.getCause();
        return origin;
    }

    @Override public UUID  mqt$getLastDamageAttacker()       { return mqt$lastAttacker; }
    @Override public float mqt$getLastDamageBaselineAmount() { return mqt$lastBaseline; }
    @Override public float mqt$getLastDamageFinalAmount()    { return mqt$lastFinal; }
    @Override public float mqt$getLastDamagePrevHealth()     { return mqt$prevHealth; }
    @Override public float mqt$getLastDamageNewHealth()      { return mqt$newHealth; }
    @Override public int   mqt$getLastDamageSeq()            { return mqt$lastDamageSeq; }
}
