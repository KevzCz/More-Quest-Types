package net.pixeldreamstudios.morequesttypes.event;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FishingCatchEventBuffer {
    public record Catch(ItemStack caughtItem, String caughtEntity, long gameTime) {
    }

    private static final Map<UUID, List<Catch>> buffer = new HashMap<>();
    private static final int MAX_PER_PLAYER = 100;
    private static final long RETENTION_TICKS = 20L;

    public static void push(UUID playerId, ItemStack item, String entity, long gameTime) {
        var list = buffer.computeIfAbsent(playerId, k -> new ArrayList<>());
        list.add(new Catch(item.copy(), entity, gameTime));
        cleanup(playerId, gameTime);
    }

    public static List<Catch> eventsSince(UUID playerId, long afterGameTime) {
        var list = buffer.get(playerId);
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<Catch> out = new ArrayList<>();
        for (var c : list) {
            if (c.gameTime() > afterGameTime) out.add(c);
        }
        return out;
    }

    private static void cleanup(UUID playerId, long currentGameTime) {
        var list = buffer.get(playerId);
        if (list == null) return;
        list.removeIf(c -> currentGameTime - c.gameTime() > RETENTION_TICKS);
        if (list.size() > MAX_PER_PLAYER) {
            list.subList(0, list.size() - MAX_PER_PLAYER).clear();
        }
    }

    private FishingCatchEventBuffer() {
    }
}
