package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.*;
import dev.ftb.mods.ftblibrary.config.ui.*;
import dev.ftb.mods.ftblibrary.icon.*;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.*;
import dev.ftb.mods.ftblibrary.util.*;
import net.fabricmc.api.*;
import net.minecraft.*;
import net.minecraft.client.*;
import net.minecraft.network.chat.*;
import net.pixeldreamstudios.morequesttypes.compat.*;
import net.pixeldreamstudios.morequesttypes.rewards.*;
import org.jetbrains.annotations.*;

import java.util.*;

@Environment(EnvType.CLIENT)
public class SpellEquipmentRewardConfig extends ConfigValue<SpellEquipmentRewardConfig.SpellData> {
    public static class SpellData {
        public String spellId = "";
        public SpellEquipmentReward.TargetSlot targetSlot = SpellEquipmentReward.TargetSlot.MAIN_HAND;
        public SpellEquipmentReward.ModifyMode modifyMode = SpellEquipmentReward.ModifyMode.ADD;
        public List<String> spellList = new ArrayList<>();
        public boolean checkHasSpell = false;
        public String checkSpellId = "";
        public String contentType = "MAGIC";

        public SpellData copy() {
            SpellData data = new SpellData();
            data.spellId = spellId;
            data.targetSlot = targetSlot;
            data.modifyMode = modifyMode;
            data.spellList = new ArrayList<>(spellList);
            data.checkHasSpell = checkHasSpell;
            data.checkSpellId = checkSpellId;
            data.contentType = contentType;
            return data;
        }
    }

    public SpellEquipmentRewardConfig() {
    }

    @Override
    public Color4I getColor(@Nullable SpellData v) {
        return Color4I.WHITE;
    }

    @Override
    public Component getStringForGUI(@Nullable SpellData v) {
        if (v == null) {
            return Component.literal("Not configured").withStyle(ChatFormatting.GRAY);
        }

        return Component.literal(v.modifyMode.name().toLowerCase() + " " +
                        (v.modifyMode == SpellEquipmentReward.ModifyMode.SET ?
                                v.spellList.size() + " spells" : v.spellId))
                .withStyle(ChatFormatting.GREEN);
    }

    @Override
    public Icon getIcon(@Nullable SpellData v) {
        return Icons.SETTINGS;
    }

    @Override
    public void onClicked(Widget clicked, MouseButton button, ConfigCallback callback) {
        if (!getCanEdit()) {
            return;
        }

        SpellData current = getValue() != null ? getValue().copy() : new SpellData();

        ConfigGroup mainGroup = new ConfigGroup("spell_equipment_config", accepted -> {
            if (accepted) {
                setValue(current);
                callback.save(true);
            } else {
                callback.save(false);
            }
        });

        ConfigGroup basicGroup = mainGroup.getOrCreateSubgroup("basic");
        basicGroup.setNameKey("morequesttypes.config.spell_equipment.basic");

        var TARGET_SLOTS = NameMap.of(current.targetSlot, SpellEquipmentReward.TargetSlot.values()).create();
        var MODIFY_MODES = NameMap.of(current.modifyMode, SpellEquipmentReward.ModifyMode.values()).create();

        List<String> contentTypeList = new ArrayList<>();
        contentTypeList.add("");
        contentTypeList.addAll(List.of("MAGIC", "ARCHERY", "MELEE"));

        var CONTENT_TYPES = NameMap.of(current.contentType, contentTypeList)
                .name(s -> Component.literal(s == null || s.isEmpty() ? "(None)" : s))
                .create();

        List<String> spellList = new ArrayList<>();
        spellList.add("");

        try {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level != null && SpellEngineCompat.isLoaded()) {
                var allSpells = SpellEngineCompat.getAllSpells(minecraft.level);
                spellList.addAll(allSpells.stream()
                        .map(Object::toString)
                        .sorted()
                        .toList());
            }
        } catch (Throwable e) {
            // If spell loading fails, spellList will just have the empty string
        }

        var SPELLS = NameMap.of(current.spellId, spellList)
                .name(s -> Component.literal(s == null || s.isEmpty() ? "(None)" : s))
                .create();
        var CHECK_SPELLS = NameMap.of(current.checkSpellId, spellList)
                .name(s -> Component.literal(s == null || s.isEmpty() ? "(None)" : s))
                .create();

        basicGroup.addEnum("spell_id", current.spellId, v -> current.spellId = v, SPELLS)
                .setNameKey("morequesttypes.config.spell_equipment.spell_id");
        basicGroup.addEnum("check_spell_id", current.checkSpellId, v -> current.checkSpellId = v, CHECK_SPELLS)
                .setNameKey("morequesttypes.config.spell_equipment.check_spell_id");

        basicGroup.addEnum("target_slot", current.targetSlot, v -> current.targetSlot = v, TARGET_SLOTS)
                .setNameKey("morequesttypes.config.spell_equipment.target_slot");

        basicGroup.addEnum("modify_mode", current.modifyMode, v -> current.modifyMode = v, MODIFY_MODES)
                .setNameKey("morequesttypes.config.spell_equipment.modify_mode");

        basicGroup.addEnum("content_type", current.contentType, v -> current.contentType = v, CONTENT_TYPES)
                .setNameKey("morequesttypes.config.spell_equipment.content_type");

        basicGroup.addList("spell_list", current.spellList, new StringConfig(), "")
                .setNameKey("morequesttypes.config.spell_equipment.spell_list");

        basicGroup.addBool("check_has_spell", current.checkHasSpell, v -> current.checkHasSpell = v, false)
                .setNameKey("morequesttypes.config.spell_equipment.check_has_spell");

        new EditConfigScreen(mainGroup).openGui();
    }

    @Override
    public void addInfo(TooltipList list) {
    }
}
