package net.pixeldreamstudios.morequesttypes.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.commands.MoreQuestTypesCommands;

public final class MoreQuestTypesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MoreQuestTypes.init();
        InteractEntityHooksFabric.register();
        UseItemHooksFabric.register();
        BreakBlockHooksFabric.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MoreQuestTypesCommands.register(dispatcher);
        });
    }

}
