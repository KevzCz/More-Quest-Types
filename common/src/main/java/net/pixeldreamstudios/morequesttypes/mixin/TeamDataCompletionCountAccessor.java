package net.pixeldreamstudios.morequesttypes.mixin;

import dev.ftb.mods.ftbquests.quest.TeamData;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import net.pixeldreamstudios.morequesttypes.api.ITeamDataCompletionCountAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TeamData.class, remap = false)
public abstract class TeamDataCompletionCountAccessor implements ITeamDataCompletionCountAccess {

    @Shadow
    private Long2IntMap completionCount;

    @Override
    public void mqt$clearCompletionCount(long questId) {
        this.completionCount.remove(questId);
    }

    @Override
    public int mqt$getCompletionCountRaw(long questId) {
        return this.completionCount.getOrDefault(questId, 0);
    }
}
