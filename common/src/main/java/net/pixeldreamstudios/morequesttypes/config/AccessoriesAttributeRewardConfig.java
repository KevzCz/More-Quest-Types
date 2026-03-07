package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.*;
import dev.ftb.mods.ftblibrary.config.ui.*;
import dev.ftb.mods.ftblibrary.icon.*;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.*;
import dev.ftb.mods.ftblibrary.util.*;
import io.wispforest.accessories.data.*;
import net.fabricmc.api.*;
import net.minecraft.*;
import net.minecraft.client.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.pixeldreamstudios.morequesttypes.rewards.*;
import org.jetbrains.annotations.*;

import java.util.*;

@Environment(EnvType.CLIENT)
public class AccessoriesAttributeRewardConfig extends ConfigValue<AccessoriesAttributeRewardConfig.AttributeData> {
    public static class AttributeData {
        public String modifierId = "";
        public String attributeId = "minecraft:generic.attack_damage";
        public double amount = 1.0;
        public AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_VALUE;
        public AccessoriesAttributeReward.TargetSlot targetSlot = AccessoriesAttributeReward.TargetSlot.MAIN_HAND;
        public String accessoriesSlotName = "any";
        public boolean isStackable = false;
        public AccessoriesAttributeReward.ModifyMode modifyMode = AccessoriesAttributeReward.ModifyMode.ADD;
        public boolean checkHasModifier = false;
        public String checkModifierId = "";

        public AttributeData copy() {
            AttributeData data = new AttributeData();
            data.modifierId = modifierId;
            data.attributeId = attributeId;
            data.amount = amount;
            data.operation = operation;
            data.targetSlot = targetSlot;
            data.accessoriesSlotName = accessoriesSlotName;
            data.isStackable = isStackable;
            data.modifyMode = modifyMode;
            data.checkHasModifier = checkHasModifier;
            data.checkModifierId = checkModifierId;
            return data;
        }
    }

    public AccessoriesAttributeRewardConfig() {
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

        ConfigGroup mainGroup = new ConfigGroup("accessories_attribute_config", accepted -> {
            if (accepted) {
                setValue(current);
                callback.save(true);
            } else {
                callback.save(false);
            }
        });

        ConfigGroup basicGroup = mainGroup.getOrCreateSubgroup("basic");
        basicGroup.setNameKey("morequesttypes.config.accessories_attribute.basic");

        var TARGET_SLOTS = NameMap.of(current.targetSlot, AccessoriesAttributeReward.TargetSlot.values()).create();
        var OPERATIONS = NameMap.of(current.operation, AttributeModifier.Operation.values()).create();
        var MODIFY_MODES = NameMap.of(current.modifyMode, AccessoriesAttributeReward.ModifyMode.values()).create();

        List<String> attributeList = new ArrayList<>();
        attributeList.add("");
        attributeList.addAll(
                BuiltInRegistries.ATTRIBUTE.keySet().stream()
                        .map(ResourceLocation::toString)
                        .sorted()
                        .toList()
        );

        List<String> availableSlots = new ArrayList<>();
        availableSlots.add("");
        availableSlots.add("any");

        try {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                var slotTypes = SlotTypeLoader.getSlotTypes(minecraft.level);
                availableSlots.addAll(slotTypes.keySet().stream()
                        .map(String::toString)
                        .sorted()
                        .toList());
            }
        } catch (Exception e) {
            availableSlots.addAll(List.of("ring", "necklace", "bracelet", "belt", "charm", "cape", "back"));
        }

        var SLOT_NAMES = NameMap.of(current.accessoriesSlotName, availableSlots)
                .name(s -> Component.literal(s == null || s.isEmpty() ? "(None)" : s))
                .create();

        basicGroup.addString("modifier_id", current.modifierId, v -> current.modifierId = v, "")
                .setNameKey("morequesttypes.reward.accessories_attribute.modifier_id");

        var ATTRS = NameMap.of(current.attributeId, attributeList)
                .name(s -> Component.literal(s == null || s.isEmpty() ? "(None)" : s))
                .create();

        basicGroup.addEnum("attribute_id", current.attributeId, v -> current.attributeId = v, ATTRS)
                .setNameKey("morequesttypes.reward.accessories_attribute.attribute_id");

        basicGroup.addDouble("amount", current.amount, v -> current.amount = v, 1.0, -1024.0, 1024.0)
                .setNameKey("morequesttypes.reward.accessories_attribute.amount");

        basicGroup.addEnum("operation", current.operation, v -> current.operation = v, OPERATIONS)
                .setNameKey("morequesttypes.reward.accessories_attribute.operation");

        basicGroup.addEnum("target_slot", current.targetSlot, v -> current.targetSlot = v, TARGET_SLOTS)
                .setNameKey("morequesttypes.reward.accessories_attribute.target_slot");

        basicGroup.addEnum("accessories_slot_name", current.accessoriesSlotName, v -> current.accessoriesSlotName = v, SLOT_NAMES)
                .setNameKey("morequesttypes.reward.accessories_attribute.accessories_slot_name");

        basicGroup.addBool("is_stackable", current.isStackable, v -> current.isStackable = v, false)
                .setNameKey("morequesttypes.reward.accessories_attribute.is_stackable");

        basicGroup.addEnum("modify_mode", current.modifyMode, v -> current.modifyMode = v, MODIFY_MODES)
                .setNameKey("morequesttypes.reward.accessories_attribute.modify_mode");

        basicGroup.addBool("check_has_modifier", current.checkHasModifier, v -> current.checkHasModifier = v, false)
                .setNameKey("morequesttypes.reward.accessories_attribute.check_has_modifier");

        basicGroup.addString("check_modifier_id", current.checkModifierId, v -> current.checkModifierId = v, "")
                .setNameKey("morequesttypes.reward.accessories_attribute.check_modifier_id");

        new EditConfigScreen(mainGroup).openGui();
    }

    @Override
    public AttributeData copy(AttributeData value) {
        if (value == null) return new AttributeData();
        return value.copy();
    }

    @Override
    public void addInfo(TooltipList list) {
    }
}
