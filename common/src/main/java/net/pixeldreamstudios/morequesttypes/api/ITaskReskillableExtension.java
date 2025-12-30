package net.pixeldreamstudios.morequesttypes.api;

import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;

public interface ITaskReskillableExtension {
    boolean shouldCheckReskillable();
    void setShouldCheckReskillable(boolean check);

    ReskillableMode getReskillableMode();
    void setReskillableMode(ReskillableMode mode);

    ComparisonMode getReskillableComparison();
    void setReskillableComparison(ComparisonMode mode);

    int getReskillableFirstNumber();
    void setReskillableFirstNumber(int value);

    int getReskillableSecondNumber();
    void setReskillableSecondNumber(int value);

    int getReskillableSkillIndex();
    void setReskillableSkillIndex(int skillIndex);

    enum ReskillableMode {
        SKILL_LEVEL,
        TOTAL_LEVEL
    }
}