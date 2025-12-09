package net.pixeldreamstudios.morequesttypes.client;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.injectables.annotations.ExpectPlatform;
import org.lwjgl.glfw.GLFW;

public class MoreQuestTypesClient {
    private static long lastKeyPressTime = 0;
    private static final long KEY_PRESS_COOLDOWN = 500;

    public static void init() {
        MoreQuestTypesKeys.register();
        initPlatformRenderer();
        ClientRawInputEvent.KEY_PRESSED.register((client, keyCode, scanCode, action, modifiers) -> {
            if (client.player == null) {
                return EventResult.pass();
            }

            if (action != GLFW.GLFW_PRESS) {
                return EventResult.pass();
            }

            if (client.screen != null) {
                return EventResult.pass();
            }

            if (MoreQuestTypesKeys.SEARCH_QUEST_BY_ITEM.matches(keyCode, scanCode)) {
                long currentTime = System.currentTimeMillis();

                if (currentTime - lastKeyPressTime < KEY_PRESS_COOLDOWN) {
                    return EventResult.pass();
                }

                lastKeyPressTime = currentTime;
                QuestSearchKeybind.searchOrOpenLinkedQuest(client.player);
                return EventResult.interruptTrue();
            }

            return EventResult.pass();
        });
    }
    @ExpectPlatform
    public static void initPlatformRenderer() {
        throw new AssertionError();
    }
}