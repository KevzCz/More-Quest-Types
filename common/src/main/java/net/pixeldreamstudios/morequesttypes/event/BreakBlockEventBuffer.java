package net.pixeldreamstudios.morequesttypes.event;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class BreakBlockEventBuffer {
    private BreakBlockEventBuffer() {}

    public record Break(BlockState state, ItemStack tool, long gameTime) {}

    private static final Map<UUID, ConcurrentHashMap<Long, ConcurrentLinkedQueue<Break>>> BUCKETS = new ConcurrentHashMap<>();

    public static void push(UUID playerId, BlockState state, ItemStack tool, long gameTime) {
        var byTick = BUCKETS.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        var q = byTick.computeIfAbsent(gameTime, k -> new ConcurrentLinkedQueue<>());
        q.add(new Break(state, tool == null ? ItemStack.EMPTY : tool.copy(), gameTime));
        pruneOld(byTick);
    }

    public static List<Break> snapshotLatest(UUID playerId) {
        var byTick = BUCKETS.get(playerId);
        if (byTick == null || byTick.isEmpty()) return List.of();
        long latest = byTick.keySet().stream().mapToLong(Long::longValue).max().orElse(Long.MIN_VALUE);
        var q = byTick.get(latest);
        return (q == null || q.isEmpty()) ? List.of() : List.copyOf(q);
    }

    public static void clear(UUID playerId) {
        var b = BUCKETS.get(playerId);
        if (b != null) b.clear();
    }

    private static void pruneOld(ConcurrentHashMap<Long, ?> map) {
        if (map.size() <= 2) return;
        long latest = map.keySet().stream().mapToLong(Long::longValue).max().orElse(Long.MIN_VALUE);
        long keep = latest - 1;
        map.keySet().removeIf(t -> t < keep);
    }
}
