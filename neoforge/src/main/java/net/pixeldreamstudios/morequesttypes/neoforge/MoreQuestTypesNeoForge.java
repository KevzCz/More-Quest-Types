package net.pixeldreamstudios.morequesttypes.neoforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.commands.MoreQuestTypesCommands;

@Mod(MoreQuestTypes.MOD_ID)
public final class MoreQuestTypesNeoForge {
    public MoreQuestTypesNeoForge() {
        MoreQuestTypes.init();
    }
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MoreQuestTypesCommands.register(event.getDispatcher());
    }
}
