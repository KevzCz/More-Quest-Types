package net.pixeldreamstudios.morequesttypes.event;

import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class FishingCatchEventBuffer {
    public record Catch(ItemStack caughtItem, String caughtEntity, long gameTime) {}

    private static final Map<UUID, List<Catch>> buffer = new HashMap<>();
    private static final int MAX_PER_PLAYER = 100;

    public static void push(UUID playerId, ItemStack item, String entity, long gameTime) {
        buffer.computeIfAbsent(playerId, k -> new ArrayList<>())
                .add(new Catch(item.copy(), entity, gameTime));
        cleanup(playerId);
    }

    public static List<Catch> snapshotLatest(UUID playerId) {
        var list = buffer.get(playerId);
        if (list == null || list.isEmpty()) return Collections.emptyList();
        var snapshot = new ArrayList<>(list);
        list.clear();
        return snapshot;
    }

    private static void cleanup(UUID playerId) {
        var list = buffer.get(playerId);
        if (list != null && list.size() > MAX_PER_PLAYER) {
            list.subList(0, list.size() - MAX_PER_PLAYER).clear();
        }
    }

    private FishingCatchEventBuffer() {}
}