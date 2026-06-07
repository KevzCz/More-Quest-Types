package net.pixeldreamstudios.morequesttypes.util;



import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import java.util.List;



public final class ItemNbtFilterData {

    public final List<FilterEntry> filters = new ArrayList<>();

    public final List<String> ignorePaths = new ArrayList<>();

    public ItemStack previewItem = ItemStack.EMPTY;



    public ItemNbtFilterData copy() {

        ItemNbtFilterData data = new ItemNbtFilterData();

        for (FilterEntry entry : filters) {

            data.filters.add(entry.copy());

        }

        data.ignorePaths.addAll(ignorePaths);

        data.previewItem = previewItem.copy();

        return data;

    }



    public static ItemNbtFilterData fromLists(List<String> filterLines, List<String> ignorePathLines) {

        ItemNbtFilterData data = new ItemNbtFilterData();

        if (filterLines != null) {

            for (String raw : filterLines) {

                FilterEntry entry = FilterEntry.decode(raw);

                if (entry != null) {
                    ItemNbtFilterRepair.repairColonSplitPattern(entry);
                    data.filters.add(entry);

                }

            }

        }

        if (ignorePathLines != null) {

            for (String path : ignorePathLines) {

                if (path != null && !path.isBlank()) {

                    data.ignorePaths.add(path.trim());

                }

            }

        }

        return data;

    }



    public List<String> toFilterLines() {

        List<String> lines = new ArrayList<>();

        for (FilterEntry entry : filters) {

            String encoded = entry.encode();

            if (!encoded.isBlank()) {

                lines.add(encoded);

            }

        }

        return lines;

    }



    public void applyTo(List<String> filterLines, List<String> ignorePathLines) {

        filterLines.clear();

        filterLines.addAll(toFilterLines());

        ignorePathLines.clear();

        ignorePathLines.addAll(ignorePaths);

    }



    public static final class FilterEntry {

        public enum Kind {

            REQUIRE,

            EXCLUDE,

            OR

        }



        public enum Type {

            SNBT,

            KEY_EXISTS,

            VALUE_EQUALS,

            VALUE_CONTAINS,

            BOOLEAN_VALUE,

            NUMERIC_COMPARE

        }



        public enum NumericOperator {

            EQUALS("=="),

            GREATER_THAN(">"),

            GREATER_OR_EQUAL(">="),

            LESS_THAN("<"),

            LESS_OR_EQUAL("<=");



            private final String symbol;



            NumericOperator(String symbol) {

                this.symbol = symbol;

            }



            public String symbol() {

                return symbol;

            }



            public static NumericOperator fromSymbol(String symbol) {

                for (NumericOperator op : values()) {

                    if (op.symbol.equals(symbol)) {

                        return op;

                    }

                }

                return EQUALS;

            }

        }



        public Kind kind = Kind.REQUIRE;

        public Type type = Type.SNBT;

        public String path = "";

        public String value = "";

        public NumericOperator numericOperator = NumericOperator.EQUALS;



        public FilterEntry() {

        }



        public FilterEntry(Kind kind, Type type, String path, String value, NumericOperator numericOperator) {

            this.kind = kind;

            this.type = type;

            this.path = path != null ? path : "";

            this.value = value != null ? value : "";

            this.numericOperator = numericOperator != null ? numericOperator : NumericOperator.EQUALS;

        }



        public FilterEntry copy() {

            return new FilterEntry(kind, type, path, value, numericOperator);

        }



        public String encode() {

            String prefix = switch (kind) {

                case EXCLUDE -> "!";

                case OR -> "?";

                case REQUIRE -> "";

            };



            return switch (type) {

                case KEY_EXISTS -> path.isBlank() ? "" : prefix + "@" + path.trim();

                case VALUE_EQUALS -> path.isBlank() || value.isBlank() ? ""
                        : prefix + "=" + FilterPathValueFormat.join(path, value);

                case VALUE_CONTAINS -> path.isBlank() || value.isBlank() ? ""
                        : prefix + "~" + FilterPathValueFormat.join(path, value);

                case BOOLEAN_VALUE -> path.isBlank() || value.isBlank() ? ""
                        : prefix + "^" + FilterPathValueFormat.join(path, value);

                case NUMERIC_COMPARE -> path.isBlank() || value.isBlank() ? ""
                        : prefix + numericOperator.symbol() + FilterPathValueFormat.join(path, value);

                case SNBT -> {
                    if (value.isBlank()) {
                        yield "";
                    }
                    if (!path.isBlank()) {
                        yield prefix + "$" + FilterPathValueFormat.join(path, value);
                    }
                    yield prefix + value.trim();
                }

            };

        }



