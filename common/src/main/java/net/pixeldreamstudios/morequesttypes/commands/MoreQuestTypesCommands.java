package net.pixeldreamstudios.morequesttypes.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftblibrary.config.Tristate;
import dev.ftb.mods.ftblibrary.util.NetworkHelper;
import dev.ftb.mods.ftbquests.net.CreateObjectResponseMessage;
import dev.ftb.mods.ftbquests.net.EditObjectResponseMessage;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.pixeldreamstudios.morequesttypes.api.IQuestExtension;
import net.pixeldreamstudios.morequesttypes.api.ITeamDataExtension;
import net.pixeldreamstudios.morequesttypes.tasks.*;

import java.util.*;
import java.util.stream.Collectors;

public final class MoreQuestTypesCommands {

    private static final SimpleCommandExceptionType NO_FILE = new SimpleCommandExceptionType(
            Component.literal("No quest file loaded!")
    );

    private static final DynamicCommandExceptionType NO_OBJECT = new DynamicCommandExceptionType(
            id -> Component.literal("Quest object not found: " + id)
    );

    private static final DynamicCommandExceptionType INVALID_ID = new DynamicCommandExceptionType(
            id -> Component.literal("Invalid quest object ID: " + id)
    );

    private static final SimpleCommandExceptionType INVALID_TASK_TYPE = new SimpleCommandExceptionType(
            Component.literal("Invalid task type specified!")
    );

