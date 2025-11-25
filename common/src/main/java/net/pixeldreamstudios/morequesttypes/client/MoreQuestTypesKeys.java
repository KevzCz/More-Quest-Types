package net.pixeldreamstudios.morequesttypes.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;

public class MoreQuestTypesKeys {
    public static final String CATEGORY = "key.categories.morequesttypes";

    public static final KeyMapping SEARCH_QUEST_BY_ITEM = new KeyMapping(
            "key.morequesttypes.search_quest_by_item",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            CATEGORY
    );

    public static void register() {
        KeyMappingRegistry.register(SEARCH_QUEST_BY_ITEM);
    }
}