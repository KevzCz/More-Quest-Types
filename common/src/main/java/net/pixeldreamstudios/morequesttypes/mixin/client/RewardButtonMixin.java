package net.pixeldreamstudios.morequesttypes.mixin.client;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbquests.client.gui.quests.RewardButton;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.pixeldreamstudios.morequesttypes.network.button.ToggleRewardRequest;
import net.pixeldreamstudios.morequesttypes.rewards.AttributeReward;
import net.pixeldreamstudios.morequesttypes.rewards.SpellReward;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RewardButton.class, remap = false)
public abstract class RewardButtonMixin {
    @Shadow Reward reward;

    @Inject(method = "onClicked", at = @At("HEAD"), cancellable = true)
    private void onClickedInject(MouseButton button, CallbackInfo ci) {
        if (reward == null) return;

        if (button.isLeft() && (reward instanceof SpellReward || reward instanceof AttributeReward)) {
            NetworkManager.sendToServer(new ToggleRewardRequest(reward.getId()));
            ci.cancel();
        }
    }
}
