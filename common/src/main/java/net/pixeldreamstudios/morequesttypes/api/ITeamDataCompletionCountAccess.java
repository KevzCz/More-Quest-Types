package net.pixeldreamstudios.morequesttypes.api;

public interface ITeamDataCompletionCountAccess {
    void mqt$clearCompletionCount(long questId);

    int mqt$getCompletionCountRaw(long questId);
}
