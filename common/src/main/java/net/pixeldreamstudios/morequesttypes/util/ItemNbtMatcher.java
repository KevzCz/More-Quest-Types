package net.pixeldreamstudios.morequesttypes.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Flexible item NBT matching for quest tasks.
 * <p>
 * Filter line syntax (all entries in one list):
 * <ul>
 *   <li>{@code {components:{...}}} — SNBT subset match (compounds, lists, exact leaves)</li>
 *   <li>{@code @components.minecraft:custom_name} — key must exist at dot path</li>
 *   <li>{@code !{...}} or {@code !@path} — negated: match means reject</li>
 *   <li>{@code ?{...}} or {@code ?@path} — OR group: at least one {@code ?} line must pass</li>
 * </ul>
 * Placeholders: {@code {player_uuid}}, {@code {player_name}}, {@code {player_uuid_array}}
 */
public final class ItemNbtMatcher {
    private static final ThreadLocal<SubmittingPlayer> SUBMITTING_PLAYER = new ThreadLocal<>();

    private ItemNbtMatcher() {
    }

    public record SubmittingPlayer(UUID uuid, String name, HolderLookup.Provider provider) {
    }

    public static void bindSubmittingPlayer(@Nullable UUID uuid, @Nullable String name, HolderLookup.Provider provider) {
        if (uuid == null && (name == null || name.isBlank())) {
            SUBMITTING_PLAYER.remove();
            return;
        }
        SUBMITTING_PLAYER.set(new SubmittingPlayer(uuid, name, provider));
    }

    public static void clearSubmittingPlayer() {
        SUBMITTING_PLAYER.remove();
    }

    public static @Nullable SubmittingPlayer submittingPlayer() {
        return SUBMITTING_PLAYER.get();
    }

    public enum MatchMode {
        /** Subset: compounds recurse, lists are multiset-subset, leaves compared bidirectionally. */
        SUBSET,
        /** Legacy partial compound match (no list subset logic). */
        PARTIAL
    }

    public static final class Options {
        private List<String> filters = List.of();
        private List<String> ignorePaths = List.of();
        private @Nullable UUID playerUuid;
        private @Nullable String playerName;
        private MatchMode mode = MatchMode.SUBSET;

        public Options filters(List<String> filters) {
            this.filters = filters != null ? filters : List.of();
            return this;
        }

        public Options ignorePaths(List<String> ignorePaths) {
            this.ignorePaths = ignorePaths != null ? ignorePaths : List.of();
            return this;
        }

        public Options player(@Nullable UUID playerUuid, @Nullable String playerName) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            return this;
        }

