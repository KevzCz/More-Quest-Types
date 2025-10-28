package net.pixeldreamstudios.morequesttypes.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DamageEventBuffer {
    private DamageEventBuffer() {}

    private static final Map<UUID, ConcurrentHashMap<Long, ConcurrentHashMap<Long, Hit>>> BUCKETS = new ConcurrentHashMap<>();

    public record Hit(
            Entity victim,
            ItemStack stack,
            long gameTime,
            long amountBaselineRounded,
            long amountFinalRounded,
            float prevHealth,
            float newHealth,
            int   damageSeq
    ) {}

    public static void push(UUID attacker, Entity victim, ItemStack stack, long gameTime,
                            long baselineRounded, long finalRounded,
                            float prevHealth, float newHealth, int damageSeq) {

        var byTick = BUCKETS.computeIfAbsent(attacker, k -> new ConcurrentHashMap<>());
        var mapForTick = byTick.computeIfAbsent(gameTime, k -> new ConcurrentHashMap<>());

        long key = (((long) victim.getId()) << 32) ^ (damageSeq & 0xFFFF_FFFFL);

        mapForTick.putIfAbsent(key, new Hit(
                victim,
                stack == null ? ItemStack.EMPTY : stack.copy(),
                gameTime,
                Math.max(0L, baselineRounded),
                Math.max(0L, finalRounded),
                prevHealth,
                newHealth,
                damageSeq
        ));

        pruneOld(attacker, byTick);
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

    public static void clear(UUID attacker) {
        var m = BUCKETS.get(attacker);
        if (m != null) m.clear();
    }

    private static <V> void pruneOld(UUID attacker, ConcurrentHashMap<Long, V> map) {
        if (map.size() <= 2) return;
        long latest = map.keySet().stream().mapToLong(Long::longValue).max().orElse(Long.MIN_VALUE);
        long keep = latest - 1;
        map.keySet().removeIf(t -> t < keep);
    }
}
