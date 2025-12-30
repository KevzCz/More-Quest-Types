package net.pixeldreamstudios.morequesttypes.api;

import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;

public interface ITaskLevelZExtension {
    boolean shouldCheckLevelZ();
    void setShouldCheckLevelZ(boolean check);

    LevelZMode getLevelZMode();
    void setLevelZMode(LevelZMode mode);

    ComparisonMode getLevelZComparison();
    void setLevelZComparison(ComparisonMode mode);

    int getLevelZFirstNumber();
    void setLevelZFirstNumber(int value);

    int getLevelZSecondNumber();
    void setLevelZSecondNumber(int value);

    int getLevelZSkillId();
    void setLevelZSkillId(int skillId);

    enum LevelZMode {
        LEVEL,
        TOTAL_LEVEL
    }
}