package net.pixeldreamstudios.morequesttypes.network.button;

import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.rewards.AttributeReward;
import net.pixeldreamstudios.morequesttypes.rewards.SpellReward;

public final class ToggleRewardHandler {
    private ToggleRewardHandler() {}

    public static void handleToggle(ServerPlayer player, long rewardId) {
        if (player == null) return;
        TeamData teamData = TeamData.get(player);
        if (teamData == null) return;

        var file = teamData.getFile();
        if (file == null) return;

        Reward r = file.getReward(rewardId);
        if (r == null) return;

        if (teamData.isRewardBlocked(r)) return;

        boolean claimed = teamData.isRewardClaimed(player.getUUID(), r);
        try {
            if (!claimed) {
                teamData.claimReward(player, r, true);

            } else {
                teamData.resetReward(player.getUUID(), r);

                if (r instanceof SpellReward sr) {
                    try { sr.removeFromPlayer(player); } catch (Throwable ignored) {}
                }
                if (r instanceof AttributeReward ar) {
                    try { ar.removeFromPlayer(player); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
        }
    }
}
