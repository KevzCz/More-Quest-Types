package net.pixeldreamstudios.morequesttypes.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class UseBlockEventBuffer {
    public record Use(BlockPos pos, BlockState state, long gameTime) {
    }

    private static final Map<UUID, List<Use>> buffer = new HashMap<>();
    private static final int MAX_PER_PLAYER = 100;
    private static final long RETENTION_TICKS = 20L;

    public static void push(UUID playerId, BlockPos pos, BlockState state, long gameTime) {
        var list = UseBlockEventBuffer.buffer.computeIfAbsent(playerId, k -> new ArrayList<>());
        list.add(new Use(pos.immutable(), state, gameTime));
        UseBlockEventBuffer.cleanup(playerId, gameTime);
    }

    public static List<Use> eventsSince(UUID playerId, long afterGameTime) {
        var list = UseBlockEventBuffer.buffer.get(playerId);
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<Use> out = new ArrayList<>();
        for (var u : list) {
            if (u.gameTime() > afterGameTime) out.add(u);
        }
        return out;
    }

    private static void cleanup(UUID playerId, long currentGameTime) {
        var list = UseBlockEventBuffer.buffer.get(playerId);
        if (list == null) return;
        list.removeIf(u -> currentGameTime - u.gameTime() > UseBlockEventBuffer.RETENTION_TICKS);
        if (list.size() > UseBlockEventBuffer.MAX_PER_PLAYER) {
            list.subList(0, list.size() - UseBlockEventBuffer.MAX_PER_PLAYER).clear();
        }
    }

    private UseBlockEventBuffer() {
    }
}
