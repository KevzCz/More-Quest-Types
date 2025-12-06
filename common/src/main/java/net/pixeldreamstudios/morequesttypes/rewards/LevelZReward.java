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
import net.pixeldreamstudios.morequesttypes.compat.LevelZCompat;

import java.util.*;

public final class LevelZReward extends Reward {
    public enum Kind { EXPERIENCE, LEVEL, SKILL_POINT }
    public enum OperationType { ADD, SET, REDUCE }

    private Kind kind = Kind.EXPERIENCE;
    private OperationType operationType = OperationType.ADD;
    private int amount = 0;
    private int skillId = -1;

    public LevelZReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.LEVELZ;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        if (!LevelZCompat.isLoaded()) return;
        if (amount == 0 && operationType != OperationType.SET) return;

        switch (kind) {
            case EXPERIENCE -> {
                switch (operationType) {
                    case ADD -> LevelZCompat.addExperience(player, amount);
                    case SET -> LevelZCompat.setExperience(player, amount);
                    case REDUCE -> {
                        int current = LevelZCompat.getTotalExperience(player);
                        LevelZCompat.setExperience(player, Math.max(0, current - amount));
                    }
                }
            }
            case LEVEL -> {
                if (skillId == -1) {
                    int currentLevel = LevelZCompat.getLevel(player);
                    int newLevel = switch (operationType) {
                        case ADD -> currentLevel + amount;
                        case SET -> amount;
                        case REDUCE -> Math.max(0, currentLevel - amount);
                    };
                    LevelZCompat.setLevel(player, newLevel);
                } else {
                    int currentLevel = LevelZCompat.getSkillLevel(player, skillId);
                    int newLevel = switch (operationType) {
                        case ADD -> currentLevel + amount;
                        case SET -> amount;
                        case REDUCE -> Math.max(0, currentLevel - amount);
                    };
                    LevelZCompat.setSkillLevel(player, skillId, newLevel);
                }
            }
            case SKILL_POINT -> {

                int currentPoints = LevelZCompat.getSkillPoints(player);
                int newPoints = switch (operationType) {
                    case ADD -> currentPoints + amount;
                    case SET -> amount;
                    case REDUCE -> Math.max(0, currentPoints - amount);
                };
                LevelZCompat.setSkillPoints(player, newPoints);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Component getAltTitle() {
        Map<Integer, String> skills = new LinkedHashMap<>();
        skills.put(-1, "player_level");
        skills.putAll(LevelZCompat.getAvailableSkills());

        String skillName = skillId == -1 ? "Player Level" : skills.getOrDefault(skillId, "Unknown");
        String kindStr = kind.name().toLowerCase(Locale.ROOT);
        String opStr = operationType.name().toLowerCase(Locale.ROOT);

        return Component.translatable(
                "morequesttypes.reward.levelz.title",
                opStr,
                amount,
                kindStr,
                skillName
        );
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        if (kind == Kind.EXPERIENCE) {
            return getType().getIconSupplier();
        }

        if (kind == Kind.SKILL_POINT) {
            return Icon.getIcon(ResourceLocation.fromNamespaceAndPath("levelz", "textures/gui/sprites/skill_point.png"));
        }

        if (skillId == -1) {
            return Icon.getIcon(ResourceLocation.fromNamespaceAndPath("levelz", "icon.png"));
        }

        Map<Integer, String> skills = LevelZCompat.getAvailableSkills();
        String skillKey = skills.get(skillId);

        if (skillKey != null) {
            try {
                ResourceLocation iconLocation = ResourceLocation.fromNamespaceAndPath("levelz",
                        "textures/gui/sprites/" + skillKey + ".png");
                return Icon.getIcon(iconLocation);
            } catch (Exception e) {
                return getType().getIconSupplier();
            }
        }

        return getType().getIconSupplier();
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("kind", kind.name());
        nbt.putString("operation_type", operationType.name());
        nbt.putInt("amount", amount);
        nbt.putInt("skill_id", skillId);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        try {
            kind = Kind.valueOf(nbt.getString("kind"));
        } catch (Throwable ignored) {
            kind = Kind.EXPERIENCE;
        }
        try {
            operationType = OperationType.valueOf(nbt.getString("operation_type"));
        } catch (Throwable ignored) {
            operationType = OperationType.ADD;
        }
        amount = nbt.getInt("amount");
        skillId = nbt.getInt("skill_id");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(kind);
        buf.writeEnum(operationType);
        buf.writeVarInt(amount);
        buf.writeVarInt(skillId);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        kind = buf.readEnum(Kind.class);
        operationType = buf.readEnum(OperationType.class);
        amount = buf.readVarInt();
        skillId = buf.readVarInt();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var KINDS = NameMap.of(Kind.EXPERIENCE, Kind.values()).create();
        config.addEnum("kind", kind, v -> kind = v, KINDS)
                .setNameKey("morequesttypes.reward.levelz.kind");

        var OPERATIONS = NameMap.of(OperationType.ADD, OperationType.values()).create();
        config.addEnum("operation_type", operationType, v -> operationType = v, OPERATIONS)
                .setNameKey("morequesttypes.reward.levelz.operation_type");

        config.addInt("amount", amount, v -> amount = v, 0, 0, 1_000_000)
                .setNameKey("morequesttypes.reward.levelz.amount");

        Map<Integer, String> skills = new LinkedHashMap<>();
        skills.put(-1, "player_level");
        skills.putAll(LevelZCompat.getAvailableSkills());

        List<Integer> skillIds = new ArrayList<>(skills.keySet());
        Integer currentSkillId = skillIds.contains(skillId) ? skillId : -1;

        var SKILL_MAP = NameMap.of(currentSkillId, skillIds.toArray(Integer[]::new))
                .name(id -> {
                    if (id == -1) return Component.literal("Player Level");
                    return Component.literal(skills.getOrDefault(id, "Unknown (" + id + ")"));
                })
                .create();

        config.addEnum("skill", currentSkillId, v -> skillId = v, SKILL_MAP)
                .setNameKey("morequesttypes.reward.levelz.skill");
    }
}