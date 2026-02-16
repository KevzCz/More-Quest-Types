package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.config.NbtPathRewardConfig;
import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class NbtPathReward extends Reward {
    public enum Operation { ADD, REMOVE, SET }
    public enum TargetSlot { MAINHAND, OFFHAND, HEAD, CHEST, LEGS, FEET }
    public enum ConditionType {
        NONE,
        PATH_EXISTS,
        PATH_NOT_EXISTS,
        ENTRY_EXISTS,
        ENTRY_NOT_EXISTS,
        VALUE_EQUALS,
        VALUE_NOT_EQUALS,
        VALUE_COMPARE
    }

    private Operation operation = Operation.ADD;
    private String path = "";
    private boolean checkExists = false;
    private final List<String> nbtEntries = new ArrayList<>();
    private TargetSlot targetSlot = TargetSlot.MAINHAND;
    private ItemStack targetItem = ItemStack.EMPTY;
    private boolean isValue = false;

    private ConditionType conditionType = ConditionType.NONE;
    private String conditionPath = "";
    private final List<String> conditionEntries = new ArrayList<>();
    private int conditionsMatchNumber = -1;
    private ComparisonMode comparisonMode = ComparisonMode.EQUALS;
    private int comparisonFirst = 0;
    private int comparisonSecond = 0;

    public NbtPathReward(long id, Quest q) {
        super(id, q);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.NBT_PATH;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        if (path.isEmpty()) return;
        applyToItems(player);
    }

    private void applyToItems(ServerPlayer player) {


        UUID claimerUuid = player.getUUID();
        String claimerName = player.getGameProfile().getName();

        ItemStack stack = switch (targetSlot) {
            case MAINHAND -> player.getMainHandItem();
            case OFFHAND -> player.getOffhandItem();
            case HEAD -> player.getItemBySlot(EquipmentSlot.HEAD);
            case CHEST -> player.getItemBySlot(EquipmentSlot.CHEST);
            case LEGS -> player.getItemBySlot(EquipmentSlot.LEGS);
            case FEET -> player.getItemBySlot(EquipmentSlot.FEET);
        };

        if (stack.isEmpty()) {
            return;
        }


        if (!targetItem.isEmpty() && !ItemStack.isSameItemSameComponents(stack, targetItem)) {
            return;
        }

        try {
            CompoundTag fullTag = (CompoundTag) stack.save(player.registryAccess());

            boolean isRootPath = path.startsWith("root.");
            String actualPath = isRootPath ? path.substring(5) : path;
            CompoundTag targetTag = isRootPath ? fullTag : fullTag.getCompound("components");


            if (checkExists && !pathExists(targetTag, actualPath)) {
                return;
            }

            boolean isConditionRoot = conditionPath.startsWith("root.");
            String actualConditionPath = isConditionRoot ? conditionPath.substring(5) : conditionPath;
            CompoundTag conditionTag = isConditionRoot ? fullTag : fullTag.getCompound("components");

            if (!checkCondition(conditionTag, actualConditionPath, player, claimerUuid, claimerName)) {
                return;
            }

            List<String> processedEntries = processPlaceholders(nbtEntries, claimerUuid, claimerName, isTeamReward());

            if (isValue) {
                switch (operation) {
                    case ADD -> addValue(targetTag, actualPath, processedEntries);
                    case REMOVE -> subtractValue(targetTag, actualPath, processedEntries);
                    case SET -> setValue(targetTag, actualPath, processedEntries);
                }
            } else {
                switch (operation) {
                    case ADD -> addToPath(targetTag, actualPath, processedEntries);
                    case REMOVE -> removeFromPath(targetTag, actualPath, processedEntries);
                    case SET -> setAtPath(targetTag, actualPath, processedEntries);
                }
            }

            if (!isRootPath) {
                fullTag.put("components", targetTag);
            }


            ItemStack newStack = ItemStack.parseOptional(player.registryAccess(), fullTag);

            switch (targetSlot) {
                case MAINHAND -> player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, newStack);
                case OFFHAND -> player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, newStack);
                case HEAD -> player.setItemSlot(EquipmentSlot.HEAD, newStack);
                case CHEST -> player.setItemSlot(EquipmentSlot.CHEST, newStack);
                case LEGS -> player.setItemSlot(EquipmentSlot.LEGS, newStack);
                case FEET -> player.setItemSlot(EquipmentSlot.FEET, newStack);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private void addValue(CompoundTag root, String path, List<String> entries) {
        if (entries.isEmpty()) return;

        String[] parts = path.split("\\.");
        CompoundTag current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!current.contains(part)) {
                current.put(part, new CompoundTag());
            }
            Tag tag = current.get(part);
            if (tag instanceof CompoundTag compound) {
                current = compound;
            } else {
                return;
            }
        }

        String lastPart = parts[parts.length - 1];
        Tag existing = current.get(lastPart);
        Tag addTag = parseNbtEntry(entries.get(0));

        if (addTag instanceof NumericTag addNum) {
            if (existing instanceof NumericTag existingNum) {
                double newValue = existingNum.getAsDouble() + addNum.getAsDouble();
                current.put(lastPart, createNumericTag(newValue, existing));
            } else {
                current.put(lastPart, addTag);
            }
        }
    }

    private void subtractValue(CompoundTag root, String path, List<String> entries) {
        if (entries.isEmpty()) return;

        String[] parts = path.split("\\.");
        Tag current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            if (current instanceof CompoundTag compound) {
                current = compound.get(parts[i]);
                if (current == null) return;
            } else {
                return;
            }
        }

        if (!(current instanceof CompoundTag compound)) return;

        String lastPart = parts[parts.length - 1];
        Tag existing = compound.get(lastPart);
        Tag subtractTag = parseNbtEntry(entries.get(0));

        if (subtractTag instanceof NumericTag subNum && existing instanceof NumericTag existingNum) {
            double newValue = existingNum.getAsDouble() - subNum.getAsDouble();
            compound.put(lastPart, createNumericTag(newValue, existing));
        }
    }

    private void setValue(CompoundTag root, String path, List<String> entries) {
        if (entries.isEmpty()) {
            return;
        }


        String[] parts = path.split("\\.");
        CompoundTag current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!current.contains(part)) {
                current.put(part, new CompoundTag());
            }
            Tag tag = current.get(part);
            if (tag instanceof CompoundTag compound) {
                current = compound;
            } else {
                return;
            }
        }

        String lastPart = parts[parts.length - 1];
        String entry = entries.get(0).trim();

        Tag existing = current.get(lastPart);

        if (existing instanceof StringTag) {
            // If entry doesn't look like complex NBT (no braces, brackets), treat as plain string
            if (!entry.startsWith("{") && !entry.startsWith("[")) {
                // Remove surrounding quotes if present
                if (entry.startsWith("\"") && entry.endsWith("\"") && entry.length() > 1) {
                    entry = entry.substring(1, entry.length() - 1);
                }
                current.putString(lastPart, entry);

                return;
            }
        }

        Tag valueTag = parseNbtEntry(entry);

        current.put(lastPart, valueTag);
    }

    private Tag createNumericTag(double value, Tag originalType) {
        if (originalType instanceof ByteTag) {
            return ByteTag.valueOf((byte) value);
        } else if (originalType instanceof ShortTag) {
            return ShortTag.valueOf((short) value);
        } else if (originalType instanceof IntTag) {
            return IntTag.valueOf((int) value);
        } else if (originalType instanceof LongTag) {
            return LongTag.valueOf((long) value);
        } else if (originalType instanceof FloatTag) {
            return FloatTag.valueOf((float) value);
        } else if (originalType instanceof DoubleTag) {
            return DoubleTag.valueOf(value);
        }
        return IntTag.valueOf((int) value);
    }

    private List<String> processPlaceholders(List<String> entries, UUID claimerUuid, String claimerName, boolean isTeamReward) {
        if (!isTeamReward) return entries;

        List<String> processed = new ArrayList<>();
        for (String entry : entries) {
            String result = entry
                    .replace("{claimer_uuid}", claimerUuid.toString())
                    .replace("{claimer_name}", claimerName)
                    .replace("{claimer_uuid_array}", uuidToIntArray(claimerUuid));
            processed.add(result);
        }
        return processed;
    }

    private String uuidToIntArray(UUID uuid) {
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();

        int[] ints = new int[4];
        ints[0] = (int) (mostSigBits >> 32);
        ints[1] = (int) mostSigBits;
        ints[2] = (int) (leastSigBits >> 32);
        ints[3] = (int) leastSigBits;

        return "[I;" + ints[0] + "," + ints[1] + "," + ints[2] + "," + ints[3] + "]";
    }

    private boolean checkCondition(CompoundTag conditionTag, String actualConditionPath, ServerPlayer player, UUID claimerUuid, String claimerName) {
        List<String> processedConditionEntries = processPlaceholders(conditionEntries, claimerUuid, claimerName, isTeamReward());

        return switch (conditionType) {
            case NONE -> true;
            case PATH_EXISTS -> pathExists(conditionTag, actualConditionPath);
            case PATH_NOT_EXISTS -> !pathExists(conditionTag, actualConditionPath);
            case ENTRY_EXISTS -> checkEntriesMatch(conditionTag, actualConditionPath, processedConditionEntries, true);
            case ENTRY_NOT_EXISTS -> checkEntriesMatch(conditionTag, actualConditionPath, processedConditionEntries, false);
            case VALUE_EQUALS -> checkValuesMatch(conditionTag, actualConditionPath, processedConditionEntries, true);
            case VALUE_NOT_EQUALS -> checkValuesMatch(conditionTag, actualConditionPath, processedConditionEntries, false);
            case VALUE_COMPARE -> checkValueCompare(conditionTag, actualConditionPath);
        };
    }

    private boolean checkValueCompare(CompoundTag components, String condPath) {
        Tag actual = navigateToPath(components, condPath);
        if (actual == null) return false;

        if (actual instanceof NumericTag numTag) {
            int value = numTag.getAsInt();
            return comparisonMode.compare(value, comparisonFirst, comparisonSecond);
        }

        return false;
    }

    private boolean checkEntriesMatch(CompoundTag components, String condPath, List<String> processedEntries, boolean shouldExist) {
        if (processedEntries.isEmpty()) return true;

        int matchCount = 0;
        int requiredMatches = (conditionsMatchNumber == -1) ? processedEntries.size() : conditionsMatchNumber;

        for (String entrySnbt : processedEntries) {
            boolean exists = entryExists(components, condPath, entrySnbt);
            if (shouldExist && exists) matchCount++;
            if (!shouldExist && !exists) matchCount++;
        }

        return matchCount >= requiredMatches;
    }

    private boolean checkValuesMatch(CompoundTag components, String condPath, List<String> processedEntries, boolean shouldEqual) {
        if (processedEntries.isEmpty()) return true;

        int matchCount = 0;
        int requiredMatches = (conditionsMatchNumber == -1) ? processedEntries.size() : conditionsMatchNumber;

        for (String expectedSnbt : processedEntries) {
            boolean equals = valueEquals(components, condPath, expectedSnbt);
            if (shouldEqual && equals) matchCount++;
            if (!shouldEqual && !equals) matchCount++;
        }

        return matchCount >= requiredMatches;
    }

    private boolean entryExists(CompoundTag root, String path, String entrySnbt) {
        if (path.isEmpty() || entrySnbt.isEmpty()) return false;

        Tag target = navigateToPath(root, path);
        if (target == null) return false;

        Tag searchTag = parseNbtEntry(entrySnbt);
        if (searchTag == null) return false;

        if (target instanceof ListTag list) {
            return containsTag(list, searchTag);
        } else if (target instanceof CompoundTag compound) {
            if (searchTag instanceof CompoundTag searchCompound) {
                for (String key : searchCompound.getAllKeys()) {
                    Tag value = compound.get(key);
                    if (value == null || !tagsEqual(value, searchCompound.get(key))) {
                        return false;
                    }
                }
                return true;
            }
        }

        return false;
    }

    private boolean valueEquals(CompoundTag root, String path, String expectedSnbt) {
        if (path.isEmpty() || expectedSnbt.isEmpty()) return false;

        Tag actual = navigateToPath(root, path);
        if (actual == null) return false;

        Tag expected = parseNbtEntry(expectedSnbt);
        if (expected == null) return false;

        if (actual instanceof NumericTag actualNum && expected instanceof NumericTag expectedNum) {
            return Math.abs(actualNum.getAsDouble() - expectedNum.getAsDouble()) < 0.0001;
        }

        return tagsEqual(actual, expected);
    }

    private Tag navigateToPath(CompoundTag root, String path) {
        if (path.isEmpty()) return root;

        String[] parts = path.split("\\.");
        Tag current = root;

        for (String part : parts) {
            if (current instanceof CompoundTag compound) {
                if (compound.contains(part)) {
                    current = compound.get(part);
                } else {
                    return null;
                }

                if (current == null) return null;
            } else {
                return null;
            }
        }

        return current;
    }

    private List<ItemStack> getTargetStacks(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>();

        switch (targetSlot) {
            case MAINHAND -> stacks.add(player.getMainHandItem());
            case OFFHAND -> stacks.add(player.getOffhandItem());
            case HEAD -> stacks.add(player.getItemBySlot(EquipmentSlot.HEAD));
            case CHEST -> stacks.add(player.getItemBySlot(EquipmentSlot.CHEST));
            case LEGS -> stacks.add(player.getItemBySlot(EquipmentSlot.LEGS));
            case FEET -> stacks.add(player.getItemBySlot(EquipmentSlot.FEET));
        }

        return stacks;
    }

    private boolean pathExists(CompoundTag root, String path) {
        return navigateToPath(root, path) != null;
    }

    private void addToPath(CompoundTag root, String path, List<String> entries) {
        String[] parts = path.split("\\.");
        CompoundTag current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!current.contains(part)) {
                current.put(part, new CompoundTag());
            }
            Tag tag = current.get(part);
            if (tag instanceof CompoundTag compound) {
                current = compound;
            } else {
                return;
            }
        }

        String lastPart = parts[parts.length - 1];

        if (!current.contains(lastPart)) {
            current.put(lastPart, new ListTag());
        }

        Tag existing = current.get(lastPart);
        if (existing instanceof ListTag list) {
            for (String entry : entries) {
                Tag parsed = parseNbtEntry(entry);
                if (parsed != null && !containsTag(list, parsed)) {
                    list.add(parsed);
                }
            }
        } else if (existing instanceof CompoundTag) {
            for (String entry : entries) {
                Tag parsed = parseNbtEntry(entry);
                if (parsed instanceof CompoundTag compound) {
                    for (String key : compound.getAllKeys()) {
                        current.getCompound(lastPart).put(key, compound.get(key));
                    }
                }
            }
        }
    }

    private void removeFromPath(CompoundTag root, String path, List<String> entries) {
        String[] parts = path.split("\\.");
        Tag current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            if (current instanceof CompoundTag compound) {
                current = compound.get(parts[i]);
                if (current == null) return;
            } else {
                return;
            }
        }

        String lastPart = parts[parts.length - 1];

        if (current instanceof CompoundTag compound) {
            Tag target = compound.get(lastPart);

            if (target instanceof ListTag list) {
                for (String entry : entries) {
                    Tag parsed = parseNbtEntry(entry);
                    if (parsed != null) {
                        list.removeIf(tag -> tagsEqual(tag, parsed));
                    }
                }
            } else if (entries.isEmpty()) {
                compound.remove(lastPart);
            }
        }
    }

    private void setAtPath(CompoundTag root, String path, List<String> entries) {
        String[] parts = path.split("\\.");
        CompoundTag current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!current.contains(part)) {
                current.put(part, new CompoundTag());
            }
            Tag tag = current.get(part);
            if (tag instanceof CompoundTag compound) {
                current = compound;
            } else {
                return;
            }
        }

        String lastPart = parts[parts.length - 1];

        ListTag list = new ListTag();
        for (String entry : entries) {
            Tag parsed = parseNbtEntry(entry);
            if (parsed != null) {
                list.add(parsed);
            }
        }
        current.put(lastPart, list);
    }

    private Tag parseNbtEntry(String entry) {
        if (entry == null || entry.isEmpty()) return null;

        entry = entry.trim();

        try {
            if (entry.equalsIgnoreCase("true")) {
                return ByteTag.valueOf(true);
            }
            if (entry.equalsIgnoreCase("false")) {
                return ByteTag.valueOf(false);
            }

            if (entry.matches("-?\\d+")) {
                return IntTag.valueOf(Integer.parseInt(entry));
            }

            if (entry.matches("-?\\d+b") || entry.matches("-?\\d+B")) {
                return ByteTag.valueOf(Byte.parseByte(entry.substring(0, entry.length() - 1)));
            }

            if (entry.matches("-?\\d+s") || entry.matches("-?\\d+S")) {
                return ShortTag.valueOf(Short.parseShort(entry.substring(0, entry.length() - 1)));
            }

            if (entry.matches("-?\\d+L") || entry.matches("-?\\d+l")) {
                return LongTag.valueOf(Long.parseLong(entry.substring(0, entry.length() - 1)));
            }

            if (entry.matches("-?\\d*\\.\\d+f") || entry.matches("-?\\d*\\.\\d+F")) {
                return FloatTag.valueOf(Float.parseFloat(entry.substring(0, entry.length() - 1)));
            }

            if (entry.matches("-?\\d*\\.\\d+d") || entry.matches("-?\\d*\\.\\d+D") || entry.matches("-?\\d*\\.\\d+")) {
                String numberPart = entry;
                if (entry.toLowerCase().endsWith("d")) {
                    numberPart = entry.substring(0, entry.length() - 1);
                }
                return DoubleTag.valueOf(Double.parseDouble(numberPart));
            }

            return TagParser.parseTag(entry);
        } catch (Exception e) {
            return StringTag.valueOf(entry);
        }
    }

    private boolean containsTag(ListTag list, Tag tag) {
        for (Tag existing : list) {
            if (tagsEqual(existing, tag)) return true;
        }
        return false;
    }

    private boolean tagsEqual(Tag a, Tag b) {
        return NbtUtils.compareNbt(a, b, true);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Component getAltTitle() {
        return Component.translatable(
                "morequesttypes.reward.nbt_path.title",
                operation.name().toLowerCase(),
                path.isEmpty() ? "?" : path
        );
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        NbtPathRewardConfig.NbtPathData data = new NbtPathRewardConfig.NbtPathData();
        data.operation = this.operation;
        data.path = this.path;
        data.checkExists = this.checkExists;
        data.nbtEntries = new ArrayList<>(this.nbtEntries);
        data.targetSlot = this.targetSlot;
        data.targetItem = this.targetItem.copy();
        data.isValue = this.isValue;
        data.conditionType = this.conditionType;
        data.conditionPath = this.conditionPath;
        data.conditionEntries = new ArrayList<>(this.conditionEntries);
        data.conditionsMatchNumber = this.conditionsMatchNumber;
        data.comparisonMode = this.comparisonMode;
        data.comparisonFirst = this.comparisonFirst;
        data.comparisonSecond = this.comparisonSecond;

        NbtPathRewardConfig configValue = new NbtPathRewardConfig();
        config.add("config", configValue, data, newData -> {
            this.operation = newData.operation;
            this.path = newData.path;
            this.checkExists = newData.checkExists;
            this.nbtEntries.clear();
            this.nbtEntries.addAll(newData.nbtEntries);
            this.targetSlot = newData.targetSlot;
            this.targetItem = newData.targetItem.copy();
            this.isValue = newData.isValue;
            this.conditionType = newData.conditionType;
            this.conditionPath = newData.conditionPath;
            this.conditionEntries.clear();
            this.conditionEntries.addAll(newData.conditionEntries);
            this.conditionsMatchNumber = newData.conditionsMatchNumber;
            this.comparisonMode = newData.comparisonMode;
            this.comparisonFirst = newData.comparisonFirst;
            this.comparisonSecond = newData.comparisonSecond;
        }, data).setNameKey("morequesttypes.reward.nbt_path.config");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("operation", operation.name());
        nbt.putString("path", path);
        nbt.putBoolean("check_exists", checkExists);

        if (!nbtEntries.isEmpty()) {
            ListTag list = new ListTag();
            for (String s : nbtEntries) list.add(StringTag.valueOf(s));
            nbt.put("nbt_entries", list);
        }

        nbt.putBoolean("is_value", isValue);
        nbt.putString("target_slot", targetSlot.name());
        if (!targetItem.isEmpty()) nbt.put("target_item", targetItem.save(provider));

        nbt.putString("condition_type", conditionType.name());
        nbt.putString("condition_path", conditionPath);

        if (!conditionEntries.isEmpty()) {
            ListTag ceList = new ListTag();
            for (String s : conditionEntries) ceList.add(StringTag.valueOf(s));
            nbt.put("condition_entries", ceList);
        }

        nbt.putInt("conditions_match_number", conditionsMatchNumber);
        nbt.putString("comparison_mode", comparisonMode.name());
        nbt.putInt("comparison_first", comparisonFirst);
        nbt.putInt("comparison_second", comparisonSecond);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);

        try {
            operation = Operation.valueOf(nbt.getString("operation"));
        } catch (Throwable ignored) {
            operation = Operation.ADD;
        }

        path = nbt.getString("path");
        checkExists = nbt.getBoolean("check_exists");

        nbtEntries.clear();
        ListTag list = nbt.getList("nbt_entries", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            nbtEntries.add(list.getString(i));
        }

        isValue = nbt.getBoolean("is_value");

        try {
            targetSlot = TargetSlot.valueOf(nbt.getString("target_slot"));
        } catch (Throwable ignored) {
            targetSlot = TargetSlot.MAINHAND;
        }

        targetItem = nbt.contains("target_item")
                ? ItemStack.parseOptional(provider, nbt.getCompound("target_item"))
                : ItemStack.EMPTY;

        if (!targetItem.isEmpty()) targetItem.setCount(1);

        try {
            conditionType = ConditionType.valueOf(nbt.getString("condition_type"));
        } catch (Throwable ignored) {
            conditionType = ConditionType.NONE;
        }

        conditionPath = nbt.getString("condition_path");

        conditionEntries.clear();
        ListTag ceList = nbt.getList("condition_entries", Tag.TAG_STRING);
        for (int i = 0; i < ceList.size(); i++) {
            conditionEntries.add(ceList.getString(i));
        }

        conditionsMatchNumber = nbt.contains("conditions_match_number") ? nbt.getInt("conditions_match_number") : -1;

        try {
            comparisonMode = ComparisonMode.valueOf(nbt.getString("comparison_mode"));
        } catch (Throwable ignored) {
            comparisonMode = ComparisonMode.EQUALS;
        }

        comparisonFirst = nbt.getInt("comparison_first");
        comparisonSecond = nbt.getInt("comparison_second");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(operation);
        buf.writeUtf(path);
        buf.writeBoolean(checkExists);
        buf.writeVarInt(nbtEntries.size());
        for (String s : nbtEntries) buf.writeUtf(s);
        buf.writeBoolean(isValue);
        buf.writeEnum(targetSlot);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, targetItem);

        buf.writeEnum(conditionType);
        buf.writeUtf(conditionPath);
        buf.writeVarInt(conditionEntries.size());
        for (String s : conditionEntries) buf.writeUtf(s);
        buf.writeVarInt(conditionsMatchNumber);
        buf.writeEnum(comparisonMode);
        buf.writeVarInt(comparisonFirst);
        buf.writeVarInt(comparisonSecond);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        operation = buf.readEnum(Operation.class);
        path = buf.readUtf();
        checkExists = buf.readBoolean();
        nbtEntries.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) nbtEntries.add(buf.readUtf());
        isValue = buf.readBoolean();
        targetSlot = buf.readEnum(TargetSlot.class);
        targetItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        if (!targetItem.isEmpty()) targetItem.setCount(1);

        conditionType = buf.readEnum(ConditionType.class);
        conditionPath = buf.readUtf();
        conditionEntries.clear();
        int ceSize = buf.readVarInt();
        for (int i = 0; i < ceSize; i++) conditionEntries.add(buf.readUtf());
        conditionsMatchNumber = buf.readVarInt();
        comparisonMode = buf.readEnum(ComparisonMode.class);
        comparisonFirst = buf.readVarInt();
        comparisonSecond = buf.readVarInt();
    }

    @Override
    public boolean getExcludeFromClaimAll() {
        return true;
    }

    @Override
    public boolean isClaimAllHardcoded() {
        return true;
    }
}