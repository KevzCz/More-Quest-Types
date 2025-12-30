package net.pixeldreamstudios.morequesttypes.api;

import net.pixeldreamstudios.morequesttypes.util.ComparisonMode;

public interface ITaskSkillsExtension {
    boolean shouldCheckSkillsLevel();
    void setShouldCheckSkillsLevel(boolean check);

    SkillsMode getSkillsMode();
    void setSkillsMode(SkillsMode mode);

    ComparisonMode getSkillsComparison();
    void setSkillsComparison(ComparisonMode mode);

    int getSkillsFirstNumber();
    void setSkillsFirstNumber(int value);

    int getSkillsSecondNumber();
    void setSkillsSecondNumber(int value);

    String getSkillsCategoryId();
    void setSkillsCategoryId(String categoryId);

    enum SkillsMode {
        TOTAL_LEVEL,
        CATEGORY_LEVEL
    }
}