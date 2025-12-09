package net.pixeldreamstudios.morequesttypes.rewards.summon;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.pixeldreamstudios.morequesttypes.api.IQuestSummonedEntity;
import net.pixeldreamstudios.morequesttypes.mixin.accessor.MobAccessor;

import java.util.*;

public final class SummonedEntityTracker {
    private static final Map<UUID, Map<Long, Set<UUID>>> TRACKED = new HashMap<>();
    private static Thread globalThread = null;

    private SummonedEntityTracker() {}

    public static void track(UUID playerUUID, long rewardId, UUID entityUUID) {
        TRACKED.computeIfAbsent(playerUUID, k -> new HashMap<>())
                .computeIfAbsent(rewardId, k -> new HashSet<>())
                .add(entityUUID);
    }

    public static void untrack(UUID playerUUID, long rewardId, UUID entityUUID) {
        var playerMap = TRACKED.get(playerUUID);
        if (playerMap == null) return;

        var rewardSet = playerMap.get(rewardId);
        if (rewardSet == null) return;

        rewardSet.remove(entityUUID);
        if (rewardSet.isEmpty()) {
            playerMap.remove(rewardId);
        }
        if (playerMap.isEmpty()) {
            TRACKED.remove(playerUUID);
        }
    }

    public static int getCountForPlayer(UUID playerUUID, long rewardId) {
        var playerMap = TRACKED.get(playerUUID);
        if (playerMap == null) return 0;

        var rewardSet = playerMap.get(rewardId);
        return rewardSet == null ? 0 : rewardSet.size();
    }

    public static void clearPlayer(UUID playerUUID) {
        TRACKED.remove(playerUUID);
    }

    public static void restoreFromWorld(ServerLevel level) {
        MinecraftServer server = level.getServer();
        if (server == null) return;

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof IQuestSummonedEntity questEntity && questEntity.isQuestSummoned()) {
                UUID ownerUuid = questEntity.getQuestOwnerUuid();
                long rewardId = questEntity.getQuestRewardId();

                if (ownerUuid != null && rewardId != -1L) {
                    track(ownerUuid, rewardId, entity.getUUID());

                    if (entity instanceof Mob mob && questEntity.getQuestFollow()) {
                        boolean hasFollowGoal = ((MobAccessor) mob).getGoalSelector().getAvailableGoals().stream()
                                .anyMatch(goal -> goal.getGoal() instanceof FollowPlayerGoal);

                        if (! hasFollowGoal) {
                            ((MobAccessor) mob).getGoalSelector().addGoal(2,
                                    new FollowPlayerGoal(mob, ownerUuid, 1.0, 2.0f, 10.0f)
                            );
                        }
                    }
                }
            }
        }

        startGlobalTrackingThread(server);
    }

    private static void startGlobalTrackingThread(MinecraftServer server) {
        if (globalThread != null && globalThread.isAlive()) {
            return;
        }

        globalThread = new Thread(() -> {
            while (! server.isStopped()) {
                try {
                    Thread.sleep(1000);

                    server.execute(() -> {
                        for (ServerLevel level : server.getAllLevels()) {
                            processEntitiesInLevel(level);
                        }
                    });

                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        globalThread.setDaemon(true);
        globalThread.start();
    }

    private static void processEntitiesInLevel(ServerLevel level) {
        List<Entity> toRemove = new ArrayList<>();
        long currentTime = level.getGameTime();

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof IQuestSummonedEntity questEntity) || !questEntity.isQuestSummoned()) {
                continue;
            }

            UUID ownerUuid = questEntity.getQuestOwnerUuid();
            long rewardId = questEntity.getQuestRewardId();

            if (ownerUuid == null || rewardId == -1L) continue;

            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid);

            boolean shouldDespawn = false;

            if (questEntity.getQuestDespawnIfOffline() && owner == null) {
                shouldDespawn = true;
            }

            if (questEntity.getQuestDespawnIfDead() && owner != null && owner.isDeadOrDying()) {
                shouldDespawn = true;
            }

            if (owner != null && questEntity.getQuestDespawnDistance() > 0) {
                if (entity.distanceToSqr(owner) > questEntity.getQuestDespawnDistance() * questEntity.getQuestDespawnDistance()) {
                    shouldDespawn = true;
                }
            }

            if (questEntity.getQuestDurationTicks() > 0 && questEntity.getQuestSpawnTime() > 0) {
                long elapsed = currentTime - questEntity.getQuestSpawnTime();
                if (elapsed >= questEntity.getQuestDurationTicks()) {
                    shouldDespawn = true;
                }
            }

            if (questEntity.getQuestFollow() && entity instanceof Mob mob) {
                boolean hasFollowGoal = ((MobAccessor) mob).getGoalSelector().getAvailableGoals().stream()
                        .anyMatch(goal -> goal.getGoal() instanceof FollowPlayerGoal);

                if (!hasFollowGoal && owner != null) {
                    ((MobAccessor) mob).getGoalSelector().addGoal(2,
                            new FollowPlayerGoal(mob, ownerUuid, 1.0, 2.0f, 10.0f)
                    );
                }
            }

            if (shouldDespawn) {
                toRemove.add(entity);
                untrack(ownerUuid, rewardId, entity.getUUID());
            }
        }

        for (Entity entity : toRemove) {
            entity.discard();
        }
    }

    public static void clearRestoredLevels() {
        if (globalThread != null) {
            globalThread.interrupt();
            globalThread = null;
        }
    }
}