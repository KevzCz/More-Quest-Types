package net.pixeldreamstudios.morequesttypes.api;

import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;

public interface ITaskDynamicDifficultyExtension {
    boolean shouldCheckDynamicDifficultyLevel();
    void setShouldCheckDynamicDifficultyLevel(boolean check);

    ComparisonMode getDynamicDifficultyComparison();
    void setDynamicDifficultyComparison(ComparisonMode mode);

    int getDynamicDifficultyFirst();
    void setDynamicDifficultyFirst(int level);

    int getDynamicDifficultySecond();
    void setDynamicDifficultySecond(int level);
}