package net.pixeldreamstudios.morequesttypes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.pixeldreamstudios.morequesttypes.event.TradingEventBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {

    @Shadow @Final private MerchantContainer slots;
    @Shadow @Final private Merchant merchant;

    @Inject(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/Merchant;notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V"))
    private void onTradeCompleted(Player player, ItemStack resultStack, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            MerchantOffer offer = this.slots.getActiveOffer();

            if (offer != null && this.merchant instanceof Entity merchantEntity) {
                TradingEventBuffer.record(
                        serverPlayer.getUUID(),
                        merchantEntity,
                        offer.getCostA().copy(),
                        offer.getResult().copy(),
                        serverPlayer.level().getGameTime()
                );
            }
        }
    }
}