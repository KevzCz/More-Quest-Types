package net.pixeldreamstudios.morequesttypes.util;

import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class SpellTooltipHelper {
    private SpellTooltipHelper() {
    }

    public static void addSpellLines(TooltipList list, List<String> spells, Component emptyFallback) {
        if (spells == null || spells.isEmpty()) {
            list.add(emptyFallback.copy().withStyle(ChatFormatting.YELLOW));
            return;
        }

        for (String raw : spells) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            list.add(Component.literal("• ")
                    .append(SpellDisplayHelper.spellName(raw))
                    .withStyle(ChatFormatting.AQUA));
        }
    }

}
