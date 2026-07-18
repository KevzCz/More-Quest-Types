package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.compat.PerPlayerQuestsBridge;
import net.pixeldreamstudios.morequesttypes.compat.PerPlayerQuestsCompat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ResetObjectReward extends Reward {
    private final List<String> targets = new ArrayList<>();
    private boolean applyToWholeTeam = false;

    public ResetObjectReward(long id, Quest q) {
        super(id, q);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.RESET_OBJECT;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        var base = getQuestFile();
        if (!(base instanceof ServerQuestFile file)) {
            return;
        }

        file.getTeamData(player).ifPresent(team -> {
            List<QuestObjectBase> resolved = resolveTargets(file, targets);
            if (resolved.isEmpty()) {
                resolved = List.of(getQuest());
            }
            List<QuestObjectBase> finalResolved = resolved;
            file.withPlayerContext(player, () -> {
                for (QuestObjectBase obj : finalResolved) {
                    if (applyToWholeTeam && PerPlayerQuestsCompat.isLoaded()
                            && obj instanceof Quest quest && PerPlayerQuestsBridge.isPerPlayer(quest)) {
                        PerPlayerQuestsBridge.resetForWholeTeam(team, quest);
                    } else {
                        resetObject(team, obj);
                    }
                }
            });
        });
    }

    private static List<QuestObjectBase> resolveTargets(ServerQuestFile file, List<String> idsOrTags) {
        if (idsOrTags == null || idsOrTags.isEmpty()) return List.of();

        Set<QuestObjectBase> out = new LinkedHashSet<>();
        for (String idOrTag : idsOrTags) {
            if (idOrTag == null || idOrTag.isBlank()) continue;

            if (idOrTag.startsWith("#")) {
                String tag = idOrTag.substring(1);
                for (QuestObjectBase qob : file.getAllObjects()) {
                    if (qob.hasTag(tag)) out.add(qob);
                }
            } else {
                try {
                    long id = QuestObjectBase.parseHexId(idOrTag).orElseThrow();
                    QuestObjectBase qob = file.getBase(id);
                    if (qob != null) out.add(qob);
                } catch (Throwable ignored) {}
            }
        }

        return new ArrayList<>(out);
    }

    private static void resetObject(TeamData team, QuestObjectBase obj) {
        if (obj instanceof Task task) {
            team.resetProgress(task);
            team.setCompleted(task.getId(), null);
            team.setStarted(task.getId(), null);
        } else if (obj instanceof Quest quest) {
            for (Task t : quest.getTasksAsList()) {
                team.resetProgress(t);
                team.setCompleted(t.getId(), null);
                team.setStarted(t.getId(), null);
            }
            team.setCompleted(quest.getId(), null);
            team.setStarted(quest.getId(), null);
            team.clearRepeatCooldown(quest);
        }
    }

    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addList("targets", targets, new StringConfig(), "")
                .setNameKey("morequesttypes.reward.reset_object.targets");
        if (PerPlayerQuestsCompat.isLoaded()) {
            config.addBool("apply_to_whole_team", applyToWholeTeam, v -> applyToWholeTeam = v, false)
                    .setNameKey("morequesttypes.reward.reset_object.apply_to_whole_team");
        }
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (!targets.isEmpty()) {
            ListTag list = new ListTag();
            for (String s : targets) list.add(StringTag.valueOf(s));
            nbt.put("targets", list);
        }
        if (applyToWholeTeam) nbt.putBoolean("apply_to_whole_team", true);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);

        targets.clear();
        ListTag list = nbt.getList("targets", Tag.TAG_STRING);
        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) targets.add(list.getString(i));
        } else {
            String single = nbt.getString("target");
            if (!single.isBlank()) targets.add(single);
        }
        applyToWholeTeam = nbt.getBoolean("apply_to_whole_team");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeVarInt(targets.size());
        for (String s : targets) buffer.writeUtf(s);
        buffer.writeBoolean(applyToWholeTeam);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        targets.clear();
        int n = buffer.readVarInt();
        for (int i = 0; i < n; i++) targets.add(buffer.readUtf());
        applyToWholeTeam = buffer.readBoolean();
    }

}