package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.morequesttypes.compat.ReskillableCompat;

import java.util.*;

public final class ReskillableReward extends Reward {
    public enum OperationType { ADD, SET, REDUCE }

    private OperationType operationType = OperationType.ADD;
    private int amount = 1;
    private int skillIndex = 0;

    public ReskillableReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.RESKILLABLE;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        if (! ReskillableCompat.isLoaded()) return;
        if (amount == 0 && operationType != OperationType.SET) return;

        switch (operationType) {
            case ADD -> {
                int current = ReskillableCompat.getSkillLevel(player, skillIndex);
                ReskillableCompat.setSkillLevel(player, skillIndex, current + amount);
            }
            case SET -> {
                ReskillableCompat.setSkillLevel(player, skillIndex, amount);
            }
            case REDUCE -> {
                int current = ReskillableCompat.getSkillLevel(player, skillIndex);
                ReskillableCompat.setSkillLevel(player, skillIndex, Math.max(1, current - amount));
            }
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Component getAltTitle() {
        Map<Integer, String> skills = ReskillableCompat.getAvailableSkills();
        String skillName = skills.getOrDefault(skillIndex, "Unknown");
        String opStr = operationType.name().toLowerCase(Locale.ROOT);

        return Component.translatable(
                "morequesttypes.reward.reskillable.title",
                opStr,
                amount,
                skillName
        );
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        return getSkillIcon(skillIndex, this.getType());
    }

    @Environment(EnvType.CLIENT)
    private static Icon getSkillIcon(int skillIndex, RewardType rewardType) {
        String iconName = switch (skillIndex) {
            case 0 -> "mining";
            case 1 -> "gathering";
            case 2 -> "attack";
            case 3 -> "defense";
            case 4 -> "building";
            case 5 -> "farming";
            case 6 -> "agility";
            case 7 -> "magic";
            default -> null;
        };

        if (iconName != null) {
            try {
                ResourceLocation iconLocation = ResourceLocation.fromNamespaceAndPath("more_quest_types",
                        "textures/reskillable_compat/" + iconName + ".png");
                return Icon.getIcon(iconLocation);
            } catch (Exception e) {
                return rewardType.getIconSupplier();
            }
        }
        return rewardType.getIconSupplier();
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("operation_type", operationType.name());
        nbt.putInt("amount", amount);
        nbt.putInt("skill_index", skillIndex);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        try {
            operationType = OperationType.valueOf(nbt.getString("operation_type"));
        } catch (Throwable ignored) {
            operationType = OperationType.ADD;
        }
        amount = nbt.getInt("amount");
        skillIndex = nbt.getInt("skill_index");

        if (nbt.contains("kind") && ! nbt.contains("operation_type")) {
            try {
                String oldKind = nbt.getString("kind");
                operationType = "SET".equals(oldKind) ? OperationType.SET : OperationType.ADD;
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(operationType);
        buf.writeVarInt(amount);
        buf.writeVarInt(skillIndex);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        operationType = buf.readEnum(OperationType.class);
        amount = buf.readVarInt();
        skillIndex = buf.readVarInt();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var OPERATIONS = NameMap.of(OperationType.ADD, OperationType.values()).create();
        config.addEnum("operation_type", operationType, v -> operationType = v, OPERATIONS)
                .setNameKey("morequesttypes.reward.reskillable.operation_type");

        config.addInt("amount", amount, v -> amount = v, 1, 0, 1_000_000)
                .setNameKey("morequesttypes.reward.reskillable.amount");

        Map<Integer, String> skills = new LinkedHashMap<>();
        skills.putAll(ReskillableCompat.getAvailableSkills());

        if (skills.isEmpty()) {
            skills.put(0, "No Skills Available");
        }

        List<Integer> skillIds = new ArrayList<>(skills.keySet());
        Integer currentSkillId = skillIds.contains(skillIndex) ? skillIndex : (skillIds.isEmpty() ? 0 : skillIds.get(0));

        RewardType rewardType = this.getType();
        var SKILL_MAP = NameMap.of(currentSkillId, skillIds.toArray(Integer[]::new))
                .name(id -> Component.literal(skills.getOrDefault(id, "Unknown (" + id + ")")))
                .icon(id -> getSkillIcon(id, rewardType))
                .create();

        config.addEnum("skill", currentSkillId, v -> skillIndex = v, SKILL_MAP)
                .setNameKey("morequesttypes.reward.reskillable.skill");
    }
}