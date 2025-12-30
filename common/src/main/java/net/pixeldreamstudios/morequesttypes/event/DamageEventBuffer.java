package net.pixeldreamstudios.morequesttypes.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DamageEventBuffer {
    private DamageEventBuffer() {}

    private static final Map<UUID, ConcurrentHashMap<Long, ConcurrentHashMap<Long, Hit>>> BUCKETS = new ConcurrentHashMap<>();

    // Track which damage events each task has processed
    // Map: TaskID -> Set of processed damage keys
    private static final Map<Long, Set<String>> PROCESSED_BY_TASK = new ConcurrentHashMap<>();

    public record Hit(
            Entity victim,
            ItemStack stack,
            long gameTime,
            long amountBaselineRounded,
            long amountFinalRounded,
            float prevHealth,
            float newHealth,
            int damageSeq
    ) {
        public String getUniqueKey() {
            return gameTime + ":" + victim.getId() + ":" + damageSeq;
        }
    }

    public static void push(UUID attacker, Entity victim, ItemStack stack, long gameTime,
                            long baselineRounded, long finalRounded,
                            float prevHealth, float newHealth, int damageSeq) {

        var byTick = BUCKETS.computeIfAbsent(attacker, k -> new ConcurrentHashMap<>());
        var mapForTick = byTick.computeIfAbsent(gameTime, k -> new ConcurrentHashMap<>());

        long key = (((long) victim.getId()) << 32) ^ (damageSeq & 0xFFFF_FFFFL);

        mapForTick.putIfAbsent(key, new Hit(
                victim,
                stack == null ?  ItemStack.EMPTY : stack.copy(),
                gameTime,
                Math.max(0L, baselineRounded),
                Math.max(0L, finalRounded),
                prevHealth,
                newHealth,
                damageSeq
        ));

        pruneOld(byTick, gameTime);
    }

    public static List<Hit> snapshotLatest(UUID attacker) {
        var byTick = BUCKETS.get(attacker);
        if (byTick == null || byTick.isEmpty()) return List.of();
        long latest = byTick.keySet().stream().mapToLong(Long::longValue).max().orElse(Long.MIN_VALUE);
        if (latest == Long.MIN_VALUE) return List.of();
        var map = byTick.get(latest);
        if (map == null || map.isEmpty()) return List.of();
        return List.copyOf(map.values());
    }

    /**
     * Get only the hits that this specific task hasn't processed yet
     */
    public static List<Hit> snapshotUnprocessed(UUID attacker, long taskId) {
        var byTick = BUCKETS.get(attacker);
        if (byTick == null || byTick.isEmpty()) return List.of();

        long latest = byTick.keySet().stream().mapToLong(Long::longValue).max().orElse(Long.MIN_VALUE);
        if (latest == Long.MIN_VALUE) return List.of();

        var map = byTick.get(latest);
        if (map == null || map.isEmpty()) return List.of();

        Set<String> processed = PROCESSED_BY_TASK.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet());

        List<Hit> unprocessed = new ArrayList<>();
        for (Hit hit : map.values()) {
            if (!processed.contains(hit.getUniqueKey())) {
                unprocessed.add(hit);
            }
        }

        return unprocessed;
    }

    /**
     * Mark hits as processed by a specific task
     */
    public static void markProcessed(long taskId, List<Hit> hits) {
        if (hits.isEmpty()) return;
        Set<String> processed = PROCESSED_BY_TASK.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet());
        for (Hit hit : hits) {
            processed.add(hit.getUniqueKey());
        }

        // Clean up old entries (keep only recent ones)
        if (processed.size() > 1000) {
            long currentTime = hits.get(0).gameTime();
            processed.removeIf(key -> {
                try {
                    long time = Long.parseLong(key.split(":")[0]);
                    return currentTime - time > 100; // Keep 100 ticks of history
                } catch (Exception e) {
                    return true;
                }
            });
        }
    }

    public static void clear(UUID attacker) {
        var m = BUCKETS.get(attacker);
        if (m != null) m.clear();
    }

    public static void clearTaskTracking(long taskId) {
        PROCESSED_BY_TASK.remove(taskId);
    }

    private static void pruneOld(ConcurrentHashMap<Long, ? > map, long currentTime) {
        if (map.size() <= 3) return;
        long keepFrom = currentTime - 2;
        map.keySet().removeIf(t -> t < keepFrom);
    }
}