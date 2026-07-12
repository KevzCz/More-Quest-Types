package net.pixeldreamstudios.morequesttypes.event;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class UseItemEventBuffer {
    private UseItemEventBuffer() {
    }

    private static final Map<UUID, ConcurrentHashMap<Long, ConcurrentLinkedQueue<Use>>> BUCKETS = new ConcurrentHashMap<>();
    private static final Map<UUID, ConcurrentHashMap<Long, Set<InteractionHand>>> SEEN = new ConcurrentHashMap<>();
    private static final long RETENTION_TICKS = 20L;

    public record Use(InteractionHand hand, ItemStack stack, long gameTime) {
    }

    public static void push(UUID playerId, InteractionHand hand, ItemStack stack, long gameTime) {
        var seenByTick = UseItemEventBuffer.SEEN.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        var seen = seenByTick.computeIfAbsent(gameTime, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        if (!seen.add(hand)) return;

        var byTick = UseItemEventBuffer.BUCKETS.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        var q = byTick.computeIfAbsent(gameTime, k -> new ConcurrentLinkedQueue<>());
        q.add(new Use(hand, stack.copy(), gameTime));

        UseItemEventBuffer.pruneOld(gameTime, byTick);
        UseItemEventBuffer.pruneOld(gameTime, seenByTick);
    }

    public static List<Use> eventsSince(UUID playerId, long afterGameTime) {
        var byTick = UseItemEventBuffer.BUCKETS.get(playerId);
        if (byTick == null || byTick.isEmpty()) return List.of();
        List<Use> out = new ArrayList<>();
        for (var entry : byTick.entrySet()) {
            if (entry.getKey() > afterGameTime) out.addAll(entry.getValue());
        }
        return out;
    }

    public static void clear(UUID playerId) {
        var b = UseItemEventBuffer.BUCKETS.get(playerId);
        if (b != null) b.clear();
        var s = UseItemEventBuffer.SEEN.get(playerId);
        if (s != null) s.clear();
    }

    private static <V> void pruneOld(long currentGameTime, ConcurrentHashMap<Long, V> map) {
        long keep = currentGameTime - UseItemEventBuffer.RETENTION_TICKS;
        map.keySet().removeIf(t -> t < keep);
    }
}
