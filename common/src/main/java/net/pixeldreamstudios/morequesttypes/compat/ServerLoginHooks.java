package net.pixeldreamstudios.morequesttypes.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;

public final class ServerLoginHooks {
    private ServerLoginHooks() {}
    @ExpectPlatform
    public static void register() {
        throw new AssertionError();
    }
}
