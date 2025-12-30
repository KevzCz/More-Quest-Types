package net.pixeldreamstudios.morequesttypes.mixin.client.accessor;

import dev.ftb.mods.ftbquests.client.gui.quests.QuestButton;
import dev.ftb.mods.ftbquests.quest.Quest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered. asm.mixin.gen. Accessor;

@Mixin(value = QuestButton.class, remap = false)
public interface QuestButtonAccessor {

    @Accessor("quest")
    Quest mqt$getQuest();
}