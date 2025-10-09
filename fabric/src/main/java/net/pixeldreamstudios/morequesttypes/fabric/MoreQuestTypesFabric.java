package net.pixeldreamstudios.morequesttypes.fabric;

import net.fabricmc.api.ModInitializer;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;

public final class MoreQuestTypesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MoreQuestTypes.init();
        InteractEntityHooksFabric.register();
    }

}
