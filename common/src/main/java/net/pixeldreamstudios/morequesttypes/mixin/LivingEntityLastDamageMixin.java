package net.pixeldreamstudios.morequesttypes.mixin;

import dev.architectury.registry.registries.RegistrarManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.pixeldreamstudios.morequesttypes.accessor.LivingEntityLastDamageAccess;
import net.pixeldreamstudios.morequesttypes.event.DamageEventBuffer;
import net.pixeldreamstudios.morequesttypes.event.ReceiveDamageEventBuffer;
import net.pixeldreamstudios.morequesttypes.network.MQTLastReceivedDamagePacket;
import net.pixeldreamstudios.morequesttypes.network.NetworkHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLastDamageMixin implements LivingEntityLastDamageAccess {

    @Unique private UUID mqt$lastAttacker;
    @Unique private float mqt$lastBaseline;
    @Unique private float mqt$lastFinal;
    @Unique private float mqt$prevHealth;
    @Unique private float mqt$newHealth;
    @Unique private int mqt$damageSeqCounter = 0;
    @Unique private int mqt$lastDamageSeq = 0;
    @Unique private ResourceLocation mqt$lastDamageType;
    @Unique private Entity mqt$lastSourceEntity;
    @Unique private ItemStack mqt$lastWeapon = ItemStack.EMPTY;
    @Unique private float mqt$pendingDamageAmount;

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"))
    private void mqt$hurtHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        mqt$pendingDamageAmount = amount;
        mqt$prevHealth = self.getHealth();
        mqt$newHealth = mqt$prevHealth;
        mqt$lastBaseline = 0.0F;
        mqt$lastFinal = 0.0F;

        Entity origin = mqt$resolveOrigin(source);
        mqt$lastSourceEntity = origin;
        if (origin == null) {
            origin = self;
        }
        mqt$lastAttacker = origin.getUUID();
        mqt$lastDamageType = mqt$resolveDamageType(source);
        mqt$lastWeapon = mqt$resolveWeapon(origin);
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void mqt$hurtReturn(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        mqt$newHealth = self.getHealth();
        float applied = Math.max(0.0F, mqt$prevHealth - mqt$newHealth);
        if (applied <= 0.0F && mqt$pendingDamageAmount > 0.0F) {
            applied = mqt$pendingDamageAmount;
        }
        if (applied <= 0.0F) {
            return;
        }

        mqt$lastDamageSeq = ++mqt$damageSeqCounter;
        mqt$lastBaseline = applied;
        mqt$lastFinal = applied;

        Level lvl = self.level();
        if (lvl.isClientSide()) {
            return;
        }

        long gt = lvl.getGameTime();
        long damageAmount = Math.round(applied);
        if (damageAmount <= 0L) {
            damageAmount = 1L;
        }

        if (mqt$lastAttacker != null) {
            var server = lvl.getServer();
            if (server != null) {
                ServerPlayer sp = server.getPlayerList().getPlayer(mqt$lastAttacker);
                if (sp != null) {
                    DamageEventBuffer.push(
                            sp.getUUID(),
                            self,
                            mqt$lastWeapon,
                            mqt$lastDamageType,
                            mqt$lastSourceEntity,
                            gt,
                            damageAmount,
                            damageAmount,
                            mqt$prevHealth,
                            mqt$newHealth,
                            mqt$lastDamageSeq
                    );
                }
            }
        }

        if (self instanceof ServerPlayer victimPlayer) {
            ReceiveDamageEventBuffer.push(
                    victimPlayer.getUUID(),
                    mqt$lastSourceEntity,
                    mqt$lastDamageType,
                    gt,
                    damageAmount,
                    mqt$lastDamageSeq
            );

            String sourceEntityType = "";
            if (mqt$lastSourceEntity instanceof LivingEntity sourceLiving) {
                ResourceLocation sourceId = RegistrarManager.getId(sourceLiving.getType(), Registries.ENTITY_TYPE);
                if (sourceId != null) {
                    sourceEntityType = sourceId.toString();
                }
            }
            NetworkHelper.sendToPlayer(victimPlayer, new MQTLastReceivedDamagePacket(
                    mqt$lastDamageType.toString(),
                    sourceEntityType,
                    damageAmount
            ));
        }
    }

    @Unique
    private static Entity mqt$resolveOrigin(DamageSource src) {
        if (src == null) {
            return null;
        }
        Entity origin = src.getEntity();
        if (origin == null) {
            origin = src.getDirectEntity();
        }
        if (origin instanceof Projectile proj && proj.getOwner() != null) {
            return proj.getOwner();
        }
        if (origin instanceof AreaEffectCloud cloud && cloud.getOwner() != null) {
            return cloud.getOwner();
        }
        if (origin instanceof LightningBolt bolt && bolt.getCause() != null) {
            return bolt.getCause();
        }
        return origin;
    }

    @Unique
    private static ResourceLocation mqt$resolveDamageType(DamageSource src) {
        if (src == null) {
            return ResourceLocation.withDefaultNamespace("generic");
        }
        return src.typeHolder().unwrapKey()
                .map(ResourceKey::location)
                .orElse(ResourceLocation.withDefaultNamespace("generic"));
    }

    @Unique
    private static ItemStack mqt$resolveWeapon(Entity origin) {
        if (origin instanceof LivingEntity le) {
            ItemStack main = le.getMainHandItem();
            if (!main.isEmpty()) {
                return main.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override public UUID mqt$getLastDamageAttacker() { return mqt$lastAttacker; }
    @Override public float mqt$getLastDamageBaselineAmount() { return mqt$lastBaseline; }
    @Override public float mqt$getLastDamageFinalAmount() { return mqt$lastFinal; }
    @Override public float mqt$getLastDamagePrevHealth() { return mqt$prevHealth; }
    @Override public float mqt$getLastDamageNewHealth() { return mqt$newHealth; }
    @Override public int mqt$getLastDamageSeq() { return mqt$lastDamageSeq; }
    @Override public ResourceLocation mqt$getLastDamageTypeId() { return mqt$lastDamageType; }
    @Override public Entity mqt$getLastDamageSourceEntity() { return mqt$lastSourceEntity; }
    @Override public ItemStack mqt$getLastDamageWeapon() { return mqt$lastWeapon == null ? ItemStack.EMPTY : mqt$lastWeapon; }
}
