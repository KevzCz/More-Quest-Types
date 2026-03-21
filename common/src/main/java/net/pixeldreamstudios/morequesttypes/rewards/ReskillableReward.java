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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ReskillableReward extends Reward {
    public enum OperationType {ADD, SET, REDUCE}

    private OperationType operationType = OperationType.ADD;
    private int amount = 1;
    private String skillId = "mining";

    public ReskillableReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.RESKILLABLE;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        if (!ReskillableCompat.isLoaded()) return;
        if (amount == 0 && operationType != OperationType.SET) return;

        switch (operationType) {
            case ADD -> {
                int current = ReskillableCompat.getSkillLevel(player, skillId);
                ReskillableCompat.setSkillLevel(player, skillId, current + amount);
            }
            case SET -> {
                ReskillableCompat.setSkillLevel(player, skillId, amount);
            }
            case REDUCE -> {
                int current = ReskillableCompat.getSkillLevel(player, skillId);
                ReskillableCompat.setSkillLevel(player, skillId, Math.max(1, current - amount));
            }
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Component getAltTitle() {
        Map<String, String> skills = ReskillableCompat.getAllSkills();
        String skillName = skills.getOrDefault(skillId, skillId);
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
        return ReskillableReward.getSkillIcon(skillId, getType());
    }

    @Environment(EnvType.CLIENT)
    private static Icon getSkillIcon(String skillId, RewardType rewardType) {
        if (skillId == null || skillId.isEmpty()) {
            return rewardType.getIconSupplier();
        }

        String normalized = skillId.toLowerCase(Locale.ROOT);
        String iconName = switch (normalized) {
            case "mining" -> "mining";
            case "gathering" -> "gathering";
            case "attack" -> "attack";
            case "defense" -> "defense";
            case "building" -> "building";
            case "farming" -> "farming";
            case "agility" -> "agility";
            case "magic" -> "magic";
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

        if (ReskillableCompat.isLoaded()) {
            try {
                String customIconPath = ReskillableCompat.getSkillIcon(skillId);
                if (customIconPath != null && !customIconPath.isEmpty()) {
                    if (customIconPath.contains(":")) {
                        String[] parts = customIconPath.split(":", 2);
                        String namespace = parts[0];
                        String path = parts[1];
                        if (path.startsWith("textures/") && path.endsWith(".png")) {
                            ResourceLocation iconLocation = ResourceLocation.fromNamespaceAndPath(namespace, path);
                            return Icon.getIcon(iconLocation);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return rewardType.getIconSupplier();
    }


    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("operation_type", operationType.name());
        nbt.putInt("amount", amount);
        nbt.putString("skill_id", skillId);
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

        if (nbt.contains("skill_id")) {
            skillId = nbt.getString("skill_id");
        } else if (nbt.contains("skill_index")) {
            int oldIndex = nbt.getInt("skill_index");
            skillId = switch (oldIndex) {
                case 0 -> "mining";
                case 1 -> "gathering";
                case 2 -> "attack";
                case 3 -> "defense";
                case 4 -> "building";
                case 5 -> "farming";
                case 6 -> "agility";
                case 7 -> "magic";
                default -> "mining";
            };
        } else {
            skillId = "mining";
        }

        if (nbt.contains("kind") && !nbt.contains("operation_type")) {
            try {
                String oldKind = nbt.getString("kind");
                operationType = "SET".equals(oldKind) ? OperationType.SET : OperationType.ADD;
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(operationType);
        buf.writeVarInt(amount);
        buf.writeUtf(skillId);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        operationType = buf.readEnum(OperationType.class);
        amount = buf.readVarInt();
        skillId = buf.readUtf();
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

        Map<String, String> skills = new LinkedHashMap<>();
        skills.putAll(ReskillableCompat.getAllSkills());

        if (skills.isEmpty()) {
            skills.put("none", "No Skills Available");
        }

        List<String> skillIds = new ArrayList<>(skills.keySet());
        String currentSkillId = skillIds.contains(skillId) ? skillId : (skillIds.isEmpty() ? "none" : skillIds.get(0));

        RewardType rewardType = getType();
        var SKILL_MAP = NameMap.of(currentSkillId, skillIds.toArray(String[]::new))
                .name(id -> Component.literal(skills.getOrDefault(id, id)))
                .icon(id -> ReskillableReward.getSkillIcon(id, rewardType))
                .create();

        config.addEnum("skill", currentSkillId, v -> skillId = v, SKILL_MAP)
                .setNameKey("morequesttypes.reward.reskillable.skill");
    }
}