package net.pixeldreamstudios.morequesttypes.api;

import java.util.UUID;

public interface ITeamDataExtension {
    int getQuestCompletionCount(long questId, UUID playerId);
    void incrementQuestCompletionCount(long questId, UUID playerId);
    void resetQuestCompletionCount(long questId, UUID playerId);
}