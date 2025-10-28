package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Collectors;

public final class CheckQuestTask extends dev.ftb.mods.ftbquests.quest.task.Task {
    public enum Mode { ANY, ALL }

    private final List<String> selectors = new ArrayList<>();
    private Mode mode = Mode.ANY;
    private long requiredCount = 0;

    private transient List<Long> cachedTargetIds = Collections.emptyList();

    public CheckQuestTask(long id, Quest quest) { super(id, quest); }

    @Override public TaskType getType() { return MoreTasksTypes.CHECK_QUEST; }

    @Override public long getMaxProgress() {
        int total = cachedTargetIds == null ? 0 : cachedTargetIds.size();
        long needed = computeNeeded(total);
        return Math.max(1, needed);
    }

    @Override public boolean hideProgressNumbers() { return false; }

    @Override public int autoSubmitOnPlayerTick() { return 1; }

    @Override
    public void submitTask(TeamData teamData, net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        if (cachedTargetIds == null || cachedTargetIds.isEmpty()) {
            cachedTargetIds = resolveTargets(teamData.getFile());
        }

        int total = cachedTargetIds.size();
        long needed = computeNeeded(total);
        if (total == 0) {
            if (teamData.getProgress(this) != 0) teamData.setProgress(this, 0);
            return;
        }

        long completed = cachedTargetIds.stream()
                .map(teamData.getFile()::getBase)
                .filter(Objects::nonNull)
                .filter(obj -> (obj instanceof Task t && teamData.isCompleted(t)) ||
                        (obj instanceof Quest q && teamData.isCompleted(q)))
                .count();

        long next = Math.min(needed, completed);
        if (teamData.getProgress(this) != next) teamData.setProgress(this, next);
    }

    private long computeNeeded(int totalResolved) {
        if (mode == Mode.ALL || requiredCount == 0) return Math.max(1, totalResolved);
        return Math.min(requiredCount, Math.max(1, totalResolved));
    }

    /** Works on both client and server – no ServerQuestFile cast. */
    private List<Long> resolveTargets(BaseQuestFile file) {
        if (file == null) return Collections.emptyList();

        Set<QuestObjectBase> result = new LinkedHashSet<>();

        for (String sel : selectors) {
            if (sel == null || sel.isBlank()) continue;

            if (sel.startsWith("#")) {
                String tag = sel.substring(1);
                for (QuestObjectBase qob : file.getAllObjects()) {
                    if (qob.hasTag(tag) && (qob instanceof Quest || qob instanceof Task)) {
                        result.add(qob);
                    }
                }
            } else {
                QuestObjectBase.parseHexId(sel.trim()).ifPresent(id -> {
                    QuestObjectBase qob = file.getBase(id);
                    if (qob instanceof Quest || qob instanceof Task) result.add(qob);
                });
            }
        }

        return result.stream().map(QuestObjectBase::getID).collect(Collectors.toList());
    }

    @Environment(EnvType.CLIENT)
    @Override
    public net.minecraft.network.chat.MutableComponent getAltTitle() {
        String suffix = (mode == Mode.ALL || requiredCount == 0) ? "ALL" : "ANY, need " + requiredCount;
        return Component.translatable("ftbquests.task.morequesttypes.check_quest.title", suffix);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return Icon.getIcon("ftbquests:item/book");
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(dev.ftb.mods.ftblibrary.util.TooltipList list, TeamData teamData) {
        if (cachedTargetIds == null || cachedTargetIds.isEmpty()) {
            cachedTargetIds = resolveTargets(getQuestFile());
        }
        String reqWord;
        if (requiredCount > 0) {
            reqWord = (mode == Mode.ALL) ? "all" : String.valueOf(requiredCount);
        } else {
            reqWord = (mode == Mode.ALL) ? "all" : "any";
        }
        list.add(Component.literal("Complete " + reqWord + " quest(s):").withStyle(net.minecraft.ChatFormatting.YELLOW));

        BaseQuestFile file = getQuestFile();
        int shown = 0;
        for (Long id : cachedTargetIds) {
            QuestObjectBase qob = file.getBase(id);
            if (qob == null) continue;

            boolean done = (qob instanceof Task t && teamData.isCompleted(t))
                    || (qob instanceof Quest q && teamData.isCompleted(q));

            Component title = qob.getTitle();
            Component line = Component.literal(" - ").append(title.copy());
            if (done) line = line.copy().withStyle(net.minecraft.ChatFormatting.GREEN);

            list.add(line);
            shown++;

            if (shown >= 30) {
                int remaining = cachedTargetIds.size() - shown;
                if (remaining > 0) {
                    list.add(Component.literal("   …and " + remaining + " more"));
                }
                break;
            }
        }

    }


    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var MODES = NameMap.of(Mode.ANY, Mode.values()).create();
        config.addEnum("mode", mode, v -> { mode = v; cachedTargetIds = Collections.emptyList(); }, MODES)
                .setNameKey("ftbquests.task.morequesttypes.check_quest.mode");

        config.addLong("required", requiredCount, v -> requiredCount = Math.max(0, v), 0L, 0L, Long.MAX_VALUE)
                .setNameKey("ftbquests.task.morequesttypes.check_quest.required");

        config.addList("targets", selectors, new dev.ftb.mods.ftblibrary.config.StringConfig(), "")
                .setNameKey("ftbquests.task.morequesttypes.check_quest.targets");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("mode", mode.name());
        nbt.putLong("required", requiredCount);
        var list = new net.minecraft.nbt.ListTag();
        for (String s : selectors) list.add(net.minecraft.nbt.StringTag.valueOf(s));
        nbt.put("targets", list);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        try { mode = Mode.valueOf(nbt.getString("mode")); } catch (Throwable ignored) { mode = Mode.ANY; }
        requiredCount = Math.max(0, nbt.getLong("required"));

        selectors.clear();
        var list = nbt.getList("targets", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) selectors.add(list.getString(i));

        cachedTargetIds = Collections.emptyList();
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(mode);
        buf.writeVarLong(requiredCount);
        buf.writeVarInt(selectors.size());
        for (String s : selectors) buf.writeUtf(s);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        mode = buf.readEnum(Mode.class);
        requiredCount = buf.readVarLong();
        selectors.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) selectors.add(buf.readUtf());
        cachedTargetIds = Collections.emptyList();
    }
}
