package net.pixeldreamstudios.morequesttypes.api;

import net.minecraft.world.item.ItemStack;

public interface IQuestExtension {
    int getMaxRepeats();
    void setMaxRepeats(int maxRepeats);
    boolean isAlwaysInvisible();
    void setAlwaysInvisible(boolean alwaysInvisible);
    ItemStack getLinkedItem();
    void setLinkedItem(ItemStack linkedItem);
}