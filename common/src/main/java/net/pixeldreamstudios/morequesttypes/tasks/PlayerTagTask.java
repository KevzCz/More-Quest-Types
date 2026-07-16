package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PlayerTagTask extends Task {
    public enum Mode {ANY, ALL}

    public enum Scope {ACTOR, ANY_TEAM_MEMBER}

    private final List<String> tags = new ArrayList<>();
    private Mode mode = Mode.ANY;
    private Scope scope = Scope.ACTOR;

    public PlayerTagTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.PLAYER_TAG;
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 20;
    }

    @Override
    public boolean hideProgressNumbers() {
        return true;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;
        if (tags.isEmpty()) return;

        boolean satisfied;
        if (scope == Scope.ANY_TEAM_MEMBER) {
            satisfied = false;
            for (ServerPlayer member : teamData.getOnlineMembers()) {
                if (matches(member)) {
                    satisfied = true;
                    break;
                }
            }
        } else {
            satisfied = matches(player);
        }

        if (satisfied) {
            teamData.setProgress(this, getMaxProgress());
        }
    }

    private boolean matches(ServerPlayer player) {
        var playerTags = player.getTags();
        if (mode == Mode.ALL) {
            for (String t : tags) {
                if (!t.isBlank() && !playerTags.contains(t)) return false;
            }
            return true;
        }
        for (String t : tags) {
            if (!t.isBlank() && playerTags.contains(t)) return true;
        }
        return false;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        return Component.translatable("morequesttypes.task.player_tag.title");
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return Icon.getIcon("minecraft:item/name_tag");
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        Component scopeText = Component.translatable(
                "morequesttypes.task.player_tag.scope." + scope.name().toLowerCase());
        list.add(scopeText.copy().withStyle(ChatFormatting.GRAY));
        for (String t : tags) {
            if (!t.isBlank()) list.add(Component.literal(" - " + t).withStyle(ChatFormatting.YELLOW));
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var MODES = NameMap.of(Mode.ANY, Mode.values()).create();
        config.addEnum("mode", mode, v -> mode = v, MODES)
                .setNameKey("morequesttypes.task.player_tag.mode");

        var SCOPES = NameMap.of(Scope.ACTOR, Scope.values())
                .name(s -> Component.translatable("morequesttypes.task.player_tag.scope." + s.name().toLowerCase()))
                .create();
        config.addEnum("scope", scope, v -> scope = v, SCOPES)
                .setNameKey("morequesttypes.task.player_tag.scope");

        config.addList("tags", tags, new StringConfig(), "")
                .setNameKey("morequesttypes.task.player_tag.tags");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("mode", mode.name());
        nbt.putString("scope", scope.name());
        ListTag list = new ListTag();
        for (String s : tags) list.add(StringTag.valueOf(s));
        nbt.put("tags", list);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        try {
            mode = Mode.valueOf(nbt.getString("mode"));
        } catch (Throwable ignored) {
            mode = Mode.ANY;
        }
        try {
            scope = Scope.valueOf(nbt.getString("scope"));
        } catch (Throwable ignored) {
            scope = Scope.ACTOR;
        }
        tags.clear();
        ListTag list = nbt.getList("tags", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) tags.add(list.getString(i));
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(mode);
        buf.writeEnum(scope);
        buf.writeVarInt(tags.size());
        for (String s : tags) buf.writeUtf(s);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        mode = buf.readEnum(Mode.class);
        scope = buf.readEnum(Scope.class);
        tags.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) tags.add(buf.readUtf());
    }
}
