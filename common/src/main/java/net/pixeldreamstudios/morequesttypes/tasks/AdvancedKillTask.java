package net.pixeldreamstudios.morequesttypes.tasks;

import dev.architectury.registry.registries.RegistrarManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancedKillTask extends KillTask {
    private static final ResourceLocation ZOMBIE = ResourceLocation.withDefaultNamespace("zombie");

    private static NameMap<ResourceLocation> ENTITY_NAME_MAP;
    private static NameMap<String> ENTITY_TAG_MAP;
    private static final Map<ResourceLocation, Icon> ENTITY_ICONS = new HashMap<>();

    private ResourceLocation entityTypeId = ZOMBIE;
    private TagKey<EntityType<?>> entityTypeTag = null;
    private long value = 1;
    private String customName = "";
    private String scoreboardTagsCsv = "";
    private int minTagsRequired = 0;
    private String nbtFilterSnbt = "";
    private transient Tag nbtFilterParsed = null;

    public AdvancedKillTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.KILL_ADVANCED;
    }

    @Override
    public long getMaxProgress() {
        return value;
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);

        nbt.putString("entity", entityTypeId.toString());
        if (entityTypeTag != null) nbt.putString("entityTypeTag", entityTypeTag.location().toString());
        nbt.putLong("value", value);
        if (!customName.isEmpty()) nbt.putString("custom_name", customName);

        if (!scoreboardTagsCsv.isEmpty()) nbt.putString("scoreboard_tags_csv", scoreboardTagsCsv);
        nbt.putInt("min_tags_required", minTagsRequired);
        if (!nbtFilterSnbt.isEmpty()) nbt.putString("nbt_filter_snbt", nbtFilterSnbt);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);

        entityTypeId = ResourceLocation.tryParse(nbt.getString("entity"));
        entityTypeTag = parseTypeTag(nbt.getString("entityTypeTag"));
        value = nbt.getLong("value");
        customName = nbt.getString("custom_name");

        scoreboardTagsCsv = nbt.getString("scoreboard_tags_csv");
        minTagsRequired = nbt.contains("min_tags_required") ? nbt.getInt("min_tags_required") : 0;
        nbtFilterSnbt = nbt.getString("nbt_filter_snbt");
        parseNbtFilter();
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(entityTypeId.toString());
        buf.writeUtf(entityTypeTag == null ? "" : entityTypeTag.location().toString());
        buf.writeVarLong(value);
        buf.writeUtf(customName);

        buf.writeUtf(scoreboardTagsCsv);
        buf.writeVarInt(minTagsRequired);
        buf.writeUtf(nbtFilterSnbt);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        entityTypeId = ResourceLocation.tryParse(buf.readUtf());
        entityTypeTag = parseTypeTag(buf.readUtf());
        value = buf.readVarLong();
        customName = buf.readUtf();

        scoreboardTagsCsv = buf.readUtf();
        minTagsRequired = buf.readVarInt();
        nbtFilterSnbt = buf.readUtf();
        parseNbtFilter();
    }

    private void parseNbtFilter() {
        if (nbtFilterSnbt == null || nbtFilterSnbt.isBlank()) {
            nbtFilterParsed = null;
            return;
        }
        try {

            Tag parsed = TagParser.parseTag(nbtFilterSnbt);
            nbtFilterParsed = (parsed instanceof CompoundTag) ? parsed : null;
        } catch (Exception ignored) {
            nbtFilterParsed = null;
        }
    }

    private static @Nullable TagKey<EntityType<?>> parseTypeTag(String tag) {
        if (tag == null || tag.isEmpty()) return null;
        if (tag.startsWith("#")) tag = tag.substring(1);
        var rl = ResourceLocation.tryParse(tag);
        return (rl != null && !rl.getPath().isEmpty()) ? TagKey.create(Registries.ENTITY_TYPE, rl) : null;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        if (ENTITY_NAME_MAP == null) {
            var ids = new ArrayList<ResourceLocation>();
            BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
                if (type.create(FTBQuestsClient.getClientLevel()) instanceof LivingEntity) {
                    ids.add(type.arch$registryName());
                }
            });
            ids.sort((a, b) -> {
                var c1 = Component.translatable("entity." + a.toLanguageKey());
                var c2 = Component.translatable("entity." + b.toLanguageKey());
                return c1.getString().compareTo(c2.getString());
            });
            ENTITY_NAME_MAP = NameMap.of(ZOMBIE, ids)
                    .nameKey(id -> "entity." + id.toLanguageKey())
                    .icon(AdvancedKillTask::iconForEntityType)
                    .create();
        }

        if (ENTITY_TAG_MAP == null) {
            var list = new ArrayList<String>(List.of(""));
            list.addAll(BuiltInRegistries.ENTITY_TYPE.getTags()
                    .map(p -> p.getFirst().location().toString())
                    .sorted()
                    .toList());
            ENTITY_TAG_MAP = NameMap.of("minecraft:zombies", list).create();
        }


        config.addEnum("entity", entityTypeId, v -> entityTypeId = v, ENTITY_NAME_MAP, ZOMBIE);
        config.addEnum("entity_type_tag", getTypeTagStr(), v -> entityTypeTag = parseTypeTag(v), ENTITY_TAG_MAP);
        config.addLong("value", value, v -> value = v, 1L, 1L, Long.MAX_VALUE);
        config.addString("custom_name", customName, v -> customName = v, "");


        config.addString("scoreboard_tags_csv", scoreboardTagsCsv, v -> scoreboardTagsCsv = v.trim(), "")
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.tags_csv");
        config.addInt("min_tags_required", minTagsRequired, v -> minTagsRequired = Math.max(0, v), 0, 0, 64)
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.min_tags");
        config.addString("nbt_filter_snbt", nbtFilterSnbt, v -> { nbtFilterSnbt = v; parseNbtFilter(); }, "")
                .setNameKey("ftbquests.task.morequesttypes.adv_kill.nbt");
    }

    private String getTypeTagStr() {
        return entityTypeTag == null ? "" : entityTypeTag.location().toString();
    }

    private static Icon iconForEntityType(ResourceLocation typeId) {
        return ENTITY_ICONS.computeIfAbsent(typeId, k -> {
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(typeId);
            if (entityType.equals(EntityType.PLAYER)) return Icons.PLAYER;

            Item item = SpawnEggItem.byId(entityType);
            if (item == null) {
                Entity e = entityType.create(FTBQuestsClient.getClientLevel());
                if (e != null) {
                    ItemStack stack = e.getPickResult();
                    if (stack != null) item = stack.getItem();
                }
            }
            return ItemIcon.getItemIcon(item != null ? item : Items.SPAWNER);
        });
    }

    @Override
    public void clearCachedData() {
        super.clearCachedData();
        ENTITY_NAME_MAP = null;
        ENTITY_TAG_MAP = null;
        ENTITY_ICONS.clear();
    }


    @Override
    public void kill(TeamData teamData, LivingEntity e) {
        if (!teamData.isCompleted(this) && match(e)) {
            teamData.addProgress(this, 1L);
        }
    }

    private boolean match(LivingEntity e) {
        boolean baseOk = (entityTypeTag == null)
                ? entityTypeId.equals(RegistrarManager.getId(e.getType(), Registries.ENTITY_TYPE)) && nameMatchOK(e)
                : e.getType().is(entityTypeTag) && nameMatchOK(e);
        if (!baseOk) return false;

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
            if (!(nbtFilterParsed instanceof CompoundTag filter) || !nbtSubsetMatches(actual, filter)) {
                return false;
            }
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

    private boolean nameMatchOK(LivingEntity e) {
        if (!customName.isEmpty()) {
            if (e instanceof Player p) {
                if (!p.getGameProfile().getName().equals(customName)) return false;
            } else {
                if (!e.getName().getString().equals(customName)) return false;
            }
        }
        return true;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        MutableComponent name = (entityTypeTag == null)
                ? Component.translatable("entity." + entityTypeId.toLanguageKey())
                : Component.literal("#" + getTypeTagStr());
        if (!customName.isEmpty()) {
            return Component.translatable("ftbquests.task.ftbquests.kill.title_named", formatMaxProgress(), name, Component.literal(customName));
        }
        return Component.translatable("ftbquests.task.ftbquests.kill.title", formatMaxProgress(), name);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        if (entityTypeTag == null) return iconForEntityType(entityTypeId);
        List<Icon> icons = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.getTag(entityTypeTag).ifPresent(set -> set.forEach(holder ->
                holder.unwrapKey().map(k -> icons.add(iconForEntityType(k.location())))));
        return icons.isEmpty() ? Icons.BARRIER : IconAnimation.fromList(icons, false);
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

            List<Tag> remaining = new ArrayList<>();
            for (int i = 0; i < aList.size(); i++) remaining.add(aList.get(i));
            outer:
            for (int i = 0; i < fList.size(); i++) {
                Tag fEl = fList.get(i);
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
}
