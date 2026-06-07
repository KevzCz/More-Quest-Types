package net.pixeldreamstudios.morequesttypes.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ItemStack;

/**
 * Fixes filter entries where {@code path:value} was split on the first {@code :} instead of the
 * separator between path and value (e.g. path {@code minecraft}, value
 * {@code custom_data.exclusiveOwnerName:"KevyDevy"}).
 */
public final class ItemNbtFilterRepair {
    private ItemNbtFilterRepair() {
    }

    public static void repair(ItemNbtFilterData.FilterEntry entry) {
        repair(entry, ItemStack.EMPTY);
    }

    @Environment(EnvType.CLIENT)
    public static void repair(ItemNbtFilterData.FilterEntry entry, ItemStack previewStack) {
        if (entry == null) {
            return;
        }
        if (entry.type == ItemNbtFilterData.FilterEntry.Type.KEY_EXISTS) {
            return;
        }

        repairColonSplitPattern(entry);

        if (!previewStack.isEmpty()) {
            if (entry.type == ItemNbtFilterData.FilterEntry.Type.SNBT
                    && entry.path.isBlank()
                    && !entry.value.isBlank()) {
                String inferred = NbtPathUtil.inferPathForSnbt(previewStack, entry.value);
                if (inferred != null) {
                    entry.path = inferred;
                }
            }
            NbtPathUtil.tryRepairColonSplitPath(entry, previewStack);
        }
    }

    /**
     * Merges path segments that were wrongly placed in the value field. Does not need a preview item.
     */
    public static boolean repairColonSplitPattern(ItemNbtFilterData.FilterEntry entry) {
        if (entry.type == ItemNbtFilterData.FilterEntry.Type.KEY_EXISTS
                || entry.type == ItemNbtFilterData.FilterEntry.Type.SNBT) {
            return false;
        }
        if (entry.path.isBlank() || entry.value == null || entry.value.isBlank()) {
            return false;
        }

        boolean repaired = false;
        for (int step = 0; step < 8; step++) {
            if (looksLikeStandaloneValue(entry.value)) {
                break;
            }

            int colon = entry.value.indexOf(':');
            if (colon <= 0) {
                break;
            }

            String segment = entry.value.substring(0, colon).trim();
            if (segment.isEmpty() || !segment.contains(".")) {
                break;
            }

            entry.path = entry.path + ":" + segment;
            entry.value = entry.value.substring(colon + 1).trim();
            repaired = true;
        }
        return repaired;
    }

    static boolean looksLikeStandaloneValue(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return true;
        }
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if (trimmed.startsWith("{player_")) {
            return true;
        }
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return false;
        }
        try {
            Double.parseDouble(trimmed);
            return true;
        } catch (NumberFormatException ignored) {
        }
        return !trimmed.contains(".") && !trimmed.contains(":");
    }
}
