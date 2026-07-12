package net.pixeldreamstudios.morequesttypes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.pixeldreamstudios.morequesttypes.event.UseBlockEventBuffer;
import net.pixeldreamstudios.morequesttypes.event.UseItemEventBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class UseItemBlockResultMixin {
    private static final ThreadLocal<ItemStack> morequesttypes$useItemStack = new ThreadLocal<>();
    private static final ThreadLocal<ItemStack> morequesttypes$useItemOnStack = new ThreadLocal<>();

    @Inject(method = "useItem", at = @At("HEAD"))
    private void morequesttypes$captureUseItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                               CallbackInfoReturnable<InteractionResult> cir) {
        morequesttypes$useItemStack.set(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void morequesttypes$onUseItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                          CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack before = morequesttypes$useItemStack.get();
        morequesttypes$useItemStack.remove();
        if (before != null && !before.isEmpty() && cir.getReturnValue().consumesAction()) {
            UseItemEventBuffer.push(player.getUUID(), hand, before, level.getGameTime());
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void morequesttypes$captureUseItemOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                                 BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        morequesttypes$useItemOnStack.set(stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void morequesttypes$onUseItemOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                            BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack before = morequesttypes$useItemOnStack.get();
        morequesttypes$useItemOnStack.remove();
        if (cir.getReturnValue().consumesAction()) {
            BlockPos pos = hitResult.getBlockPos();
            UseBlockEventBuffer.push(player.getUUID(), pos, level.getBlockState(pos), level.getGameTime());
            if (before != null && !before.isEmpty()) {
                UseItemEventBuffer.push(player.getUUID(), hand, before, level.getGameTime());
            }
        }
    }
}
