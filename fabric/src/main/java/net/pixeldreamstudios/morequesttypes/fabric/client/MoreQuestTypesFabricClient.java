package net.pixeldreamstudios.morequesttypes.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.pixeldreamstudios.morequesttypes.client.MoreQuestTypesClient;

public final class MoreQuestTypesFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MoreQuestTypesClient.init();
    }
}