package net.pixeldreamstudios.morequesttypes.rewards.manager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class EquipmentBonusManager {
    public static Supplier<DataComponentType<EquipmentBonuses>> EQUIPMENT_BONUSES;

    public static void init(Supplier<DataComponentType<EquipmentBonuses>> supplier) {
        EQUIPMENT_BONUSES = supplier;
    }

    public enum ModifyMode {
        ADD,
        REMOVE,
        SET,
        ADD_TO,
        SUBTRACT_FROM,
        REPLACE_WITH
    }

    public record BonusEntry(
            String modifierId,
            String attributeId,
            double amount,
            AttributeModifier.Operation operation,
            String slot
    ) {
        public static final Codec<BonusEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("modifier_id").forGetter(BonusEntry::modifierId),
                Codec.STRING.fieldOf("attribute").forGetter(BonusEntry::attributeId),
                Codec.DOUBLE.fieldOf("amount").forGetter(BonusEntry::amount),
                Codec.STRING.fieldOf("operation").xmap(
                        AttributeModifier.Operation::valueOf,
                        Enum::name
                ).forGetter(BonusEntry::operation),
                Codec.STRING.fieldOf("slot").forGetter(BonusEntry::slot)
        ).apply(instance, BonusEntry::new));

        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, BonusEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, BonusEntry::modifierId,
                ByteBufCodecs.STRING_UTF8, BonusEntry::attributeId,
                ByteBufCodecs.DOUBLE, BonusEntry::amount,
                ByteBufCodecs.fromCodec(Codec.STRING.xmap(
                        AttributeModifier.Operation::valueOf,
                        Enum::name
                )), BonusEntry::operation,
                ByteBufCodecs.STRING_UTF8, BonusEntry::slot,
                BonusEntry::new
        );
    }

    public record EquipmentBonuses(List<BonusEntry> bonuses) {
        public static final Codec<EquipmentBonuses> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BonusEntry.CODEC.listOf().fieldOf("bonuses").forGetter(EquipmentBonuses::bonuses)
        ).apply(instance, EquipmentBonuses::new));

        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, EquipmentBonuses> STREAM_CODEC =
                BonusEntry.STREAM_CODEC.apply(ByteBufCodecs.list())
                        .map(EquipmentBonuses::new, EquipmentBonuses::bonuses);
    }

    public static void modifyBonus(
            ItemStack stack,
            String modifierId,
            String attributeId,
            double amount,
            AttributeModifier.Operation operation,
            String slot,
            ModifyMode mode,
            boolean checkAttributeExists,
            boolean checkConditionAttributeExists,
            String conditionAttribute,
            net.pixeldreamstudios.morequesttypes.util.ComparisonMode comparisonMode,
            double conditionFirst,
            double conditionSecond,
            String replaceWithAttribute,
            Double replaceWithValue
    ) {
        if (stack.isEmpty() || EQUIPMENT_BONUSES == null) return;

        EquipmentBonuses existing = stack.get(EQUIPMENT_BONUSES.get());
        List<BonusEntry> bonuses = existing != null ? new ArrayList<>(existing.bonuses()) : new ArrayList<>();

        if (checkAttributeExists) {
            boolean attributeExists = bonuses.stream()
                    .anyMatch(entry -> entry.attributeId().equals(attributeId) && entry.slot().equals(slot));
            if (!attributeExists) return;
        }

        if (conditionAttribute != null && !conditionAttribute.isEmpty()) {
            if (checkConditionAttributeExists) {
                boolean conditionExists = bonuses.stream()
                        .anyMatch(entry -> entry.attributeId().equals(conditionAttribute) && entry.slot().equals(slot));

                if (!conditionExists) {
                    double middleValue = (conditionFirst + conditionSecond) / 2.0;
                    if (!comparisonMode.compare((int) middleValue, (int) conditionFirst, (int) conditionSecond)) {
                        return;
                    }
                } else {
                    if (!checkCondition(bonuses, conditionAttribute, slot, comparisonMode, conditionFirst, conditionSecond)) {
                        return;
                    }
                }
            } else {
                if (!checkCondition(bonuses, conditionAttribute, slot, comparisonMode, conditionFirst, conditionSecond)) {
                    return;
                }
            }
        }

        String finalModifierId = (modifierId == null || modifierId.isEmpty())
                ? net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()
                : modifierId;

        switch (mode) {
            case ADD -> bonuses.add(new BonusEntry(finalModifierId, attributeId, amount, operation, slot));

            case REMOVE -> bonuses.removeIf(entry ->
                    entry.modifierId().equals(finalModifierId) &&
                            entry.attributeId().equals(attributeId) &&
                            entry.slot().equals(slot)
            );

            case SET -> {
                bonuses.removeIf(entry ->
                        entry.modifierId().equals(finalModifierId) &&
                                entry.attributeId().equals(attributeId) &&
                                entry.slot().equals(slot)
                );
                bonuses.add(new BonusEntry(finalModifierId, attributeId, amount, operation, slot));
            }

            case ADD_TO -> {
                boolean found = false;
                for (int i = 0; i < bonuses.size(); i++) {
                    BonusEntry entry = bonuses.get(i);
                    if (entry.modifierId().equals(finalModifierId) &&
                            entry.attributeId().equals(attributeId) &&
                            entry.slot().equals(slot)) {
                        bonuses.set(i, new BonusEntry(
                                entry.modifierId(),
                                entry.attributeId(),
                                entry.amount() + amount,
                                entry.operation(),
                                entry.slot()
                        ));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    bonuses.add(new BonusEntry(finalModifierId, attributeId, amount, operation, slot));
                }
            }

            case SUBTRACT_FROM -> {
                for (int i = 0; i < bonuses.size(); i++) {
                    BonusEntry entry = bonuses.get(i);
                    if (entry.modifierId().equals(finalModifierId) &&
                            entry.attributeId().equals(attributeId) &&
                            entry.slot().equals(slot)) {
                        double newAmount = entry.amount() - amount;
                        if (newAmount <= 0) {
                            bonuses.remove(i);
                        } else {
                            bonuses.set(i, new BonusEntry(
                                    entry.modifierId(),
                                    entry.attributeId(),
                                    newAmount,
                                    entry.operation(),
                                    entry.slot()
                            ));
                        }
                        break;
                    }
                }
            }

            case REPLACE_WITH -> {
                String newAttribute = (replaceWithAttribute != null && !replaceWithAttribute.isEmpty())
                        ? replaceWithAttribute
                        : attributeId;

                for (int i = 0; i < bonuses.size(); i++) {
                    BonusEntry entry = bonuses.get(i);
                    if (entry.modifierId().equals(finalModifierId) &&
                            entry.attributeId().equals(attributeId) &&
                            entry.slot().equals(slot)) {
                        double newAmount = (replaceWithValue != null) ? replaceWithValue : entry.amount();
                        bonuses.set(i, new BonusEntry(
                                finalModifierId,
                                newAttribute,
                                newAmount,
                                operation,
                                slot
                        ));
                        break;
                    }
                }
            }
        }

        if (bonuses.isEmpty()) {
            stack.remove(EQUIPMENT_BONUSES.get());
        } else {
            stack.set(EQUIPMENT_BONUSES.get(), new EquipmentBonuses(bonuses));
        }
    }

    private static boolean checkCondition(
            List<BonusEntry> bonuses,
            String conditionAttribute,
            String slot,
            net.pixeldreamstudios.morequesttypes.util.ComparisonMode comparisonMode,
            double first,
            double second
    ) {
        Optional<BonusEntry> matching = bonuses.stream()
                .filter(entry -> entry.attributeId().equals(conditionAttribute) && entry.slot().equals(slot))
                .findFirst();

        if (matching.isEmpty()) {
            return comparisonMode.compare(0, (int) first, (int) second);
        }

        double value = matching.get().amount();
        return comparisonMode.compare((int) value, (int) first, (int) second);
    }

    public static Optional<EquipmentBonuses> getBonuses(ItemStack stack) {
        if (EQUIPMENT_BONUSES == null) return Optional.empty();
        return Optional.ofNullable(stack.get(EQUIPMENT_BONUSES.get()));
    }

    private EquipmentBonusManager() {}
}