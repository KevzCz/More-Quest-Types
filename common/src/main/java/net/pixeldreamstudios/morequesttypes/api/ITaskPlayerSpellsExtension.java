package net.pixeldreamstudios.morequesttypes.api;

import java.util.List;

public interface ITaskPlayerSpellsExtension {
    enum MatchMode {
        ALL,
        ANY
    }

    boolean shouldCheckPlayerSpells();

    void setShouldCheckPlayerSpells(boolean check);

    MatchMode getPlayerSpellsMatchMode();

    void setPlayerSpellsMatchMode(MatchMode mode);

    List<String> getRequiredPlayerSpells();

    void setRequiredPlayerSpells(List<String> spells);

    long getPlayerSpellsRequiredCount();

    void setPlayerSpellsRequiredCount(long count);
}
