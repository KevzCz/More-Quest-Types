package net.pixeldreamstudios.morequesttypes.util;

/**
 * Encodes path/value pairs in filter lines. Uses {@code |} so NBT paths may contain {@code :} (e.g.
 * {@code minecraft:custom_data.exclusiveOwnerName}).
 */
public final class FilterPathValueFormat {
    public static final char SEPARATOR = '|';

    private FilterPathValueFormat() {
    }

    public static String join(String path, String value) {
        return path.trim() + SEPARATOR + value.trim();
    }

    public static PathValue split(String rest) {
        if (rest == null || rest.isBlank()) {
            return new PathValue("", "");
        }

        int pipe = rest.indexOf(SEPARATOR);
        if (pipe > 0) {
            return new PathValue(rest.substring(0, pipe).trim(), rest.substring(pipe + 1).trim());
        }

        int lastColon = rest.lastIndexOf(':');
        if (lastColon > 0) {
            String path = rest.substring(0, lastColon).trim();
            String value = rest.substring(lastColon + 1).trim();
            if (looksLikeStandaloneValue(value)) {
                return new PathValue(path, value);
            }
        }

        int colon = rest.indexOf(':');
        if (colon > 0) {
            return new PathValue(rest.substring(0, colon).trim(), rest.substring(colon + 1).trim());
        }

        return new PathValue(rest.trim(), "");
    }

    private static boolean looksLikeStandaloneValue(String value) {
        return ItemNbtFilterRepair.looksLikeStandaloneValue(value);
    }

    public record PathValue(String path, String value) {
    }
}
