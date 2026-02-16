package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.pixeldreamstudios.morequesttypes.rewards.EquipmentAttributeReward;
import net.pixeldreamstudios.morequesttypes.rewards.manager.EquipmentBonusManager;
import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class EquipmentAttributeRewardConfig extends ConfigValue<EquipmentAttributeRewardConfig.AttributeData> {

    public static class AttributeData {
        public String modifierId = "";
        public String attributeId = "minecraft:generic.attack_damage";
        public double amount = 1.0;
        public AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_VALUE;
        public EquipmentAttributeReward.EquipSlot targetSlot = EquipmentAttributeReward.EquipSlot.MAIN_HAND;
        public EquipmentAttributeReward.EquipSlot attributeSlot = EquipmentAttributeReward.EquipSlot.MAIN_HAND;
        public EquipmentBonusManager.ModifyMode modifyMode = EquipmentBonusManager.ModifyMode.ADD;
        public boolean checkAttributeExists = false;

        public boolean checkConditionAttributeExists = false;
        public String conditionAttribute = "";
        public ComparisonMode comparisonMode = ComparisonMode.GREATER_OR_EQUAL;
        public double conditionFirst = 0;
        public double conditionSecond = 0;

        public String replaceWithAttribute = "";
        public double replaceWithValue = 0;
        public boolean useReplaceWithValue = false;

        public AttributeData copy() {
            AttributeData data = new AttributeData();
            data.modifierId = this.modifierId;
            data.attributeId = this.attributeId;
            data.amount = this.amount;
            data.operation = this.operation;
            data.targetSlot = this.targetSlot;
            data.attributeSlot = this.attributeSlot;
            data.modifyMode = this.modifyMode;
            data.checkAttributeExists = this.checkAttributeExists;
            data.checkConditionAttributeExists = this.checkConditionAttributeExists;
            data.conditionAttribute = this.conditionAttribute;
            data.comparisonMode = this.comparisonMode;
            data.conditionFirst = this.conditionFirst;
            data.conditionSecond = this.conditionSecond;
            data.replaceWithAttribute = this.replaceWithAttribute;
            data.replaceWithValue = this.replaceWithValue;
            data.useReplaceWithValue = this.useReplaceWithValue;
            return data;
        }
    }

    public EquipmentAttributeRewardConfig() {
    }

    @Override
    public Color4I getColor(@Nullable AttributeData v) {
        return Color4I.WHITE;
    }

    @Override
    public Component getStringForGUI(@Nullable AttributeData v) {
        if (v == null) {
            return Component.literal("Not configured").withStyle(ChatFormatting.GRAY);
        }

        return Component.literal(v.modifyMode.name().toLowerCase() + " " + v.attributeId)
                .withStyle(ChatFormatting.GREEN);
    }

    @Override
    public Icon getIcon(@Nullable AttributeData v) {
        return Icons.SETTINGS;
    }

    @Override
    public void onClicked(Widget clicked, MouseButton button, ConfigCallback callback) {
        if (!getCanEdit()) {
            return;
        }

        AttributeData current = getValue() != null ? getValue().copy() : new AttributeData();

        ConfigGroup mainGroup = new ConfigGroup("attribute_config", accepted -> {
            if (accepted) {
                setValue(current);
                callback.save(true);
            } else {
                callback.save(false);
            }
        });

        ConfigGroup basicGroup = mainGroup.getOrCreateSubgroup("basic");
        basicGroup.setNameKey("morequesttypes.config.equipment_attribute.basic");

        List<String> attributeList = new ArrayList<>();
        attributeList.add("");
        attributeList.addAll(
                BuiltInRegistries.ATTRIBUTE.keySet().stream()
                        .map(ResourceLocation::toString)
                        .sorted()
                        .toList()
        );

        basicGroup.addString("modifier_id", current.modifierId, v -> current.modifierId = v, "")
                .setNameKey("morequesttypes.config.equipment_attribute.modifier_id");

        var SLOTS = NameMap.of(current.targetSlot, EquipmentAttributeReward.EquipSlot.values()).create();
        basicGroup.addEnum("target_slot", current.targetSlot, v -> current.targetSlot = v, SLOTS)
                .setNameKey("morequesttypes.config.equipment_attribute.target_slot");

        var ATTRS = NameMap.of(current.attributeId, attributeList)
                .name(s -> Component.literal(s == null || s.isEmpty() ? "(None)" : s))
                .create();

        basicGroup.addEnum("attribute", current.attributeId, v -> current.attributeId = v, ATTRS)
                .setNameKey("morequesttypes.config.equipment_attribute.attribute");

        basicGroup.addDouble("amount", current.amount, v -> current.amount = v, 1.0, -1024.0, 1024.0)
                .setNameKey("morequesttypes.config.equipment_attribute.amount");

        var OPS = NameMap.of(current.operation, AttributeModifier.Operation.values()).create();
        basicGroup.addEnum("operation", current.operation, v -> current.operation = v, OPS)
                .setNameKey("morequesttypes.config.equipment_attribute.operation");

        basicGroup.addEnum("attribute_slot", current.attributeSlot, v -> current.attributeSlot = v, SLOTS)
                .setNameKey("morequesttypes.config.equipment_attribute.attribute_slot");

        var MODES = NameMap.of(current.modifyMode, EquipmentBonusManager.ModifyMode.values()).create();
        basicGroup.addEnum("modify_mode", current.modifyMode, v -> current.modifyMode = v, MODES)
                .setNameKey("morequesttypes.config.equipment_attribute.modify_mode");

        basicGroup.addBool("check_attribute_exists", current.checkAttributeExists,
                        v -> current.checkAttributeExists = v, false)
                .setNameKey("morequesttypes.config.equipment_attribute.check_attribute_exists");

        ConfigGroup advancedGroup = mainGroup.getOrCreateSubgroup("advanced");
        advancedGroup.setNameKey("morequesttypes.config.equipment_attribute.advanced");

        advancedGroup.addBool("check_condition_attribute_exists", current.checkConditionAttributeExists,
                        v -> current.checkConditionAttributeExists = v, false)
                .setNameKey("morequesttypes.config.equipment_attribute.check_condition_attribute_exists");

        var CONDITION_ATTRS = NameMap.of(current.conditionAttribute, attributeList)
                .name(s -> Component.literal(s == null || s.isEmpty() ? "(None)" : s))
                .create();

        advancedGroup.addEnum("condition_attribute", current.conditionAttribute,
                        v -> current.conditionAttribute = v, CONDITION_ATTRS)
                .setNameKey("morequesttypes.config.equipment_attribute.condition_attribute");

        var COMPARISONS = NameMap.of(current.comparisonMode, ComparisonMode.values())
                .name(mode -> Component.translatable(mode.getTranslationKey()))
                .create();
        advancedGroup.addEnum("comparison_mode", current.comparisonMode, v -> current.comparisonMode = v, COMPARISONS)
                .setNameKey("morequesttypes.config.equipment_attribute.comparison_mode");

        advancedGroup.addDouble("condition_first", current.conditionFirst, v -> current.conditionFirst = v,
                        0, -1024.0, 1024.0)
                .setNameKey("morequesttypes.config.equipment_attribute.condition_first");

        advancedGroup.addDouble("condition_second", current.conditionSecond, v -> current.conditionSecond = v,
                        0, -1024.0, 1024.0)
                .setNameKey("morequesttypes.config.equipment_attribute.condition_second");

        var REPLACE_ATTRS = NameMap.of(current.replaceWithAttribute, attributeList)
                .name(s -> Component.literal(s == null || s.isEmpty() ? "(None)" : s))
                .create();

        advancedGroup.addEnum("replace_with_attribute", current.replaceWithAttribute,
                        v -> current.replaceWithAttribute = v, REPLACE_ATTRS)
                .setNameKey("morequesttypes.config.equipment_attribute.replace_with_attribute");

        advancedGroup.addBool("use_replace_with_value", current.useReplaceWithValue,
                        v -> current.useReplaceWithValue = v, false)
                .setNameKey("morequesttypes.config.equipment_attribute.use_replace_with_value");

        advancedGroup.addDouble("replace_with_value", current.replaceWithValue, v -> current.replaceWithValue = v,
                        0, -1024.0, 1024.0)
                .setNameKey("morequesttypes.config.equipment_attribute.replace_with_value");

        new EditConfigScreen(mainGroup).openGui();
    }

    @Override
    public AttributeData copy(AttributeData value) {
        if (value == null) return new AttributeData();
        return value.copy();
    }

    @Override
    public void addInfo(dev.ftb.mods.ftblibrary.util.TooltipList list) {
        super.addInfo(list);
        list.add(Component.translatable("morequesttypes.config.equipment_attribute.info"));
    }
}