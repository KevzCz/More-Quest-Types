package net.pixeldreamstudios.morequesttypes.rewards;

import dev.ftb.mods.ftblibrary.config.*;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.reward.*;
import io.wispforest.accessories.api.components.*;
import net.fabricmc.api.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.pixeldreamstudios.morequesttypes.config.*;

public final class AccessoriesAttributeReward extends Reward {
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
        REPLACE
    }

    private String modifierId = "";
    private String attributeId = "minecraft:generic.attack_damage";
    private double amount = 1.0;
    private AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_VALUE;
    private TargetSlot targetSlot = TargetSlot.MAIN_HAND;
    private String accessoriesSlotName = "any";
    private boolean isStackable = false;
    private ModifyMode modifyMode = ModifyMode.ADD;
    private boolean checkHasModifier = false;
    private String checkModifierId = "";

    public AccessoriesAttributeReward(long id, Quest q) {
        super(id, q);
    }

    @Override
    public RewardType getType() {
        return MoreRewardTypes.ACCESSORIES_ATTRIBUTE;
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        ItemStack stack = getTargetStack(player);

        if (stack.isEmpty()) {
            return;
        }

        // Check condition if enabled
        var registry = player.level().holderLookup(Registries.ATTRIBUTE);
        ResourceKey<Attribute> attributeKey = ResourceKey.create(Registries.ATTRIBUTE, ResourceLocation.parse(attributeId));
        var attributeHolderOpt = registry.get(attributeKey);

        if (attributeHolderOpt.isEmpty()) {
            return;
        }

        Holder<Attribute> attributeHolder = attributeHolderOpt.get();
        ResourceLocation modifierLocation = ResourceLocation.parse(modifierId);

        var component = stack.getOrDefault(AccessoriesDataComponents.ATTRIBUTES, AccessoryItemAttributeModifiers.EMPTY);

        if (checkHasModifier) {
            boolean hasModifier = component.hasModifier(attributeHolder, ResourceLocation.parse(checkModifierId));
            if (!hasModifier) {
                return;
            }
        }

        AccessoryItemAttributeModifiers newComponent = component;

        switch (modifyMode) {
            case ADD -> {
                newComponent = component.withModifierAdded(
                        attributeHolder,
                        new AttributeModifier(modifierLocation, amount, operation),
                        accessoriesSlotName,
                        isStackable
                );
            }
            case REMOVE -> {
                newComponent = component.withoutModifier(attributeHolder, modifierLocation);
            }
            case REPLACE -> {
                newComponent = component.withoutModifier(attributeHolder, modifierLocation);
                newComponent = newComponent.withModifierAdded(
                        attributeHolder,
                        new AttributeModifier(modifierLocation, amount, operation),
                        accessoriesSlotName,
                        isStackable
                );
            }
        }

        stack.set(AccessoriesDataComponents.ATTRIBUTES, newComponent);
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
                "morequesttypes.reward.accessories_attribute.title",
                modifyMode.name().toLowerCase(),
                attributeId,
                targetSlot.name().toLowerCase()
        );
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);

        AccessoriesAttributeRewardConfig.AttributeData data = new AccessoriesAttributeRewardConfig.AttributeData();
        data.modifierId = this.modifierId;
        data.attributeId = this.attributeId;
        data.amount = this.amount;
        data.operation = this.operation;
        data.targetSlot = this.targetSlot;
        data.accessoriesSlotName = this.accessoriesSlotName;
        data.isStackable = this.isStackable;
        data.modifyMode = this.modifyMode;
        data.checkHasModifier = this.checkHasModifier;
        data.checkModifierId = this.checkModifierId;

        config.add("config", new AccessoriesAttributeRewardConfig(), data, newData -> {
            this.modifierId = newData.modifierId;
            this.attributeId = newData.attributeId;
            this.amount = newData.amount;
            this.operation = newData.operation;
            this.targetSlot = newData.targetSlot;
            this.accessoriesSlotName = newData.accessoriesSlotName;
            this.isStackable = newData.isStackable;
            this.modifyMode = newData.modifyMode;
            this.checkHasModifier = newData.checkHasModifier;
            this.checkModifierId = newData.checkModifierId;
        }, data).setNameKey("morequesttypes.reward.accessories_attribute.config");
    }

    @Override
    public void writeData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.writeData(nbt, provider);
        nbt.putString("modifier_id", modifierId);
        nbt.putString("attribute_id", attributeId);
        nbt.putDouble("amount", amount);
        nbt.putString("operation", operation.name());
        nbt.putString("target_slot", targetSlot.name());
        nbt.putString("accessories_slot_name", accessoriesSlotName);
        nbt.putBoolean("is_stackable", isStackable);
        nbt.putString("modify_mode", modifyMode.name());

        if (checkHasModifier) {
            nbt.putBoolean("check_has_modifier", checkHasModifier);
            nbt.putString("check_modifier_id", checkModifierId);
        }
    }

    @Override
    public void readData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.readData(nbt, provider);
        modifierId = nbt.getString("modifier_id");
        attributeId = nbt.getString("attribute_id");
        amount = nbt.getDouble("amount");
        operation = AttributeModifier.Operation.valueOf(nbt.getString("operation"));
        targetSlot = TargetSlot.valueOf(nbt.getString("target_slot"));
        accessoriesSlotName = nbt.getString("accessories_slot_name");
        isStackable = nbt.getBoolean("is_stackable");
        modifyMode = ModifyMode.valueOf(nbt.getString("modify_mode"));
        checkHasModifier = nbt.getBoolean("check_has_modifier");
        checkModifierId = nbt.getString("check_modifier_id");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(modifierId);
        buffer.writeUtf(attributeId);
        buffer.writeDouble(amount);
        buffer.writeEnum(operation);
        buffer.writeEnum(targetSlot);
        buffer.writeUtf(accessoriesSlotName);
        buffer.writeBoolean(isStackable);
        buffer.writeEnum(modifyMode);
        buffer.writeBoolean(checkHasModifier);
        buffer.writeUtf(checkModifierId);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        modifierId = buffer.readUtf();
        attributeId = buffer.readUtf();
        amount = buffer.readDouble();
        operation = buffer.readEnum(AttributeModifier.Operation.class);
        targetSlot = buffer.readEnum(TargetSlot.class);
        accessoriesSlotName = buffer.readUtf();
        isStackable = buffer.readBoolean();
        modifyMode = buffer.readEnum(ModifyMode.class);
        checkHasModifier = buffer.readBoolean();
        checkModifierId = buffer.readUtf();
    }
}
