package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.compat.SpellEngineCompat;
import net.pixeldreamstudios.morequesttypes.config.MultiSelectListConfig;
import net.pixeldreamstudios.morequesttypes.util.SpellDisplayHelper;
import net.pixeldreamstudios.morequesttypes.util.SpellTooltipHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SpellEquippedTask extends Task {
    public enum MatchMode {ALL, ANY}

    private final List<String> spells = new ArrayList<>();
    private MatchMode matchMode = MatchMode.ALL;
    private long requiredCount = 0;

    public SpellEquippedTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.SPELL_EQUIPPED;
    }

    @Override
    public long getMaxProgress() {
        return 1L;
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 20;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;
        if (!SpellEngineCompat.isLoaded()) return;
        if (spells.isEmpty()) return;

        Set<String> equipped = new HashSet<>();
        for (ResourceLocation id : SpellEngineCompat.getPlayerEquippedSpellIds(player)) {
            equipped.add(id.toString());
        }

        long needed = computeNeeded();
        long matched = spells.stream().filter(equipped::contains).count();
        if (matched >= needed) {
            teamData.setProgress(this, 1L);
        }
    }

    private long computeNeeded() {
        int total = spells.size();
        if (total == 0) {
            return 1L;
        }
        if (matchMode == MatchMode.ALL) {
            return total;
        }
        if (requiredCount == 0) {
            return 1L;
        }
        return Math.min(requiredCount, total);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        if (matchMode == MatchMode.ALL) {
            return Component.translatable("morequesttypes.task.spell_equipped.title", spells.size(), "all");
        }
        if (requiredCount > 0) {
            return Component.translatable("morequesttypes.task.spell_equipped.title.required", requiredCount, spells.size());
        }
        return Component.translatable("morequesttypes.task.spell_equipped.title", spells.size(), "any");
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return SpellDisplayHelper.spellListIcon(spells);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        list.blankLine();
        MutableComponent modeLine;
        if (matchMode == MatchMode.ALL) {
            modeLine = Component.translatable("morequesttypes.task.spell_equipped.tooltip.mode_all");
        } else if (requiredCount > 0) {
            modeLine = Component.translatable("morequesttypes.task.spell_equipped.tooltip.mode_required", requiredCount);
        } else {
            modeLine = Component.translatable("morequesttypes.task.spell_equipped.tooltip.mode_any");
        }
        list.add(modeLine.withStyle(ChatFormatting.GRAY));
        SpellTooltipHelper.addSpellLines(list, spells,
                Component.translatable("morequesttypes.task.spell_equipped.tooltip.none"));
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        config.add("spells", new MultiSelectListConfig(
                SpellEquippedTask::spellChoices,
                "morequesttypes.task.spell_equipped.spells",
                SpellDisplayHelper::spellName
        ), spells, v -> {
            spells.clear();
            if (v != null) spells.addAll(v);
        }, new ArrayList<>()).setNameKey("morequesttypes.task.spell_equipped.spells");

        var MATCH_MODES = NameMap.of(matchMode, MatchMode.values()).create();
        config.addEnum("match_mode", matchMode, v -> matchMode = v, MATCH_MODES)
                .setNameKey("morequesttypes.task.spell_equipped.match_mode");

        config.addLong("required", requiredCount, v -> requiredCount = Math.max(0, v), 0L, 0L, Long.MAX_VALUE)
                .setNameKey("morequesttypes.task.spell_equipped.required");
    }

    @Environment(EnvType.CLIENT)
    private static List<String> spellChoices() {
        List<String> choices = new ArrayList<>();
        try {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level != null && SpellEngineCompat.isLoaded()) {
                SpellEngineCompat.getAllSpells(minecraft.level).stream()
                        .map(ResourceLocation::toString)
                        .sorted()
                        .forEach(choices::add);
            }
        } catch (Throwable ignored) {
        }
        return choices;
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (!spells.isEmpty()) {
            ListTag list = new ListTag();
            for (String s : spells) list.add(StringTag.valueOf(s));
            nbt.put("spells", list);
        }
        nbt.putString("match_mode", matchMode.name());
        nbt.putLong("required", requiredCount);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        spells.clear();
        if (nbt.contains("spells")) {
            var list = nbt.getList("spells", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String entry = list.getString(i);
                if (!entry.isBlank()) spells.add(entry.trim());
            }
        }
        try {
            matchMode = MatchMode.valueOf(nbt.getString("match_mode"));
        } catch (Throwable ignored) {
            matchMode = MatchMode.ALL;
        }
        requiredCount = Math.max(0, nbt.getLong("required"));
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeVarInt(spells.size());
        for (String s : spells) buf.writeUtf(s == null ? "" : s);
        buf.writeEnum(matchMode);
        buf.writeVarLong(requiredCount);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        spells.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            String s = buf.readUtf();
            if (!s.isBlank()) spells.add(s.trim());
        }
        matchMode = buf.readEnum(MatchMode.class);
        requiredCount = buf.readVarLong();
    }
}
