package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.compat.SkillsCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class SkillsLevelTask extends Task {
    public enum Mode { TOTAL_LEVEL, CATEGORY_LEVEL }

    private Mode mode = Mode.TOTAL_LEVEL;
    private int requiredLevel = 10;
    private String categoryId = "";

    public SkillsLevelTask(long id, dev.ftb.mods.ftbquests.quest.Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.SKILLS_LEVEL;
    }



    @Override
    public long getMaxProgress() {
        return Math.max(1, requiredLevel);
    }

    @Override
    public boolean hideProgressNumbers() {
        return false;
    }

    @Override
    public String formatMaxProgress() {
        return Long.toString(getMaxProgress());
    }

    @Environment(EnvType.CLIENT)
    @Override
    public String formatProgress(TeamData teamData, long progress) {
        long shown = Math.max(0, Math.min(progress, getMaxProgress()));
        return Long.toString(shown);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getButtonText() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return Component.literal("? / " + getMaxProgress());

        long p = TeamData.get(player).getProgress(this);
        long shown = Math.max(0, Math.min(p, getMaxProgress()));
        return Component.literal(shown + " / " + getMaxProgress());
    }



    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var MODE_MAP = NameMap.of(Mode.TOTAL_LEVEL, Mode.values()).create();
        config.addEnum("mode", mode, v -> mode = v, MODE_MAP)
                .setNameKey("ftbquests.task.skills_level.mode");

        config.addInt("required_level", requiredLevel, v -> requiredLevel = Math.max(1, v), 10, 1, 100000)
                .setNameKey("ftbquests.task.skills_level.required");

        final var NONE = ResourceLocation.withDefaultNamespace("none");

        ArrayList<ResourceLocation> cats = new ArrayList<>();
        if (SkillsCompat.isLoaded()) {
            cats.addAll(SkillsCompat.getCategories(true));
        }
        cats.add(0, NONE);

        ResourceLocation current = (mode == Mode.TOTAL_LEVEL)
                ? NONE
                : Objects.requireNonNullElse(ResourceLocation.tryParse(categoryId),
                (cats.size() > 1 ? cats.get(1) : NONE));

        if (current.equals(NONE) && cats.size() > 1) current = cats.get(1);

        var CAT_MAP = NameMap
                .of(current, cats.toArray(ResourceLocation[]::new))
                .name(rl -> rl.equals(NONE) ? Component.literal("None") : Component.literal(rl.toString()))
                .create();

        config.addEnum("category", current, rl -> {
            categoryId = rl.equals(NONE) ? "" : rl.toString();
        }, CAT_MAP).setNameKey("ftbquests.task.skills_level.category");
    }



    @Override
    public void writeData(net.minecraft.nbt.CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("mode", mode.name());
        nbt.putInt("required_level", requiredLevel);
        if (!categoryId.isBlank()) nbt.putString("category", categoryId);
    }

    @Override
    public void readData(net.minecraft.nbt.CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        if (nbt.contains("mode")) {
            try { mode = Mode.valueOf(nbt.getString("mode")); }
            catch (IllegalArgumentException ignored) { mode = Mode.TOTAL_LEVEL; }
        }
        requiredLevel = Math.max(1, nbt.getInt("required_level"));
        categoryId = nbt.getString("category");
    }

    @Override
    public void writeNetData(net.minecraft.network.RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(mode);
        buf.writeVarInt(requiredLevel);
        buf.writeUtf(categoryId);
    }

    @Override
    public void readNetData(net.minecraft.network.RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        mode = buf.readEnum(Mode.class);
        requiredLevel = Math.max(1, buf.readVarInt());
        categoryId = buf.readUtf();
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 20;
    }

    @Override
    public boolean checkOnLogin() {
        return true;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, net.minecraft.world.item.ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;
        if (!SkillsCompat.isLoaded()) return;


        Collection<ServerPlayer> online = teamData.getOnlineMembers();
        if (online == null || online.isEmpty()) return;
        if (!online.iterator().next().getUUID().equals(player.getUUID())) return;

        int best = 0;
        if (mode == Mode.TOTAL_LEVEL) {
            for (ServerPlayer p : online) {
                best = Math.max(best, SkillsCompat.getTotalLevel(p));
            }
        } else {
            var cat = parseCategoryOrNull(categoryId);
            if (cat == null) return;
            for (ServerPlayer p : online) {
                best = Math.max(best, SkillsCompat.getCategoryLevel(p, cat));
            }
        }

        long max = getMaxProgress();
        long current = teamData.getProgress(this);
        long target = Math.max(current, Math.min(max, Math.max(0, best)));
        if (target != current) {
            teamData.setProgress(this, target);
        }

    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        if (mode == Mode.TOTAL_LEVEL) {
            list.add(Component.translatable("ftbquests.task.skills_level.tooltip.total", requiredLevel));
        } else {
            var shown = categoryId.isEmpty() ? "?" : categoryId;
            list.add(Component.translatable("ftbquests.task.skills_level.tooltip.category", shown, requiredLevel));
        }
    }

    private static ResourceLocation parseCategoryOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return ResourceLocation.tryParse(s);
    }
}
