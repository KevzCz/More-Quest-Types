package net.pixeldreamstudios.morequesttypes.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SpellCastEventBuffer {
    private SpellCastEventBuffer() {}

    private static final Map<UUID, ConcurrentHashMap<Long, ConcurrentHashMap<Long, CastEvent>>> BUCKETS = new ConcurrentHashMap<>();
    private static final Map<Long, Set<String>> PROCESSED_BY_TASK = new ConcurrentHashMap<>();
    private static long castSeq = 0L;

    public record CastEvent(
            ResourceLocation spellId,
            List<Entity> targets,
            long gameTime,
            long sequence
    ) {
        public String getUniqueKey() {
            return gameTime + ":" + sequence;
        }
    }

    public static void push(UUID caster, ResourceLocation spellId, List<Entity> targets, long gameTime) {
        var byTick = BUCKETS.computeIfAbsent(caster, k -> new ConcurrentHashMap<>());
        var mapForTick = byTick.computeIfAbsent(gameTime, k -> new ConcurrentHashMap<>());

        long seq = ++castSeq;
        mapForTick.put(seq, new CastEvent(spellId, targets == null ? List.of() : List.copyOf(targets), gameTime, seq));
        pruneOld(byTick, gameTime);
    }

    public static List<CastEvent> snapshotUnprocessed(UUID caster, long taskId) {
        var byTick = BUCKETS.get(caster);
        if (byTick == null || byTick.isEmpty()) return List.of();

        long latest = byTick.keySet().stream().mapToLong(Long::longValue).max().orElse(Long.MIN_VALUE);
        if (latest == Long.MIN_VALUE) return List.of();

        var map = byTick.get(latest);
        if (map == null || map.isEmpty()) return List.of();

        Set<String> processed = PROCESSED_BY_TASK.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet());

        List<CastEvent> unprocessed = new ArrayList<>();
        for (CastEvent event : map.values()) {
            if (!processed.contains(event.getUniqueKey())) {
                unprocessed.add(event);
            }
        }

        return unprocessed;
    }

    public static void markProcessed(long taskId, List<CastEvent> events) {
        if (events.isEmpty()) return;
        Set<String> processed = PROCESSED_BY_TASK.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet());
        for (CastEvent event : events) {
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
