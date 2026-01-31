package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ImageResourceConfig;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;
import net.pixeldreamstudios.morequesttypes.api.IQuestSummonedEntity;
import net.pixeldreamstudios.morequesttypes.api.IRewardDynamicDifficultyExtension;
import net.pixeldreamstudios.morequesttypes.compat.DynamicDifficultyCompat;
import net.pixeldreamstudios.morequesttypes.mixin.accessor.MobAccessor;
import net.pixeldreamstudios.morequesttypes.network.QuestEntityDataSyncPacket;
import net.pixeldreamstudios.morequesttypes.rewards.summon.SummonedEntityTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SummonReward extends Reward {
    public enum AggroType {
        NONE("none"),
        CLOSEST("closest"),
        FARTHEST("farthest"),
        CLAIMER("claimer");

        private final String id;
        AggroType(String id) { this.id = id; }
        public String getId() { return id; }
    }

    private String entityId = "minecraft:zombie";
    private double randomSpawnArea = 0.0;
    private int amount = 1;
    private int durationTicks = -1;
    private int spawnLimit = -1;
    private double despawnDistance = 64.0;
    private boolean despawnIfOffline = true;
    private boolean despawnIfDead = true;
    private boolean persistent = false;
    private boolean setOwner = false;
    private boolean follow = false;
    private boolean smartSpawn = true;
    private AggroType aggro = AggroType.NONE;

    private double spawnX = 0;
    private double spawnY = 0;
    private double spawnZ = 0;
    private boolean useAbsoluteCoords = false;

    private ItemStack helmet = ItemStack.EMPTY;
    private ItemStack chestplate = ItemStack.EMPTY;
    private ItemStack leggings = ItemStack.EMPTY;
    private ItemStack boots = ItemStack.EMPTY;
    private ItemStack mainhand = ItemStack.EMPTY;
    private ItemStack offhand = ItemStack.EMPTY;

    private String textureAboveMob = "";
    private float textureScale = 1.0f;
    private double textureOffsetX = 0.0;
    private double textureOffsetY = 0.0;
    private double textureOffsetZ = 0.0;
    private String scoreboardTags = "";
    private String customNbtSnbt = "";

    public SummonReward(long id, dev.ftb.mods.ftbquests.quest.Quest q) {
        super(id, q);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.SUMMON;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        ResourceLocation rl = ResourceLocation.tryParse(entityId);
        if (rl == null) return;

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(rl);
        if (entityType == null) return;

        ServerLevel level = player.serverLevel();

        if (spawnLimit > 0) {
            int existing = SummonedEntityTracker.getCountForPlayer(player.getUUID(), this.id);
            if (existing >= spawnLimit) return;
        }

        Vec3 basePos = useAbsoluteCoords
                ? new Vec3(spawnX, spawnY, spawnZ)
                : player.position().add(spawnX, spawnY, spawnZ);

        List<UUID> spawnedEntities = new ArrayList<>();
        long currentTime = level.getGameTime();

        for (int i = 0; i < amount; i++) {
            Vec3 spawnPos = basePos;

            if (randomSpawnArea > 0) {
                double offsetX = (level.random.nextDouble() - 0.5) * 2 * randomSpawnArea;
                double offsetZ = (level.random.nextDouble() - 0.5) * 2 * randomSpawnArea;
                spawnPos = spawnPos.add(offsetX, 0, offsetZ);
            }

            if (smartSpawn) {
                spawnPos = findValidSpawnPos(level, spawnPos);
            }

            Entity entity = entityType.create(level);
            if (entity == null) continue;

            entity.moveTo(spawnPos.x, spawnPos.y, spawnPos.z,
                    level.random.nextFloat() * 360.0F, 0.0F);

            if (entity instanceof IQuestSummonedEntity questEntity) {
                questEntity.setQuestSummoned(true);
                questEntity.setQuestOwnerUuid(player.getUUID());
                questEntity.setQuestRewardId(this.id);
                questEntity.setQuestTexture(textureAboveMob);
                questEntity.setQuestTextureScale(textureScale);
                questEntity.setQuestDurationTicks(durationTicks);
                questEntity.setQuestDespawnDistance(despawnDistance);
                questEntity.setQuestDespawnIfOffline(despawnIfOffline);
                questEntity.setQuestDespawnIfDead(despawnIfDead);
                questEntity.setQuestFollow(follow);
                questEntity.setQuestSpawnTime(currentTime);
                questEntity.setQuestTextureOffsetX(textureOffsetX);
                questEntity.setQuestTextureOffsetY(textureOffsetY);
                questEntity.setQuestTextureOffsetZ(textureOffsetZ);
            }

            if (!  customNbtSnbt.isEmpty()) {
                try {
                    CompoundTag parsedNbt = TagParser.parseTag(customNbtSnbt);
                    entity.load(parsedNbt);
                } catch (Exception e) {

                }
            }

            if (entity instanceof LivingEntity living) {
                if (!  helmet.isEmpty()) living.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
                if (! chestplate.isEmpty()) living.setItemSlot(EquipmentSlot.CHEST, chestplate.copy());
                if (!leggings.isEmpty()) living.setItemSlot(EquipmentSlot.LEGS, leggings.copy());
                if (! boots.isEmpty()) living.setItemSlot(EquipmentSlot.FEET, boots.copy());
                if (!mainhand.isEmpty()) living.setItemSlot(EquipmentSlot.MAINHAND, mainhand.copy());
                if (!offhand.isEmpty()) living.setItemSlot(EquipmentSlot.OFFHAND, offhand.copy());
            }

            if (DynamicDifficultyCompat.isLoaded() && entity instanceof LivingEntity living) {
                IRewardDynamicDifficultyExtension ext = (IRewardDynamicDifficultyExtension) (Object) this;
                if (ext.shouldSetMobLevel() && DynamicDifficultyCompat.canHaveLevel(living)) {
                    try {
                        DynamicDifficultyCompat.setAndUpdateLevel(living, ext.getMobLevel());
                    } catch (Exception e) {

                    }
                }
            }

            if (entity instanceof Mob mob) {
                if (persistent) {
                    mob.setPersistenceRequired();
                }

                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()),
                        MobSpawnType.COMMAND, null);

                if (setOwner && entity instanceof net.minecraft.world.entity.TamableAnimal tamable) {
                    tamable.tame(player);
                }

                if (follow) {
                    ((MobAccessor) mob).getGoalSelector().addGoal(2,
                            new net.pixeldreamstudios.morequesttypes.rewards.summon.FollowPlayerGoal(
                                    mob, player.getUUID(), 1.0, 2.0f, 10.0f
                            )
                    );
                }

                applyAggro(mob, player, level, spawnPos);
            }

            if (!  scoreboardTags.isEmpty()) {
                for (String tag : scoreboardTags.split(",")) {
                    entity.addTag(tag.trim());
                }
            }

            level.addFreshEntity(entity);
            spawnedEntities.add(entity.getUUID());

            if (entity instanceof IQuestSummonedEntity questEntity) {
                QuestEntityDataSyncPacket syncPacket = new QuestEntityDataSyncPacket(
                        entity.getId(),
                        questEntity.isQuestSummoned(),
                        questEntity.getQuestOwnerUuid(),
                        questEntity.getQuestTexture(),
                        questEntity.getQuestTextureScale(),
                        questEntity.getQuestRewardId(),
                        questEntity.getQuestFollow(),
                        false,
                        questEntity.getQuestTextureOffsetX(),
                        questEntity.getQuestTextureOffsetY(),
                        questEntity.getQuestTextureOffsetZ()
                );

                level.getServer().getPlayerList().getPlayers().forEach(serverPlayer -> {
                    if (serverPlayer.distanceToSqr(entity) < 128 * 128) {
                        net.pixeldreamstudios.morequesttypes.network.NetworkHelper.sendToPlayer(serverPlayer, syncPacket);
                    }
                });
            }

            SummonedEntityTracker.track(player.getUUID(), this.id, entity.getUUID());
        }

        if (!spawnedEntities.isEmpty()) {
            scheduleDespawnTracking(level, spawnedEntities, player.getUUID(), level.getServer());
        }
    }

    private void applyAggro(Mob mob, ServerPlayer player, ServerLevel level, Vec3 spawnPos) {
        GoalSelector goalSelector = ((MobAccessor) mob).getGoalSelector();

        switch (aggro) {
            case CLAIMER:
                if (mob.getTarget() == null) {
                    mob.setTarget(player);
                }
                goalSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                        mob, ServerPlayer.class, 10, true, false,
                        e -> e.getUUID().equals(player.getUUID())
                ));
                break;

            case CLOSEST:
                Player closestPlayer = level.getNearestPlayer(spawnPos.x, spawnPos.y, spawnPos.z, 64, false);
                if (closestPlayer instanceof ServerPlayer closest && mob.getTarget() == null) {
                    mob.setTarget(closest);
                }
                break;

            case FARTHEST:
                ServerPlayer farthest = null;
                double maxDist = 0;
                for (ServerPlayer sp : level.players()) {
                    double dist = sp.distanceToSqr(spawnPos);
                    if (dist > maxDist && dist <= 64 * 64) {
                        maxDist = dist;
                        farthest = sp;
                    }
                }
                if (farthest != null && mob.getTarget() == null) {
                    mob.setTarget(farthest);
                }
                break;

            case NONE:
            default:
                break;
        }
    }

    private Vec3 findValidSpawnPos(ServerLevel level, Vec3 pos) {
        for (int y = 0; y < 10; y++) {
            Vec3 checkPos = new Vec3(pos.x, pos.y - y, pos.z);
            var blockPos = net.minecraft.core.BlockPos.containing(checkPos);
            if (level.getBlockState(blockPos.below()).isSolid() &&
                    !  level.getBlockState(blockPos).isSolid()) {
                return checkPos;
            }
        }
        for (int y = 1; y < 10; y++) {
            Vec3 checkPos = new Vec3(pos.x, pos.y + y, pos.z);
            var blockPos = net.minecraft.core.BlockPos.containing(checkPos);
            if (level.getBlockState(blockPos.below()).isSolid() &&
                    ! level.getBlockState(blockPos).isSolid()) {
                return checkPos;
            }
        }
        return pos;
    }

    private void scheduleDespawnTracking(ServerLevel level, List<UUID> entityUUIDs,
                                         UUID playerUUID, MinecraftServer server) {
        new Thread(() -> {
            int elapsed = 0;
            while (true) {
                try {
                    Thread.sleep(1000);
                    elapsed += 20;

                    int finalElapsed = elapsed;
                    server.execute(() -> {
                        trackAndDespawn(level, entityUUIDs, playerUUID, finalElapsed, server);
                    });

                    if (entityUUIDs.isEmpty()) break;
                    if (durationTicks > 0 && elapsed >= durationTicks) break;

                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void trackAndDespawn(ServerLevel level, List<UUID> entityUUIDs,
                                 UUID playerUUID, int elapsedTicks, MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);

        boolean shouldDespawnAll = false;

        if (despawnIfOffline && player == null) shouldDespawnAll = true;
        if (despawnIfDead && player != null && player.isDeadOrDying()) shouldDespawnAll = true;
        if (durationTicks > 0 && elapsedTicks >= durationTicks) shouldDespawnAll = true;

        boolean finalShouldDespawnAll = shouldDespawnAll;
        entityUUIDs.removeIf(uuid -> {
            Entity entity = level.getEntity(uuid);
            if (entity == null || ! entity.isAlive()) {
                SummonedEntityTracker.untrack(playerUUID, this.id, uuid);
                return true;
            }

            boolean despawn = finalShouldDespawnAll;

            if (player != null && despawnDistance > 0) {
                if (entity.distanceToSqr(player) > despawnDistance * despawnDistance) {
                    despawn = true;
                }
            }

            if (despawn) {
                if (entity instanceof IQuestSummonedEntity questEntity) {
                    QuestEntityDataSyncPacket despawnPacket = new QuestEntityDataSyncPacket(
                            entity.getId(),
                            questEntity.isQuestSummoned(),
                            questEntity.getQuestOwnerUuid(),
                            questEntity.getQuestTexture(),
                            questEntity.getQuestTextureScale(),
                            questEntity.getQuestRewardId(),
                            questEntity.getQuestFollow(),
                            true,
                            questEntity.getQuestTextureOffsetX(),
                            questEntity.getQuestTextureOffsetY(),
                            questEntity.getQuestTextureOffsetZ()
                    );

                    server.getPlayerList().getPlayers().forEach(serverPlayer -> {
                        if (serverPlayer.distanceToSqr(entity) < 128 * 128) {
                            net.pixeldreamstudios.morequesttypes.network.NetworkHelper.sendToPlayer(serverPlayer, despawnPacket);
                        }
                    });
                }

                entity.discard();
                SummonedEntityTracker.untrack(playerUUID, this.id, uuid);
                return true;
            }

            return false;
        });
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var entityChoices = new ArrayList<String>();
        BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(entityChoices::add);

        if (!  entityId.isEmpty() && ! entityChoices.contains(entityId)) {
            entityChoices.add(entityId);
            entityChoices.sort(String::compareTo);
        }

        var ENTITIES = NameMap.of(entityId, entityChoices)
                .name(s -> Component.literal(s == null || s.isEmpty() ? "?" : s))
                .create();

        config.addEnum("entity", entityId, v -> entityId = v, ENTITIES)
                .setNameKey("morequesttypes.reward.summon.entity");

        ConfigGroup spawn = config.getOrCreateSubgroup("spawn");

        spawn.addDouble("random_spawn_area", randomSpawnArea, v -> randomSpawnArea = Math.max(0, v), 0.0, 0.0, 100.0)
                .setNameKey("morequesttypes.reward.summon.random_spawn_area");

        spawn.addInt("amount", amount, v -> amount = Math.max(1, v), 1, 1, 100)
                .setNameKey("morequesttypes.reward.summon.amount");

        spawn.addBool("smart_spawn", smartSpawn, v -> smartSpawn = v, true)
                .setNameKey("morequesttypes.reward.summon.smart_spawn");

        spawn.addBool("use_absolute_coords", useAbsoluteCoords, v -> useAbsoluteCoords = v, false)
                .setNameKey("morequesttypes.reward.summon.use_absolute_coords");

        spawn.addDouble("spawn_x", spawnX, v -> spawnX = v, 0.0, -30000000, 30000000)
                .setNameKey("morequesttypes.reward.summon.spawn_x");

        spawn.addDouble("spawn_y", spawnY, v -> spawnY = v, 0.0, -64, 320)
                .setNameKey("morequesttypes.reward.summon.spawn_y");

        spawn.addDouble("spawn_z", spawnZ, v -> spawnZ = v, 0.0, -30000000, 30000000)
                .setNameKey("morequesttypes.reward.summon.spawn_z");

        ConfigGroup despawn = config.getOrCreateSubgroup("despawn");

        despawn.addInt("duration_ticks", durationTicks, v -> durationTicks = v, -1, -1, 72000)
                .setNameKey("morequesttypes.reward.summon.duration_ticks");

        despawn.addInt("spawn_limit", spawnLimit, v -> spawnLimit = v, -1, -1, 1000)
                .setNameKey("morequesttypes.reward.summon.spawn_limit");

        despawn.addDouble("despawn_distance", despawnDistance, v -> despawnDistance = v, 64.0, 0.0, 512.0)
                .setNameKey("morequesttypes.reward.summon.despawn_distance");

        despawn.addBool("despawn_if_offline", despawnIfOffline, v -> despawnIfOffline = v, true)
                .setNameKey("morequesttypes.reward.summon.despawn_if_offline");

        despawn.addBool("despawn_if_dead", despawnIfDead, v -> despawnIfDead = v, true)
                .setNameKey("morequesttypes.reward.summon.despawn_if_dead");

        ConfigGroup behavior = config.getOrCreateSubgroup("behavior");

        behavior.addBool("persistent", persistent, v -> persistent = v, false)
                .setNameKey("morequesttypes.reward.summon.persistent");

        behavior.addBool("set_owner", setOwner, v -> setOwner = v, false)
                .setNameKey("morequesttypes.reward.summon.set_owner");

        behavior.addBool("follow", follow, v -> follow = v, false)
                .setNameKey("morequesttypes.reward.summon.follow");

        var AGGRO = NameMap.of(aggro, AggroType.values())
                .name(e -> Component.translatable("morequesttypes.reward.summon.aggro." + e.getId()))
                .create();

        behavior.addEnum("aggro", aggro, v -> aggro = v, AGGRO)
                .setNameKey("morequesttypes.reward.summon.aggro");

        ConfigGroup equipment = config.getOrCreateSubgroup("equipment");

        equipment.addItemStack("helmet", helmet, v -> helmet = v, ItemStack.EMPTY, true, true)
                .setNameKey("morequesttypes.reward.summon.helmet");

        equipment.addItemStack("chestplate", chestplate, v -> chestplate = v, ItemStack.EMPTY, true, true)
                .setNameKey("morequesttypes.reward.summon.chestplate");

        equipment.addItemStack("leggings", leggings, v -> leggings = v, ItemStack.EMPTY, true, true)
                .setNameKey("morequesttypes.reward.summon.leggings");

        equipment.addItemStack("boots", boots, v -> boots = v, ItemStack.EMPTY, true, true)
                .setNameKey("morequesttypes.reward.summon.boots");

        equipment.addItemStack("mainhand", mainhand, v -> mainhand = v, ItemStack.EMPTY, true, true)
                .setNameKey("morequesttypes.reward.summon.mainhand");

        equipment.addItemStack("offhand", offhand, v -> offhand = v, ItemStack.EMPTY, true, true)
                .setNameKey("morequesttypes.reward.summon.offhand");

        ConfigGroup visual = config.getOrCreateSubgroup("visual");

        ResourceLocation currentTexture = textureAboveMob.isEmpty()
                ? ImageResourceConfig.NONE
                : ResourceLocation.tryParse(textureAboveMob);

        visual.add("texture_above_mob", new ImageResourceConfig(), currentTexture, v -> {
            textureAboveMob = v != null && ! v.equals(ImageResourceConfig.NONE) ? v.toString() : "";
        }, ImageResourceConfig.NONE).setNameKey("morequesttypes.reward.summon.texture_above_mob");

        visual.addDouble("texture_scale", textureScale, v -> textureScale = (float) Math.max(0.1f, v), 1.0f, 0.1f, 10.0f)
                .setNameKey("morequesttypes.reward.summon.texture_scale");

        visual.addDouble("texture_offset_x", textureOffsetX, v -> textureOffsetX = v, 0.0, -10.0, 10.0)
                .setNameKey("morequesttypes.reward.summon.texture_offset_x");

        visual.addDouble("texture_offset_y", textureOffsetY, v -> textureOffsetY = v, 0.0, -10.0, 10.0)
                .setNameKey("morequesttypes.reward.summon.texture_offset_y");

        visual.addDouble("texture_offset_z", textureOffsetZ, v -> textureOffsetZ = v, 0.0, -10.0, 10.0)
                .setNameKey("morequesttypes.reward.summon.texture_offset_z");

        ConfigGroup advanced = config.getOrCreateSubgroup("advanced");

        advanced.addString("scoreboard_tags", scoreboardTags, v -> scoreboardTags = v, "")
                .setNameKey("morequesttypes.reward.summon.scoreboard_tags");

        advanced.addString("custom_nbt", customNbtSnbt, v -> customNbtSnbt = v, "")
                .setNameKey("morequesttypes.reward.summon.custom_nbt");
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        ResourceLocation rl = ResourceLocation.tryParse(entityId);
        if (rl == null) return super.getAltIcon();

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(rl);
        if (entityType == null) return super.getAltIcon();

        Item spawnEgg = SpawnEggItem.byId(entityType);
        if (spawnEgg != null && spawnEgg != Items.AIR) {
            return ItemIcon.getItemIcon(spawnEgg);
        }

        return ItemIcon.getItemIcon(Items.SPAWNER);
    }

    @Override
    public void writeData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("entity", entityId);
        nbt.putDouble("random_spawn_area", randomSpawnArea);
        nbt.putInt("amount", amount);
        nbt.putInt("duration_ticks", durationTicks);
        nbt.putInt("spawn_limit", spawnLimit);
        nbt.putDouble("despawn_distance", despawnDistance);
        nbt.putBoolean("despawn_if_offline", despawnIfOffline);
        nbt.putBoolean("despawn_if_dead", despawnIfDead);
        nbt.putBoolean("persistent", persistent);
        nbt.putBoolean("set_owner", setOwner);
        nbt.putBoolean("follow", follow);
        nbt.putBoolean("smart_spawn", smartSpawn);
        nbt.putString("aggro", aggro.getId());
        nbt.putDouble("spawn_x", spawnX);
        nbt.putDouble("spawn_y", spawnY);
        nbt.putDouble("spawn_z", spawnZ);
        nbt.putBoolean("use_absolute_coords", useAbsoluteCoords);
        if (!  helmet.isEmpty()) nbt.put("helmet", helmet.save(provider));
        if (!chestplate.isEmpty()) nbt.put("chestplate", chestplate.save(provider));
        if (!leggings.isEmpty()) nbt.put("leggings", leggings.save(provider));
        if (!boots.isEmpty()) nbt.put("boots", boots.save(provider));
        if (!mainhand.isEmpty()) nbt.put("mainhand", mainhand.save(provider));
        if (!offhand.isEmpty()) nbt.put("offhand", offhand.save(provider));
        nbt.putString("texture_above_mob", textureAboveMob);
        nbt.putFloat("texture_scale", textureScale);
        nbt.putDouble("texture_offset_x", textureOffsetX);
        nbt.putDouble("texture_offset_y", textureOffsetY);
        nbt.putDouble("texture_offset_z", textureOffsetZ);
        nbt.putString("scoreboard_tags", scoreboardTags);
        if (!customNbtSnbt.isEmpty()) nbt.putString("custom_nbt_snbt", customNbtSnbt);
    }

    @Override
    public void readData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        entityId = nbt.getString("entity");
        randomSpawnArea = nbt.getDouble("random_spawn_area");
        amount = nbt.getInt("amount");
        durationTicks = nbt.getInt("duration_ticks");
        spawnLimit = nbt.getInt("spawn_limit");
        despawnDistance = nbt.getDouble("despawn_distance");
        despawnIfOffline = nbt.getBoolean("despawn_if_offline");
        despawnIfDead = nbt.getBoolean("despawn_if_dead");
        persistent = nbt.getBoolean("persistent");
        setOwner = nbt.getBoolean("set_owner");
        follow = nbt.getBoolean("follow");
        smartSpawn = !  nbt.contains("smart_spawn") || nbt.getBoolean("smart_spawn");
        try {
            aggro = AggroType.valueOf(nbt.getString("aggro").toUpperCase());
        } catch (Exception e) {
            aggro = AggroType.NONE;
        }
        spawnX = nbt.getDouble("spawn_x");
        spawnY = nbt.getDouble("spawn_y");
        spawnZ = nbt.getDouble("spawn_z");
        useAbsoluteCoords = nbt.getBoolean("use_absolute_coords");
        helmet = nbt.contains("helmet") ? ItemStack.parseOptional(provider, nbt.getCompound("helmet")) : ItemStack.EMPTY;
        chestplate = nbt.contains("chestplate") ? ItemStack.parseOptional(provider, nbt.getCompound("chestplate")) : ItemStack.EMPTY;
        leggings = nbt.contains("leggings") ? ItemStack.parseOptional(provider, nbt.getCompound("leggings")) : ItemStack.EMPTY;
        boots = nbt.contains("boots") ? ItemStack.parseOptional(provider, nbt.getCompound("boots")) : ItemStack.EMPTY;
        mainhand = nbt.contains("mainhand") ? ItemStack.parseOptional(provider, nbt.getCompound("mainhand")) : ItemStack.EMPTY;
        offhand = nbt.contains("offhand") ? ItemStack.parseOptional(provider, nbt.getCompound("offhand")) : ItemStack.EMPTY;
        textureAboveMob = nbt.getString("texture_above_mob");
        textureScale = nbt.contains("texture_scale") ? nbt.getFloat("texture_scale") : 1.0f;
        textureOffsetX = nbt.getDouble("texture_offset_x");
        textureOffsetY = nbt.getDouble("texture_offset_y");
        textureOffsetZ = nbt.getDouble("texture_offset_z");
        scoreboardTags = nbt.getString("scoreboard_tags");
        customNbtSnbt = nbt.getString("custom_nbt_snbt");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(entityId);
        buffer.writeDouble(randomSpawnArea);
        buffer.writeInt(amount);
        buffer.writeInt(durationTicks);
        buffer.writeInt(spawnLimit);
        buffer.writeDouble(despawnDistance);
        buffer.writeBoolean(despawnIfOffline);
        buffer.writeBoolean(despawnIfDead);
        buffer.writeBoolean(persistent);
        buffer.writeBoolean(setOwner);
        buffer.writeBoolean(follow);
        buffer.writeBoolean(smartSpawn);
        buffer.writeEnum(aggro);
        buffer.writeDouble(spawnX);
        buffer.writeDouble(spawnY);
        buffer.writeDouble(spawnZ);
        buffer.writeBoolean(useAbsoluteCoords);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, helmet);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, chestplate);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, leggings);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, boots);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, mainhand);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, offhand);
        buffer.writeUtf(textureAboveMob);
        buffer.writeFloat(textureScale);
        buffer.writeDouble(textureOffsetX);
        buffer.writeDouble(textureOffsetY);
        buffer.writeDouble(textureOffsetZ);
        buffer.writeUtf(scoreboardTags);
        buffer.writeUtf(customNbtSnbt);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        entityId = buffer.readUtf();
        randomSpawnArea = buffer.readDouble();
        amount = buffer.readInt();
        durationTicks = buffer.readInt();
        spawnLimit = buffer.readInt();
        despawnDistance = buffer.readDouble();
        despawnIfOffline = buffer.readBoolean();
        despawnIfDead = buffer.readBoolean();
        persistent = buffer.readBoolean();
        setOwner = buffer.readBoolean();
        follow = buffer.readBoolean();
        smartSpawn = buffer.readBoolean();
        aggro = buffer.readEnum(AggroType.class);
        spawnX = buffer.readDouble();
        spawnY = buffer.readDouble();
        spawnZ = buffer.readDouble();
        useAbsoluteCoords = buffer.readBoolean();
        helmet = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        chestplate = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        leggings = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        boots = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        mainhand = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        offhand = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        textureAboveMob = buffer.readUtf();
        textureScale = buffer.readFloat();
        textureOffsetX = buffer.readDouble();
        textureOffsetY = buffer.readDouble();
        textureOffsetZ = buffer.readDouble();
        scoreboardTags = buffer.readUtf();
        customNbtSnbt = buffer.readUtf();
    }

    @Override
    public boolean getExcludeFromClaimAll() {
        return true;
    }
}