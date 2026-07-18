package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.platform.Platform;

public final class PerPlayerQuestsCompat {
    private PerPlayerQuestsCompat() {}

    private static Boolean loaded = null;

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = Platform.isModLoaded("perplayerquests");
        }
        return loaded;
    }
}
