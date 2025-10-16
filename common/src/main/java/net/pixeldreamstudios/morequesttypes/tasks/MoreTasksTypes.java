package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.pixeldreamstudios.morequesttypes.compat.BlabberCompat;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;

public final class MoreTasksTypes {
    private MoreTasksTypes() {}

    public static TaskType TIMER;
    public static TaskType KILL_ADVANCED;
    public static TaskType SKILLS_LEVEL;
    public static TaskType INTERACT_ENTITY;
    public static TaskType DIALOGUE;
    public static TaskType ATTRIBUTES;
    public static TaskType DAMAGE;
    public static void init() {
        TIMER = TaskTypes.register(
                FTBQuestsAPI.rl("timer"),
                TimerTask::new,
                () -> Icon.getIcon("minecraft:item/clock_00")
        );

        if (SkillsCompat.isLoaded()) {
            SKILLS_LEVEL = TaskTypes.register(
                    FTBQuestsAPI.rl("skills_level"),
                    SkillsLevelTask::new,
                    () -> Icon.getIcon("minecraft:item/experience_bottle")
            );
        }
        if (BlabberCompat.isLoaded()){
            DIALOGUE = TaskTypes.register(
                    FTBQuestsAPI.rl("dialogue"),
                    DialogueTask::new,
                    () -> Icon.getIcon("minecraft:item/writable_book")
            );
        }
        KILL_ADVANCED = TaskTypes.register(
                FTBQuestsAPI.rl("kill_advanced"),
                AdvancedKillTask::new,
                () -> Icon.getIcon("minecraft:item/iron_sword")
        );

        INTERACT_ENTITY = TaskTypes.register(
                FTBQuestsAPI.rl("interact_entity"),
                InteractEntityTask::new,
                () -> Icon.getIcon("minecraft:item/lead")
        );
        ATTRIBUTES = TaskTypes.register(
                FTBQuestsAPI.rl("attributes"),
                AttributesTask::new,
                () -> Icon.getIcon("minecraft:item/iron_chestplate")
        );
        DAMAGE = TaskTypes.register(
                FTBQuestsAPI.rl("damage"),
                DamageTask::new,
                () -> Icon.getIcon("minecraft:item/wooden_sword")
        );
    }
}