    private static final SuggestionProvider<CommandSourceStack> ITEM_TASK_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.asList("item", "hold_item", "use_item"),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> BLOCK_TASK_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.asList("item", "break_block", "use_item", "hold_item"),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> MOB_TASK_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.asList("kill", "advanced_kill", "find_entity", "interact_entity", "damage"),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> MOD_ID_SUGGESTIONS = (ctx, builder) -> {
        Set<String> modIds = new HashSet<>();
        BuiltInRegistries.ITEM.keySet().forEach(id -> modIds.add(id.getNamespace()));
        BuiltInRegistries.BLOCK.keySet().forEach(id -> modIds.add(id.getNamespace()));
        BuiltInRegistries.ENTITY_TYPE.keySet().forEach(id -> modIds.add(id.getNamespace()));
        return SharedSuggestionProvider.suggest(modIds.stream().sorted().toList(), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("morequesttypes")
                        .requires(stack -> stack.hasPermission(2))
                        .then(Commands.literal("change_progress")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.literal("complete")
                                                .then(Commands.argument("quest_object", StringArgumentType.string())
                                                        .executes(ctx -> applyProgress(ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                false,
                                                                StringArgumentType.getString(ctx, "quest_object")))))
                                        .then(Commands.literal("reset")
                                                .then(Commands.argument("quest_object", StringArgumentType.string())
                                                        .executes(ctx -> applyProgress(ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                true,
                                                                StringArgumentType.getString(ctx, "quest_object")))))
                                )
                        )
                        .then(Commands.literal("refresh_chapter")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("chapter_id", StringArgumentType.string())
                                                .executes(ctx -> refreshChapter(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "chapter_id")))
                                        )
                                )
                        )
                        .then(Commands.literal("reset_repeat_counter")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("quest_id", StringArgumentType.string())
                                                .executes(ctx -> resetRepeatCounter(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "quest_id")))
                                        )
                                )
                        )
                        .then(Commands.literal("link_quest")
                                .then(Commands.argument("quest_id", StringArgumentType.string())
                                        .executes(ctx -> linkQuestToItem(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "quest_id")))
                                )
                        )
                        .then(buildGenerateChapterCommand())
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildGenerateChapterCommand() {
        return Commands.literal("generate_chapter")
                .then(Commands.literal("advancements")
                        .executes(ctx -> generateAdvancementChapter(ctx.getSource(), ""))
                        .then(Commands.argument("mod_id", StringArgumentType.string())
                                .suggests(MOD_ID_SUGGESTIONS)
                                .executes(ctx -> generateAdvancementChapter(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "mod_id")))
                        )
                )
                .then(Commands.literal("items")
                        .then(Commands.argument("mod_id", StringArgumentType.string())
                                .suggests(MOD_ID_SUGGESTIONS)
                                .then(Commands.argument("task_type", StringArgumentType.word())
                                        .suggests(ITEM_TASK_SUGGESTIONS)
                                        .executes(ctx -> generateItemChapter(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mod_id"),
                                                StringArgumentType.getString(ctx, "task_type")))
                                )
                        )
                )
                .then(Commands.literal("blocks")
                        .then(Commands.argument("mod_id", StringArgumentType.string())
                                .suggests(MOD_ID_SUGGESTIONS)
                                .then(Commands.argument("task_type", StringArgumentType.word())
                                        .suggests(BLOCK_TASK_SUGGESTIONS)
                                        .executes(ctx -> generateBlockChapter(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mod_id"),
                                                StringArgumentType.getString(ctx, "task_type")))
                                )
                        )
                )
                .then(Commands.literal("all")
                        .then(Commands.argument("mod_id", StringArgumentType.string())
                                .suggests(MOD_ID_SUGGESTIONS)
                                .then(Commands.argument("item_task_type", StringArgumentType.word())
                                        .suggests(ITEM_TASK_SUGGESTIONS)
                                        .executes(ctx -> generateAllChapter(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mod_id"),
                                                StringArgumentType.getString(ctx, "item_task_type")))
                                )
                        )
                )
                .then(Commands.literal("mobs")
                        .then(Commands.argument("mod_id", StringArgumentType.string())
                                .suggests(MOD_ID_SUGGESTIONS)
                                .then(Commands.argument("task_type", StringArgumentType.word())
                                        .suggests(MOB_TASK_SUGGESTIONS)
                                        .executes(ctx -> generateMobChapter(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mod_id"),
                                                StringArgumentType.getString(ctx, "task_type")))
                                )
                        )
                )
                .then(Commands.literal("item_tag")
                        .then(Commands.argument("tag", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                        BuiltInRegistries.ITEM.getTagNames().map(TagKey::location),
                                        builder
                                ))
                                .then(Commands.argument("task_type", StringArgumentType.word())
                                        .suggests(ITEM_TASK_SUGGESTIONS)
                                        .executes(ctx -> generateItemTagChapter(
                                                ctx.getSource(),
                                                ResourceLocationArgument.getId(ctx, "tag"),
                                                StringArgumentType.getString(ctx, "task_type")))
                                )
                        )
                )
                .then(Commands.literal("entity_tag")
                        .then(Commands.argument("tag", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                        BuiltInRegistries.ENTITY_TYPE.getTagNames().map(TagKey::location),
                                        builder
                                ))
                                .then(Commands.argument("task_type", StringArgumentType.word())
                                        .suggests(MOB_TASK_SUGGESTIONS)
                                        .executes(ctx -> generateEntityTagChapter(
                                                ctx.getSource(),
                                                ResourceLocationArgument.getId(ctx, "tag"),
                                                StringArgumentType.getString(ctx, "task_type")))
                                )
                        )
                );
    }

    private static int generateAdvancementChapter(CommandSourceStack source, String modId) {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) {
            source.sendFailure(Component.literal("No quest file loaded!"));
            return 0;
        }

        var advancements = source.getServer().getAdvancements().getAllAdvancements()
                .stream()
                .filter(adv -> {
                    if (!modId.isEmpty() && !adv.id().getNamespace().equals(modId)) {
                        return false;
                    }

                    if (adv.id().getPath().startsWith("recipes/")) {
                        return false;
                    }

                    return adv.value().display().isPresent();
                })
                .sorted(Comparator.comparing(adv -> adv.id().toString()))
                .collect(Collectors.toList());

        if (advancements.isEmpty()) {
            source.sendFailure(Component.literal("No advancements found for mod: " + (modId.isEmpty() ? "all" : modId)));
            return 0;
        }

        String title = modId.isEmpty()
                ? "All Advancements [" + advancements.size() + "]"
                : "Advancements from " + modId + " [" + advancements.size() + "]";

        Chapter chapter = createChapter(file, title, new ItemStack(Items.KNOWLEDGE_BOOK), source);

        int col = 0, row = 0;
        String currentMod = "";

        for (var advancement : advancements) {
            String namespace = advancement.id().getNamespace();
            if (!currentMod.equals(namespace)) {
                currentMod = namespace;
                col = 0;
                row += 2;
            } else if (col >= 40) {
                col = 0;
                row++;
            }

            Quest quest = createQuest(file, chapter, col, row, "", source);

            dev.ftb.mods.ftbquests.quest.task.AdvancementTask advTask =
                    new dev.ftb.mods.ftbquests.quest.task.AdvancementTask(file.newID(), quest);
            advTask.onCreated();

            CompoundTag nbt = new CompoundTag();
            nbt.putString("advancement", advancement.id().toString());
            nbt.putString("criterion", "");

            advTask.readData(nbt, source.registryAccess());

            NetworkHelper.sendToAll(source.getServer(), CreateObjectResponseMessage.create(advTask, null));

            col++;
        }

        finalizeChapter(file, source);
        source.sendSuccess(() -> Component.literal("Generated advancement chapter with " + advancements.size() + " quests!").withStyle(ChatFormatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int generateItemChapter(CommandSourceStack source, String modId, String taskType) throws CommandSyntaxException {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) {
            source.sendFailure(Component.literal("No quest file loaded!"));
            return 0;
        }

        List<Item> items = BuiltInRegistries.ITEM.stream()
                .filter(item -> {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    return id.getNamespace().equals(modId) && item != Items.AIR;
                })
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .collect(Collectors.toList());

        if (items.isEmpty()) {
            source.sendFailure(Component.literal("No items found for mod: " + modId));
            return 0;
        }

        Chapter chapter = createChapter(file, "Items from " + modId + " [" + items.size() + "]", new ItemStack(Items.CHEST), source);

        int col = 0, row = 0;

        for (Item item : items) {
            if (col >= 40) {
                col = 0;
                row++;
            }

            ItemStack stack = new ItemStack(item);
            Quest quest = createQuest(file, chapter, col, row, "", source);
            quest.setRawIcon(stack);

            createItemTask(file, quest, stack, taskType, source);

            col++;
        }

        finalizeChapter(file, source);
        source.sendSuccess(() -> Component.literal("Generated item chapter with " + items.size() + " quests!").withStyle(ChatFormatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int generateBlockChapter(CommandSourceStack source, String modId, String taskType) throws CommandSyntaxException {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) {
            source.sendFailure(Component.literal("No quest file loaded!"));
            return 0;
        }

        List<Block> blocks = BuiltInRegistries.BLOCK.stream()
                .filter(block -> {
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                    return id.getNamespace().equals(modId);
                })
                .sorted(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()))
                .collect(Collectors.toList());

        if (blocks.isEmpty()) {
            source.sendFailure(Component.literal("No blocks found for mod: " + modId));
            return 0;
        }

        Chapter chapter = createChapter(file, "Blocks from " + modId + " [" + blocks.size() + "]", new ItemStack(Items.BRICKS), source);

        int col = 0, row = 0;

        for (Block block : blocks) {
            if (col >= 40) {
                col = 0;
                row++;
            }

            ItemStack stack = new ItemStack(block.asItem());
            if (stack.isEmpty()) continue;

            Quest quest = createQuest(file, chapter, col, row, "", source);
            quest.setRawIcon(stack);

            if ("break_block".equals(taskType)) {
                createBreakBlockTask(file, quest, block, source);
            } else {
                createItemTask(file, quest, stack, taskType, source);
            }

            col++;
        }

        finalizeChapter(file, source);
        source.sendSuccess(() -> Component.literal("Generated block chapter with " + blocks.size() + " quests!").withStyle(ChatFormatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int generateAllChapter(CommandSourceStack source, String modId, String itemTaskType) throws CommandSyntaxException {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) {
            source.sendFailure(Component.literal("No quest file loaded!"));
            return 0;
        }

        List<Item> items = BuiltInRegistries.ITEM.stream()
                .filter(item -> {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    return id.getNamespace().equals(modId) && item != Items.AIR;
                })
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .collect(Collectors.toList());

        var advancements = source.getServer().getAdvancements().getAllAdvancements()
                .stream()
                .filter(adv -> {
                    if (!adv.id().getNamespace().equals(modId)) {
                        return false;
                    }

                    if (adv.id().getPath().startsWith("recipes/")) {
                        return false;
                    }

                    return adv.value().display().isPresent();
                })
                .sorted(Comparator.comparing(adv -> adv.id().toString()))
                .collect(Collectors.toList());

        int total = items.size() + advancements.size();
        if (total == 0) {
            source.sendFailure(Component.literal("No items or advancements found for mod: " + modId));
            return 0;
        }

        Chapter chapter = createChapter(file, "All from " + modId + " [" + total + "]", new ItemStack(Items.BEACON), source);

        int col = 0, row = 0;

        for (Item item : items) {
            if (col >= 40) {
                col = 0;
                row++;
            }

            ItemStack stack = new ItemStack(item);
            Quest quest = createQuest(file, chapter, col, row, "", source);
            quest.setRawIcon(stack);

            createItemTask(file, quest, stack, itemTaskType, source);

            col++;
        }

        if (!advancements.isEmpty()) {
            row += 2;
            col = 0;

            for (var advancement : advancements) {
                if (col >= 40) {
                    col = 0;
                    row++;
                }

                Quest quest = createQuest(file, chapter, col, row, "", source);

                dev.ftb.mods.ftbquests.quest.task.AdvancementTask advTask =
                        new dev.ftb.mods.ftbquests.quest.task.AdvancementTask(file.newID(), quest);
                advTask.onCreated();

                CompoundTag nbt = new CompoundTag();
                nbt.putString("advancement", advancement.id().toString());
                nbt.putString("criterion", "");

                advTask.readData(nbt, source.registryAccess());

                NetworkHelper.sendToAll(source.getServer(), CreateObjectResponseMessage.create(advTask, null));

                col++;
            }
        }

        finalizeChapter(file, source);
        source.sendSuccess(() -> Component.literal("Generated combined chapter with " + total + " quests!").withStyle(ChatFormatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int generateMobChapter(CommandSourceStack source, String modId, String taskType) throws CommandSyntaxException {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) {
            source.sendFailure(Component.literal("No quest file loaded!"));
            return 0;
        }

        List<EntityType<?>> entityTypes = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> {
                    ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                    return id.getNamespace().equals(modId);
                })
                .sorted(Comparator.comparing(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()))
                .collect(Collectors.toList());

        if (entityTypes.isEmpty()) {
            source.sendFailure(Component.literal("No mobs found for mod: " + modId));
            return 0;
        }

        Chapter chapter = createChapter(file, "Mobs from " + modId + " [" + entityTypes.size() + "]", new ItemStack(Items.IRON_SWORD), source);

        int col = 0, row = 0;

        for (EntityType<?> entityType : entityTypes) {
            if (col >= 40) {
                col = 0;
                row++;
            }

            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            Quest quest = createQuest(file, chapter, col, row, "", source);

            createMobTask(file, quest, entityId, taskType, source);

            col++;
        }

        finalizeChapter(file, source);
        source.sendSuccess(() -> Component.literal("Generated mob chapter with " + entityTypes.size() + " quests!").withStyle(ChatFormatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int generateItemTagChapter(CommandSourceStack source, ResourceLocation tagId, String taskType) throws CommandSyntaxException {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) {
            source.sendFailure(Component.literal("No quest file loaded!"));
            return 0;
        }

        TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
        Optional<HolderSet.Named<Item>> tagSet = BuiltInRegistries.ITEM.getTag(tag);

        if (tagSet.isEmpty()) {
            source.sendFailure(Component.literal("Tag not found: #" + tagId));
            return 0;
        }

        List<Item> items = tagSet.get().stream()
                .map(Holder::value)
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .collect(Collectors.toList());

        if (items.isEmpty()) {
            source.sendFailure(Component.literal("Tag is empty: #" + tagId));
            return 0;
        }

        Chapter chapter = createChapter(file, "Items in #" + tagId + " [" + items.size() + "]", new ItemStack(Items.NAME_TAG), source);

        int col = 0, row = 0;

        for (Item item : items) {
            if (col >= 40) {
                col = 0;
                row++;
            }

            ItemStack stack = new ItemStack(item);
            Quest quest = createQuest(file, chapter, col, row, "", source);
            quest.setRawIcon(stack);

            createItemTask(file, quest, stack, taskType, source);

            col++;
        }

        finalizeChapter(file, source);
        source.sendSuccess(() -> Component.literal("Generated tag chapter with " + items.size() + " quests!").withStyle(ChatFormatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int generateEntityTagChapter(CommandSourceStack source, ResourceLocation tagId, String taskType) throws CommandSyntaxException {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) {
            source.sendFailure(Component.literal("No quest file loaded!"));
            return 0;
        }

        TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagId);
        Optional<HolderSet.Named<EntityType<?>>> tagSet = BuiltInRegistries.ENTITY_TYPE.getTag(tag);

        if (tagSet.isEmpty()) {
            source.sendFailure(Component.literal("Tag not found: #" + tagId));
            return 0;
        }

        List<EntityType<?>> entityTypes = tagSet.get().stream()
                .map(Holder::value)
                .sorted(Comparator.comparing(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()))
                .collect(Collectors.toList());

        if (entityTypes.isEmpty()) {
            source.sendFailure(Component.literal("Tag is empty or contains no living entities: #" + tagId));
            return 0;
        }

        Chapter chapter = createChapter(file, "Entities in #" + tagId + " [" + entityTypes.size() + "]", new ItemStack(Items.LEAD), source);

        int col = 0, row = 0;

        for (EntityType<?> entityType : entityTypes) {
            if (col >= 40) {
                col = 0;
                row++;
            }

            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            Quest quest = createQuest(file, chapter, col, row, "", source);

            createMobTask(file, quest, entityId, taskType, source);

            col++;
        }

        finalizeChapter(file, source);
        source.sendSuccess(() -> Component.literal("Generated entity tag chapter with " + entityTypes.size() + " quests!").withStyle(ChatFormatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static Chapter createChapter(ServerQuestFile file, String title, ItemStack icon, CommandSourceStack source) {
        long newId = file.newID();
        Chapter chapter = new Chapter(newId, file, file.getDefaultChapterGroup());
        chapter.onCreated();

        chapter.setRawTitle(title);
        chapter.setRawIcon(icon);
        chapter.setDefaultQuestShape("rsquare");

        NetworkHelper.sendToAll(source.getServer(), CreateObjectResponseMessage.create(chapter, null));

        return chapter;
    }

    private static Quest createQuest(ServerQuestFile file, Chapter chapter, int col, int row, String subtitle, CommandSourceStack source) {
        Quest quest = new Quest(file.newID(), chapter);
        quest.onCreated();
        quest.setX((double) col);
        quest.setY((double) row);
        if (!subtitle.isEmpty()) {
            quest.setRawSubtitle(subtitle);
        }

        NetworkHelper.sendToAll(source.getServer(), CreateObjectResponseMessage.create(quest, null));
        return quest;
    }

    private static void createItemTask(ServerQuestFile file, Quest quest, ItemStack stack, String taskType, CommandSourceStack source) throws CommandSyntaxException {
        Task task;

        switch (taskType.toLowerCase()) {
            case "item" -> {
                dev.ftb.mods.ftbquests.quest.task.ItemTask itemTask = new dev.ftb.mods.ftbquests.quest.task.ItemTask(file.newID(), quest);
                itemTask.onCreated();
                itemTask.setStackAndCount(stack, 1);
                itemTask.setConsumeItems(Tristate.FALSE);
                task = itemTask;
            }
            case "hold_item" -> {
                HoldItemTask holdTask = new HoldItemTask(file.newID(), quest);
                holdTask.onCreated();

                CompoundTag taskData = new CompoundTag();
                holdTask.writeData(taskData, source.registryAccess());

                taskData.put("item", stack.copyWithCount(1).save(source.registryAccess()));
                taskData.putDouble("duration_seconds", 5.0);

                holdTask.readData(taskData, source.registryAccess());
                task = holdTask;
            }
            case "use_item" -> {
                UseItemTask useTask = new UseItemTask(file.newID(), quest);
                useTask.onCreated();

                CompoundTag taskData = new CompoundTag();
                useTask.writeData(taskData, source.registryAccess());

                taskData.put("item", stack.copyWithCount(1).save(source.registryAccess()));
                taskData.putLong("value", 1L);
                taskData.putString("hand_mode", "ANY");

                useTask.readData(taskData, source.registryAccess());
                task = useTask;
            }
            default -> throw INVALID_TASK_TYPE.create();
        }

        NetworkHelper.sendToAll(source.getServer(), CreateObjectResponseMessage.create(task, null));
    }

    private static void createBreakBlockTask(ServerQuestFile file, Quest quest, Block block, CommandSourceStack source) {
        BreakBlockTask task = new BreakBlockTask(file.newID(), quest);
        task.onCreated();

        CompoundTag taskData = new CompoundTag();
        task.writeData(taskData, source.registryAccess());

        ItemStack blockStack = new ItemStack(block.asItem());
        taskData.put("block_item", blockStack.copyWithCount(1).save(source.registryAccess()));
        taskData.putLong("value", 1L);

        task.readData(taskData, source.registryAccess());

        NetworkHelper.sendToAll(source.getServer(), CreateObjectResponseMessage.create(task, null));
    }

    private static void createMobTask(ServerQuestFile file, Quest quest, ResourceLocation entityId, String taskType, CommandSourceStack source) throws CommandSyntaxException {
        Task task;

        switch (taskType.toLowerCase()) {
            case "kill" -> {
                KillTask killTask = new KillTask(file.newID(), quest);
                killTask.onCreated();

                CompoundTag taskData = new CompoundTag();
                killTask.writeData(taskData, source.registryAccess());

                taskData.putString("entity", entityId.toString());
                taskData.putLong("value", 1L);

                killTask.readData(taskData, source.registryAccess());
                task = killTask;
            }
            case "advanced_kill" -> {
                AdvancedKillTask advKillTask = new AdvancedKillTask(file.newID(), quest);
                advKillTask.onCreated();

                CompoundTag taskData = new CompoundTag();
                advKillTask.writeData(taskData, source.registryAccess());

                taskData.putString("entity", entityId.toString());
                taskData.putLong("value", 1L);

                advKillTask.readData(taskData, source.registryAccess());
                task = advKillTask;
            }
            case "find_entity" -> {
                FindEntityTask findTask = new FindEntityTask(file.newID(), quest);
                findTask.onCreated();

                CompoundTag taskData = new CompoundTag();
                findTask.writeData(taskData, source.registryAccess());

                taskData.putString("entity", entityId.toString());
                taskData.putInt("target_radius_blocks", 5);
                taskData.putInt("search_radius_blocks", 64);

                findTask.readData(taskData, source.registryAccess());
                task = findTask;
            }
            case "interact_entity" -> {
                InteractEntityTask interactTask = new InteractEntityTask(file.newID(), quest);
                interactTask.onCreated();

                CompoundTag taskData = new CompoundTag();
                interactTask.writeData(taskData, source.registryAccess());

                taskData.putString("entity", entityId.toString());
                taskData.putLong("value", 1L);
                taskData.putString("hand_mode", "ANY");

                interactTask.readData(taskData, source.registryAccess());
                task = interactTask;
            }
            case "damage" -> {
                DamageTask damageTask = new DamageTask(file.newID(), quest);
                damageTask.onCreated();

                CompoundTag taskData = new CompoundTag();
                damageTask.writeData(taskData, source.registryAccess());

                taskData.putString("entity", entityId.toString());
                taskData.putLong("value", 100L);
                taskData.putString("mode", "TOTAL");

                damageTask.readData(taskData, source.registryAccess());
                task = damageTask;
            }
            default -> throw INVALID_TASK_TYPE.create();
        }

        NetworkHelper.sendToAll(source.getServer(), CreateObjectResponseMessage.create(task, null));
    }

    private static void finalizeChapter(ServerQuestFile file, CommandSourceStack source) {
        file.refreshIDMap();
        file.clearCachedData();
        file.markDirty();
        file.saveNow();

        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            file.getTeamData(player).ifPresent(TeamData::clearCachedProgress);
        }

        dev.ftb.mods.ftbquests.net.SyncQuestsMessage msg = new dev.ftb.mods.ftbquests.net.SyncQuestsMessage(file);
        NetworkHelper.sendToAll(source.getServer(), msg);
    }

    private static int linkQuestToItem(CommandSourceStack source, String questId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.isEmpty()) {
            source.sendFailure(Component.translatable("morequesttypes.command.link.no_item"));
            return 0;
        }

        QuestObjectBase object = getQuestObjectForString(questId);
        if (!(object instanceof Quest quest)) {
            throw NO_OBJECT.create(questId);
        }

        heldItem.update(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY,
                data -> data.update(tag -> tag.putLong("LinkedQuestId", quest.id)));

        source.sendSuccess(() -> Component.translatable("morequesttypes.command.link.success",
                        quest.getTitle(), heldItem.getHoverName()),
                false);

        return Command.SINGLE_SUCCESS;
    }

    private static int resetRepeatCounter(CommandSourceStack source, ServerPlayer player, String questId) throws CommandSyntaxException {
        QuestObjectBase object = getQuestObjectForString(questId);

        if (!(object instanceof Quest quest)) {
            throw NO_OBJECT.create(questId);
        }

        if (!quest.canBeRepeated()) {
            source.sendFailure(Component.literal("This quest is not repeatable!"));
            return 0;
        }

        IQuestExtension ext = (IQuestExtension) (Object) quest;
        if (ext.getMaxRepeats() <= 0) {
            source.sendFailure(Component.literal("This quest has no repeat limit!"));
            return 0;
        }

        ServerQuestFile.INSTANCE.getTeamData(player).ifPresent(teamData -> {
            ((ITeamDataExtension) teamData).resetQuestCompletionCount(quest.id, player.getUUID());
            teamData.markDirty();
        });

        source.sendSuccess(() -> Component.literal(
                        "Reset repeat counter for quest '" + quest.getTitle().getString() + "' for player " + player.getName().getString()),
                true);

        return Command.SINGLE_SUCCESS;
    }

    private static QuestObjectBase getQuestObjectForString(String idStr) throws CommandSyntaxException {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) {
            throw NO_FILE.create();
        }

        if (idStr.startsWith("#")) {
            String val = idStr.substring(1);
            for (QuestObjectBase qob : file.getAllObjects()) {
                if (qob.hasTag(val)) {
                    return qob;
                }
            }
            throw NO_OBJECT.create(idStr);
        } else {
            long id = QuestObjectBase.parseHexId(idStr)
                    .orElseThrow(() -> INVALID_ID.create(idStr));
            QuestObjectBase qob = file.getBase(id);
            if (qob == null) {
                throw NO_OBJECT.create(idStr);
            }
            return qob;
        }
    }

    private static int refreshChapter(CommandSourceStack source, ServerPlayer player, String chapterId) throws CommandSyntaxException {
        QuestObjectBase object = getQuestObjectForString(chapterId);

        if (!(object instanceof Chapter chapter)) {
            throw NO_OBJECT.create(chapterId);
        }

        ServerQuestFile.INSTANCE.clearCachedData();
        chapter.clearCachedData();

        NetworkManager.sendToPlayer(player, new EditObjectResponseMessage(chapter));

        source.sendSuccess(() -> Component.literal(
                        "Refreshed chapter '" + chapter.getTitle().getString() + "' for player " + player.getName().getString()),
                true);

        return Command.SINGLE_SUCCESS;
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
                (reset ? "Reset" : "Completed") + " " + targets.size() +
                        " object(s) for " + players.size() + " player(s)."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static List<QuestObjectBase> resolveTargets(ServerQuestFile file, String idOrTag) throws CommandSyntaxException {
        if (idOrTag.startsWith("#")) {
            String tag = idOrTag.substring(1);
            List<QuestObjectBase> all = new ArrayList<>();
            for (QuestObjectBase qob : file.getAllObjects()) {
                if (qob.hasTag(tag)) {
                    all.add(qob);
                }
            }
            return all;
        } else {
            long id = QuestObjectBase.parseHexId(idOrTag)
                    .orElseThrow(() -> INVALID_ID.create(idOrTag));
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