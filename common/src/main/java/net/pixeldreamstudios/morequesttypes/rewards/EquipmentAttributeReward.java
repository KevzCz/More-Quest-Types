package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.config.EquipmentAttributeRewardConfig;
import net.pixeldreamstudios.morequesttypes.rewards.manager.EquipmentBonusManager;
import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;

public final class EquipmentAttributeReward extends Reward {
    public enum EquipSlot {
        MAIN_HAND(EquipmentSlotGroup.MAINHAND),
        OFF_HAND(EquipmentSlotGroup.OFFHAND),
        HEAD(EquipmentSlotGroup.HEAD),
        CHEST(EquipmentSlotGroup.CHEST),
        LEGS(EquipmentSlotGroup.LEGS),
        FEET(EquipmentSlotGroup.FEET);

        public final EquipmentSlotGroup slotGroup;

        EquipSlot(EquipmentSlotGroup slotGroup) {
            this.slotGroup = slotGroup;
        }
    }

    private String modifierId = "";
    private String attributeId = "minecraft:generic.attack_damage";
    private double amount = 1.0;
    private AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_VALUE;
    private EquipSlot targetSlot = EquipSlot.MAIN_HAND;
    private EquipSlot attributeSlot = EquipSlot.MAIN_HAND;
    private EquipmentBonusManager.ModifyMode modifyMode = EquipmentBonusManager.ModifyMode.ADD;
    private boolean checkAttributeExists = false;

    private boolean checkConditionAttributeExists = false;
    private String conditionAttribute = "";
    private ComparisonMode comparisonMode = ComparisonMode.GREATER_OR_EQUAL;
    private double conditionFirst = 0;
    private double conditionSecond = 0;

    private String replaceWithAttribute = "";
    private double replaceWithValue = 0;
    private boolean useReplaceWithValue = false;

    public EquipmentAttributeReward(long id, dev.ftb.mods.ftbquests.quest.Quest q) {
        super(id, q);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.EQUIPMENT_ATTRIBUTE;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        ItemStack stack = switch (targetSlot) {
            case MAIN_HAND -> player.getMainHandItem();
            case OFF_HAND -> player.getOffhandItem();
            case HEAD -> player.getInventory().getArmor(3);
            case CHEST -> player.getInventory().getArmor(2);
            case LEGS -> player.getInventory().getArmor(1);
            case FEET -> player.getInventory().getArmor(0);
        };

        if (stack.isEmpty()) {
            return;
        }

        EquipmentBonusManager.modifyBonus(
                stack,
                modifierId,
                attributeId,
                amount,
                operation,
                attributeSlot.slotGroup.getSerializedName(),
                modifyMode,
                checkAttributeExists,
                checkConditionAttributeExists,
                conditionAttribute.isEmpty() ? null : conditionAttribute,
                comparisonMode,
                conditionFirst,
                conditionSecond,
                replaceWithAttribute.isEmpty() ? null : replaceWithAttribute,
                useReplaceWithValue ? replaceWithValue : null
        );
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Component getAltTitle() {
        return Component.translatable(
                "morequesttypes.reward.equipment_attribute.title",
                modifyMode.name().toLowerCase(),
                attributeId,
                targetSlot.name().toLowerCase(),
                attributeSlot.name().toLowerCase()
        );
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        EquipmentAttributeRewardConfig.AttributeData data = new EquipmentAttributeRewardConfig.AttributeData();
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

        EquipmentAttributeRewardConfig configValue = new EquipmentAttributeRewardConfig();
        config.add("config", configValue, data, newData -> {
            this.modifierId = newData.modifierId;
            this.attributeId = newData.attributeId;
            this.amount = newData.amount;
            this.operation = newData.operation;
            this.targetSlot = newData.targetSlot;
            this.attributeSlot = newData.attributeSlot;
            this.modifyMode = newData.modifyMode;
            this.checkAttributeExists = newData.checkAttributeExists;
            this.checkConditionAttributeExists = newData.checkConditionAttributeExists;
            this.conditionAttribute = newData.conditionAttribute;
            this.comparisonMode = newData.comparisonMode;
            this.conditionFirst = newData.conditionFirst;
            this.conditionSecond = newData.conditionSecond;
            this.replaceWithAttribute = newData.replaceWithAttribute;
            this.replaceWithValue = newData.replaceWithValue;
            this.useReplaceWithValue = newData.useReplaceWithValue;
        }, data).setNameKey("morequesttypes.reward.equipment_attribute.config");
    }

    @Override
    public void writeData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("modifier_id", modifierId);
        nbt.putString("attribute", attributeId);
        nbt.putDouble("amount", amount);
        nbt.putString("operation", operation.name());
        nbt.putString("target_slot", targetSlot.name());
        nbt.putString("attribute_slot", attributeSlot.name());
        nbt.putString("modify_mode", modifyMode.name());
        nbt.putBoolean("check_attribute_exists", checkAttributeExists);
        nbt.putBoolean("check_condition_attribute_exists", checkConditionAttributeExists);
        nbt.putString("condition_attribute", conditionAttribute);
        nbt.putString("comparison_mode", comparisonMode.name());
        nbt.putDouble("condition_first", conditionFirst);
        nbt.putDouble("condition_second", conditionSecond);
        nbt.putString("replace_with_attribute", replaceWithAttribute);
        nbt.putBoolean("use_replace_with_value", useReplaceWithValue);
        nbt.putDouble("replace_with_value", replaceWithValue);
    }

