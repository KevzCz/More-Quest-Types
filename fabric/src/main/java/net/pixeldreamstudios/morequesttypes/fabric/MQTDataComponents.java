package net.pixeldreamstudios.morequesttypes.fabric;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.morequesttypes.MoreQuestTypes;
import net.pixeldreamstudios.morequesttypes.rewards.manager.EquipmentBonusManager;

public final class MQTDataComponents {
    public static final DataComponentType<EquipmentBonusManager.EquipmentBonuses> EQUIPMENT_BONUSES =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    ResourceLocation.fromNamespaceAndPath(MoreQuestTypes.MOD_ID, "equipment_bonuses"),
                    DataComponentType.<EquipmentBonusManager.EquipmentBonuses>builder()
                            .persistent(EquipmentBonusManager.EquipmentBonuses.CODEC)
                            .networkSynchronized(EquipmentBonusManager.EquipmentBonuses.STREAM_CODEC)
                            .build()
            );

    public static void init() {
    }

    private MQTDataComponents() {}
}