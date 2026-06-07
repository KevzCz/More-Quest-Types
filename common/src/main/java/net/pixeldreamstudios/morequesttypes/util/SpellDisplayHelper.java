package net.pixeldreamstudios.morequesttypes.util;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.morequesttypes.compat.SpellEngineCompat;

import java.util.ArrayList;
import java.util.List;

public final class SpellDisplayHelper {
    private static final Icon ENCHANTED_BOOK = Icon.getIcon("minecraft:item/enchanted_book");

    private SpellDisplayHelper() {}

    public static Component spellName(ResourceLocation spellId) {
        if (spellId == null) {
            return Component.empty();
        }
        return Component.translatable("spell." + spellId.getNamespace() + "." + spellId.getPath() + ".name");
    }

    public static Component spellName(String spellId) {
        ResourceLocation rl = ResourceLocation.tryParse(spellId);
        return rl == null ? Component.literal(spellId) : spellName(rl);
    }

    @Environment(EnvType.CLIENT)
    public static Icon spellListIcon(List<String> spellIds) {
        return spellListIcon(spellIds, ENCHANTED_BOOK);
    }

    @Environment(EnvType.CLIENT)
    public static Icon spellListIcon(List<String> spellIds, Icon fallback) {
        if (spellIds == null || spellIds.isEmpty()) {
            return fallback;
        }

        List<Icon> icons = new ArrayList<>();
        for (String raw : spellIds) {
            ResourceLocation spellId = ResourceLocation.tryParse(raw);
            if (spellId == null || !SpellEngineCompat.isLoaded()) {
                continue;
            }
            icons.add(spellIcon(spellId, fallback));
        }

        if (icons.isEmpty()) {
            return fallback;
        }
        if (icons.size() == 1) {
            return icons.getFirst();
        }
        return IconAnimation.fromList(icons, false);
    }

    @Environment(EnvType.CLIENT)
    public static Icon spellIcon(ResourceLocation spellId) {
        return spellIcon(spellId, ENCHANTED_BOOK);
    }

    @Environment(EnvType.CLIENT)
    public static Icon spellIcon(ResourceLocation spellId, Icon fallback) {
        if (spellId == null || !SpellEngineCompat.isLoaded()) {
            return fallback;
        }

        ResourceLocation tex = SpellEngineCompat.getSpellIconTexture(spellId);
        if (tex == null || !textureExists(tex)) {
            return fallback;
        }

        try {
            return Icon.getIcon(tex);
        } catch (Throwable ignored) {
            try {
                return Icon.getIcon(tex.toString());
            } catch (Throwable ignored2) {
                return fallback;
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private static boolean textureExists(ResourceLocation texture) {
        try {
            var minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getResourceManager() == null) {
                return false;
            }
            return minecraft.getResourceManager().getResource(texture).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
