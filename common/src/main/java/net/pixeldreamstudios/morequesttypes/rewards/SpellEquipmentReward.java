package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.*;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.reward.*;
import net.fabricmc.api.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.pixeldreamstudios.morequesttypes.compat.*;
import net.pixeldreamstudios.morequesttypes.config.*;

import java.util.*;

public final class SpellEquipmentReward extends Reward {
    public enum TargetSlot {
        MAIN_HAND,
        OFF_HAND,
        HEAD,
        CHEST,
        LEGS,
        FEET
    }

    public enum ModifyMode {
        ADD,
        REMOVE,
        SET
    }

    private String spellId = "";
    private TargetSlot targetSlot = TargetSlot.MAIN_HAND;
    private ModifyMode modifyMode = ModifyMode.ADD;
    private List<String> spellList = new ArrayList<>();
    private boolean checkHasSpell = false;
    private String checkSpellId = "";
    private String contentType = "MAGIC";

    public SpellEquipmentReward(long id, Quest q) {
        super(id, q);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.SPELL_EQUIPMENT;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        if (!SpellEngineCompat.isLoaded()) return;

        ItemStack stack = getTargetStack(player);

        if (stack.isEmpty()) {
            return;
        }
        if (checkHasSpell && !checkSpellId.isEmpty()) {
            ResourceLocation checkLoc = ResourceLocation.tryParse(checkSpellId);
            if (checkLoc == null) return;

            boolean hasSpell = SpellEngineCompat.hasItemSpell(stack, checkLoc);
            if (!hasSpell) {
                return;
            }
        }

        switch (modifyMode) {
            case ADD -> {
                ResourceLocation spellLoc = ResourceLocation.tryParse(spellId);
                if (spellLoc != null) {
                    SpellEngineCompat.addItemSpell(stack, spellLoc, contentType);
                }
            }
            case REMOVE -> {
                ResourceLocation spellLoc = ResourceLocation.tryParse(spellId);
                if (spellLoc != null) {
                    SpellEngineCompat.removeItemSpell(stack, spellLoc);
                }
            }
            case SET -> {
                List<ResourceLocation> spells = new ArrayList<>();
                for (String s : spellList) {
                    ResourceLocation loc = ResourceLocation.tryParse(s);
                    if (loc != null) {
                        spells.add(loc);
                    }
                }
                SpellEngineCompat.setItemSpells(stack, spells, contentType);
            }
        }
    }

    private ItemStack getTargetStack(ServerPlayer player) {
        return switch (targetSlot) {
            case MAIN_HAND -> player.getMainHandItem();
            case OFF_HAND -> player.getOffhandItem();
            case HEAD -> player.getInventory().getArmor(3);
            case CHEST -> player.getInventory().getArmor(2);
            case LEGS -> player.getInventory().getArmor(1);
            case FEET -> player.getInventory().getArmor(0);
        };
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Component getAltTitle() {
        return Component.translatable(
                "morequesttypes.reward.spell_equipment.title",
                modifyMode.name().toLowerCase(),
                modifyMode == ModifyMode.SET ? spellList.size() + " spells" : spellId,
                targetSlot.name().toLowerCase()
        );
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        SpellEquipmentRewardConfig.SpellData data = new SpellEquipmentRewardConfig.SpellData();
        data.spellId = this.spellId;
        data.targetSlot = this.targetSlot;
        data.modifyMode = this.modifyMode;
        data.spellList = new ArrayList<>(this.spellList);
        data.checkHasSpell = this.checkHasSpell;
        data.checkSpellId = this.checkSpellId;
        data.contentType = this.contentType;

        config.add("config", new SpellEquipmentRewardConfig(), data, newData -> {
            this.spellId = newData.spellId;
            this.targetSlot = newData.targetSlot;
            this.modifyMode = newData.modifyMode;
            this.spellList = new ArrayList<>(newData.spellList);
            this.checkHasSpell = newData.checkHasSpell;
            this.checkSpellId = newData.checkSpellId;
            this.contentType = newData.contentType;
        }, data).setNameKey("morequesttypes.reward.spell_equipment.config");
    }

    @Override
    public void writeData(CompoundTag tag, HolderLookup.Provider provider) {
        super.writeData(tag, provider);
        if (!spellId.isEmpty()) tag.putString("spell_id", spellId);
        tag.putString("target_slot", targetSlot.name());
        tag.putString("modify_mode", modifyMode.name());
        tag.putString("content_type", contentType);

        if (!spellList.isEmpty()) {
            CompoundTag listTag = new CompoundTag();
            for (int i = 0; i < spellList.size(); i++) {
                listTag.putString("spell_" + i, spellList.get(i));
            }
            listTag.putInt("count", spellList.size());
            tag.put("spell_list", listTag);
        }

        tag.putBoolean("check_has_spell", checkHasSpell);
        if (!checkSpellId.isEmpty()) tag.putString("check_spell_id", checkSpellId);
    }

    @Override
    public void readData(CompoundTag tag, HolderLookup.Provider provider) {
        super.readData(tag, provider);
        spellId = tag.getString("spell_id");

        try {
            targetSlot = TargetSlot.valueOf(tag.getString("target_slot"));
        } catch (Exception e) {
            targetSlot = TargetSlot.MAIN_HAND;
        }

        try {
            modifyMode = ModifyMode.valueOf(tag.getString("modify_mode"));
        } catch (Exception e) {
            modifyMode = ModifyMode.ADD;
        }

        spellList.clear();
        if (tag.contains("spell_list")) {
            CompoundTag listTag = tag.getCompound("spell_list");
            int count = listTag.getInt("count");
            for (int i = 0; i < count; i++) {
                String spell = listTag.getString("spell_" + i);
                if (!spell.isEmpty()) {
                    spellList.add(spell);
                }
            }
        }

        checkHasSpell = tag.getBoolean("check_has_spell");
        checkSpellId = tag.getString("check_spell_id");
        contentType = tag.contains("content_type") ? tag.getString("content_type") : "MAGIC";
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buf) {
        super.writeNetData(buf);
        buf.writeUtf(spellId);
        buf.writeEnum(targetSlot);
        buf.writeEnum(modifyMode);
        buf.writeInt(spellList.size());
        for (String s : spellList) {
            buf.writeUtf(s);
        }
        buf.writeBoolean(checkHasSpell);
        buf.writeUtf(checkSpellId);
        buf.writeUtf(contentType);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buf) {
        super.readNetData(buf);
        spellId = buf.readUtf();
        targetSlot = buf.readEnum(TargetSlot.class);
        modifyMode = buf.readEnum(ModifyMode.class);
        int count = buf.readInt();
        spellList.clear();
        for (int i = 0; i < count; i++) {
            spellList.add(buf.readUtf());
        }
        checkHasSpell = buf.readBoolean();
        checkSpellId = buf.readUtf();
        contentType = buf.readUtf();
    }
}
