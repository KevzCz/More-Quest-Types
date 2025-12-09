package net.pixeldreamstudios.morequesttypes.api;

import java.util.UUID;

public interface IQuestSummonedEntity {
    UUID getQuestOwnerUuid();
    void setQuestOwnerUuid(UUID uuid);

    String getQuestTexture();
    void setQuestTexture(String textureId);

    boolean isQuestSummoned();
    void setQuestSummoned(boolean summoned);

    float getQuestTextureScale();
    void setQuestTextureScale(float scale);

    long getQuestRewardId();
    void setQuestRewardId(long rewardId);

    int getQuestDurationTicks();
    void setQuestDurationTicks(int ticks);

    double getQuestDespawnDistance();
    void setQuestDespawnDistance(double distance);

    boolean getQuestDespawnIfOffline();
    void setQuestDespawnIfOffline(boolean despawn);

    boolean getQuestDespawnIfDead();
    void setQuestDespawnIfDead(boolean despawn);

    boolean getQuestFollow();
    void setQuestFollow(boolean follow);

    long getQuestSpawnTime();
    void setQuestSpawnTime(long time);

    double getQuestTextureOffsetX();
    void setQuestTextureOffsetX(double offset);

    double getQuestTextureOffsetY();
    void setQuestTextureOffsetY(double offset);

    double getQuestTextureOffsetZ();
    void setQuestTextureOffsetZ(double offset);
}