    @Override
    public void readData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        modifierId = nbt.getString("modifier_id");
        attributeId = nbt.getString("attribute");
        amount = nbt.getDouble("amount");
        try { operation = AttributeModifier.Operation.valueOf(nbt.getString("operation")); }
        catch (Throwable ignored) { operation = AttributeModifier.Operation.ADD_VALUE; }
        try { targetSlot = EquipSlot.valueOf(nbt.getString("target_slot")); }
        catch (Throwable ignored) {
            try { targetSlot = EquipSlot.valueOf(nbt.getString("slot")); }
            catch (Throwable ignored2) { targetSlot = EquipSlot.MAIN_HAND; }
        }
        try { attributeSlot = EquipSlot.valueOf(nbt.getString("attribute_slot")); }
        catch (Throwable ignored) {
            attributeSlot = targetSlot;
        }
        try { modifyMode = EquipmentBonusManager.ModifyMode.valueOf(nbt.getString("modify_mode")); }
        catch (Throwable ignored) { modifyMode = EquipmentBonusManager.ModifyMode.ADD; }
        checkAttributeExists = nbt.getBoolean("check_attribute_exists");
        checkConditionAttributeExists = nbt.getBoolean("check_condition_attribute_exists");
        conditionAttribute = nbt.getString("condition_attribute");
        try { comparisonMode = ComparisonMode.valueOf(nbt.getString("comparison_mode")); }
        catch (Throwable ignored) { comparisonMode = ComparisonMode.GREATER_OR_EQUAL; }
        conditionFirst = nbt.getDouble("condition_first");
        conditionSecond = nbt.getDouble("condition_second");
        replaceWithAttribute = nbt.getString("replace_with_attribute");
        useReplaceWithValue = nbt.getBoolean("use_replace_with_value");
        replaceWithValue = nbt.getDouble("replace_with_value");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(modifierId);
        buffer.writeUtf(attributeId);
        buffer.writeDouble(amount);
        buffer.writeEnum(operation);
        buffer.writeEnum(targetSlot);
        buffer.writeEnum(attributeSlot);
        buffer.writeEnum(modifyMode);
        buffer.writeBoolean(checkAttributeExists);
        buffer.writeBoolean(checkConditionAttributeExists);
        buffer.writeUtf(conditionAttribute);
        buffer.writeEnum(comparisonMode);
        buffer.writeDouble(conditionFirst);
        buffer.writeDouble(conditionSecond);
        buffer.writeUtf(replaceWithAttribute);
        buffer.writeBoolean(useReplaceWithValue);
        buffer.writeDouble(replaceWithValue);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        modifierId = buffer.readUtf();
        attributeId = buffer.readUtf();
        amount = buffer.readDouble();
        operation = buffer.readEnum(AttributeModifier.Operation.class);
        targetSlot = buffer.readEnum(EquipSlot.class);
        attributeSlot = buffer.readEnum(EquipSlot.class);
        modifyMode = buffer.readEnum(EquipmentBonusManager.ModifyMode.class);
        checkAttributeExists = buffer.readBoolean();
        checkConditionAttributeExists = buffer.readBoolean();
        conditionAttribute = buffer.readUtf();
        comparisonMode = buffer.readEnum(ComparisonMode.class);
        conditionFirst = buffer.readDouble();
        conditionSecond = buffer.readDouble();
        replaceWithAttribute = buffer.readUtf();
        useReplaceWithValue = buffer.readBoolean();
        replaceWithValue = buffer.readDouble();
    }
}