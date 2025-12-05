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
    public enum Kind { ADD, SET }

    private Kind kind = Kind.ADD;
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
        if (amount == 0) return;

        switch (kind) {
            case ADD -> {

                int current = ReskillableCompat.getSkillLevel(player, skillIndex);
                ReskillableCompat.setSkillLevel(player, skillIndex, current + amount);
            }
            case SET -> {

                ReskillableCompat.setSkillLevel(player, skillIndex, amount);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Component getAltTitle() {
        Map<Integer, String> skills = ReskillableCompat.getAvailableSkills();
        String skillName = skills.getOrDefault(skillIndex, "Unknown");
        String kindStr = kind.name().toLowerCase(Locale.ROOT);

        return Component.translatable(
                "morequesttypes.reward.reskillable.title",
                kindStr,
                skillName,
                amount
        );
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        Icon icon = getSkillIcon(skillIndex, this.getType());
        return icon;
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

                Icon icon = Icon.getIcon(iconLocation);
                return icon;
            } catch (Exception e) {
                return rewardType.getIconSupplier();
            }
        }

        return rewardType.getIconSupplier();
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("kind", kind.name());
        nbt.putInt("amount", amount);
        nbt.putInt("skill_index", skillIndex);
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        try {
            kind = Kind.valueOf(nbt.getString("kind"));
        } catch (Throwable ignored) {
            kind = Kind.ADD;
        }
        amount = nbt.getInt("amount");
        skillIndex = nbt.getInt("skill_index");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(kind);
        buf.writeVarInt(amount);
        buf.writeVarInt(skillIndex);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        kind = buf.readEnum(Kind.class);
        amount = buf.readVarInt();
        skillIndex = buf.readVarInt();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        var KINDS = NameMap.of(Kind.ADD, Kind.values()).create();
        config.addEnum("kind", kind, v -> kind = v, KINDS)
                .setNameKey("morequesttypes.reward.reskillable.kind");

        config.addInt("amount", amount, v -> amount = v, 1, 0, 1_000_000)
                .setNameKey("morequesttypes.reward.reskillable.amount");

        Map<Integer, String> skills = new LinkedHashMap<>();
        skills.putAll(ReskillableCompat.getAvailableSkills());

        List<Integer> skillIds = new ArrayList<>(skills.keySet());
        Integer currentSkillId = skillIds.contains(skillIndex) ?  skillIndex : (skillIds.isEmpty() ? 0 : skillIds.get(0));


        RewardType rewardType = this.getType();
        var SKILL_MAP = NameMap.of(currentSkillId, skillIds.toArray(Integer[]::new))
                .name(id -> Component.literal(skills.getOrDefault(id, "Unknown (" + id + ")")))
                .icon(id -> {
                    return getSkillIcon(id, rewardType);
                })
                .create();

        config.addEnum("skill", currentSkillId, v -> {
                    skillIndex = v;
                }, SKILL_MAP)
                .setNameKey("morequesttypes.reward.reskillable.skill");
    }
}