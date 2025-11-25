package net.pixeldreamstudios.morequesttypes.mixin.client;

import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.morequesttypes.api.IQuestExtension;
import net.pixeldreamstudios.morequesttypes.api.ITeamDataExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = QuestButton.class, remap = false)
public class QuestButtonMixin {

    @Shadow @Final private Quest quest;

    @Inject(method = "addMouseOverText", at = @At("TAIL"), remap = false)
    private void addRepeatCountTooltip(TooltipList list, CallbackInfo ci) {
        if (quest.canBeRepeated()) {
            IQuestExtension ext = (IQuestExtension) (Object) quest;
            int maxRepeats = ext.getMaxRepeats();

            if (maxRepeats > 0) {
                try {
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        TeamData teamData = TeamData.get(player);
                        ITeamDataExtension teamExt = (ITeamDataExtension) (Object) teamData;

                        int currentCount = teamExt.getQuestCompletionCount(quest.id, player.getUUID());
                        int remaining = Math.max(0, maxRepeats - currentCount);

                        List<Component> lines = list.getLines();

                        if (!lines.isEmpty()) {
                            lines.remove(lines.size() - 1);

                            list.add(Component.translatable("morequesttypes.quest.repeatable_remaining",
                                    remaining, maxRepeats).withStyle(ChatFormatting.GRAY));
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
    }
}