        public Options mode(MatchMode mode) {
            this.mode = mode != null ? mode : MatchMode.SUBSET;
            return this;
        }
    }

    public static boolean matches(ItemStack stack, List<String> filters, List<String> ignorePaths, HolderLookup.Provider provider) {
        return matches(stack, filters, ignorePaths, null, null, provider);
    }

    public static boolean matches(ItemStack stack, List<String> filters, List<String> ignorePaths,
                                  @Nullable UUID playerUuid, @Nullable String playerName, HolderLookup.Provider provider) {
        return matches(stack, new Options()
                .filters(filters)
                .ignorePaths(ignorePaths)
                .player(playerUuid, playerName), provider);
    }

    public static boolean matches(ItemStack stack, Options options, HolderLookup.Provider provider) {
        if (options.filters == null || options.filters.isEmpty()) {
            return true;
        }
        if (stack.isEmpty()) {
            return false;
        }
        try {
            CompoundTag fullTag = (CompoundTag) stack.save(provider);
            return matchesTag(fullTag, options);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean matchesTag(CompoundTag tag, Options options) {
        if (options.filters == null || options.filters.isEmpty()) {
            return true;
        }
        try {
            CompoundTag working = tag.copy();
            applyIgnorePaths(working, options.ignorePaths);
            return evaluateFilters(working, processPlaceholders(options.filters, options.playerUuid, options.playerName), options.mode);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean nbtSubsetMatches(Tag actual, Tag filter) {
        if (filter instanceof CompoundTag f) {
            if (!(actual instanceof CompoundTag a)) return false;
            for (String key : f.getAllKeys()) {
                Tag fVal = f.get(key);
                Tag aVal = a.get(key);
                if (aVal == null || !nbtSubsetMatches(aVal, fVal)) return false;
            }
            return true;
        }
        if (filter instanceof ListTag fList) {
            if (!(actual instanceof ListTag aList)) return false;
            List<Tag> remaining = new ArrayList<>(aList);
            outer:
            for (Tag fEl : fList) {
                for (int j = 0; j < remaining.size(); j++) {
                    if (nbtSubsetMatches(remaining.get(j), fEl)) {
                        remaining.remove(j);
                        continue outer;
                    }
                }
                return false;
            }
            return true;
        }
        return NbtUtils.compareNbt(filter, actual, false) && NbtUtils.compareNbt(actual, filter, false);
    }

    private static boolean evaluateFilters(CompoundTag tag, List<String> filters, MatchMode mode) {
        boolean anyOrRequired = false;
        boolean anyOrPassed = false;

        for (String raw : filters) {
            if (raw == null || raw.isBlank()) continue;

            FilterLine line = parseFilterLine(raw);
            if (line == null) {
                return false;
            }

            boolean matched = evaluateLine(tag, line, mode);

            if (line.exclude) {
                if (matched) return false;
                continue;
            }

            if (line.or) {
                anyOrRequired = true;
                if (matched) anyOrPassed = true;
            } else if (!matched) {
                return false;
            }
        }

        return !anyOrRequired || anyOrPassed;
    }

    private static boolean evaluateLine(CompoundTag tag, FilterLine line, MatchMode mode) {
        return switch (line.type) {
            case KEY_EXISTS -> getTagAtItemPath(tag, line.path) != null;
            case SNBT -> line.tag != null && matchesFilterTag(tag, line.tag, mode);
            case VALUE_EQUALS -> valueEqualsAtPath(tag, line.path, line.expectedValue, mode);
            case VALUE_CONTAINS -> valueContainsAtPath(tag, line.path, line.expectedValue);
            case BOOLEAN_VALUE -> booleanEqualsAtPath(tag, line.path, line.expectedValue);
            case NUMERIC_COMPARE -> numericCompareAtPath(tag, line.path, line.numericOperator, line.expectedValue);
        };
    }

    private static boolean matchesFilterTag(CompoundTag tag, Tag filter, MatchMode mode) {
        if (filter instanceof CompoundTag compound) {
            return mode == MatchMode.SUBSET
                    ? nbtSubsetMatches(tag, compound)
                    : containsPartialNbt(tag, compound);
        }
        return nbtSubsetMatches(tag, filter);
    }

    private static @Nullable FilterLine parseFilterLine(String raw) {
        String line = cleanQuotes(raw.trim());
        boolean exclude = false;
        boolean or = false;

        while (!line.isEmpty()) {
            char c = line.charAt(0);
            if (c == '!') {
                exclude = true;
                line = line.substring(1).trim();
            } else if (c == '?') {
                or = true;
                line = line.substring(1).trim();
            } else {
                break;
            }
        }

        if (line.isEmpty()) return null;

        if (line.startsWith("@")) {
            String path = line.substring(1).trim();
            if (path.isEmpty()) return null;
            return new FilterLine(exclude, or, FilterType.KEY_EXISTS, path, null, null, null);
        }

        if (line.startsWith("$")) {
            FilterPathValueFormat.PathValue pathValue = FilterPathValueFormat.split(line.substring(1).trim());
            if (!pathValue.path().isEmpty() && !pathValue.value().isEmpty()) {
                try {
                    Tag parsed = TagParser.parseTag(pathValue.value());
                    return new FilterLine(exclude, or, FilterType.SNBT, pathValue.path(), parsed, null, null);
                } catch (Exception ignored) {
                }
            }
        }

        FilterLine parsedPathLine = parsePathValueLine(exclude, or, line);
        if (parsedPathLine != null) {
            return parsedPathLine;
        }

        FilterLine booleanFallback = parseBooleanFallback(exclude, or, line);
        if (booleanFallback != null) {
            return booleanFallback;
        }

        try {
            Tag parsed = TagParser.parseTag(line);
            return new FilterLine(exclude, or, FilterType.SNBT, null, parsed, null, null);
        } catch (Exception e) {
            return null;
        }
    }

    private static @Nullable FilterLine parseBooleanFallback(boolean exclude, boolean or, String line) {
        FilterPathValueFormat.PathValue pathValue = FilterPathValueFormat.split(line);
        if (pathValue.path().isEmpty() || pathValue.value().isEmpty()) {
            return null;
        }
        if (!"true".equalsIgnoreCase(pathValue.value()) && !"false".equalsIgnoreCase(pathValue.value())) {
            return null;
        }
        return new FilterLine(exclude, or, FilterType.BOOLEAN_VALUE, pathValue.path(), null, pathValue.value(), null);
    }

    private static @Nullable FilterLine parsePathValueLine(boolean exclude, boolean or, String line) {
        for (String symbol : new String[]{">=", "<=", "==", ">", "<"}) {
            if (line.startsWith(symbol)) {
                FilterPathValueFormat.PathValue pathValue =
                        FilterPathValueFormat.split(line.substring(symbol.length()).trim());
                if (!pathValue.path().isEmpty() && !pathValue.value().isEmpty()) {
                    return new FilterLine(exclude, or, FilterType.NUMERIC_COMPARE,
                            pathValue.path(),
                            null,
                            pathValue.value(),
                            symbol);
                }
            }
        }

        if (line.startsWith("=")) {
            FilterPathValueFormat.PathValue pathValue = FilterPathValueFormat.split(line.substring(1).trim());
            if (!pathValue.path().isEmpty() && !pathValue.value().isEmpty()) {
                return new FilterLine(exclude, or, FilterType.VALUE_EQUALS,
                        pathValue.path(),
                        null,
                        pathValue.value(),
                        null);
            }
        }

        if (line.startsWith("~")) {
            FilterPathValueFormat.PathValue pathValue = FilterPathValueFormat.split(line.substring(1).trim());
            if (!pathValue.path().isEmpty() && !pathValue.value().isEmpty()) {
                return new FilterLine(exclude, or, FilterType.VALUE_CONTAINS,
                        pathValue.path(),
                        null,
                        pathValue.value(),
                        null);
            }
        }

        if (line.startsWith("^")) {
            FilterPathValueFormat.PathValue pathValue = FilterPathValueFormat.split(line.substring(1).trim());
            if (!pathValue.path().isEmpty() && !pathValue.value().isEmpty()) {
                return new FilterLine(exclude, or, FilterType.BOOLEAN_VALUE,
                        pathValue.path(),
                        null,
                        pathValue.value(),
                        null);
            }
        }

        return null;
    }

    private static String cleanQuotes(String value) {
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean valueEqualsAtPath(CompoundTag root, @Nullable String path, @Nullable String expected, MatchMode mode) {
        if (path == null || path.isBlank() || expected == null || expected.isBlank()) {
            return false;
        }
        Tag actual = getTagAtItemPath(root, path);
        if (actual == null) {
            return false;
        }

        String expectedRaw = expected.trim();
        if (actual instanceof StringTag stringTag) {
            String expectedContent = cleanQuotes(expectedRaw);
            try {
                Tag expectedTag = TagParser.parseTag(expectedRaw);
                if (expectedTag instanceof StringTag expectedString) {
                    return stringTag.getAsString().equals(expectedString.getAsString());
                }
            } catch (Exception ignored) {
            }
            return stringTag.getAsString().equals(expectedContent);
        }

        try {
            Tag expectedTag = TagParser.parseTag(expectedRaw);
            return nbtSubsetMatches(actual, expectedTag) && nbtSubsetMatches(expectedTag, actual);
        } catch (Exception ignored) {
            return actual.toString().equals(expectedRaw);
        }
    }

    private static boolean valueContainsAtPath(CompoundTag root, @Nullable String path, @Nullable String expected) {
        if (path == null || path.isBlank() || expected == null || expected.isBlank()) {
            return false;
        }
        Tag actual = getTagAtItemPath(root, path);
        if (actual instanceof StringTag stringTag) {
            return stringTag.getAsString().contains(expected);
        }
        return actual != null && actual.toString().contains(expected);
    }

    private static boolean booleanEqualsAtPath(CompoundTag root, @Nullable String path, @Nullable String expected) {
        if (path == null || path.isBlank() || expected == null || expected.isBlank()) {
            return false;
        }
        Tag actual = getTagAtItemPath(root, path);
        if (!(actual instanceof ByteTag byteTag)) {
            return false;
        }
        boolean expectedBool = expected.equalsIgnoreCase("true") || expected.equals("1");
        boolean actualBool = byteTag.getAsByte() != 0;
        return actualBool == expectedBool;
    }

    private static boolean numericCompareAtPath(CompoundTag root, @Nullable String path, @Nullable String operator, @Nullable String expected) {
        if (path == null || path.isBlank() || operator == null || expected == null || expected.isBlank()) {
            return false;
        }
        Tag actual = getTagAtItemPath(root, path);
        if (!(actual instanceof NumericTag numericTag)) {
            return false;
        }
        double actualValue = numericTag.getAsDouble();
        double expectedValue;
        try {
            expectedValue = Double.parseDouble(expected.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        return switch (operator) {
            case ">" -> actualValue > expectedValue;
            case ">=" -> actualValue >= expectedValue;
            case "<" -> actualValue < expectedValue;
            case "<=" -> actualValue <= expectedValue;
            default -> actualValue == expectedValue;
        };
    }

    private static @Nullable Tag getTagAtItemPath(CompoundTag fullTag, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (path.startsWith("root.")) {
            return navigateToPath(fullTag, NbtPathUtil.splitPath(path.substring(5)));
        }
        Tag direct = navigateToPath(fullTag, NbtPathUtil.splitPath(path));
        if (direct != null) {
            return direct;
        }
        CompoundTag components = fullTag.getCompound("components");
        return navigateToPath(components, NbtPathUtil.splitPath(path));
    }

    private static @Nullable Tag navigateToPath(CompoundTag root, String[] parts) {
        if (parts.length == 0) {
            return root;
        }
        Tag current = root;
        for (String part : parts) {
            int bracket = part.indexOf('[');
            if (bracket >= 0) {
                String key = part.substring(0, bracket);
                int end = part.indexOf(']', bracket);
                if (end < 0) {
                    return null;
                }
                int index;
                try {
                    index = Integer.parseInt(part.substring(bracket + 1, end));
                } catch (NumberFormatException e) {
                    return null;
                }
                if (!(current instanceof CompoundTag compound)) {
                    return null;
                }
                Tag listTag = compound.get(key);
                if (!(listTag instanceof ListTag list) || index < 0 || index >= list.size()) {
                    return null;
                }
                current = list.get(index);
            } else if (current instanceof CompoundTag compound) {
                current = compound.get(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return current;
    }

    private static void applyIgnorePaths(CompoundTag root, List<String> ignorePaths) {
        if (ignorePaths == null || ignorePaths.isEmpty()) return;
        for (String path : ignorePaths) {
            if (path == null || path.isBlank()) continue;
            removeAtItemPath(root, path.trim());
        }
    }

    private static void removeAtItemPath(CompoundTag root, String path) {
        if (path.startsWith("root.")) {
            removeAtPath(root, NbtPathUtil.splitPath(path.substring(5)));
            return;
        }
        if (removeAtPath(root, NbtPathUtil.splitPath(path))) {
            return;
        }
        CompoundTag components = root.getCompound("components");
        removeAtPath(components, NbtPathUtil.splitPath(path));
    }

    private static boolean removeAtPath(CompoundTag root, String[] parts) {
        if (parts.length == 0) {
            return false;
        }
        CompoundTag current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            int bracket = part.indexOf('[');
            if (bracket >= 0) {
                return false;
            }
            Tag tag = current.get(part);
            if (!(tag instanceof CompoundTag compound)) {
                return false;
            }
            current = compound;
        }
        String last = parts[parts.length - 1];
        if (last.contains("[")) {
            return false;
        }
        if (!current.contains(last)) {
            return false;
        }
        current.remove(last);
        return true;
    }

    private static List<String> processPlaceholders(List<String> entries, @Nullable UUID playerUuid, @Nullable String playerName) {
        List<String> processed = new ArrayList<>();
        for (String entry : entries) {
            String result = entry;
            if (playerUuid != null) {
                result = result
                        .replace("{player_uuid}", playerUuid.toString())
                        .replace("{player_uuid_array}", uuidToIntArray(playerUuid));
            }
            if (playerName != null) {
                result = result.replace("{player_name}", playerName);
            }
            processed.add(result);
        }
        return processed;
    }

    private static String uuidToIntArray(UUID uuid) {
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();
        int[] ints = new int[4];
        ints[0] = (int) (mostSigBits >> 32);
        ints[1] = (int) mostSigBits;
        ints[2] = (int) (leastSigBits >> 32);
        ints[3] = (int) leastSigBits;
        return "[I;" + ints[0] + "," + ints[1] + "," + ints[2] + "," + ints[3] + "]";
    }

    private static boolean containsPartialNbt(CompoundTag itemTag, CompoundTag filter) {
        for (String key : filter.getAllKeys()) {
            Tag filterValue = filter.get(key);
            Tag itemValue = itemTag.get(key);
            if (itemValue == null) return false;

            if (filterValue instanceof CompoundTag filterCompound && itemValue instanceof CompoundTag itemCompound) {
                if (!containsPartialNbt(itemCompound, filterCompound)) return false;
            } else if (!tagsEqual(itemValue, filterValue)) {
                return false;
            }
        }
        return true;
    }

    private static boolean tagsEqual(Tag a, Tag b) {
        if (a instanceof CompoundTag ca && b instanceof CompoundTag cb) {
            if (cb.isEmpty()) return true;
            return containsPartialNbt(ca, cb);
        }
        return NbtUtils.compareNbt(a, b, true);
    }

    private enum FilterType {
        KEY_EXISTS,
        SNBT,
        VALUE_EQUALS,
        VALUE_CONTAINS,
        BOOLEAN_VALUE,
        NUMERIC_COMPARE
    }

    private record FilterLine(
            boolean exclude,
            boolean or,
            FilterType type,
            @Nullable String path,
            @Nullable Tag tag,
            @Nullable String expectedValue,
            @Nullable String numericOperator
    ) {
    }
}
