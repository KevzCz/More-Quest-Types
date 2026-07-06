package net.pixeldreamstudios.morequesttypes.config;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.util.ItemNbtFilterData;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class ItemNbtConfigPanels {
    private ItemNbtConfigPanels() {
    }

    public static void addNbtMatching(ConfigGroup config, List<String> nbtFilters, List<String> nbtIgnorePaths) {
        addNbtMatching(config, nbtFilters, nbtIgnorePaths, ItemStack.EMPTY);
    }

    public static void addNbtMatching(ConfigGroup config, List<String> nbtFilters, List<String> nbtIgnorePaths, ItemStack previewItem) {
        ItemNbtFilterData data = ItemNbtFilterData.fromLists(nbtFilters, nbtIgnorePaths);
        if (!previewItem.isEmpty()
                && (data.previewItem.isEmpty() || data.previewItem.getItem() != previewItem.getItem())) {
            data.previewItem = previewItem.copyWithCount(1);
        }

        ItemNbtMatcherConfig configValue = new ItemNbtMatcherConfig();
        configValue.setInitialPreview(previewItem);
        config.add("nbt_matching", configValue, data, newData -> newData.applyTo(nbtFilters, nbtIgnorePaths), data)
                .setNameKey("morequesttypes.config.item_nbt_matching");
    }
}
