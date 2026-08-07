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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.compat.MobJournalCompat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class JournalDiscoverCountTask extends Task {
    public enum Pool {ALL, ENTITY_TAG, NAMESPACE}

    public enum Scope {ACTOR, ANY_TEAM_MEMBER}

    private static final String DEFAULT_NAMESPACE = "minecraft";

    private Pool pool = Pool.ALL;
    private TagKey<EntityType<?>> entityTypeTag = null;
    private String namespace = JournalDiscoverCountTask.DEFAULT_NAMESPACE;
    private long value = 10L;
    private Scope scope = Scope.ACTOR;

    @Environment(EnvType.CLIENT)
    private static NameMap<String> ENTITY_TAG_MAP;

    @Environment(EnvType.CLIENT)
    private static NameMap<String> NAMESPACE_MAP;

    public JournalDiscoverCountTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.JOURNAL_DISCOVER_COUNT;
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 20;
    }

    @Override
    public long getMaxProgress() {
        return Math.max(1L, value);
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        long best;
        if (scope == Scope.ANY_TEAM_MEMBER) {
            best = 0L;
            for (ServerPlayer member : teamData.getOnlineMembers()) {
                best = Math.max(best, countDiscovered(member));
            }
        } else {
            best = countDiscovered(player);
        }

        if (best > teamData.getProgress(this)) {
            teamData.setProgress(this, Math.min(best, getMaxProgress()));
        }
    }

    private long countDiscovered(ServerPlayer player) {
        long count = 0L;
        for (ResourceLocation id : MobJournalCompat.getDiscoveredMobs(player)) {
            if (matchesPool(id)) count++;
        }
        return count;
    }

    private boolean matchesPool(ResourceLocation id) {
        if (!MobJournalCompat.isDiscoverable(id)) return false;
        return switch (pool) {
            case ALL -> true;
            case NAMESPACE -> id.getNamespace().equals(namespace);
            case ENTITY_TAG -> {
                if (entityTypeTag == null) yield false;
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
                yield type != null && type.is(entityTypeTag);
            }
        };
    }

    private Set<ResourceLocation> availableMobs() {
        Set<ResourceLocation> result = new LinkedHashSet<>();
        BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            ResourceLocation id = type.arch$registryName();
            if (id != null && matchesPool(id)) result.add(id);
        });
        return result;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        return switch (pool) {
            case ALL -> Component.translatable("morequesttypes.task.journal_discover_count.title.all", value);
            case NAMESPACE -> Component.translatable("morequesttypes.task.journal_discover_count.title.namespace",
                    value, namespace);
            case ENTITY_TAG -> Component.translatable("morequesttypes.task.journal_discover_count.title.entity_tag",
                    value, entityTypeTag == null ? "-" : "#" + entityTypeTag.location());
        };
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return Icon.getIcon("journal:item/book-item");
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        list.add(Component.translatable("morequesttypes.task.journal_discover_count.scope." + scope.name().toLowerCase())
                .withStyle(ChatFormatting.GRAY));

        int available = availableMobs().size();
        MutableComponent poolText = Component.translatable(
                "morequesttypes.task.journal_discover_count.available", value, available);
        list.add(poolText.withStyle(available < value ? ChatFormatting.RED : ChatFormatting.GRAY));
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        if (JournalDiscoverCountTask.ENTITY_TAG_MAP == null) {
            var tags = new ArrayList<>(List.of(""));
            tags.addAll(BuiltInRegistries.ENTITY_TYPE.getTags()
                    .map(p -> p.getFirst().location().toString())
                    .sorted()
                    .toList());
            JournalDiscoverCountTask.ENTITY_TAG_MAP = NameMap.of("", tags).create();
        }

        if (JournalDiscoverCountTask.NAMESPACE_MAP == null) {
            var namespaces = new ArrayList<>(BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                    .map(ResourceLocation::getNamespace)
                    .distinct()
                    .sorted()
                    .toList());
            JournalDiscoverCountTask.NAMESPACE_MAP =
                    NameMap.of(JournalDiscoverCountTask.DEFAULT_NAMESPACE, namespaces).create();
        }

        var POOLS = NameMap.of(Pool.ALL, Pool.values())
                .name(p -> Component.translatable(
                        "morequesttypes.task.journal_discover_count.pool." + p.name().toLowerCase()))
                .create();
        config.addEnum("pool", pool, v -> pool = v, POOLS)
                .setNameKey("morequesttypes.task.journal_discover_count.pool");

        config.addEnum("entity_type_tag", getTypeTagStr(),
                        v -> entityTypeTag = JournalDiscoverCountTask.parseTypeTag(v),
                        JournalDiscoverCountTask.ENTITY_TAG_MAP)
                .setNameKey("morequesttypes.task.journal_discover_count.entity_type_tag");

        config.addEnum("namespace", namespace, v -> namespace = v,
                        JournalDiscoverCountTask.NAMESPACE_MAP, JournalDiscoverCountTask.DEFAULT_NAMESPACE)
                .setNameKey("morequesttypes.task.journal_discover_count.namespace");

        config.addLong("value", value, v -> value = Math.max(1L, v), 10L, 1L, Long.MAX_VALUE)
                .setNameKey("morequesttypes.task.journal_discover_count.value");

        var SCOPES = NameMap.of(Scope.ACTOR, Scope.values())
                .name(s -> Component.translatable(
                        "morequesttypes.task.journal_discover_count.scope." + s.name().toLowerCase()))
                .create();
        config.addEnum("scope", scope, v -> scope = v, SCOPES)
                .setNameKey("morequesttypes.task.journal_discover_count.scope");
    }

    @Override
    public void clearCachedData() {
        super.clearCachedData();
        JournalDiscoverCountTask.ENTITY_TAG_MAP = null;
        JournalDiscoverCountTask.NAMESPACE_MAP = null;
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("pool", pool.name());
        nbt.putString("scope", scope.name());
        nbt.putString("namespace", namespace);
        nbt.putLong("value", value);
        if (entityTypeTag != null) nbt.putString("entity_type_tag", entityTypeTag.location().toString());
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        try {
            pool = Pool.valueOf(nbt.getString("pool"));
        } catch (Throwable ignored) {
            pool = Pool.ALL;
        }
        try {
            scope = Scope.valueOf(nbt.getString("scope"));
        } catch (Throwable ignored) {
            scope = Scope.ACTOR;
        }
        namespace = nbt.getString("namespace");
        if (namespace.isEmpty()) namespace = JournalDiscoverCountTask.DEFAULT_NAMESPACE;
        value = Math.max(1L, nbt.getLong("value"));
        entityTypeTag = JournalDiscoverCountTask.parseTypeTag(nbt.getString("entity_type_tag"));
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(pool);
        buf.writeEnum(scope);
        buf.writeUtf(namespace);
        buf.writeVarLong(value);
        buf.writeUtf(entityTypeTag == null ? "" : entityTypeTag.location().toString());
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        pool = buf.readEnum(Pool.class);
        scope = buf.readEnum(Scope.class);
        namespace = buf.readUtf();
        value = Math.max(1L, buf.readVarLong());
        entityTypeTag = JournalDiscoverCountTask.parseTypeTag(buf.readUtf());
    }

    private String getTypeTagStr() {
        return entityTypeTag == null ? "" : entityTypeTag.location().toString();
    }

    private static TagKey<EntityType<?>> parseTypeTag(String tag) {
        if (tag == null || tag.isEmpty()) return null;
        if (tag.startsWith("#")) tag = tag.substring(1);
        ResourceLocation rl = ResourceLocation.tryParse(tag);
        return (rl != null && !rl.getPath().isEmpty()) ? TagKey.create(Registries.ENTITY_TYPE, rl) : null;
    }
}
