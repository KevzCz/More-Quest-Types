package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import net.pixeldreamstudios.morequesttypes.compat.BlabberCompat;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;

public final class MoreRewardTypes {
    private MoreRewardTypes() {}

    public static RewardType DIALOGUE;
    public static RewardType SKILLS_LEVEL;

    public static void init() {
        if(BlabberCompat.isLoaded()) {
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
    }
}
