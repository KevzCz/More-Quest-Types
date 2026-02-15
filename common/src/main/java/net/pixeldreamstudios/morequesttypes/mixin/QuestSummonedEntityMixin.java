package net.pixeldreamstudios.morequesttypes.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.pixeldreamstudios.morequesttypes.api.IQuestSummonedEntity;
import net.pixeldreamstudios.morequesttypes.mixin.accessor.MobAccessor;
import net.pixeldreamstudios.morequesttypes.network.NetworkHelper;
import net.pixeldreamstudios.morequesttypes.network.QuestEntityDataSyncPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class QuestSummonedEntityMixin implements IQuestSummonedEntity {

    @Unique
    private UUID mqt$questOwnerUuid = null;

    @Unique
    private String mqt$questTexture = "";

    @Unique
    private boolean mqt$questSummoned = false;

    @Unique
    private float mqt$questTextureScale = 1.0f;

    @Unique
    private long mqt$questRewardId = -1L;

    @Unique
    private int mqt$questDurationTicks = -1;

    @Unique
    private double mqt$questDespawnDistance = 64.0;

    @Unique
    private boolean mqt$questDespawnIfOffline = true;

    @Unique
    private boolean mqt$questDespawnIfDead = true;

    @Unique
    private boolean mqt$questFollow = false;

    @Unique
    private long mqt$questSpawnTime = 0L;

    @Unique
    private boolean mqt$followGoalInitialized = false;

    @Unique
    private double mqt$questTextureOffsetX = 0.0;

    @Unique
    private double mqt$questTextureOffsetY = 0.0;

    @Unique
    private double mqt$questTextureOffsetZ = 0.0;

    @Unique
    private String mqt$questCustomName = "";

    @Unique
    private String mqt$questCustomDrops = "";

    @Unique
    private String mqt$questEquipmentDropRates = "";

    @Unique
    private String mqt$questRewardTables = "";

    @Override
    public UUID getQuestOwnerUuid() {
        return mqt$questOwnerUuid;
    }

    @Override
    public void setQuestOwnerUuid(UUID uuid) {
        this.mqt$questOwnerUuid = uuid;
    }

    @Override
    public String getQuestTexture() {
        return mqt$questTexture;
    }

    @Override
    public void setQuestTexture(String textureId) {
        this.mqt$questTexture = textureId;
    }

    @Override
    public boolean isQuestSummoned() {
        return mqt$questSummoned;
    }

    @Override
    public void setQuestSummoned(boolean summoned) {
        this.mqt$questSummoned = summoned;
    }

    @Override
    public float getQuestTextureScale() {
        return mqt$questTextureScale;
    }

    @Override
    public void setQuestTextureScale(float scale) {
        this.mqt$questTextureScale = scale;
    }

    @Override
    public long getQuestRewardId() {
        return mqt$questRewardId;
    }

    @Override
    public void setQuestRewardId(long rewardId) {
        this.mqt$questRewardId = rewardId;
    }

    @Override
    public int getQuestDurationTicks() {
        return mqt$questDurationTicks;
    }

    @Override
    public void setQuestDurationTicks(int ticks) {
        this.mqt$questDurationTicks = ticks;
    }

    @Override
    public double getQuestDespawnDistance() {
        return mqt$questDespawnDistance;
    }

    @Override
    public void setQuestDespawnDistance(double distance) {
        this.mqt$questDespawnDistance = distance;
    }

    @Override
    public boolean getQuestDespawnIfOffline() {
        return mqt$questDespawnIfOffline;
    }

    @Override
    public void setQuestDespawnIfOffline(boolean despawn) {
        this.mqt$questDespawnIfOffline = despawn;
    }

    @Override
    public boolean getQuestDespawnIfDead() {
        return mqt$questDespawnIfDead;
    }

    @Override
    public void setQuestDespawnIfDead(boolean despawn) {
        this.mqt$questDespawnIfDead = despawn;
    }

    @Override
    public boolean getQuestFollow() {
        return mqt$questFollow;
    }

    @Override
    public void setQuestFollow(boolean follow) {
        this.mqt$questFollow = follow;
        this.mqt$followGoalInitialized = false;
    }

    @Override
    public long getQuestSpawnTime() {
        return mqt$questSpawnTime;
    }

    @Override
    public void setQuestSpawnTime(long time) {
        this.mqt$questSpawnTime = time;
    }

    @Override
    public double getQuestTextureOffsetX() {
        return mqt$questTextureOffsetX;
    }

    @Override
    public void setQuestTextureOffsetX(double offset) {
        this.mqt$questTextureOffsetX = offset;
    }

    @Override
    public double getQuestTextureOffsetY() {
        return mqt$questTextureOffsetY;
    }

    @Override
    public void setQuestTextureOffsetY(double offset) {
        this.mqt$questTextureOffsetY = offset;
    }

    @Override
    public double getQuestTextureOffsetZ() {
        return mqt$questTextureOffsetZ;
    }

    @Override
    public void setQuestTextureOffsetZ(double offset) {
        this.mqt$questTextureOffsetZ = offset;
    }

    @Override
    public String getQuestCustomName() {
        return mqt$questCustomName;
    }

    @Override
    public void setQuestCustomName(String name) {
        this.mqt$questCustomName = name;
    }

    @Override
    public String getQuestCustomDrops() {
        return mqt$questCustomDrops;
    }

    @Override
    public void setQuestCustomDrops(String drops) {
        this.mqt$questCustomDrops = drops;
    }

    @Override
    public String getQuestEquipmentDropRates() {
        return mqt$questEquipmentDropRates;
    }

    @Override
    public void setQuestEquipmentDropRates(String rates) {
        this.mqt$questEquipmentDropRates = rates;
    }

    @Override
    public String getQuestRewardTables() {
        return mqt$questRewardTables;
    }

    @Override
    public void setQuestRewardTables(String rewardTables) {
        this.mqt$questRewardTables = rewardTables;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void mqt$saveQuestData(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
        if (mqt$questSummoned) {
            CompoundTag questData = new CompoundTag();
            questData.putBoolean("QuestSummoned", true);

            if (mqt$questOwnerUuid != null) {
                questData.putUUID("QuestOwner", mqt$questOwnerUuid);
            }

            if (mqt$questTexture != null && !mqt$questTexture.isEmpty()) {
                questData.putString("QuestTexture", mqt$questTexture);
            }

            questData.putFloat("QuestTextureScale", mqt$questTextureScale);
            questData.putLong("QuestRewardId", mqt$questRewardId);
            questData.putInt("QuestDurationTicks", mqt$questDurationTicks);
            questData.putDouble("QuestDespawnDistance", mqt$questDespawnDistance);
            questData.putBoolean("QuestDespawnIfOffline", mqt$questDespawnIfOffline);
            questData.putBoolean("QuestDespawnIfDead", mqt$questDespawnIfDead);
            questData.putBoolean("QuestFollow", mqt$questFollow);
            questData.putLong("QuestSpawnTime", mqt$questSpawnTime);

            questData.putDouble("QuestTextureOffsetX", mqt$questTextureOffsetX);
            questData.putDouble("QuestTextureOffsetY", mqt$questTextureOffsetY);
            questData.putDouble("QuestTextureOffsetZ", mqt$questTextureOffsetZ);

            if (!mqt$questCustomName.isEmpty()) {
                questData.putString("QuestCustomName", mqt$questCustomName);
            }

            if (!mqt$questCustomDrops.isEmpty()) {
                questData.putString("QuestCustomDrops", mqt$questCustomDrops);
            }

            if (!mqt$questEquipmentDropRates.isEmpty()) {
                questData.putString("QuestEquipmentDropRates", mqt$questEquipmentDropRates);
            }

            if (!mqt$questRewardTables.isEmpty()) {
                questData.putString("QuestRewardTables", mqt$questRewardTables);
            }

            nbt.put("MQTData", questData);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void mqt$loadQuestData(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("MQTData")) {
            CompoundTag questData = nbt.getCompound("MQTData");

            mqt$questSummoned = questData.getBoolean("QuestSummoned");

            if (questData.contains("QuestOwner")) {
                mqt$questOwnerUuid = questData.getUUID("QuestOwner");
            }

            if (questData.contains("QuestTexture")) {
                mqt$questTexture = questData.getString("QuestTexture");
            }

            mqt$questTextureScale = questData.contains("QuestTextureScale") ? questData.getFloat("QuestTextureScale") : 1.0f;
            mqt$questRewardId = questData.contains("QuestRewardId") ? questData.getLong("QuestRewardId") : -1L;
            mqt$questDurationTicks = questData.contains("QuestDurationTicks") ? questData.getInt("QuestDurationTicks") : -1;
            mqt$questDespawnDistance = questData.contains("QuestDespawnDistance") ? questData.getDouble("QuestDespawnDistance") : 64.0;
            mqt$questDespawnIfOffline = !questData.contains("QuestDespawnIfOffline") || questData.getBoolean("QuestDespawnIfOffline");
            mqt$questDespawnIfDead = !questData.contains("QuestDespawnIfDead") || questData.getBoolean("QuestDespawnIfDead");
            mqt$questFollow = questData.contains("QuestFollow") && questData.getBoolean("QuestFollow");
            mqt$questSpawnTime = questData.contains("QuestSpawnTime") ? questData.getLong("QuestSpawnTime") : 0L;
            mqt$questTextureOffsetX = questData.contains("QuestTextureOffsetX") ? questData.getDouble("QuestTextureOffsetX") : 0.0;
            mqt$questTextureOffsetY = questData.contains("QuestTextureOffsetY") ? questData.getDouble("QuestTextureOffsetY") : 0.0;
            mqt$questTextureOffsetZ = questData.contains("QuestTextureOffsetZ") ? questData.getDouble("QuestTextureOffsetZ") : 0.0;

            mqt$questCustomName = questData.contains("QuestCustomName") ? questData.getString("QuestCustomName") : "";
            mqt$questCustomDrops = questData.contains("QuestCustomDrops") ? questData.getString("QuestCustomDrops") : "";
            mqt$questEquipmentDropRates = questData.contains("QuestEquipmentDropRates") ? questData.getString("QuestEquipmentDropRates") : "";
            mqt$questRewardTables = questData.contains("QuestRewardTables") ? questData.getString("QuestRewardTables") : "";

            mqt$followGoalInitialized = false;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void mqt$addQuestData(CompoundTag nbt, CallbackInfo ci) {
        if (mqt$questSummoned) {
            CompoundTag questData = new CompoundTag();
            questData.putBoolean("QuestSummoned", true);

            if (mqt$questOwnerUuid != null) {
                questData.putUUID("QuestOwner", mqt$questOwnerUuid);
            }

            if (mqt$questTexture != null && !mqt$questTexture.isEmpty()) {
                questData.putString("QuestTexture", mqt$questTexture);
            }

            questData.putFloat("QuestTextureScale", mqt$questTextureScale);
            questData.putLong("QuestRewardId", mqt$questRewardId);
            questData.putInt("QuestDurationTicks", mqt$questDurationTicks);
            questData.putDouble("QuestDespawnDistance", mqt$questDespawnDistance);
            questData.putBoolean("QuestDespawnIfOffline", mqt$questDespawnIfOffline);
            questData.putBoolean("QuestDespawnIfDead", mqt$questDespawnIfDead);
            questData.putBoolean("QuestFollow", mqt$questFollow);
            questData.putLong("QuestSpawnTime", mqt$questSpawnTime);
            questData.putDouble("QuestTextureOffsetX", mqt$questTextureOffsetX);
            questData.putDouble("QuestTextureOffsetY", mqt$questTextureOffsetY);
            questData.putDouble("QuestTextureOffsetZ", mqt$questTextureOffsetZ);

            if (!mqt$questCustomName.isEmpty()) {
                questData.putString("QuestCustomName", mqt$questCustomName);
            }

            if (!mqt$questCustomDrops.isEmpty()) {
                questData.putString("QuestCustomDrops", mqt$questCustomDrops);
            }

            if (!mqt$questEquipmentDropRates.isEmpty()) {
                questData.putString("QuestEquipmentDropRates", mqt$questEquipmentDropRates);
            }

            if (!mqt$questRewardTables.isEmpty()) {
                questData.putString("QuestRewardTables", mqt$questRewardTables);
            }

            nbt.put("MQTData", questData);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void mqt$readQuestData(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("MQTData")) {
            CompoundTag questData = nbt.getCompound("MQTData");

            mqt$questSummoned = questData.getBoolean("QuestSummoned");

            if (questData.contains("QuestOwner")) {
                mqt$questOwnerUuid = questData.getUUID("QuestOwner");
            }

            if (questData.contains("QuestTexture")) {
                mqt$questTexture = questData.getString("QuestTexture");
            }

            mqt$questTextureScale = questData.contains("QuestTextureScale") ? questData.getFloat("QuestTextureScale") : 1.0f;
            mqt$questRewardId = questData.contains("QuestRewardId") ? questData.getLong("QuestRewardId") : -1L;
            mqt$questDurationTicks = questData.contains("QuestDurationTicks") ? questData.getInt("QuestDurationTicks") : -1;
            mqt$questDespawnDistance = questData.contains("QuestDespawnDistance") ? questData.getDouble("QuestDespawnDistance") : 64.0;
            mqt$questDespawnIfOffline = !questData.contains("QuestDespawnIfOffline") || questData.getBoolean("QuestDespawnIfOffline");
            mqt$questDespawnIfDead = !questData.contains("QuestDespawnIfDead") || questData.getBoolean("QuestDespawnIfDead");
            mqt$questFollow = questData.contains("QuestFollow") && questData.getBoolean("QuestFollow");
            mqt$questSpawnTime = questData.contains("QuestSpawnTime") ? questData.getLong("QuestSpawnTime") : 0L;
            mqt$questTextureOffsetX = questData.contains("QuestTextureOffsetX") ? questData.getDouble("QuestTextureOffsetX") : 0.0;
            mqt$questTextureOffsetY = questData.contains("QuestTextureOffsetY") ? questData.getDouble("QuestTextureOffsetY") : 0.0;
            mqt$questTextureOffsetZ = questData.contains("QuestTextureOffsetZ") ? questData.getDouble("QuestTextureOffsetZ") : 0.0;

            mqt$questCustomName = questData.contains("QuestCustomName") ? questData.getString("QuestCustomName") : "";
            mqt$questCustomDrops = questData.contains("QuestCustomDrops") ? questData.getString("QuestCustomDrops") : "";
            mqt$questEquipmentDropRates = questData.contains("QuestEquipmentDropRates") ? questData.getString("QuestEquipmentDropRates") : "";
            mqt$questRewardTables = questData.contains("QuestRewardTables") ? questData.getString("QuestRewardTables") : "";

            mqt$followGoalInitialized = false;
        }
    }

    @Inject(method = "startSeenByPlayer", at = @At("RETURN"))
    private void mqt$syncOnStartTracking(ServerPlayer player, CallbackInfo ci) {
        if (mqt$questSummoned) {
            Entity self = (Entity) (Object) this;

            if (mqt$questFollow && !mqt$followGoalInitialized && self instanceof Mob mob) {
                mqt$ensureFollowGoal(mob);
            }

            QuestEntityDataSyncPacket syncPacket = new QuestEntityDataSyncPacket(
                    self.getId(),
                    mqt$questSummoned,
                    mqt$questOwnerUuid,
                    mqt$questTexture,
                    mqt$questTextureScale,
                    mqt$questRewardId,
                    mqt$questFollow,
                    false,
                    mqt$questTextureOffsetX,
                    mqt$questTextureOffsetY,
                    mqt$questTextureOffsetZ
            );

            NetworkHelper.sendToPlayer(player, syncPacket);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void mqt$tickFollowGoal(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide && mqt$questSummoned && mqt$questFollow && !mqt$followGoalInitialized) {
            if (self instanceof Mob mob) {
                mqt$ensureFollowGoal(mob);
            }
        }
    }

    @Unique
    private void mqt$ensureFollowGoal(Mob mob) {
        if (mqt$questOwnerUuid == null || mqt$followGoalInitialized) {
            return;
        }

        try {
            ((MobAccessor) mob).getGoalSelector().getAvailableGoals().removeIf(goal ->
                    goal.getGoal() instanceof net.pixeldreamstudios.morequesttypes.rewards.summon.FollowPlayerGoal
            );

            ((MobAccessor) mob).getGoalSelector().addGoal(2,
                    new net.pixeldreamstudios.morequesttypes.rewards.summon.FollowPlayerGoal(
                            mob, mqt$questOwnerUuid, 1.0, 2.0f, 10.0f
                    )
            );

            mqt$followGoalInitialized = true;
        } catch (Exception e) {
            mqt$followGoalInitialized = false;
        }
    }
}