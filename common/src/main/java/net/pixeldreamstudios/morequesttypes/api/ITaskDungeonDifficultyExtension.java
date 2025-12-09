package net.pixeldreamstudios.morequesttypes.api;

import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;

public interface ITaskDungeonDifficultyExtension {
    boolean shouldCheckDungeonDifficultyLevel();
    void setShouldCheckDungeonDifficultyLevel(boolean check);

    ComparisonMode getDungeonDifficultyComparison();
    void setDungeonDifficultyComparison(ComparisonMode mode);

    int getDungeonDifficultyFirst();
    void setDungeonDifficultyFirst(int level);

    int getDungeonDifficultySecond();
    void setDungeonDifficultySecond(int level);
}