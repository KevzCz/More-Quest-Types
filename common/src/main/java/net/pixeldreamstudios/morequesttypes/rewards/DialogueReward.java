package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
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
import net.pixeldreamstudios.morequesttypes.compat.BlabberCompat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class DialogueReward extends Reward {
    public enum SortMode { NEAREST, FURTHEST, RANDOM }
    private String dialogueId = "";
    private String interlocutorType = "";
    private SortMode sort = SortMode.NEAREST;
    private String customName = "";
    private String scoreboardTagsCsv = "";
    private int minTagsRequired = 0;
    private String nbtFilterSnbt = "";
    private transient Tag nbtFilterParsed = null;
    public DialogueReward(long id, Quest q) {
        super(id, q);
    }
    @Override
    public RewardType getType() {
        return MoreRewardTypes.DIALOGUE;
    }
    @Override
    public void claim(ServerPlayer player, boolean notify) {
        if (!BlabberCompat.isLoaded()) return;
        ResourceLocation id = ResourceLocation.tryParse(dialogueId);
        if (id == null) return;

        Entity interlocutor = selectInterlocutor(player);
        BlabberCompat.startDialogue(player, id, interlocutor);
    }
    private Entity selectInterlocutor(ServerPlayer player) {
        if (interlocutorType.equalsIgnoreCase("player")) return player;

        EntityType<?> filter = null;
        ResourceLocation rl = parse(interlocutorType);
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


        if (!scoreboardTagsCsv.isBlank()) {
            var required = parseCsv(scoreboardTagsCsv);
            var present = e.getTags();
            int count = 0;
            for (var r : required) if (present.contains(r)) count++;
            int need = (minTagsRequired <= 0) ? required.size() : minTagsRequired;
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
        String id = dialogueId.isBlank() ? "?" : dialogueId;
        String who = interlocutorType.isBlank() ? "@e" : interlocutorType;
        return Component.translatable("morequesttypes.reward.dialogue.title",
                id, who, sort.name().toLowerCase(Locale.ROOT));
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return this.getType().getIconSupplier();
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (!dialogueId.isBlank()) nbt.putString("dialogue_id", dialogueId);
        if (!interlocutorType.isBlank()) nbt.putString("interlocutor_type", interlocutorType);
        nbt.putString("sort", sort.name());

        if (!customName.isEmpty()) nbt.putString("custom_name", customName);
        if (!scoreboardTagsCsv.isEmpty()) nbt.putString("scoreboard_tags_csv", scoreboardTagsCsv);
        if (minTagsRequired > 0) nbt.putInt("min_tags_required", minTagsRequired);
        if (!nbtFilterSnbt.isEmpty()) nbt.putString("nbt_filter_snbt", nbtFilterSnbt);


        nbt.putBoolean("exclude_from_claim_all", true);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        dialogueId = nbt.getString("dialogue_id");
        interlocutorType = nbt.getString("interlocutor_type");
        try { sort = SortMode.valueOf(nbt.getString("sort")); } catch (Throwable ignored) { sort = SortMode.NEAREST; }


        customName = nbt.getString("custom_name");
        scoreboardTagsCsv = nbt.getString("scoreboard_tags_csv");
        minTagsRequired = nbt.contains("min_tags_required") ? nbt.getInt("min_tags_required") : 0;
        nbtFilterSnbt = nbt.getString("nbt_filter_snbt");
        parseNbtFilter();
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(dialogueId);
        buffer.writeUtf(interlocutorType);
        buffer.writeEnum(sort);


        buffer.writeUtf(customName);
        buffer.writeUtf(scoreboardTagsCsv);
        buffer.writeVarInt(minTagsRequired);
        buffer.writeUtf(nbtFilterSnbt);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        dialogueId = buffer.readUtf();
        interlocutorType = buffer.readUtf();
        sort = buffer.readEnum(SortMode.class);

        customName = buffer.readUtf();
        scoreboardTagsCsv = buffer.readUtf();
        minTagsRequired = buffer.readVarInt();
        nbtFilterSnbt = buffer.readUtf();
        parseNbtFilter();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.addString("dialogue_id", dialogueId, v -> dialogueId = v.trim(), "")
                .setNameKey("morequesttypes.reward.dialogue.dialogue_id");

        var SORTS = NameMap.of(SortMode.NEAREST, SortMode.values()).create();
        config.addEnum("sort", sort, v -> sort = v, SORTS)
                .setNameKey("morequesttypes.reward.dialogue.sort");

        var ids = BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .map(Objects::toString)
                .sorted()
                .toList();
        var TYPES = NameMap.of("", ids.toArray(String[]::new))
                .name(s -> (s == null || s.isEmpty())
                        ? Component.literal("@e")
                        : Component.literal(s))
                .create();
        config.addEnum("interlocutor_type", interlocutorType, v -> interlocutorType = v.trim(), TYPES)
                .setNameKey("morequesttypes.reward.dialogue.interlocutor_type");

        config.addString("custom_name", customName, v -> customName = v.trim(), "")
                .setNameKey("morequesttypes.reward.dialogue_custom_name");;
        config.addString("scoreboard_tags_csv", scoreboardTagsCsv, v -> scoreboardTagsCsv = v.trim(), "")
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
