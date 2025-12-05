package net.pixeldreamstudios.morequesttypes.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class TradingEventBuffer {
    private TradingEventBuffer() {}

    public record TradeEvent(
            Entity trader,
            ItemStack buyItem,
            ItemStack sellItem,
            long gameTime
    ) {}

    private static final Map<UUID, List<TradeEvent>> BUFFER = new WeakHashMap<>();
    private static final int MAX_SIZE = 128;

    public static void record(UUID playerId, Entity trader, ItemStack buyItem, ItemStack sellItem, long gameTime) {
        if (playerId == null || trader == null) return;

        List<TradeEvent> list = BUFFER.computeIfAbsent(playerId, k -> new ArrayList<>());
        list.add(new TradeEvent(trader, buyItem.copy(), sellItem.copy(), gameTime));

        if (list.size() > MAX_SIZE) {
            list.remove(0);
        }
    }

    public static List<TradeEvent> snapshotLatest(UUID playerId) {
        List<TradeEvent> list = BUFFER.get(playerId);
        if (list == null || list.isEmpty()) return List.of();
        return new ArrayList<>(list);
    }

    public static void clear(UUID playerId) {
        BUFFER.remove(playerId);
    }
}