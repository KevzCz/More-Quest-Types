package net.pixeldreamstudios.morequesttypes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.morequesttypes.event.TameEventBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalTameMixin {

    @Inject(method = "tame", at = @At("TAIL"))
    private void mqt$onTame(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer sp && !sp.level().isClientSide()) {
            TameEventBuffer.push(sp.getUUID(), (TamableAnimal) (Object) this, sp.level().getGameTime());
        }
    }
}
