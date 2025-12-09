package net.pixeldreamstudios.morequesttypes.api;

public interface IRewardDynamicDifficultyExtension {
    boolean shouldSetMobLevel();
    void setShouldSetMobLevel(boolean set);

    int getMobLevel();
    void setMobLevel(int level);
}