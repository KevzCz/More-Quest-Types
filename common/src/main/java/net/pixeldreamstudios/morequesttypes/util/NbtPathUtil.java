package net.pixeldreamstudios.morequesttypes.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class NbtPathUtil {
    private NbtPathUtil() {}

    @Environment(EnvType.CLIENT)
    public static List<String> extractPaths(ItemStack previewStack) {
        List<String> paths = new ArrayList<>();
        if (previewStack == null || previewStack.isEmpty()) {
            return paths;
        }

        try {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return paths;
            }

            CompoundTag fullTag = (CompoundTag) previewStack.save(player.level().registryAccess());
            collectAllPaths(fullTag, "root", paths);

            CompoundTag components = fullTag.getCompound("components");
            if (!components.isEmpty()) {
                collectAllPaths(components, "", paths);
            }
        } catch (Exception ignored) {
        }

        paths.sort(Comparator.naturalOrder());
        return paths;
    }

    @Environment(EnvType.CLIENT)
    public static List<String> extractDisplayPaths(ItemStack previewStack) {
        List<String> all = extractPaths(previewStack);
        Set<String> seen = new LinkedHashSet<>();
        List<String> display = new ArrayList<>();

        for (String path : all) {
            if (getTagAtPath(previewStack, path) == null) {
                continue;
            }
            String canonical = canonicalPathKey(path);
            if (seen.add(canonical)) {
                display.add(path);
            }
        }
        return display;
    }

    private static String canonicalPathKey(String path) {
        if (path.startsWith("root.components.")) {
            return path.substring("root.components.".length());
        }
        if (path.startsWith("root.")) {
            return path.substring("root.".length());
        }
        return path;
    }

    private static void collectAllPaths(CompoundTag tag, String prefix, List<String> paths) {
        for (String key : tag.getAllKeys()) {
            String currentPath = prefix.isEmpty() ? key : prefix + "." + key;
            paths.add(currentPath);

            Tag value = tag.get(key);
            if (value instanceof CompoundTag compound) {
                collectAllPaths(compound, currentPath, paths);
            } else if (value instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    String listPath = currentPath + "[" + i + "]";
                    paths.add(listPath);
                    Tag element = list.get(i);
                    if (element instanceof CompoundTag elementCompound) {
                        collectAllPaths(elementCompound, listPath, paths);
                    }
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public static Tag getTagAtPath(ItemStack previewStack, String path) {
        if (previewStack == null || previewStack.isEmpty() || path == null || path.isBlank()) {
            return null;
        }

        try {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return null;
            }

            CompoundTag fullTag = (CompoundTag) previewStack.save(player.level().registryAccess());

            if (path.startsWith("root.")) {
                return navigateToPath(fullTag, path.substring(5));
            }

            CompoundTag components = fullTag.getCompound("components");
            if (!components.isEmpty()) {
                Tag fromComponents = navigateToPath(components, path);
                if (fromComponents != null) {
                    return fromComponents;
                }
            }

            return navigateToPath(fullTag, path);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Environment(EnvType.CLIENT)
    public static String tagToFilterSnbt(Tag tag) {
        if (tag == null) {
            return "";
        }
        if (tag instanceof CompoundTag compound) {
            return compound.toString();
        }
        if (tag instanceof ListTag list) {
            return list.toString();
        }
        return tag.toString();
    }

    @Environment(EnvType.CLIENT)
    public static String snbtForPath(ItemStack previewStack, String path) {
        return snbtForPathWithValue(previewStack, path, null);
    }

    @Environment(EnvType.CLIENT)
    public static String snbtForPathWithValue(ItemStack previewStack, String path, @Nullable String valueOverride) {
        Tag tag;
        if (valueOverride == null || valueOverride.isBlank()) {
            tag = getTagAtPath(previewStack, path);
        } else {
            tag = parseValueOverride(valueOverride);
        }
        if (tag == null) {
            return "";
        }
        return snbtForItemFilter(path, tag);
    }

    public static String snbtForItemFilter(String path, Tag leafTag) {
        PathTarget target = resolvePathTarget(path);
        CompoundTag built = buildCompoundAtPath(splitPath(target.relativePath), leafTag);

        if (target.wrapInComponents) {
            CompoundTag outer = new CompoundTag();
            outer.put("components", built);
            return outer.toString();
        }

        return built.toString();
    }

    public static Tag parseValueOverride(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            return TagParser.parseTag(trimmed);
        } catch (Exception ignored) {
        }

        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return StringTag.valueOf(trimmed.substring(1, trimmed.length() - 1));
        }

        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return ByteTag.valueOf((byte) (Boolean.parseBoolean(trimmed) ? 1 : 0));
        }

        try {
            if (trimmed.contains(".")) {
                return DoubleTag.valueOf(Double.parseDouble(trimmed));
            }
            return IntTag.valueOf(Integer.parseInt(trimmed));
        } catch (NumberFormatException ignored) {
        }

        return StringTag.valueOf(trimmed);
    }

    @Environment(EnvType.CLIENT)
    public static String pathRowLabel(ItemStack previewStack, String path) {
        return path;
    }

    @Environment(EnvType.CLIENT)
    public static String pathValueLabel(ItemStack previewStack, String path) {
        return formatTagValue(getTagAtPath(previewStack, path));
    }

    @Environment(EnvType.CLIENT)
    public static String defaultEditableValue(ItemStack previewStack, String path) {
        Tag tag = getTagAtPath(previewStack, path);
        if (tag == null) {
            return "";
        }
        if (tag instanceof StringTag stringTag) {
            return "\"" + stringTag.getAsString() + "\"";
        }
        return tag.toString();
    }

    private static CompoundTag buildCompoundAtPath(String[] parts, Tag leafTag) {
        if (parts.length == 0) {
            if (leafTag instanceof CompoundTag compound) {
                return compound.copy();
            }
            CompoundTag wrapper = new CompoundTag();
            wrapper.put("value", leafTag);
            return wrapper;
        }

        CompoundTag built = new CompoundTag();
        CompoundTag current = built;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            int bracket = part.indexOf('[');
            CompoundTag next = new CompoundTag();
            if (bracket >= 0) {
                current.put(part.substring(0, bracket), next);
            } else {
                current.put(part, next);
            }
            current = next;
        }

        String last = parts[parts.length - 1];
        int bracket = last.indexOf('[');
        if (bracket >= 0) {
            ListTag list = new ListTag();
            list.add(leafTag);
            current.put(last.substring(0, bracket), list);
        } else {
            current.put(last, leafTag);
        }

        return built;
    }

    private record PathTarget(String relativePath, boolean wrapInComponents) {}

    private static PathTarget resolvePathTarget(String path) {
        String normalized = path;

        if (normalized.startsWith("root.")) {
            normalized = normalized.substring(5);
        }

        if (normalized.startsWith("components.")) {
            normalized = normalized.substring("components.".length());
        }

        boolean wrapInComponents = !normalized.equals("count") && !normalized.startsWith("count[")
                && !normalized.equals("id") && !normalized.startsWith("id[");

        return new PathTarget(normalized, wrapInComponents);
    }

    public static String formatTagValue(Tag tag) {
        if (tag == null) {
            return "(not found)";
        }
        if (tag instanceof CompoundTag compound) {
            return "compound{" + compound.size() + " keys}";
        }
        if (tag instanceof ListTag list) {
            return "list[" + list.size() + "]";
        }
        if (tag instanceof StringTag stringTag) {
            return "\"" + stringTag.getAsString() + "\"";
        }
        if (tag instanceof NumericTag numericTag) {
            return numericTag.getType().getName() + ":" + numericTag.getAsString();
        }
        return tag.toString();
    }

    public static String[] splitPath(String path) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '.') {
                if (!current.isEmpty()) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts.toArray(String[]::new);
    }

    private static Tag navigateToPath(CompoundTag root, String path) {
        if (path.isEmpty()) {
            return root;
        }

        String[] parts = splitPath(path);
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

    @Environment(EnvType.CLIENT)
    public static @Nullable String inferPathForSnbt(ItemStack previewStack, String snbt) {
        if (previewStack == null || previewStack.isEmpty() || snbt == null || snbt.isBlank()) {
            return null;
        }

        String normalized = snbt.trim();
        for (String path : extractDisplayPaths(previewStack)) {
            String built = snbtForPath(previewStack, path);
            if (normalized.equals(built.trim())) {
                return path;
            }
        }
        return null;
    }

    public static String editableValueFromSnbt(String path, String snbt) {
        if (path == null || path.isBlank() || snbt == null || snbt.isBlank()) {
            return "";
        }

        try {
            Tag parsed = TagParser.parseTag(snbt.trim());
            PathTarget target = resolvePathTarget(path);
            Tag root = parsed;
            if (target.wrapInComponents && parsed instanceof CompoundTag compound && compound.contains("components")) {
                root = compound.getCompound("components");
            }
            if (!(root instanceof CompoundTag compoundRoot)) {
                return "";
            }
            Tag leaf = navigateToPath(compoundRoot, target.relativePath);
            return formatEditableTag(leaf);
        } catch (Exception ignored) {
            return "";
        }
    }

    @Environment(EnvType.CLIENT)
    public static boolean tryRepairColonSplitPath(ItemNbtFilterData.FilterEntry entry, ItemStack previewStack) {
        if (entry.path.isBlank() || previewStack.isEmpty()) {
            return false;
        }
        if (entry.type == ItemNbtFilterData.FilterEntry.Type.KEY_EXISTS
                || entry.type == ItemNbtFilterData.FilterEntry.Type.SNBT) {
            return false;
        }
        if (entry.value == null || entry.value.isBlank()) {
            return false;
        }
        if (isLeafPathValue(entry.path, entry.value, previewStack)) {
            return false;
        }

        boolean repaired = false;
        for (int step = 0; step < 8; step++) {
            int colon = entry.value.indexOf(':');
            if (colon <= 0) {
                break;
            }

            String segment = entry.value.substring(0, colon).trim();
            if (segment.isEmpty()) {
                break;
            }

            String candidatePath = entry.path + ":" + segment;
            if (getTagAtPath(previewStack, candidatePath) == null) {
                break;
            }

            entry.path = candidatePath;
            entry.value = entry.value.substring(colon + 1).trim();
            repaired = true;

            if (isLeafPathValue(entry.path, entry.value, previewStack)) {
                break;
            }
        }

        if (!repaired) {
            repaired = tryRepairFromKnownPaths(entry, previewStack);
        }
        return repaired;
    }

    @Environment(EnvType.CLIENT)
    private static boolean tryRepairFromKnownPaths(ItemNbtFilterData.FilterEntry entry, ItemStack previewStack) {
        String brokenTail = entry.value;
        int valueColon = brokenTail.indexOf(':');
        if (valueColon > 0) {
            brokenTail = brokenTail.substring(0, valueColon).trim();
        }

        for (String knownPath : extractDisplayPaths(previewStack)) {
            if (!knownPath.startsWith(entry.path + ":")) {
                continue;
            }
            if (knownPath.endsWith(brokenTail) || knownPath.equals(entry.path + ":" + brokenTail)) {
                entry.path = knownPath;
                if (valueColon > 0) {
                    entry.value = entry.value.substring(valueColon + 1).trim();
                }
                return true;
            }
        }
        return false;
    }

    @Environment(EnvType.CLIENT)
    private static boolean isLeafPathValue(String path, String value, ItemStack previewStack) {
        Tag tag = getTagAtPath(previewStack, path);
        if (tag == null) {
            return false;
        }
        if (tag instanceof CompoundTag || tag instanceof ListTag) {
            return false;
        }
        return value != null && !value.isBlank();
    }

    private static String formatEditableTag(@Nullable Tag tag) {
        if (tag == null) {
            return "";
        }
        if (tag instanceof StringTag stringTag) {
            return "\"" + stringTag.getAsString() + "\"";
        }
        return tag.toString();
    }
}
