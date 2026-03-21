package net.pixeldreamstudios.morequesttypes.neoforge;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.event.CommandEventBuffer;

@EventBusSubscriber(modid = MoreQuestTypes.MOD_ID)
public final class CommandHooksNeoForge {
    @SubscribeEvent
    public static void onCommandExecute(CommandEvent event) {
        ParseResults<CommandSourceStack> parseResults = event.getParseResults();
        CommandSourceStack source = parseResults.getContext().getSource();
        
        if (source.getEntity() instanceof ServerPlayer sp) {
            String command = parseResults.getReader().getString();
            long gt = sp.level().getGameTime();
            CommandEventBuffer.push(sp.getUUID(), command, gt);
        }
    }
    
    private CommandHooksNeoForge() {}
}
