package net.pixeldreamstudios.morequesttypes.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ReceiveDamageEventBuffer {
    private ReceiveDamageEventBuffer() {}

    private static final Map<UUID, ConcurrentHashMap<Long, ConcurrentHashMap<Long, ReceiveHit>>> BUCKETS = new ConcurrentHashMap<>();
    private static final Map<Long, Set<String>> PROCESSED_BY_TASK = new ConcurrentHashMap<>();

    public record ReceiveHit(
            Entity source,
            ResourceLocation damageType,
            long gameTime,
            long amountRounded,
            int damageSeq
    ) {
        public String getUniqueKey() {
            int sourceId = source == null ? 0 : source.getId();
            return gameTime + ":" + sourceId + ":" + damageSeq;
        }
    }

    public static void push(UUID victim, Entity source, ResourceLocation damageType, long gameTime,
                            long amountRounded, int damageSeq) {
        var byTick = BUCKETS.computeIfAbsent(victim, k -> new ConcurrentHashMap<>());
        var mapForTick = byTick.computeIfAbsent(gameTime, k -> new ConcurrentHashMap<>());

        long key = (((long) (source == null ? 0 : source.getId())) << 32) ^ (damageSeq & 0xFFFF_FFFFL);

        mapForTick.putIfAbsent(key, new ReceiveHit(
                source,
                damageType,
                gameTime,
                Math.max(0L, amountRounded),
                damageSeq
        ));

        pruneOld(byTick, gameTime);
    }

    public static List<ReceiveHit> snapshotUnprocessed(UUID victim, long taskId) {
        var byTick = BUCKETS.get(victim);
        if (byTick == null || byTick.isEmpty()) return List.of();

        Set<String> processed = PROCESSED_BY_TASK.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet());

        List<ReceiveHit> unprocessed = new ArrayList<>();
        for (var map : byTick.values()) {
            if (map == null || map.isEmpty()) continue;
            for (ReceiveHit hit : map.values()) {
                if (!processed.contains(hit.getUniqueKey())) {
                    unprocessed.add(hit);
                }
            }
        }

        return unprocessed;
    }

    public static void markProcessed(long taskId, List<ReceiveHit> hits) {
        if (hits.isEmpty()) return;
        Set<String> processed = PROCESSED_BY_TASK.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet());
        for (ReceiveHit hit : hits) {
            processed.add(hit.getUniqueKey());
        }

        if (processed.size() > 1000) {
            long currentTime = hits.getFirst().gameTime();
            processed.removeIf(key -> {
                try {
                    long time = Long.parseLong(key.split(":")[0]);
                    return currentTime - time > 100;
                } catch (Exception e) {
                    return true;
                }
            });
        }
    }

    public static void clearTaskTracking(long taskId) {
        PROCESSED_BY_TASK.remove(taskId);
    }

    private static void pruneOld(ConcurrentHashMap<Long, ?> map, long currentTime) {
        if (map.size() <= 40) return;
        long keepFrom = currentTime - 40;
        map.keySet().removeIf(t -> t < keepFrom);
    }
}
