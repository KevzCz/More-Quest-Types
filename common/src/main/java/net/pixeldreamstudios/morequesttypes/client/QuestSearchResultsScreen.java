package net.pixeldreamstudios.morequesttypes.client;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.SelectQuestObjectScreen;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.util.ConfigQuestObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class QuestSearchResultsScreen<T extends QuestObjectBase> extends SelectQuestObjectScreen<T> {
    private final ItemStack searchedItem;
    private final ConfigQuestObject<T> questConfig;
    private final ConfigCallback questCallback;
    private boolean hasCleared = false;

    public QuestSearchResultsScreen(ConfigQuestObject<T> config, ConfigCallback callback, ItemStack searchedItem) {
        super(config, callback);
        this.searchedItem = searchedItem;
        this.questConfig = config;
        this.questCallback = callback;

        setTitle(Component.translatable("morequesttypes.search.title", searchedItem.getHoverName()));
    }

    @Override
    public void tick() {
        super.tick();

        if (!hasCleared) {
            clearSearchBox(this);
            hasCleared = true;
        }
    }

    private void clearSearchBox(Widget widget) {
        if (widget instanceof dev.ftb.mods.ftblibrary.ui.TextBox textBox) {
            textBox.setText("");
            return;
        }

        if (widget instanceof Panel panel) {
            for (Widget child : panel.getWidgets()) {
                clearSearchBox(child);
            }
        }
    }

    @Override
    public void addButtons(Panel panel) {
        ClientQuestFile file = ClientQuestFile.INSTANCE;

        for (QuestObjectBase objectBase : file.getAllObjects()) {
            if (questConfig.predicate.test(objectBase) &&
                    (file.canEdit() || (objectBase instanceof QuestObject qo && qo.isSearchable(file.selfTeamData)))) {

                panel.add(new QuestSearchButton(panel, (T) objectBase));
            }
        }
    }

    private class QuestSearchButton extends SimpleTextButton {
        private final T quest;

        public QuestSearchButton(Panel panel, T quest) {
            super(panel, quest.getTitle(), quest.getIcon());
            this.quest = quest;
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            questConfig.setValue(quest);
            questCallback.save(true);
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            if (isMouseOver()) {
                Color4I.WHITE.withAlpha(33).draw(graphics, x, y, w, h);
            }

            Color4I.GRAY.withAlpha(60).draw(graphics, x, y + h - 1, w, 1);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            quest.getIcon().draw(graphics, x + 2, y + 2, 16, 16);

            drawBackground(graphics, theme, x, y, w, h);

            Component title = quest.getMutableTitle().withStyle(quest.getObjectType().getColor());
            theme.drawString(graphics, title, x + 22, y + 6, Color4I.WHITE, 0);

            if (quest instanceof Quest q) {
                Component chapterName = Component.literal(q.getChapter().getTitle().getString())
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
                int chapterWidth = theme.getStringWidth(chapterName);
                theme.drawString(graphics, chapterName, x + w - chapterWidth - 4, y + 6, Color4I.GRAY, 0);
            }
        }
    }
}