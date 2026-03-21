package net.pixeldreamstudios.morequesttypes.event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class CommandEventBuffer {
    private CommandEventBuffer() {
    }

    private static final Map<UUID, ConcurrentHashMap<Long, ConcurrentLinkedQueue<CommandExecution>>> BUCKETS = new ConcurrentHashMap<>();

    public record CommandExecution(String command, long gameTime) {
    }

    public static void push(UUID playerId, String command, long gameTime) {
        var byTick = BUCKETS.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        var q = byTick.computeIfAbsent(gameTime, k -> new ConcurrentLinkedQueue<>());
        q.add(new CommandExecution(command, gameTime));

        pruneOld(playerId, byTick);
    }

    public static List<CommandExecution> snapshotLatest(UUID playerId) {
        var byTick = BUCKETS.get(playerId);
        if (byTick == null || byTick.isEmpty()) return List.of();
        long latest = byTick.keySet().stream().mapToLong(Long::longValue).max().orElse(Long.MIN_VALUE);
        if (latest == Long.MIN_VALUE) return List.of();
        var q = byTick.get(latest);
        if (q == null || q.isEmpty()) return List.of();
        var snapshot = List.copyOf(q);
        byTick.remove(latest);
        return snapshot;
    }

    public static void clear(UUID playerId) {
        var b = BUCKETS.get(playerId);
        if (b != null) b.clear();
    }

    private static <V> void pruneOld(UUID playerId, ConcurrentHashMap<Long, V> map) {
        if (map.size() <= 2) return;
        long latest = map.keySet().stream().mapToLong(Long::longValue).max().orElse(Long.MIN_VALUE);
        long keep = latest - 1;
        map.keySet().removeIf(t -> t < keep);
    }
}
