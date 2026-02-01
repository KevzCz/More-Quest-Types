package net.pixeldreamstudios.morequesttypes.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.rewards.manager.EquipmentBonusManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

@Mixin(ItemStack.class)
public abstract class ItemStackAttributeMixin {

    @Inject(
            method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V",
            at = @At("RETURN")
    )
    private void morequesttypes$addQuestBonusesSlotGroup(
            EquipmentSlotGroup slotGroup,
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
            CallbackInfo ci
    ) {
        ItemStack self = (ItemStack) (Object) this;
        addBonusesForSlot(self, slotGroup.getSerializedName(), consumer);
    }

    @Inject(
            method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
            at = @At("RETURN")
    )
    private void morequesttypes$addQuestBonusesSlot(
            EquipmentSlot slot,
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
            CallbackInfo ci
    ) {
        ItemStack self = (ItemStack) (Object) this;
        addBonusesForSlot(self, slot.getName(), consumer);
    }

    private static void addBonusesForSlot(ItemStack stack, String slotName, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        EquipmentBonusManager.getBonuses(stack).ifPresent(bonuses -> {
            for (var bonus : bonuses.bonuses()) {
                if (!bonus.slot().equals(slotName)) {
                    continue;
                }

                ResourceLocation attrId = ResourceLocation.tryParse(bonus.attributeId());
                if (attrId == null) continue;

                Holder<Attribute> attrHolder = BuiltInRegistries.ATTRIBUTE.getHolder(attrId).orElse(null);
                if (attrHolder == null) continue;

                ResourceLocation modId = ResourceLocation.tryParse(bonus.modifierId());
                if (modId == null) {
                    modId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                }

                AttributeModifier modifier = new AttributeModifier(
                        modId,
                        bonus.amount(),
                        bonus.operation()
                );

                consumer.accept(attrHolder, modifier);
            }
        });
    }
}