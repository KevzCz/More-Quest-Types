package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.event.CommandEventBuffer;

import java.util.ArrayList;
import java.util.List;

public final class CommandTask extends Task {
    public enum MatchMode {
        EXACT,
        STARTS_WITH,
        CONTAINS
    }

    private final List<String> commandPatterns = new ArrayList<>();
    private MatchMode matchMode = MatchMode.EXACT;
    private long value = 1;
    private boolean ignoreCase = true;

    public CommandTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.COMMAND;
    }

    @Override
    public long getMaxProgress() {
        return Math.max(1L, value);
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 1;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        long cur = teamData.getProgress(this);
        if (cur >= getMaxProgress()) return;

        var events = CommandEventBuffer.snapshotLatest(player.getUUID());
        if (events.isEmpty()) return;

        int matches = 0;
        for (var event : events) {
            if (commandMatches(event.command())) {
                matches++;
            }
        }

        if (matches > 0) {
            long next = Math.min(getMaxProgress(), cur + matches);
            teamData.setProgress(this, next);
        }
    }

    private boolean commandMatches(String executedCommand) {
        if (commandPatterns.isEmpty()) return false;

        String cmd = executedCommand;
        if (ignoreCase) {
            cmd = cmd.toLowerCase();
        }

        for (String pattern : commandPatterns) {
            String p = normalizePattern(pattern);
            if (ignoreCase) {
                p = p.toLowerCase();
            }

            boolean match = switch (matchMode) {
                case EXACT -> cmd.equals(p);
                case STARTS_WITH -> cmd.startsWith(p);
                case CONTAINS -> cmd.contains(p);
            };

            if (match) return true;
        }

        return false;
    }

    private String normalizePattern(String pattern) {
        if (pattern.startsWith("/")) {
            return pattern.substring(1);
        }
        return pattern;
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putLong("value", value);
        nbt.putString("match_mode", matchMode.name());
        nbt.putBoolean("ignore_case", ignoreCase);

        if (!commandPatterns.isEmpty()) {
            ListTag list = new ListTag();
            for (String pattern : commandPatterns) {
                list.add(StringTag.valueOf(pattern));
            }
            nbt.put("command_patterns", list);
        }
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        value = nbt.getLong("value");

        if (nbt.contains("match_mode")) {
            try {
                matchMode = MatchMode.valueOf(nbt.getString("match_mode"));
            } catch (IllegalArgumentException e) {
                matchMode = MatchMode.EXACT;
            }
        }

        if (nbt.contains("ignore_case")) {
            ignoreCase = nbt.getBoolean("ignore_case");
        }

        commandPatterns.clear();
        if (nbt.contains("command_patterns")) {
            ListTag list = nbt.getList("command_patterns", 8);
            for (int i = 0; i < list.size(); i++) {
                commandPatterns.add(list.getString(i));
            }
        }
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeVarLong(value);
        buffer.writeEnum(matchMode);
        buffer.writeBoolean(ignoreCase);
        buffer.writeVarInt(commandPatterns.size());
        for (String pattern : commandPatterns) {
            buffer.writeUtf(pattern);
        }
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        value = buffer.readVarLong();
        matchMode = buffer.readEnum(MatchMode.class);
        ignoreCase = buffer.readBoolean();
        int size = buffer.readVarInt();
        commandPatterns.clear();
        for (int i = 0; i < size; i++) {
            commandPatterns.add(buffer.readUtf());
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        if (commandPatterns.isEmpty()) {
            return Component.translatable("morequesttypes.task.command.title.empty");
        }

        String patternsStr = String.join(", ", commandPatterns);
        return Component.translatable("morequesttypes.task.command.title",
                patternsStr,
                matchMode.name().toLowerCase()
        );
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return Icon.getIcon("minecraft:block/command_block_side");
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        config.addLong("value", value, v -> value = Math.max(1L, v), 1L, 1L, Long.MAX_VALUE)
                .setNameKey("morequesttypes.task.command.value");

        config.addList("command_patterns", commandPatterns, new StringConfig(), "")
                .setNameKey("morequesttypes.task.command.patterns");

        var MATCH_MODES = NameMap.of(MatchMode.EXACT, MatchMode.values()).create();
        config.addEnum("match_mode", matchMode, v -> matchMode = v, MATCH_MODES)
                .setNameKey("morequesttypes.task.command.match_mode");

        config.addBool("ignore_case", ignoreCase, v -> ignoreCase = v, true)
                .setNameKey("morequesttypes.task.command.ignore_case");
    }
}