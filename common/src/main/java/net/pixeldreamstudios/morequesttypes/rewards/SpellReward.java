package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.pixeldreamstudios.morequesttypes.compat.SpellEngineCompat;

import java.util.List;
import java.util.Objects;

public final class SpellReward extends Reward {
    private ResourceLocation spellId = null;
    private boolean locked = false;

    public SpellReward(long id, Quest q) {
        super(id, q);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.SPELL;
    }

    private String baseKey() {
        return "morequesttypes/reward_" + id;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        try {
            applyToPlayer(player);
        } catch (Throwable ignored) {}
    }

    public void applyToPlayer(ServerPlayer player) {
        if (player == null || spellId == null) return;
        if (!SpellEngineCompat.isLoaded()) return;
        try {
            SpellEngineCompat.installSpells(player, baseKey(), List.of(spellId));
        } catch (Throwable ignored) {}
    }

    public void removeFromPlayer(ServerPlayer player) {
        if (player == null) return;
        if (!SpellEngineCompat.isLoaded()) return;
        try {
            SpellEngineCompat.uninstallSpells(player, baseKey());
        } catch (Throwable ignored) {}
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Component getAltTitle() {
        if (spellId == null) return Component.literal("");

        String key = "spell." + spellId.getNamespace() + "." + spellId.getPath() + ".name";
        return Component.translatable(key);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Icon getAltIcon() {
        try {
            if (spellId != null && SpellEngineCompat.isLoaded()) {
                ResourceLocation tex = SpellEngineCompat.getSpellIconTexture(spellId);
                if (tex != null) {
                    try {
                        return Icon.getIcon(tex);
                    } catch (Throwable t) {
                        try {
                            return Icon.getIcon(tex.toString());
                        } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}

        return super.getAltIcon();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addMouseOverText(TooltipList list) {
        super.addMouseOverText(list);

        try {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                var teamData = dev.ftb.mods.ftbquests.quest.TeamData.get(player);
                boolean isClaimed = teamData.isRewardClaimed(player.getUUID(), this);

                Component status = isClaimed
                        ? Component.translatable("morequesttypes.reward.status.on").withStyle(ChatFormatting.GREEN)
                        : Component.translatable("morequesttypes.reward.status.off").withStyle(ChatFormatting.RED);

                list.add(Component.translatable("morequesttypes.reward.status", status));

                if (locked) {
                    list.add(Component.translatable("morequesttypes.reward.locked").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
                } else {
                    list.add(Component.translatable("morequesttypes.reward.toggle_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                }
            }
        } catch (Throwable ignored) {}
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        try {
            Level level = net.minecraft.client.Minecraft.getInstance().level;
            java.util.List<String> keys;
            if (SpellEngineCompat.isLoaded() && level != null) {
                var all = SpellEngineCompat.getAllSpells(level);
                keys = all.stream().map(Objects::toString).sorted().toList();
            } else {
                keys = List.of();
            }

            var SPELLS = NameMap.of(spellId == null ? "" : spellId.toString(), keys.toArray(String[]::new))
                    .name(Component::literal)
                    .create();

            config.addEnum("spell", spellId == null ? "" : spellId.toString(), s -> {
                        ResourceLocation rl = ResourceLocation.tryParse(s);
                        if (rl != null) spellId = rl;
                    }, SPELLS).setNameKey("morequesttypes.reward.spell.spell")
                    .setCanEdit(true);

        } catch (Throwable t) {
            config.addString("spell", spellId == null ? "" : spellId.toString(), s -> spellId = ResourceLocation.tryParse(s), "");
        }

        config.addBool("locked", locked, v -> locked = v, false)
                .setNameKey("morequesttypes.reward.locked");
    }

    @Override
    public void writeData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        if (spellId != null) nbt.putString("spell", spellId.toString());
        if (locked) nbt.putBoolean("locked", true);
    }

    @Override
    public void readData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        if (nbt.contains("spell")) spellId = ResourceLocation.tryParse(nbt.getString("spell"));
        else spellId = null;
        locked = nbt.getBoolean("locked");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(spellId == null ? "" : spellId.toString());
        buf.writeBoolean(locked);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        String s = buf.readUtf();
        spellId = s.isEmpty() ? null : ResourceLocation.tryParse(s);
        locked = buf.readBoolean();
    }

    public boolean isLocked() {
        return locked;
    }
    @Override
    public boolean getExcludeFromClaimAll() { return true; }
}