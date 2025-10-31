package net.pixeldreamstudios.morequesttypes;

import net.pixeldreamstudios.morequesttypes.compat.ServerLoginHooks;
import net.pixeldreamstudios.morequesttypes.network.MQTNetwork;
import net.pixeldreamstudios.morequesttypes.rewards.MoreRewardTypes;
import net.pixeldreamstudios.morequesttypes.tasks.MoreTasksTypes;

public final class MoreQuestTypes {
    public static final String MOD_ID = "more_quest_types";
    public static void init() {
        MoreTasksTypes.init();
        MoreRewardTypes.init();
        MQTNetwork.init();
        ServerLoginHooks.register();
    }
}
