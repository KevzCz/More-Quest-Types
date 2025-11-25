package net.pixeldreamstudios.morequesttypes.client;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.ConfigQuestObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.api.IQuestExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class QuestSearchKeybind {

    public static void searchOrOpenLinkedQuest(LocalPlayer player) {
        if (!ClientQuestFile.exists()) {
            player.displayClientMessage(
                    Component.translatable("morequesttypes.search.no_quest_file").withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("morequesttypes.search.no_item").withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        List<Quest> questsWithLinkedItem = findQuestsWithLinkedItem(heldItem);
        if (!questsWithLinkedItem.isEmpty()) {
            if (questsWithLinkedItem.size() == 1) {
                Quest quest = questsWithLinkedItem.get(0);
                ClientQuestFile.openBookToQuestObject(quest.id);
                player.displayClientMessage(
                        Component.translatable("morequesttypes.link.opened_quest_item", quest.getTitle())
                                .withStyle(ChatFormatting.GREEN),
                        true
                );
            } else {
                openQuestSelectionScreen(questsWithLinkedItem, heldItem);
                player.displayClientMessage(
                        Component.translatable("morequesttypes.link.multiple_linked", questsWithLinkedItem.size())
                                .withStyle(ChatFormatting.GREEN),
                        true
                );
            }
            return;
        }

        long questId = getLinkedQuestId(heldItem);
        if (questId != 0) {
            Quest linkedQuest = ClientQuestFile.INSTANCE.getQuest(questId);
            if (linkedQuest != null && !ClientQuestFile.INSTANCE.selfTeamData.isCompleted(linkedQuest)) {
                ClientQuestFile.openBookToQuestObject(questId);
                player.displayClientMessage(
                        Component.translatable("morequesttypes.link.opened", linkedQuest.getTitle())
                                .withStyle(ChatFormatting.GREEN),
                        true
                );
                return;
            }
        }

        List<Quest> matchingQuests = findQuestsWithItem(heldItem);

        if (matchingQuests.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("morequesttypes.search.no_quests_found", heldItem.getHoverName())
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
        } else if (matchingQuests.size() == 1) {
            ClientQuestFile.openBookToQuestObject(matchingQuests.get(0).id);
            player.displayClientMessage(
                    Component.translatable("morequesttypes.search.found_one", matchingQuests.get(0).getTitle())
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
        } else {
            openQuestSelectionScreen(matchingQuests, heldItem);
            player.displayClientMessage(
                    Component.translatable("morequesttypes.search.found_multiple", matchingQuests.size())
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
        }
    }

    private static List<Quest> findQuestsWithLinkedItem(ItemStack heldItem) {
        List<Quest> linkedQuests = new ArrayList<>();

        for (QuestObjectBase obj : ClientQuestFile.INSTANCE.getAllObjects()) {
            if (obj instanceof Quest quest) {
                if (ClientQuestFile.INSTANCE.selfTeamData.isCompleted(quest)) {
                    continue;
                }

                if (!ClientQuestFile.INSTANCE.canEdit() && !quest.isVisible(ClientQuestFile.INSTANCE.selfTeamData)) {
                    continue;
                }

                IQuestExtension ext = (IQuestExtension) (Object) quest;
                ItemStack linkedItem = ext.getLinkedItem();

                if (!linkedItem.isEmpty()) {
                    if (ItemMatchingSystem.INSTANCE.doesItemMatch(linkedItem, heldItem,
                            ItemMatchingSystem.ComponentMatchType.STRICT)) {
                        linkedQuests.add(quest);
                    }
                }
            }
        }

        return linkedQuests;
    }

    private static List<Quest> findQuestsWithItem(ItemStack stack) {
        List<Quest> matchingQuests = new ArrayList<>();

        for (QuestObjectBase obj : ClientQuestFile.INSTANCE.getAllObjects()) {
            if (obj instanceof Quest quest) {
                if (ClientQuestFile.INSTANCE.selfTeamData.isCompleted(quest)) {
                    continue;
                }

                if (!ClientQuestFile.INSTANCE.canEdit() && !quest.isVisible(ClientQuestFile.INSTANCE.selfTeamData)) {
                    continue;
                }

                for (Task task : quest.getTasksAsList()) {
                    if (task instanceof ItemTask itemTask) {
                        if (itemTask.test(stack)) {
                            matchingQuests.add(quest);
                            break;
                        }
                    }
                }
            }
        }

        return matchingQuests;
    }

    private static void openQuestSelectionScreen(List<Quest> quests, ItemStack searchedItem) {
        Predicate<QuestObjectBase> filter = quests::contains;
        ConfigQuestObject<Quest> config = new ConfigQuestObject<>(filter);

        new QuestSearchResultsScreen<>(config, accepted -> {
            if (accepted && config.getValue() != null) {
                ClientQuestFile.openBookToQuestObject(config.getValue().id);
            }
        }, searchedItem).openGui();
    }

    private static long getLinkedQuestId(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("LinkedQuestId")) {
                return tag.getLong("LinkedQuestId");
            }
        }
        return 0L;
    }
}