package net.pixeldreamstudios.morequesttypes.mixin.easynpc;

import de.markusbordihn.easynpc.data.dialog.DialogDataEntry;
import de.markusbordihn.easynpc.entity.easynpc.data.DialogDataCapable;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.compat.EasyNPCCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
@Pseudo
@Mixin(value = DialogDataCapable.class, remap = false)
public interface DialogDataCapableMixin {

    @Inject(method = "openDialog", at = @At("HEAD"))
    default void onOpenDialog(ServerPlayer serverPlayer, UUID dialogId, CallbackInfo ci) {
        if (this instanceof DialogDataCapable<?> dialogCapable) {
            var dialogDataSet = dialogCapable.getDialogDataSet();
            if (dialogDataSet != null) {
                DialogDataEntry dialog = dialogDataSet.getDialog(dialogId);
                if (dialog != null && dialog.getLabel() != null) {
                    EasyNPCCompat.markDialogueCompleted(serverPlayer, dialog.getLabel());
                }
            }
        }
    }
}