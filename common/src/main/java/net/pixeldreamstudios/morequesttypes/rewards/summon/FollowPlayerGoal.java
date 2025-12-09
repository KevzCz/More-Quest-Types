package net.pixeldreamstudios.morequesttypes.rewards.summon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

public class FollowPlayerGoal extends Goal {
    private final Mob mob;
    private final UUID playerUUID;
    private final double speedModifier;
    private final float stopDistance;
    private final float startDistance;
    private final boolean canFly;
    private int timeToRecalcPath;

    public FollowPlayerGoal(Mob mob, UUID playerUUID, double speedModifier, float stopDistance, float startDistance) {
        this.mob = mob;
        this.playerUUID = playerUUID;
        this.speedModifier = speedModifier;
        this.stopDistance = stopDistance;
        this.startDistance = startDistance;
        this.canFly = mob.getNavigation() instanceof FlyingPathNavigation;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private ServerPlayer getPlayer() {
        if (mob.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getServer().getPlayerList().getPlayer(playerUUID);
        }
        return null;
    }

    @Override
    public boolean canUse() {
        ServerPlayer player = getPlayer();
        if (player == null || !player.isAlive() || player.isSpectator()) return false;

        double distanceSq = mob.distanceToSqr(player);
        if (distanceSq < (double)(stopDistance * stopDistance)) return false;
        return distanceSq > (double)(startDistance * startDistance);
    }

    @Override
    public boolean canContinueToUse() {
        ServerPlayer player = getPlayer();
        if (player == null || !player.isAlive() || player.isSpectator()) return false;
        if (mob.getNavigation().isDone()) return false;

        double distanceSq = mob.distanceToSqr(player);
        return distanceSq > (double)(stopDistance * stopDistance);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        ServerPlayer player = getPlayer();
        if (player == null) return;

        mob.getLookControl().setLookAt(player, 10.0F, (float)mob.getMaxHeadXRot());

        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            double distanceSq = mob.distanceToSqr(player);

            if (distanceSq > (double)(stopDistance * stopDistance)) {
                if (canFly) {
                    Vec3 playerPos = player.position();
                    mob.getNavigation().moveTo(playerPos.x, playerPos.y, playerPos.z, speedModifier);
                } else {
                    if (! mob.getNavigation().moveTo(player, speedModifier)) {
                        if (distanceSq > (double)(startDistance * startDistance * 4)) {
                            tryTeleportToPlayer(player);
                        }
                    }
                }
            } else {
                mob.getNavigation().stop();
            }
        }
    }

    private void tryTeleportToPlayer(ServerPlayer player) {
        if (! canFly && mob.onGround() && player.onGround()) {
            Vec3 playerPos = player.position();
            double x = playerPos.x + (mob.getRandom().nextDouble() - 0.5) * 4.0;
            double y = playerPos.y;
            double z = playerPos.z + (mob.getRandom().nextDouble() - 0.5) * 4.0;

            mob.moveTo(x, y, z, mob.getYRot(), mob.getXRot());
            mob.getNavigation().stop();
        }
    }
}