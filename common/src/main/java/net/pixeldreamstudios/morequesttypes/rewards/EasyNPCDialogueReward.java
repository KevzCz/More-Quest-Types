package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.pixeldreamstudios.morequesttypes.compat.EasyNPCCompat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class EasyNPCDialogueReward extends Reward {
    public enum SortMode { NEAREST, FURTHEST, RANDOM }

    private String dialogueLabel = "";
    private String npcType = "";
    private SortMode sort = SortMode.NEAREST;
    private String customName = "";
    private final List<String> scoreboardTags = new ArrayList<>();
    private int minTagsRequired = 0;
    private String nbtFilterSnbt = "";
    private transient Tag nbtFilterParsed = null;

    public EasyNPCDialogueReward(long id, Quest q) {
        super(id, q);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.EASY_NPC_DIALOGUE;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        if (!EasyNPCCompat.isLoaded()) return;
        if (dialogueLabel.isBlank()) return;

        Entity npc = selectNPC(player);
        if (npc == null) return;

        EasyNPCCompat.startDialogueByLabel(player, npc, dialogueLabel);
    }

    private Entity selectNPC(ServerPlayer player) {
        EntityType<?> filter = null;
        ResourceLocation rl = parse(npcType);
        if (rl != null) filter = BuiltInRegistries.ENTITY_TYPE.get(rl);
        final EntityType<?> f = filter;

        var level = player.serverLevel();
        AABB box = player.getBoundingBox().inflate(256.0D);

        List<Entity> list = level.getEntities((Entity) null, box,
                e -> e.isAlive()
                        && e != player
                        && (f == null || e.getType() == f)
                        && entityMatches(e));

        if (list.isEmpty()) return null;

        return switch (sort) {
            case RANDOM -> list.get(level.getRandom().nextInt(list.size()));
            case FURTHEST -> list.stream()
                    .max(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                    .orElse(null);
            default -> list.stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                    .orElse(null);
        };
    }

    private boolean entityMatches(Entity e) {
        if (!customName.isEmpty()) {
            if (e instanceof Player p) {
                if (!p.getGameProfile().getName().equals(customName)) return false;
            } else if (!e.getName().getString().equals(customName)) {
                return false;
            }
        }

        if (!scoreboardTags.isEmpty()) {
            var present = e.getTags();
            int count = 0;
            for (var r : scoreboardTags) if (present.contains(r)) count++;
            int need = (minTagsRequired <= 0) ? scoreboardTags.size() : minTagsRequired;
            if (count < need) return false;
        }

        if (nbtFilterParsed != null) {
            var actual = new CompoundTag();
            e.saveWithoutId(actual);
            return nbtFilterParsed instanceof CompoundTag filter && nbtSubsetMatches(actual, filter);
        }

        return true;
    }

    private static List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        String[] parts = csv.split(",");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static boolean nbtSubsetMatches(Tag actual, Tag filter) {
        if (filter instanceof CompoundTag f) {
            if (!(actual instanceof CompoundTag a)) return false;
            for (String key : f.getAllKeys()) {
                Tag fVal = f.get(key);
                Tag aVal = a.get(key);
                if (aVal == null || !nbtSubsetMatches(aVal, fVal)) return false;
            }
            return true;
        }

        if (filter instanceof ListTag fList) {
            if (!(actual instanceof ListTag aList)) return false;

            List<Tag> remaining = new ArrayList<>(aList);

            outer:
            for (Tag fEl : fList) {
                for (int j = 0; j < remaining.size(); j++) {
                    if (nbtSubsetMatches(remaining.get(j), fEl)) {
                        remaining.remove(j);
                        continue outer;
                    }
                }
                return false;
            }
            return true;
        }

        return NbtUtils.compareNbt(filter, actual, false) && NbtUtils.compareNbt(actual, filter, false);
    }

    private void parseNbtFilter() {
        if (nbtFilterSnbt == null || nbtFilterSnbt.isBlank()) {
            nbtFilterParsed = null;
            return;
        }
        try {
            nbtFilterParsed = TagParser.parseTag(nbtFilterSnbt);
        } catch (Exception ignored) {
            nbtFilterParsed = null;
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Component getAltTitle() {
        String label = dialogueLabel.isBlank() ? "?" : dialogueLabel;
        String who = npcType.isBlank() ? "@e" : npcType;
        return Component.translatable("morequesttypes.reward.easynpc_dialogue.title",
                label, who, sort.name().toLowerCase(Locale.ROOT));
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return this.getType().getIconSupplier();
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (!dialogueLabel.isBlank()) nbt.putString("dialogue_label", dialogueLabel);
        if (!npcType.isBlank()) nbt.putString("npc_type", npcType);
        nbt.putString("sort", sort.name());

        if (!customName.isEmpty()) nbt.putString("custom_name", customName);
        if (!scoreboardTags.isEmpty()) {
            ListTag tagList = new ListTag();
            for (String s : scoreboardTags) tagList.add(StringTag.valueOf(s));
            nbt.put("scoreboard_tags", tagList);
        }
        if (minTagsRequired > 0) nbt.putInt("min_tags_required", minTagsRequired);
        if (!nbtFilterSnbt.isEmpty()) nbt.putString("nbt_filter_snbt", nbtFilterSnbt);

        nbt.putBoolean("exclude_from_claim_all", true);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        dialogueLabel = nbt.getString("dialogue_label");
        npcType = nbt.getString("npc_type");
        try { sort = SortMode.valueOf(nbt.getString("sort")); } catch (Throwable ignored) { sort = SortMode.NEAREST; }

        customName = nbt.getString("custom_name");

        scoreboardTags.clear();
        ListTag list = nbt.getList("scoreboard_tags", Tag.TAG_STRING);
        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                String s = list.getString(i);
                if (!s.isBlank()) scoreboardTags.add(s.trim());
            }
        } else {
            String csv = nbt.getString("scoreboard_tags_csv");
            if (!csv.isBlank()) scoreboardTags.addAll(parseCsv(csv));
        }

        minTagsRequired = nbt.contains("min_tags_required") ? nbt.getInt("min_tags_required") : 0;
        nbtFilterSnbt = nbt.getString("nbt_filter_snbt");
        parseNbtFilter();
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(dialogueLabel);
        buffer.writeUtf(npcType);
        buffer.writeEnum(sort);

        buffer.writeUtf(customName);
        buffer.writeVarInt(scoreboardTags.size());
        for (String s : scoreboardTags) buffer.writeUtf(s == null ? "" : s);
        buffer.writeVarInt(minTagsRequired);
        buffer.writeUtf(nbtFilterSnbt);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        dialogueLabel = buffer.readUtf();
        npcType = buffer.readUtf();
        sort = buffer.readEnum(SortMode.class);

        customName = buffer.readUtf();
        scoreboardTags.clear();
        int nTags = buffer.readVarInt();
        for (int i = 0; i < nTags; i++) {
            String s = buffer.readUtf();
            if (!s.isBlank()) scoreboardTags.add(s.trim());
        }
        minTagsRequired = buffer.readVarInt();
        nbtFilterSnbt = buffer.readUtf();
        parseNbtFilter();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addString("dialogue_label", dialogueLabel, v -> dialogueLabel = v.trim(), "")
                .setNameKey("morequesttypes.reward.easynpc_dialogue.dialogue_label");

        var SORTS = NameMap.of(SortMode.NEAREST, SortMode.values()).create();
        config.addEnum("sort", sort, v -> sort = v, SORTS)
                .setNameKey("morequesttypes.reward.easynpc_dialogue.sort");

        var ids = BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .map(Objects::toString)
                .sorted()
                .toList();
        var TYPES = NameMap.of("", ids.toArray(String[]::new))
                .name(s -> (s == null || s.isEmpty())
                        ? Component.literal("@e")
                        : Component.literal(s))
                .create();
        config.addEnum("npc_type", npcType, v -> npcType = v.trim(), TYPES)
                .setNameKey("morequesttypes.reward.easynpc_dialogue.npc_type");

        config.addString("custom_name", customName, v -> customName = v.trim(), "")
                .setNameKey("morequesttypes.reward.easynpc_dialogue.custom_name");
        config.addList("scoreboard_tags", scoreboardTags, new StringConfig(), "")
                .setNameKey("morequesttypes.task.tags_csv");
        config.addInt("min_tags_required", minTagsRequired, v -> minTagsRequired = Math.max(0, v), 0, 0, 64)
                .setNameKey("morequesttypes.task.min_tags");
        config.addString("nbt_filter_snbt", nbtFilterSnbt, v -> { nbtFilterSnbt = v; parseNbtFilter(); }, "")
                .setNameKey("morequesttypes.task.nbt");
    }

    private static ResourceLocation parse(String s) {
        if (s == null || s.isBlank()) return null;
        return ResourceLocation.tryParse(s);
    }

    @Override public boolean getExcludeFromClaimAll() { return true; }
    @Override public boolean isClaimAllHardcoded() { return true; }
}