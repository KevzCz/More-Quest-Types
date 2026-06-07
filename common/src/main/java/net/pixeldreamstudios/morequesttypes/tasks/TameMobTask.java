package net.pixeldreamstudios.morequesttypes.tasks;

import dev.architectury.registry.registries.RegistrarManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.event.TameEventBuffer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class TameMobTask extends Task {
    private static final ResourceLocation WOLF = ResourceLocation.withDefaultNamespace("wolf");
    private static NameMap<ResourceLocation> ENTITY_NAME_MAP;
    private static NameMap<String> ENTITY_TAG_MAP;

    private ResourceLocation entityTypeId = TameMobTask.WOLF;
    private TagKey<EntityType<?>> entityTypeTag = null;
    private long value = 1L;
    private String nbtFilterSnbt = "";
    private transient Tag nbtFilterParsed = null;

    public TameMobTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.TAME_MOB;
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

        var online = teamData.getOnlineMembers();
        if (online == null || online.isEmpty()) return;

        UUID leaderId = online.stream()
                .map(ServerPlayer::getUUID)
                .min(Comparator.comparing(UUID::toString))
                .orElse(player.getUUID());
        if (!player.getUUID().equals(leaderId)) return;

        var events = TameEventBuffer.snapshotUnprocessed(player.getUUID(), id);
        if (events.isEmpty()) return;

        long tick = events.getLast().gameTime();
        if (player.level().getGameTime() - tick > 1) return;

        long count = 0L;
        List<TameEventBuffer.TameEvent> matched = new ArrayList<>();
        for (var event : events) {
            if (matches(event.entity())) {
                count++;
                matched.add(event);
            }
        }

        if (count > 0) {
            long cur = teamData.getProgress(this);
            long next = Math.min(getMaxProgress(), cur + count);
            if (next != cur) teamData.setProgress(this, next);
            TameEventBuffer.markProcessed(id, matched);
        }
    }

    private boolean matches(LivingEntity entity) {
        boolean typeOk = entityTypeTag == null
                ? entityTypeId.equals(RegistrarManager.getId(entity.getType(), Registries.ENTITY_TYPE))
                : entity.getType().is(entityTypeTag);
        if (!typeOk) return false;

        if (nbtFilterParsed != null) {
            var actual = new CompoundTag();
            entity.saveWithoutId(actual);
            return nbtFilterParsed instanceof CompoundTag filter && TameMobTask.nbtSubsetMatches(actual, filter);
        }

        return true;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        MutableComponent name = entityTypeTag == null
                ? Component.translatable("entity." + entityTypeId.toLanguageKey())
                : Component.literal("#" + entityTypeTag.location());
        return Component.translatable("morequesttypes.task.tame_mob.title", formatMaxProgress(), name);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        if (entityTypeTag == null) return AdvancedKillTask.iconForEntityType(entityTypeId);
        List<Icon> icons = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.getTag(entityTypeTag).ifPresent(set -> set.forEach(holder ->
                holder.unwrapKey().map(k -> icons.add(AdvancedKillTask.iconForEntityType(k.location())))));
        return icons.isEmpty() ? Icons.BARRIER : IconAnimation.fromList(icons, false);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        if (TameMobTask.ENTITY_NAME_MAP == null) {
            var ids = new ArrayList<ResourceLocation>();
            BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
                try {
                    if (type.create(FTBQuestsClient.getClientLevel()) instanceof LivingEntity) {
                        ids.add(type.arch$registryName());
                    }
                } catch (Exception ignored) {
                }
            });
            ids.sort((a, b) -> Component.translatable("entity." + a.toLanguageKey()).getString()
                    .compareTo(Component.translatable("entity." + b.toLanguageKey()).getString()));
            TameMobTask.ENTITY_NAME_MAP = NameMap.of(TameMobTask.WOLF, ids)
                    .nameKey(id -> "entity." + id.toLanguageKey())
                    .icon(AdvancedKillTask::iconForEntityType)
                    .create();
        }

        if (TameMobTask.ENTITY_TAG_MAP == null) {
            var list = new ArrayList<>(List.of(""));
            list.addAll(BuiltInRegistries.ENTITY_TYPE.getTags()
                    .map(p -> p.getFirst().location().toString())
                    .sorted()
                    .toList());
            TameMobTask.ENTITY_TAG_MAP = NameMap.of("", list).create();
        }

        config.addEnum("entity", entityTypeId, v -> entityTypeId = v, TameMobTask.ENTITY_NAME_MAP, TameMobTask.WOLF)
                .setNameKey("morequesttypes.task.tame_mob.entity");
        config.addEnum("entity_type_tag", getTypeTagStr(), v -> entityTypeTag = TameMobTask.parseTypeTag(v), TameMobTask.ENTITY_TAG_MAP)
                .setNameKey("morequesttypes.task.tame_mob.entity_type_tag");
        config.addLong("value", value, v -> value = Math.max(1L, v), 1L, 1L, Long.MAX_VALUE)
                .setNameKey("morequesttypes.task.tame_mob.value");
        config.addString("nbt_filter_snbt", nbtFilterSnbt, v -> {
            nbtFilterSnbt = v;
            parseNbtFilter();
        }, "").setNameKey("morequesttypes.task.nbt");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("entity", entityTypeId.toString());
        if (entityTypeTag != null) nbt.putString("entity_type_tag", entityTypeTag.location().toString());
        nbt.putLong("value", value);
        if (!nbtFilterSnbt.isEmpty()) nbt.putString("nbt_filter_snbt", nbtFilterSnbt);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        entityTypeId = ResourceLocation.tryParse(nbt.getString("entity"));
        if (entityTypeId == null) entityTypeId = TameMobTask.WOLF;
        entityTypeTag = TameMobTask.parseTypeTag(nbt.getString("entity_type_tag"));
        value = Math.max(1L, nbt.getLong("value"));
        nbtFilterSnbt = nbt.getString("nbt_filter_snbt");
        parseNbtFilter();
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(entityTypeId.toString());
        buf.writeUtf(entityTypeTag == null ? "" : entityTypeTag.location().toString());
        buf.writeVarLong(value);
        buf.writeUtf(nbtFilterSnbt);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        entityTypeId = ResourceLocation.tryParse(buf.readUtf());
        if (entityTypeId == null) entityTypeId = TameMobTask.WOLF;
        entityTypeTag = TameMobTask.parseTypeTag(buf.readUtf());
        value = Math.max(1L, buf.readVarLong());
        nbtFilterSnbt = buf.readUtf();
        parseNbtFilter();
    }

    @Override
    public void clearCachedData() {
        super.clearCachedData();
        TameMobTask.ENTITY_NAME_MAP = null;
        TameMobTask.ENTITY_TAG_MAP = null;
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

    private String getTypeTagStr() {
        return entityTypeTag == null ? "" : entityTypeTag.location().toString();
    }

    private static TagKey<EntityType<?>> parseTypeTag(String tag) {
        if (tag == null || tag.isEmpty()) return null;
        if (tag.startsWith("#")) tag = tag.substring(1);
        var rl = ResourceLocation.tryParse(tag);
        return (rl != null && !rl.getPath().isEmpty()) ? TagKey.create(Registries.ENTITY_TYPE, rl) : null;
    }

    private static boolean nbtSubsetMatches(Tag actual, Tag filter) {
        if (filter instanceof CompoundTag f) {
            if (!(actual instanceof CompoundTag a)) return false;
            for (String key : f.getAllKeys()) {
                Tag fVal = f.get(key);
                Tag aVal = a.get(key);
                if (aVal == null || !TameMobTask.nbtSubsetMatches(aVal, fVal)) return false;
            }
            return true;
        }
        if (filter instanceof ListTag fList) {
            if (!(actual instanceof ListTag aList)) return false;
            List<Tag> remaining = new ArrayList<>(aList);
            outer:
            for (Tag fEl : fList) {
                for (int j = 0; j < remaining.size(); j++) {
                    if (TameMobTask.nbtSubsetMatches(remaining.get(j), fEl)) {
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
