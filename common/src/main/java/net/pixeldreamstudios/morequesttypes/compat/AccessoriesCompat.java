package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.platform.Platform;

public class AccessoriesCompat {
    private static Boolean loaded = null;

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = Platform.isModLoaded("accessories");
        }
        return loaded;
    }
}
