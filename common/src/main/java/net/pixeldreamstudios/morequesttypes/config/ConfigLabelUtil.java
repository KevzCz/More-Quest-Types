package net.pixeldreamstudios.morequesttypes.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

@Environment(EnvType.CLIENT)
public final class ConfigLabelUtil {
    private ConfigLabelUtil() {
    }

    public static String nameKey(Component label) {
        if (label.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return label.getString();
    }

    public static String safeRowId(String value) {
        if (value == null || value.isBlank()) {
            return "row";
        }
        String sanitized = value
                .replace(':', '_')
                .replace('.', '_')
                .replace('/', '_')
                .replace(' ', '_');
        if (sanitized.length() > 40) {
            sanitized = sanitized.substring(0, 40);
        }
        return "r_" + Math.abs(value.hashCode()) + "_" + sanitized;
    }
}
