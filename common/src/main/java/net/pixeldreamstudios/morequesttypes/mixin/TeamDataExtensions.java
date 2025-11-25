package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.pixeldreamstudios.morequesttypes.api.ITeamDataExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = TeamData.class, remap = false)
public class TeamDataExtensions implements ITeamDataExtension {
    @Unique
    private final Object2IntMap<String> questCompletionCounts = new Object2IntOpenHashMap<>();

    @Unique
    private String makeKey(long questId, UUID playerId) {
        return questId + ":" + playerId.toString();
    }

    @Override
    public int getQuestCompletionCount(long questId, UUID playerId) {
        return questCompletionCounts.getOrDefault(makeKey(questId, playerId), 0);
    }

    @Override
    public void incrementQuestCompletionCount(long questId, UUID playerId) {
        String key = makeKey(questId, playerId);
        questCompletionCounts.put(key, questCompletionCounts.getOrDefault(key, 0) + 1);
        ((TeamData)(Object)this).markDirty();
    }

    @Override
    public void resetQuestCompletionCount(long questId, UUID playerId) {
        questCompletionCounts.removeInt(makeKey(questId, playerId));
        ((TeamData)(Object)this).markDirty();
    }
    @Inject(method = "serializeNBT", at = @At("RETURN"), remap = false)
    private void saveCompletionCounts(CallbackInfoReturnable<SNBTCompoundTag> cir) {
        if (!questCompletionCounts.isEmpty()) {
            SNBTCompoundTag nbt = cir.getReturnValue();
            CompoundTag countsTag = new CompoundTag();
            questCompletionCounts.forEach((key, count) -> {
                countsTag.putInt(key, count);
            });
            nbt.put("quest_completion_counts", countsTag);
        }
    }

    @Inject(method = "deserializeNBT", at = @At("TAIL"), remap = false)
    private void loadCompletionCounts(SNBTCompoundTag nbt, CallbackInfo ci) {
        questCompletionCounts.clear();
        if (nbt.contains("quest_completion_counts")) {
            CompoundTag countsTag = nbt.getCompound("quest_completion_counts");
            countsTag.getAllKeys().forEach(key -> {
                questCompletionCounts.put(key, countsTag.getInt(key));
            });
        }
    }
}