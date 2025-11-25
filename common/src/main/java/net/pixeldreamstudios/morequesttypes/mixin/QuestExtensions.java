package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.util.ProgressChange;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.morequesttypes.api.IQuestExtension;
import net.pixeldreamstudios.morequesttypes.api.ITeamDataExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = Quest.class, remap = false)
public abstract class QuestExtensions implements IQuestExtension {
    @Unique
    private int maxRepeats = 0;

    @Unique
    private boolean alwaysInvisible = false;

    @Unique
    private ItemStack linkedItem = ItemStack.EMPTY;

    @Inject(method = "writeData", at = @At("TAIL"), remap = false)
    private void writeCustomData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        if (maxRepeats > 0) {
            nbt.putInt("max_repeats", maxRepeats);
        }
        if (alwaysInvisible) {
            nbt.putBoolean("always_invisible", true);
        }
        if (!linkedItem.isEmpty()) {
            nbt.put("linked_item", linkedItem.save(provider));
        }
    }

    @Inject(method = "readData", at = @At("TAIL"), remap = false)
    private void readCustomData(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci) {
        maxRepeats = nbt.getInt("max_repeats");
        alwaysInvisible = nbt.getBoolean("always_invisible");
        if (nbt.contains("linked_item")) {
            linkedItem = ItemStack.parseOptional(provider, nbt.getCompound("linked_item"));
        } else {
            linkedItem = ItemStack.EMPTY;
        }
    }

    @Inject(method = "writeNetData", at = @At("TAIL"), remap = false)
    private void writeCustomNetData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        buffer.writeVarInt(maxRepeats);
        buffer.writeBoolean(alwaysInvisible);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, linkedItem);
    }

    @Inject(method = "readNetData", at = @At("TAIL"), remap = false)
    private void readCustomNetData(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        maxRepeats = buffer.readVarInt();
        alwaysInvisible = buffer.readBoolean();
        linkedItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
    }

    @Inject(method = "fillConfigGroup", at = @At("TAIL"), remap = false)
    private void addCustomConfig(ConfigGroup config, CallbackInfo ci) {
        Quest self = (Quest) (Object) this;

        ConfigGroup misc = config.getOrCreateSubgroup("misc");
        misc.addInt("max_repeats", maxRepeats, (v) -> {
            maxRepeats = v;
        }, 0, 0, Integer.MAX_VALUE).setCanEdit(self.canBeRepeated()).setNameKey("morequesttypes.quest.max_repeats");

        ConfigGroup visibility = config.getOrCreateSubgroup("visibility");
        visibility.addBool("always_invisible", alwaysInvisible, (v) -> {
            alwaysInvisible = v;
        }, false).setNameKey("morequesttypes.quest.always_invisible");

        visibility.addItemStack("linked_item", linkedItem, (v) -> {
            linkedItem = v;
        }, ItemStack.EMPTY, true, false).setNameKey("morequesttypes.quest.linked_item");
    }

    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true, remap = false)
    private void handleAlwaysInvisible(TeamData data, CallbackInfoReturnable<Boolean> cir) {
        if (alwaysInvisible) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "checkRepeatable", at = @At("HEAD"), cancellable = true, remap = false)
    private void checkRepeatLimit(TeamData data, UUID player, CallbackInfo ci) {
        if (maxRepeats <= 0) {
            return;
        }

        Quest self = (Quest) (Object) this;
        int completionCount = ((ITeamDataExtension) data).getQuestCompletionCount(self.id, player);

        if (completionCount >= maxRepeats) {
            ci.cancel();
        }
    }

    @Inject(method = "checkRepeatable", at = @At("HEAD"), remap = false)
    private void trackCompletion(TeamData data, UUID player, CallbackInfo ci) {
        Quest self = (Quest) (Object) this;
        if (self.canBeRepeated()) {
            ((ITeamDataExtension) data).incrementQuestCompletionCount(self.id, player);
        }
    }

    @Inject(method = "forceProgress", at = @At("HEAD"), remap = false)
    private void resetCompletionCountOnManualReset(TeamData teamData, ProgressChange progressChange, CallbackInfo ci) {
        if (progressChange.shouldReset()) {
            Quest self = (Quest) (Object) this;

            if (!teamData.isCompleted(self)) {
                UUID player = progressChange.getPlayerId();
                if (player != null) {
                    ((ITeamDataExtension) teamData).resetQuestCompletionCount(self.id, player);
                } else {
                    UUID teamOwner = teamData.getTeamId();
                    ((ITeamDataExtension) teamData).resetQuestCompletionCount(self.id, teamOwner);
                }
            }
        }
    }

    @Override
    public int getMaxRepeats() {
        return maxRepeats;
    }

    @Override
    public void setMaxRepeats(int maxRepeats) {
        this.maxRepeats = maxRepeats;
    }

    @Override
    public boolean isAlwaysInvisible() {
        return alwaysInvisible;
    }

    @Override
    public void setAlwaysInvisible(boolean alwaysInvisible) {
        this.alwaysInvisible = alwaysInvisible;
    }

    @Override
    public ItemStack getLinkedItem() {
        return linkedItem;
    }

    @Override
    public void setLinkedItem(ItemStack linkedItem) {
        this.linkedItem = linkedItem;
    }
}