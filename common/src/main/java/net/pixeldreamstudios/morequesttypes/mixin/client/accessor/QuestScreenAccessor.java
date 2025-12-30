package net.pixeldreamstudios.morequesttypes.mixin.client.accessor;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Movable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = QuestScreen.class, remap = false)
public interface QuestScreenAccessor {

    @Accessor("file")
    ClientQuestFile mqt$getFile();

    @Accessor("selectedChapter")
    Chapter mqt$getSelectedChapter();

    @Accessor("scrollWidth")
    double mqt$getScrollWidth();

    @Accessor("scrollHeight")
    double mqt$getScrollHeight();

    @Accessor("movingObjects")
    boolean mqt$isMovingObjects();

    @Accessor("selectedObjects")
    List<Movable> mqt$getSelectedObjects();

    @Accessor("grid")
    static boolean mqt$isGridEnabled() {
        throw new AssertionError();
    }
}