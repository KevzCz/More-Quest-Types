package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.util.StringUtils;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimerTask extends Task {
    private double durationSeconds = 10.0D;
    private boolean useRealTime = false;
    private final Map<UUID, Long> fallbackStartMs = new HashMap<>();

    public TimerTask(long id, Quest quest) {
        super(id, quest);
    }

    private long maxTicks() {
        return Math.max(1L, Math.round(durationSeconds * 20.0D));
    }

    private long startMs(TeamData teamData) {
        Date started = teamData.getStartedTime(getQuest().id).orElse(null);
        if (started != null) return started.getTime();
        Long fallback = fallbackStartMs.get(teamData.getTeamId());
        return fallback != null ? fallback : -1L;
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.TIMER;
    }

    @Override
    public long getMaxProgress() {
        return useRealTime ? Math.max(1L, Math.round(durationSeconds)) : maxTicks();
    }

    @Override
    public boolean hideProgressNumbers() {
        return false;
    }

    @Override
    public String formatMaxProgress() {
        return StringUtils.formatDouble(durationSeconds, true) + "s";
    }

    @Override
    public String formatProgress(TeamData teamData, long progress) {
        double remainingSeconds = useRealTime
                ? Math.max(0.0D, durationSeconds - progress)
                : Math.max(0L, maxTicks() - progress) / 20.0D;
        return StringUtils.formatDouble(remainingSeconds, true) + "s";
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 1;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        var online = teamData.getOnlineMembers();
        if (online == null || online.isEmpty() || !online.iterator().next().getUUID().equals(player.getUUID())) {
            return;
        }

        if (useRealTime) {
            submitRealTime(teamData);
            return;
        }

        long current = teamData.getProgress(this);
        long next = current + 1L;
        long max = maxTicks();

        teamData.setProgress(this, Math.min(next, max));
    }

    private void submitRealTime(TeamData teamData) {
        long start = startMs(teamData);
        if (start < 0L) {
            start = System.currentTimeMillis();
            fallbackStartMs.put(teamData.getTeamId(), start);
        }

        long elapsedSeconds = Math.max(0L, (System.currentTimeMillis() - start) / 1000L);
        long next = Math.min(getMaxProgress(), elapsedSeconds);
        if (next != teamData.getProgress(this)) {
            teamData.setProgress(this, next);
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void addMouseOverHeader(TooltipList list, TeamData teamData, boolean advanced) {
        list.add(getTitle());
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        long p = teamData.getProgress(this);
        double remainS = useRealTime
                ? Math.max(0.0D, durationSeconds - p)
                : Math.max(0L, maxTicks() - p) / 20.0D;
        list.add(Component.translatable("morequesttypes.task.timer.remaining", StringUtils.formatDouble(remainS, true) + "s"));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void drawGUI(TeamData teamData, GuiGraphics graphics, int x, int y, int w, int h) {
        getIcon().draw(graphics, x, y, w, h);
    }

    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addDouble(
                "duration_seconds",
                durationSeconds,
                v -> durationSeconds = v,
                10.0D,
                0.05D,
                86400.0D
        ).setNameKey("morequesttypes.task.timer.duration");

        config.addBool(
                "use_real_time",
                useRealTime,
                v -> useRealTime = v,
                false
        ).setNameKey("morequesttypes.task.timer.use_real_time");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putDouble("duration_seconds", durationSeconds);
        if (useRealTime) nbt.putBoolean("use_real_time", true);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        if (nbt.contains("duration_seconds")) {
            durationSeconds = Math.max(0.05D, nbt.getDouble("duration_seconds"));
        }
        useRealTime = nbt.getBoolean("use_real_time");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeDouble(durationSeconds);
        buffer.writeBoolean(useRealTime);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        durationSeconds = Math.max(0.05D, buffer.readDouble());
        useRealTime = buffer.readBoolean();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public Component getAltTitle() {
        String timeLabel = StringUtils.formatDouble(durationSeconds, true) + "s";
        String typeName = getType().getDisplayName().getString();
        return Component.literal( timeLabel + " " + typeName);
    }
}
