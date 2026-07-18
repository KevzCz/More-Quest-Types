package net.pixeldreamstudios.morequesttypes.compat;

import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.perplayerquests.api.IPerPlayerObject;
import net.pixeldreamstudios.perplayerquests.progression.IPerPlayerStore;
import net.pixeldreamstudios.perplayerquests.progression.PerPlayerCollapse;
import net.pixeldreamstudios.perplayerquests.progression.PerPlayerReset;
import net.pixeldreamstudios.perplayerquests.progression.PerPlayerServer;

import java.util.List;
import java.util.UUID;

public final class PerPlayerQuestsBridge {
    private PerPlayerQuestsBridge() {}

    public static boolean isPerPlayer(QuestObjectBase obj) {
        return obj instanceof IPerPlayerObject ppo && ppo.mqt$isPerPlayer();
    }

    public static boolean isCompletedForPlayer(TeamData teamData, QuestObjectBase obj, UUID player) {
        if (!(teamData instanceof IPerPlayerStore store)) return false;
        return store.mqt$isPerPlayerCompleted(player, obj.id);
    }

    public static void resetForWholeTeam(TeamData teamData, Quest quest) {
        if (!(teamData instanceof IPerPlayerStore store)) return;
        List<UUID> members = PerPlayerCollapse.teamMembers(teamData);
        for (UUID member : members) {
            PerPlayerReset.resetQuest(store, teamData, member, quest);
        }
        PerPlayerServer.broadcastTeam(teamData);
    }

    public static void completeForWholeTeam(TeamData teamData, Quest quest) {
        if (!(teamData instanceof IPerPlayerStore)) return;
        if (!(teamData.getFile() instanceof ServerQuestFile file)) return;
        for (ServerPlayer player : teamData.getOnlineMembers()) {
            file.withPlayerContext(player, () -> PerPlayerReset.completeQuest(teamData, quest));
        }
        PerPlayerServer.broadcastTeam(teamData);
    }

    public static int getCompletionCountForPlayer(TeamData teamData, Quest quest, UUID player) {
        if (!(teamData instanceof IPerPlayerStore store)) return 0;
        return store.mqt$getPerPlayerCompletionCount(player, quest.id);
    }

    public static void clearRepeatStateForPlayer(TeamData teamData, Quest quest, UUID player) {
        if (!(teamData instanceof IPerPlayerStore store)) return;
        store.mqt$clearPerPlayerRepeatState(player, quest.id);
    }
}
