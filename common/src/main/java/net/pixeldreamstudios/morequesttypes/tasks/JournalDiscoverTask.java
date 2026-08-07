package net.pixeldreamstudios.morequesttypes.tasks;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.EnumConfig;
import dev.ftb.mods.ftblibrary.config.ListConfig;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.compat.MobJournalCompat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class JournalDiscoverTask extends Task {
    public enum Mode {ANY, ALL}

    public enum Scope {ACTOR, ANY_TEAM_MEMBER}

    private final List<String> mobs = new ArrayList<>();
    private TagKey<EntityType<?>> entityTypeTag = null;
    private Mode mode = Mode.ALL;
    private Scope scope = Scope.ACTOR;

    private static final ResourceLocation DEFAULT_MOB = ResourceLocation.withDefaultNamespace("zombie");

    @Environment(EnvType.CLIENT)
    private static NameMap<String> ENTITY_TAG_MAP;

    @Environment(EnvType.CLIENT)
    private static NameMap<ResourceLocation> ENTITY_NAME_MAP;

    public JournalDiscoverTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return MoreTasksTypes.JOURNAL_DISCOVER;
    }

    @Override
    public int autoSubmitOnPlayerTick() {
        return 20;
    }

    @Override
    public long getMaxProgress() {
        if (mode == Mode.ANY) return 1L;
        int size = targetMobs().size();
        return size == 0 ? 1L : size;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (teamData.isCompleted(this)) return;
        if (!checkTaskSequence(teamData)) return;

        Set<ResourceLocation> targets = targetMobs();
        if (targets.isEmpty()) return;

        long best = 0L;
        if (scope == Scope.ANY_TEAM_MEMBER) {
            for (ServerPlayer member : teamData.getOnlineMembers()) {
                best = Math.max(best, countDiscovered(member, targets));
            }
        } else {
            best = countDiscovered(player, targets);
        }

        long progress = mode == Mode.ANY ? (best > 0 ? 1L : 0L) : best;
        if (progress > teamData.getProgress(this)) {
            teamData.setProgress(this, progress);
        }
    }

    private long countDiscovered(ServerPlayer player, Set<ResourceLocation> targets) {
        long count = 0L;
        for (ResourceLocation id : targets) {
            if (MobJournalCompat.hasDiscovered(player, id)) count++;
        }
        return count;
    }

    private Set<ResourceLocation> targetMobs() {
        Set<ResourceLocation> result = new LinkedHashSet<>();
        for (String s : mobs) {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id != null && MobJournalCompat.isDiscoverable(id)) result.add(id);
        }
        if (entityTypeTag != null) {
            BuiltInRegistries.ENTITY_TYPE.getTag(entityTypeTag).ifPresent(set -> set.forEach(holder ->
                    holder.unwrapKey().ifPresent(key -> {
                        if (MobJournalCompat.isDiscoverable(key.location())) result.add(key.location());
                    })));
        }
        return result;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public MutableComponent getAltTitle() {
        Set<ResourceLocation> targets = targetMobs();
        if (targets.size() == 1) {
            ResourceLocation only = targets.iterator().next();
            return Component.translatable("morequesttypes.task.journal_discover.title.single",
                    Component.translatable("entity." + only.toLanguageKey()));
        }
        return Component.translatable("morequesttypes.task.journal_discover.title." + mode.name().toLowerCase(),
                targets.size());
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        List<Icon> icons = new ArrayList<>();
        for (ResourceLocation id : targetMobs()) {
            icons.add(AdvancedKillTask.iconForEntityType(id));
        }
        if (icons.isEmpty()) return Icons.BARRIER;
        return icons.size() == 1 ? icons.get(0) : IconAnimation.fromList(icons, false);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        list.add(Component.translatable("morequesttypes.task.journal_discover.scope." + scope.name().toLowerCase())
                .withStyle(ChatFormatting.GRAY));
        for (ResourceLocation id : targetMobs()) {
            list.add(Component.literal(" - ")
                    .append(Component.translatable("entity." + id.toLanguageKey()))
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        if (JournalDiscoverTask.ENTITY_TAG_MAP == null) {
            var tags = new ArrayList<>(List.of(""));
            tags.addAll(BuiltInRegistries.ENTITY_TYPE.getTags()
                    .map(p -> p.getFirst().location().toString())
                    .sorted()
                    .toList());
            JournalDiscoverTask.ENTITY_TAG_MAP = NameMap.of("", tags).create();
        }

        var MODES = NameMap.of(Mode.ALL, Mode.values())
                .name(m -> Component.translatable("morequesttypes.task.journal_discover.mode." + m.name().toLowerCase()))
                .create();
        config.addEnum("mode", mode, v -> mode = v, MODES)
                .setNameKey("morequesttypes.task.journal_discover.mode");

        var SCOPES = NameMap.of(Scope.ACTOR, Scope.values())
                .name(s -> Component.translatable("morequesttypes.task.journal_discover.scope." + s.name().toLowerCase()))
                .create();
        config.addEnum("scope", scope, v -> scope = v, SCOPES)
                .setNameKey("morequesttypes.task.journal_discover.scope");

        if (JournalDiscoverTask.ENTITY_NAME_MAP == null) {
            JournalDiscoverTask.ENTITY_NAME_MAP = NameMap.of(
                            JournalDiscoverTask.DEFAULT_MOB,
                            JournalDiscoverTask.clientMobChoices())
                    .nameKey(id -> "entity." + id.toLanguageKey())
                    .icon(AdvancedKillTask::iconForEntityType)
                    .create();
        }

        List<ResourceLocation> mobIds = new ArrayList<>();
        for (String s : mobs) {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id != null) mobIds.add(id);
        }

        config.add("mobs", new ListConfig<>(
                        new EnumConfig<>(JournalDiscoverTask.ENTITY_NAME_MAP)),
                mobIds, v -> {
                    mobs.clear();
                    if (v != null) {
                        for (ResourceLocation id : v) {
                            if (id != null) mobs.add(id.toString());
                        }
                    }
                }, new ArrayList<>())
                .setNameKey("morequesttypes.task.journal_discover.mobs");

        config.addEnum("entity_type_tag", getTypeTagStr(),
                        v -> entityTypeTag = JournalDiscoverTask.parseTypeTag(v), JournalDiscoverTask.ENTITY_TAG_MAP)
                .setNameKey("morequesttypes.task.journal_discover.entity_type_tag");
    }

    @Environment(EnvType.CLIENT)
    private static List<ResourceLocation> clientMobChoices() {
        var ids = new ArrayList<ResourceLocation>();
        BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            try {
                if (type.create(FTBQuestsClient.getClientLevel()) instanceof LivingEntity) {
                    ResourceLocation id = type.arch$registryName();
                    if (id != null && MobJournalCompat.isDiscoverable(id)) ids.add(id);
                }
            } catch (Exception ignored) {
            }
        });
        ids.sort((a, b) -> Component.translatable("entity." + a.toLanguageKey()).getString()
                .compareTo(Component.translatable("entity." + b.toLanguageKey()).getString()));
        return ids;
    }

    @Override
    public void clearCachedData() {
        super.clearCachedData();
        JournalDiscoverTask.ENTITY_TAG_MAP = null;
        JournalDiscoverTask.ENTITY_NAME_MAP = null;
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("mode", mode.name());
        nbt.putString("scope", scope.name());
        ListTag list = new ListTag();
        for (String s : mobs) list.add(StringTag.valueOf(s));
        nbt.put("mobs", list);
        if (entityTypeTag != null) nbt.putString("entity_type_tag", entityTypeTag.location().toString());
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        try {
            mode = Mode.valueOf(nbt.getString("mode"));
        } catch (Throwable ignored) {
            mode = Mode.ALL;
        }
        try {
            scope = Scope.valueOf(nbt.getString("scope"));
        } catch (Throwable ignored) {
            scope = Scope.ACTOR;
        }
        mobs.clear();
        ListTag list = nbt.getList("mobs", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) mobs.add(list.getString(i));
        entityTypeTag = JournalDiscoverTask.parseTypeTag(nbt.getString("entity_type_tag"));
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(mode);
        buf.writeEnum(scope);
        buf.writeVarInt(mobs.size());
        for (String s : mobs) buf.writeUtf(s);
        buf.writeUtf(entityTypeTag == null ? "" : entityTypeTag.location().toString());
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        mode = buf.readEnum(Mode.class);
        scope = buf.readEnum(Scope.class);
        mobs.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) mobs.add(buf.readUtf());
        entityTypeTag = JournalDiscoverTask.parseTypeTag(buf.readUtf());
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
