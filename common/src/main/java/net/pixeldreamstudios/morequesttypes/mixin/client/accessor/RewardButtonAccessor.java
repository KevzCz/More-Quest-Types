package net.pixeldreamstudios.morequesttypes.mixin.client.accessor;

import dev.ftb.mods.ftbquests.client.gui.quests.RewardButton;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RewardButton.class, remap = false)
public interface RewardButtonAccessor {

    @Accessor("reward")
    Reward mqt$getReward();
}