        public static FilterEntry decode(String raw) {

            if (raw == null || raw.isBlank()) {

                return null;

            }



            String line = raw.trim();

            Kind kind = Kind.REQUIRE;

            while (!line.isEmpty() && (line.charAt(0) == '!' || line.charAt(0) == '?')) {

                if (line.charAt(0) == '!') {

                    kind = Kind.EXCLUDE;

                    line = line.substring(1).trim();

                } else {

                    kind = Kind.OR;

                    line = line.substring(1).trim();

                }

            }



            if (line.isEmpty()) {

                return null;

            }



            if (line.startsWith("@")) {

                String path = line.substring(1).trim();

                return path.isEmpty() ? null : new FilterEntry(kind, Type.KEY_EXISTS, path, "", NumericOperator.EQUALS);

            }

            if (line.startsWith("$")) {
                FilterPathValueFormat.PathValue pathValue = FilterPathValueFormat.split(line.substring(1).trim());
                if (!pathValue.path().isEmpty() && !pathValue.value().isEmpty()) {
                    return new FilterEntry(kind, Type.SNBT, pathValue.path(), pathValue.value(), NumericOperator.EQUALS);
                }
            }

            for (NumericOperator op : new NumericOperator[]{

                    NumericOperator.GREATER_OR_EQUAL,

                    NumericOperator.LESS_OR_EQUAL,

                    NumericOperator.GREATER_THAN,

                    NumericOperator.LESS_THAN,

                    NumericOperator.EQUALS

            }) {

                if (line.startsWith(op.symbol())) {

                    FilterPathValueFormat.PathValue pathValue =
                            FilterPathValueFormat.split(line.substring(op.symbol().length()).trim());

                    if (!pathValue.path().isEmpty() && !pathValue.value().isEmpty()) {

                        return new FilterEntry(kind, Type.NUMERIC_COMPARE,

                                pathValue.path(),

                                pathValue.value(),

                                op);

                    }

                }

            }



            if (line.startsWith("=")) {

                FilterPathValueFormat.PathValue pathValue = FilterPathValueFormat.split(line.substring(1).trim());

                if (!pathValue.path().isEmpty() && !pathValue.value().isEmpty()) {

                    return new FilterEntry(kind, Type.VALUE_EQUALS,

                            pathValue.path(),

                            pathValue.value(),

                            NumericOperator.EQUALS);

                }

            }



            if (line.startsWith("~")) {

                FilterPathValueFormat.PathValue pathValue = FilterPathValueFormat.split(line.substring(1).trim());

                if (!pathValue.path().isEmpty() && !pathValue.value().isEmpty()) {

                    return new FilterEntry(kind, Type.VALUE_CONTAINS,

                            pathValue.path(),

                            pathValue.value(),

                            NumericOperator.EQUALS);

                }

            }



            if (line.startsWith("^")) {

                FilterPathValueFormat.PathValue pathValue = FilterPathValueFormat.split(line.substring(1).trim());

                if (!pathValue.path().isEmpty() && !pathValue.value().isEmpty()) {

                    return new FilterEntry(kind, Type.BOOLEAN_VALUE,

                            pathValue.path(),

                            pathValue.value(),

                            NumericOperator.EQUALS);

                }

            }



            FilterEntry booleanFallback = decodeBooleanFallback(kind, line);
            if (booleanFallback != null) {
                return booleanFallback;
            }

            return new FilterEntry(kind, Type.SNBT, "", line, NumericOperator.EQUALS);

        }

        private static @Nullable FilterEntry decodeBooleanFallback(Kind kind, String line) {
            FilterPathValueFormat.PathValue pathValue = FilterPathValueFormat.split(line);
            if (pathValue.path().isEmpty() || pathValue.value().isEmpty()) {
                return null;
            }
            if (!"true".equalsIgnoreCase(pathValue.value()) && !"false".equalsIgnoreCase(pathValue.value())) {
                return null;
            }
            return new FilterEntry(kind, Type.BOOLEAN_VALUE, pathValue.path(), pathValue.value(), NumericOperator.EQUALS);
        }

    }

}


