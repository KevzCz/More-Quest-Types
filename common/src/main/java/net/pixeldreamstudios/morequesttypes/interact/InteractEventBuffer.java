package net.pixeldreamstudios.morequesttypes.interact;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.UUID;

public final class InteractEventBuffer {

    private static final Map<UUID, ConcurrentHashMap<Long, ConcurrentLinkedQueue<Interaction>>> BUCKETS = new ConcurrentHashMap<>();

    private static final class Key {
        final int id; final InteractionHand hand;
        Key(int id, InteractionHand hand) { this.id = id; this.hand = hand; }
        @Override public boolean equals(Object o){ return o instanceof Key k && id==k.id && hand==k.hand; }
        @Override public int hashCode(){ return Objects.hash(id, hand); }
    }
    private static final Map<UUID, ConcurrentHashMap<Long, Set<Key>>> SEEN = new ConcurrentHashMap<>();

    private InteractEventBuffer() {}
    public static void push(UUID playerId, Entity target, InteractionHand hand, ItemStack stack, long gameTime) {

        var seenByTick = SEEN.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        var seen = seenByTick.computeIfAbsent(gameTime, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        if (!seen.add(new Key(target.getId(), hand))) return;

        var byTick = BUCKETS.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        var q = byTick.computeIfAbsent(gameTime, k -> new ConcurrentLinkedQueue<>());
        q.add(new Interaction(target, hand, stack.copy(), gameTime));

        pruneOld(playerId, byTick);
        pruneOld(playerId, seenByTick);
    }

    public static List<Interaction> snapshotLatest(UUID playerId) {
        var byTick = BUCKETS.get(playerId);
        if (byTick == null || byTick.isEmpty()) return List.of();
        long latest = byTick.keySet().stream().mapToLong(Long::longValue).max().orElse(Long.MIN_VALUE);
        if (latest == Long.MIN_VALUE) return List.of();
        var q = byTick.get(latest);
        if (q == null || q.isEmpty()) return List.of();
        return List.copyOf(q);
    }

    public static void clear(UUID playerId) {
        var b = BUCKETS.get(playerId); if (b != null) b.clear();
        var s = SEEN.get(playerId);    if (s != null) s.clear();
    }

    private static <V> void pruneOld(UUID playerId, ConcurrentHashMap<Long, V> map) {
        if (map.size() <= 2) return;
        long latest = map.keySet().stream().mapToLong(Long::longValue).max().orElse(Long.MIN_VALUE);
        long keep = latest - 1;
        map.keySet().removeIf(t -> t < keep);
    }

    public record Interaction(Entity entity, InteractionHand hand, ItemStack stack, long gameTime) {}
}
