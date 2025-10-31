package net.pixeldreamstudios.morequesttypes.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class MoreQuestTypesCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                ((LiteralArgumentBuilder<CommandSourceStack>) Commands.literal("morequesttypes")
                        .requires(stack -> stack.hasPermission(2))
                        .then(Commands.literal("change_progress")
                                .then(((RequiredArgumentBuilder<CommandSourceStack, ?>) Commands.argument("players", EntityArgument.players()))
                                        .then(Commands.literal("complete")
                                                .then(Commands.argument("quest_object", StringArgumentType.string())
                                                        .executes(ctx -> applyProgress(ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                false,
                                                                StringArgumentType.getString(ctx, "quest_object"))))))
                                .then(Commands.literal("reset")
                                        .then(Commands.argument("quest_object", StringArgumentType.string())
                                                .executes(ctx -> applyProgress(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "players"),
                                                        true,
                                                        StringArgumentType.getString(ctx, "quest_object")))))
                        )
                )
        );
    }

    private static int applyProgress(CommandSourceStack source,
                                     Collection<ServerPlayer> players,
                                     boolean reset,
                                     String idOrTag) throws CommandSyntaxException {

        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) {
            source.sendFailure(Component.literal("No quest file loaded!"));
            return 0;
        }

        List<QuestObjectBase> targets = resolveTargets(file, idOrTag);

        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("No matching quest/task for: " + idOrTag));
            return 0;
        }

        for (ServerPlayer player : players) {
            file.getTeamData(player).ifPresent(team -> {
                for (QuestObjectBase target : targets) {
                    applyToObject(team, player, target, reset);
                }
            });
        }

        source.sendSuccess(() -> Component.literal(
                (reset ? "Reset" : "Completed") + " " + targets.size() + " object(s) for " + players.size() + " player(s)."), false);
        return 1;
    }

    private static List<QuestObjectBase> resolveTargets(ServerQuestFile file, String idOrTag) throws CommandSyntaxException {
        if (idOrTag.startsWith("#")) {
            String tag = idOrTag.substring(1);
            List<QuestObjectBase> all = new ArrayList<>();
            for (QuestObjectBase qob : file.getAllObjects()) {
                if (qob.hasTag(tag)) all.add(qob);
            }
            return all;
        } else {
            long id = QuestObjectBase.parseHexId(idOrTag).orElseThrow(
                    () -> new CommandSyntaxException(null, Component.literal("Invalid ID: " + idOrTag)));
            QuestObjectBase qob = file.getBase(id);
            return (qob == null) ? List.of() : List.of(qob);
        }
    }

    private static void applyToObject(TeamData team, ServerPlayer player, QuestObjectBase obj, boolean reset) {
        if (obj instanceof Task task) {
            team.setProgress(task, reset ? 0L : task.getMaxProgress());
            return;
        }
        if (obj instanceof Quest quest) {
            for (Task t : quest.getTasksAsList()) {
                team.setProgress(t, reset ? 0L : t.getMaxProgress());
            }
        }
    }
}
