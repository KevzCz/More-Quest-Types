package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.util.StringUtils;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;

public class TimerTask extends Task {
    private double durationSeconds = 10.0D;
    public TimerTask(long id, dev.ftb.mods.ftbquests.quest.Quest quest) {
        super(id, quest);
    }

    private long maxTicks() {
        long ticks = Math.max(1L, Math.round(durationSeconds * 20.0D));
        return ticks;
    }

    @Override
    public dev.ftb.mods.ftbquests.quest.task.TaskType getType() {
        return MoreTasksTypes.TIMER;
    }

    @Override
    public long getMaxProgress() {
        return maxTicks();
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
        long remainingTicks = Math.max(0L, maxTicks() - progress);
        double remainingSeconds = remainingTicks / 20.0D;
        return StringUtils.formatDouble(remainingSeconds, true) + "s";
    }


    @Override
    public int autoSubmitOnPlayerTick() {
        return 1;
    }

    @Override
    public void submitTask(TeamData teamData, net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        var online = teamData.getOnlineMembers();
        if (online == null || online.isEmpty() || !online.iterator().next().getUUID().equals(player.getUUID())) {
            return;
        }

        long current = teamData.getProgress(this);
        long next = current + 1L;
        long max = maxTicks();

        teamData.setProgress(this, Math.min(next, max));
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
        long remaining = Math.max(0L, maxTicks() - p);
        double remainS = remaining / 20.0D;
        list.add(Component.translatable("ftbquests.morequesttypes.task.timer.remaining", StringUtils.formatDouble(remainS, true) + "s"));
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
        ).setNameKey("ftbquests.task.timer.duration");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putDouble("duration_seconds", durationSeconds);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        if (nbt.contains("duration_seconds")) {
            durationSeconds = Math.max(0.05D, nbt.getDouble("duration_seconds"));
        }
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeDouble(durationSeconds);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        durationSeconds = Math.max(0.05D, buffer.readDouble());
    }
}
