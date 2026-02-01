package net.pixeldreamstudios.morequesttypes.neoforge;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.rewards.manager.EquipmentBonusManager;

import java.util.function.Supplier;

public final class MQTDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MoreQuestTypes.MOD_ID);

    public static final Supplier<DataComponentType<EquipmentBonusManager.EquipmentBonuses>> EQUIPMENT_BONUSES =
            DATA_COMPONENTS.register("equipment_bonuses", () -> DataComponentType.<EquipmentBonusManager.EquipmentBonuses>builder()
                    .persistent(EquipmentBonusManager.EquipmentBonuses.CODEC)
                    .networkSynchronized(EquipmentBonusManager.EquipmentBonuses.STREAM_CODEC)
                    .build());

    private MQTDataComponents() {}
}