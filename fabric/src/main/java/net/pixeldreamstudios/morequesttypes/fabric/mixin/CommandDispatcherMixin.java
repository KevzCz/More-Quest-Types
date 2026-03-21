package net.pixeldreamstudios.morequesttypes.fabric.mixin;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.event.CommandEventBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandDispatcherMixin {
    @Inject(
            method = "performCommand",
            at = @At("HEAD")
    )
    private void captureCommandExecution(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo ci) {
        CommandSourceStack source = parseResults.getContext().getSource();

        if (source.getEntity() instanceof ServerPlayer sp) {
            long gt = sp.level().getGameTime();
            CommandEventBuffer.push(sp.getUUID(), command, gt);
        }
    }
}