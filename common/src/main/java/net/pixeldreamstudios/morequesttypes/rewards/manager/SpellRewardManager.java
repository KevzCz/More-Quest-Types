package net.pixeldreamstudios.morequesttypes.rewards.manager;

import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.compat.SpellEngineCompat;
import net.pixeldreamstudios.morequesttypes.rewards.SpellReward;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpellRewardManager {
    private SpellRewardManager() {}

    private static final Pattern BASE_KEY_PATTERN = Pattern.compile("^morequesttypes/reward_(\\d+)(?:/.*)?$");

    public static void syncForPlayer(ServerPlayer player) {
        if (player == null) return;

        TeamData teamData = TeamData.get(player);
        var file = teamData.getFile();
        if (file == null) return;

        Collection<? extends QuestObjectBase> all = file.getAllObjects();
        if (all != null) {
            for (QuestObjectBase obj : all) {
                if (!(obj instanceof Reward r)) continue;
                if (!(r instanceof SpellReward sr)) continue;

                boolean claimed = teamData.isRewardClaimed(player.getUUID(), sr);
                if (claimed) {
                    try { sr.applyToPlayer(player); } catch (Throwable ignored) {}
                } else {
                    try { sr.removeFromPlayer(player); } catch (Throwable ignored) {}
                }
            }
        }

        if (!SpellEngineCompat.isLoaded()) return;

        try {
            Collection<String> installedKeys = SpellEngineCompat.getInstalledSpellKeys(player);
            if (installedKeys == null || installedKeys.isEmpty()) return;

            List<String> keysToRemove = new ArrayList<>();
            for (String key : installedKeys) {
                if (key == null) continue;
                Matcher m = BASE_KEY_PATTERN.matcher(key);
                if (!m.matches()) continue;

                String idPart = m.group(1);
                long rewardId;
                try {
                    rewardId = Long.parseLong(idPart);
                } catch (NumberFormatException nfe) {
                    keysToRemove.add(key);
                    continue;
                }

                var rewardObj = file.getReward(rewardId);
                boolean shouldKeep = false;
                if (rewardObj instanceof SpellReward srExisting) {
                    shouldKeep = teamData.isRewardClaimed(player.getUUID(), srExisting);
                }

                if (!shouldKeep) keysToRemove.add(key);
            }

            if (!keysToRemove.isEmpty()) {
                SpellEngineCompat.removeInstalledSpellKeys(player, keysToRemove);
            }
        } catch (Throwable ignored) {}
    }
}
