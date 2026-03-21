package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.pixeldreamstudios.morequesttypes.compat.BlabberCompat;
import net.pixeldreamstudios.morequesttypes.compat.EasyNPCCompat;
import net.pixeldreamstudios.morequesttypes.compat.LevelZCompat;
import net.pixeldreamstudios.morequesttypes.compat.OriginsCompat;
import net.pixeldreamstudios.morequesttypes.compat.ReskillableCompat;
import net.pixeldreamstudios.morequesttypes.compat.SGEconomyCompat;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;

public final class MoreTasksTypes {
    private MoreTasksTypes() {
    }

    public static TaskType TIMER;
    public static TaskType KILL_ADVANCED;
    public static TaskType SKILLS_LEVEL;
    public static TaskType INTERACT_ENTITY;
    public static TaskType DIALOGUE;
    public static TaskType ATTRIBUTES;
    public static TaskType DAMAGE;
    public static TaskType USE_ITEM;
    public static TaskType HOLD_ITEM;
    public static TaskType FIND_ENTITY;
    public static TaskType CHECK_QUEST;
    public static TaskType BREAK_BLOCK;
    public static TaskType POTION_EFFECT;
    public static TaskType TRADING;
    public static TaskType LEVELZ;
    public static TaskType RESKILLABLE;
    public static TaskType ORIGIN;
    public static TaskType PLACE_BLOCK;
    public static TaskType EASY_NPC_DIALOGUE;
    public static TaskType USE_BLOCK;
    public static TaskType FISHING_CATCH;
    public static TaskType COMMAND;
    public static TaskType PAY;

    public static void init() {
        MoreTasksTypes.TIMER = TaskTypes.register(
                FTBQuestsAPI.rl("timer"),
                TimerTask::new,
                () -> Icon.getIcon("minecraft:item/clock_00")
        );
        if (SkillsCompat.isLoaded()) {
            MoreTasksTypes.SKILLS_LEVEL = TaskTypes.register(
                    FTBQuestsAPI.rl("skills_level"),
                    SkillsLevelTask::new,
                    () -> Icon.getIcon("minecraft:item/experience_bottle")
            );
        }
        if (BlabberCompat.isLoaded()) {
            MoreTasksTypes.DIALOGUE = TaskTypes.register(
                    FTBQuestsAPI.rl("dialogue"),
                    DialogueTask::new,
                    () -> Icon.getIcon("blabber:icon.png")
            );
        }
        if (OriginsCompat.isLoaded()) {
            MoreTasksTypes.ORIGIN = TaskTypes.register(
                    FTBQuestsAPI.rl("origin"),
                    OriginTask::new,
                    () -> Icon.getIcon("origins:icon.png")
            );
        }
        MoreTasksTypes.PLACE_BLOCK = TaskTypes.register(
                FTBQuestsAPI.rl("place_block"),
                PlaceBlockTask::new,
                () -> Icon.getIcon("minecraft:block/grass_block_side")
        );
        MoreTasksTypes.KILL_ADVANCED = TaskTypes.register(
                FTBQuestsAPI.rl("kill_advanced"),
                AdvancedKillTask::new,
                () -> Icon.getIcon("minecraft:item/iron_sword")
        );
        MoreTasksTypes.INTERACT_ENTITY = TaskTypes.register(
                FTBQuestsAPI.rl("interact_entity"),
                InteractEntityTask::new,
                () -> Icon.getIcon("minecraft:item/lead")
        );
        MoreTasksTypes.ATTRIBUTES = TaskTypes.register(
                FTBQuestsAPI.rl("attributes"),
                AttributesTask::new,
                () -> Icon.getIcon("minecraft:item/iron_chestplate")
        );
        MoreTasksTypes.DAMAGE = TaskTypes.register(
                FTBQuestsAPI.rl("damage"),
                DamageTask::new,
                () -> Icon.getIcon("minecraft:item/wooden_sword")
        );
        MoreTasksTypes.USE_ITEM = TaskTypes.register(
                FTBQuestsAPI.rl("use_item"),
                UseItemTask::new,
                () -> Icon.getIcon("minecraft:item/lead")
        );
        MoreTasksTypes.HOLD_ITEM = TaskTypes.register(
                FTBQuestsAPI.rl("hold_item"),
                HoldItemTask::new,
                () -> Icon.getIcon("minecraft:item/totem_of_undying")
        );
        MoreTasksTypes.FIND_ENTITY = TaskTypes.register(
                FTBQuestsAPI.rl("find_entity"),
                FindEntityTask::new,
                () -> Icon.getIcon("minecraft:item/spyglass")
        );
        MoreTasksTypes.CHECK_QUEST = TaskTypes.register(
                FTBQuestsAPI.rl("check_quest"),
                CheckQuestTask::new,
                () -> Icon.getIcon("ftbquests:item/book")
        );
        MoreTasksTypes.BREAK_BLOCK = TaskTypes.register(
                FTBQuestsAPI.rl("break_block"),
                BreakBlockTask::new,
                () -> Icon.getIcon("minecraft:item/iron_pickaxe")
        );
        MoreTasksTypes.POTION_EFFECT = TaskTypes.register(
                FTBQuestsAPI.rl("potion_effect"),
                PotionEffectTask::new,
                () -> Icon.getIcon("minecraft:item/potion")
        );
        MoreTasksTypes.TRADING = TaskTypes.register(
                FTBQuestsAPI.rl("trading"),
                TradingTask::new,
                () -> Icon.getIcon("minecraft:item/emerald")
        );
        if (LevelZCompat.isLoaded()) {
            MoreTasksTypes.LEVELZ = TaskTypes.register(
                    FTBQuestsAPI.rl("levelz"),
                    LevelZTask::new,
                    () -> Icon.getIcon("levelz:icon.png")
            );
        }
        if (ReskillableCompat.isLoaded()) {
            MoreTasksTypes.RESKILLABLE = TaskTypes.register(
                    FTBQuestsAPI.rl("reskillable"),
                    ReskillableTask::new,
                    () -> Icon.getIcon("minecraft:item/experience_bottle")
            );
        }
        if (EasyNPCCompat.isLoaded()) {
            MoreTasksTypes.EASY_NPC_DIALOGUE = TaskTypes.register(
                    FTBQuestsAPI.rl("easynpc_dialogue"),
                    EasyNPCDialogueTask::new,
                    () -> Icon.getIcon("easy_npc:block/easy_npc_spawner/boss_spawner")
            );
        }
        MoreTasksTypes.USE_BLOCK = TaskTypes.register(
                FTBQuestsAPI.rl("use_block"),
                UseBlockTask::new,
                () -> Icon.getIcon("minecraft:item/oak_door")
        );
        MoreTasksTypes.FISHING_CATCH = TaskTypes.register(
                FTBQuestsAPI.rl("fishing_catch"),
                FishingCatchTask::new,
                () -> Icon.getIcon("minecraft:item/fishing_rod")
        );
        MoreTasksTypes.COMMAND = TaskTypes.register(
                FTBQuestsAPI.rl("command"),
                CommandTask::new,
                () -> Icon.getIcon("minecraft:block/command_block_back")
        );
        if (SGEconomyCompat.isLoaded()) {
            MoreTasksTypes.PAY = TaskTypes.register(
                    FTBQuestsAPI.rl("pay"),
                    SGEconomyTask::new,
                    () -> Icon.getIcon("minecraft:item/gold_ingot")
            );
        }
    }
}