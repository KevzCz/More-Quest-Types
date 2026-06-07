package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.Theme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.List;

@Environment(EnvType.CLIENT)
final class UiTextWrap {
    static final int LINE_HEIGHT = 10;

    private UiTextWrap() {
    }

    static List<FormattedText> wrap(Theme theme, Component text, int maxWidth) {
        return theme.listFormattedStringToWidth(text, Math.max(40, maxWidth));
    }

    static int lineCount(Theme theme, Component text, int maxWidth) {
        return Math.max(1, wrap(theme, text, maxWidth).size());
    }

    static int height(Theme theme, Component text, int maxWidth) {
        return lineCount(theme, text, maxWidth) * LINE_HEIGHT + 6;
    }

    static void draw(GuiGraphics graphics, Theme theme, Component text, int x, int y, int maxWidth, Color4I color) {
        int lineY = y + 2;
        for (FormattedText line : wrap(theme, text, maxWidth)) {
            theme.drawString(graphics, line, x, lineY, color, 0);
            lineY += LINE_HEIGHT;
        }
    }
}
