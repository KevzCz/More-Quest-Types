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
    public enum Kind { EXPERIENCE, LEVEL }

    private Kind kind = Kind.EXPERIENCE;
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
        if (! LevelZCompat.isLoaded()) return;
        if (amount == 0) return;

        switch (kind) {
            case EXPERIENCE -> {

                LevelZCompat.addExperience(player, amount);
            }
            case LEVEL -> {

                if (skillId == -1) {

                    int current = LevelZCompat.getLevel(player);
                    LevelZCompat.setLevel(player, current + amount);
                } else {

                    int current = LevelZCompat.getSkillLevel(player, skillId);
                    LevelZCompat.setSkillLevel(player, skillId, current + amount);
                }
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

        return Component.translatable(
                "morequesttypes.reward.levelz.title",
                kindStr,
                skillName,
                amount
        );
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        if (kind == Kind.EXPERIENCE) {
            return getType().getIconSupplier();
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
        amount = nbt.getInt("amount");
        skillId = nbt.getInt("skill_id");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeEnum(kind);
        buf.writeVarInt(amount);
        buf.writeVarInt(skillId);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        kind = buf.readEnum(Kind.class);
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