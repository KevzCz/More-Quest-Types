package net.pixeldreamstudios.morequesttypes.fabric.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.pixeldreamstudios.morequesttypes.event.FishingCatchEventBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Shadow
    public abstract Player getPlayerOwner();

    @Redirect(
            method = "retrieve",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private boolean captureItemOnSpawn(Level level, net.minecraft.world.entity.Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            Player player = getPlayerOwner();
            if (player instanceof ServerPlayer sp) {
                ItemStack stack = itemEntity.getItem();
                if (!stack.isEmpty()) {
                    long gt = sp.level().getGameTime();
                    FishingCatchEventBuffer.push(sp.getUUID(), stack, "", gt);
                }
            }
        }

        return level.addFreshEntity(entity);
    }
}