package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import net.pixeldreamstudios.morequesttypes.compat.*;

public final class MoreRewardTypes {
    private MoreRewardTypes() {}

    public static RewardType DIALOGUE;
    public static RewardType SKILLS_LEVEL;
    public static RewardType COMPLETE_OBJECT;
    public static RewardType PLAY_SOUND;
    public static RewardType LOOT_TABLE;
    public static RewardType ATTRIBUTE;
    public static RewardType SPELL;
    public static RewardType POTION;
    public static RewardType LEVELZ;
    public static RewardType RESKILLABLE;
    public static RewardType SUMMON;
    public static void init() {
        if (BlabberCompat.isLoaded()) {
            DIALOGUE = RewardTypes.register(
                    FTBQuestsAPI.rl("dialogue"),
                    DialogueReward::new,
                    () -> Icon.getIcon("minecraft:item/writable_book")
            );
        }
        if (SkillsCompat.isLoaded()) {
            SKILLS_LEVEL = RewardTypes.register(
                    FTBQuestsAPI.rl("skills_level"),
                    SkillsLevelReward::new,
                    () -> Icon.getIcon("minecraft:item/experience_bottle")
            );
        }
        if (SpellEngineCompat.isLoaded()) {
            SPELL = RewardTypes.register(
                    FTBQuestsAPI.rl("spell"),
                    SpellReward::new,
                    () -> Icon.getIcon("minecraft:item/lapis_lazuli")
            );
        }
        ATTRIBUTE = RewardTypes.register(
                FTBQuestsAPI.rl("attribute"),
                AttributeReward::new,
                () -> Icon.getIcon("minecraft:item/nether_star")
        );
        COMPLETE_OBJECT = RewardTypes.register(
                FTBQuestsAPI.rl("complete_object"),
                CompleteObjectReward::new,
                () -> Icon.getIcon("ftbquests:item/book")
        );
        PLAY_SOUND = RewardTypes.register(
                FTBQuestsAPI.rl("play_sound"),
                PlaySoundReward::new,
                () -> Icon.getIcon("minecraft:block/sculk_catalyst_top_bloom")
        );
        LOOT_TABLE = RewardTypes.register(
                FTBQuestsAPI.rl("loot_table"),
                LootTableReward::new,
                () -> Icon.getIcon("minecraft:item/chest_minecart")
        );
        POTION = RewardTypes.register(
                FTBQuestsAPI.rl("potion"),
                PotionReward::new,
                () -> Icon.getIcon("minecraft:item/potion")
        );
        SUMMON = RewardTypes.register(
                FTBQuestsAPI.rl("summon"),
                SummonReward::new,
                () -> Icon.getIcon("minecraft:item/totem_of_undying")
        );
        if (LevelZCompat.isLoaded()) {
            LEVELZ = RewardTypes.register(
                    FTBQuestsAPI.rl("levelz"),
                    LevelZReward::new,
                    () -> Icon.getIcon("levelz:icon.png")
            );
        }
        if (ReskillableCompat.isLoaded()) {
            RESKILLABLE = RewardTypes.register(
                    FTBQuestsAPI.rl("reskillable"),
                    ReskillableReward::new,
                    () -> Icon.getIcon("minecraft:item/experience_bottle")
            );
        }
    }
}
