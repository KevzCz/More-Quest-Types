package net.pixeldreamstudios.morequesttypes.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public final class UseBlockEventBuffer {
    public record Use(BlockPos pos, BlockState state, long gameTime) {}

    private static final Map<UUID, List<Use>> buffer = new HashMap<>();
    private static final int MAX_PER_PLAYER = 100;

    public static void push(UUID playerId, BlockPos pos, BlockState state, long gameTime) {
        buffer.computeIfAbsent(playerId, k -> new ArrayList<>()).add(new Use(pos.immutable(), state, gameTime));
        cleanup(playerId);
    }

    public static List<Use> snapshotLatest(UUID playerId) {
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

    private UseBlockEventBuffer() {}
}