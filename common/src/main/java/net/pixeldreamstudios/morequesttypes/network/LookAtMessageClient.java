package net.pixeldreamstudios.morequesttypes.network;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class LookAtMessageClient {
    public static void handleClient(LookAtMessage msg) {
        if (!ClientQuestFile.exists()) {
            return;
        }

        long chapterId = msg.data().getLong("ChapterId");
        double questX = msg.data().getDouble("QuestX");
        double questY = msg.data().getDouble("QuestY");

        Chapter chapter = ClientQuestFile.INSTANCE.getChapter(chapterId);
        if (chapter == null) {
            return;
        }

        Minecraft.getInstance().tell(() -> {
            QuestScreen screen = ClientQuestFile.openGui();
            if (screen != null) {
                screen.open(chapter, false);
                screen.questPanel.scrollTo(questX, questY);
            }
        });
    }
}