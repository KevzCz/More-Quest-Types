package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
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

import java.util.ArrayList;
import java.util.List;

public final class NbtPathReward extends Reward {
    public enum Operation { ADD, REMOVE, SET }
    public enum TargetSlot {
        MAINHAND, OFFHAND, HEAD, CHEST, LEGS, FEET
    }

    private Operation operation = Operation.ADD;
    private String path = "";
    private boolean checkExists = false;
    private final List<String> nbtEntries = new ArrayList<>();
    private TargetSlot targetSlot = TargetSlot.MAINHAND;
    private ItemStack targetItem = ItemStack.EMPTY;

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
        List<ItemStack> targets = getTargetStacks(player);

        for (ItemStack stack : targets) {
            if (stack.isEmpty()) continue;
            if (!targetItem.isEmpty() && !ItemStack.isSameItemSameComponents(stack, targetItem)) continue;

            try {
                CompoundTag fullTag = (CompoundTag) stack.save(player.registryAccess());

                CompoundTag components = fullTag.getCompound("components");

                if (checkExists && !pathExists(components, path)) continue;

                switch (operation) {
                    case ADD -> addToPath(components, path, nbtEntries);
                    case REMOVE -> removeFromPath(components, path, nbtEntries);
                    case SET -> setAtPath(components, path, nbtEntries);
                }

                fullTag.put("components", components);

                ItemStack newStack = ItemStack.parseOptional(player.registryAccess(), fullTag);
                stack.applyComponents(newStack.getComponents());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        String[] parts = path.split("\\.");
        Tag current = root;

        for (String part : parts) {
            if (current instanceof CompoundTag compound) {
                if (!compound.contains(part)) return false;
                current = compound.get(part);
            } else {
                return false;
            }
        }

        return true;
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

        try {
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

        var OPS = NameMap.of(Operation.ADD, Operation.values()).create();
        config.addEnum("operation", operation, v -> operation = v, OPS)
                .setNameKey("morequesttypes.reward.nbt_path.operation");

        config.addString("path", path, v -> path = v, "")
                .setNameKey("morequesttypes.reward.nbt_path.path");

        config.addBool("check_exists", checkExists, v -> checkExists = v, false)
                .setNameKey("morequesttypes.reward.nbt_path.check_exists");

        config.addList("nbt_entries", nbtEntries, new dev.ftb.mods.ftblibrary.config.StringConfig(), "")
                .setNameKey("morequesttypes.reward.nbt_path.nbt_entries");

        var SLOTS = NameMap.of(TargetSlot.MAINHAND, TargetSlot.values()).create();
        config.addEnum("target_slot", targetSlot, v -> targetSlot = v, SLOTS)
                .setNameKey("morequesttypes.reward.nbt_path.target_slot");

        dev.ftb.mods.ftbquests.client.ConfigIconItemStack cis = new dev.ftb.mods.ftbquests.client.ConfigIconItemStack();
        config.add("target_item", cis, targetItem, v -> {
            targetItem = v.copy();
            if (!targetItem.isEmpty()) targetItem.setCount(1);
        }, ItemStack.EMPTY).setNameKey("morequesttypes.reward.nbt_path.target_item");
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

        nbt.putString("target_slot", targetSlot.name());
        if (!targetItem.isEmpty()) nbt.put("target_item", targetItem.save(provider));
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

        try {
            targetSlot = TargetSlot.valueOf(nbt.getString("target_slot"));
        } catch (Throwable ignored) {
            targetSlot = TargetSlot.MAINHAND;
        }

        targetItem = nbt.contains("target_item")
                ? ItemStack.parseOptional(provider, nbt.getCompound("target_item"))
                : ItemStack.EMPTY;

        if (!targetItem.isEmpty()) targetItem.setCount(1);
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(operation);
        buf.writeUtf(path);
        buf.writeBoolean(checkExists);
        buf.writeVarInt(nbtEntries.size());
        for (String s : nbtEntries) buf.writeUtf(s);
        buf.writeEnum(targetSlot);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, targetItem);
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
        targetSlot = buf.readEnum(TargetSlot.class);
        targetItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        if (!targetItem.isEmpty()) targetItem.setCount(1);
    }
}