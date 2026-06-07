package net.pixeldreamstudios.morequesttypes.event;

import net.minecraft.world.entity.LivingEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TameEventBuffer {
    private TameEventBuffer() {}

    private static final Map<UUID, ConcurrentHashMap<Long, ConcurrentHashMap<Long, TameEvent>>> BUCKETS = new ConcurrentHashMap<>();
    private static final Map<Long, Set<String>> PROCESSED_BY_TASK = new ConcurrentHashMap<>();

    public record TameEvent(LivingEntity entity, long gameTime) {
        public String getUniqueKey() {
            return gameTime + ":" + entity.getId();
        }
    }

    public static void push(UUID player, LivingEntity entity, long gameTime) {
        var byTick = BUCKETS.computeIfAbsent(player, k -> new ConcurrentHashMap<>());
        var mapForTick = byTick.computeIfAbsent(gameTime, k -> new ConcurrentHashMap<>());
        mapForTick.putIfAbsent((long) entity.getId(), new TameEvent(entity, gameTime));
        pruneOld(byTick, gameTime);
    }

    public static List<TameEvent> snapshotUnprocessed(UUID player, long taskId) {
        var byTick = BUCKETS.get(player);
        if (byTick == null || byTick.isEmpty()) return List.of();

        long latest = byTick.keySet().stream().mapToLong(Long::longValue).max().orElse(Long.MIN_VALUE);
        if (latest == Long.MIN_VALUE) return List.of();

        var map = byTick.get(latest);
        if (map == null || map.isEmpty()) return List.of();

        Set<String> processed = PROCESSED_BY_TASK.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet());

        List<TameEvent> unprocessed = new ArrayList<>();
        for (TameEvent event : map.values()) {
            if (!processed.contains(event.getUniqueKey())) {
                unprocessed.add(event);
            }
        }

        return unprocessed;
    }

    public static void markProcessed(long taskId, List<TameEvent> events) {
        if (events.isEmpty()) return;
        Set<String> processed = PROCESSED_BY_TASK.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet());
        for (TameEvent event : events) {
            processed.add(event.getUniqueKey());
        }
    }

    public static void clearTaskTracking(long taskId) {
        PROCESSED_BY_TASK.remove(taskId);
    }

    private static void pruneOld(ConcurrentHashMap<Long, ?> map, long currentTime) {
        if (map.size() <= 3) return;
        long keepFrom = currentTime - 2;
        map.keySet().removeIf(t -> t < keepFrom);
    }
}
