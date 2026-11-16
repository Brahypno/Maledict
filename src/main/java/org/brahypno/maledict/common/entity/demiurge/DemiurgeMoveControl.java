package org.brahypno.maledict.common.entity.demiurge;

import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/**
 * Ground steering checks the full trailing corridor before advancing the raised front.
 */
final class DemiurgeMoveControl extends MoveControl {
    private static final int BLOCKED_ATTEMPTS_BEFORE_UNCOIL = 6;
    private static final int UNCOIL_TICKS = 8;
    private final DemiurgeStatueEntity statue;
    private float safeYaw;
    private int blockedAttempts;
    private int uncoilTicks;
    private int clearFailures;

    DemiurgeMoveControl(DemiurgeStatueEntity statue) {
        super(statue);
        this.statue = statue;
    }

    @Override
    public void tick() {
        safeYaw = statue.getYRot();
        if (uncoilTicks > 0 && statue.canNavigate()){
            // The body corridor can close around the head when the statue turns
            // in place. Creep forward for a few ticks without the self-clearance
            // check so the trailing chain can uncoil; collision with real blocks
            // is still handled by travel().
            uncoilTicks--;
            operation = Operation.MOVE_TO;
            super.tick();
            return;
        }
        if (hasWanted() && statue.canNavigate()){
            float targetYaw = (float) Math.toDegrees(Math.atan2(statue.getX() - wantedX, wantedZ - statue.getZ()));
            var turn = DemiurgePatrolSteering.steer(statue.position(), statue.getYRot(), targetYaw,
                                                    statue.horizontalStep(), statue.patrolBody());
            if (turn.blocked()){
                clearFailures = 0;
                if (++blockedAttempts >= BLOCKED_ATTEMPTS_BEFORE_UNCOIL){
                    blockedAttempts = 0;
                    uncoilTicks = UNCOIL_TICKS;
                    startUncoil();
                    return;
                }
                stopAtObstacle();
                return;
            }
            blockedAttempts = 0;
            safeYaw = turn.yaw();
            if (!DemiurgeBlockBreaking.clearAhead(statue, safeYaw)){
                // A few solid, non-breakable obstacles can be transient (a block
                // update, a door, an event-protected block that will change). Keep
                // the path for that short window; after eight consecutive failures
                // the route is actually unusable and is dropped for a new one.
                if (++clearFailures >= 8){
                    clearFailures = 0;
                    statue.onTerrainBlocked();
                }
                stopAtObstacle();
                return;
            }
            clearFailures = 0;
        }
        super.tick();
    }

    /**
     * Move a few blocks along the current heading, ignoring only the body corridor.
     */
    private void startUncoil() {
        operation = Operation.MOVE_TO;
        Vec3 forward = Vec3.directionFromRotation(0, statue.getYRot());
        wantedX = statue.getX() + forward.x * 3;
        wantedY = statue.getY();
        wantedZ = statue.getZ() + forward.z * 3;
        super.tick();
    }

    private void stopAtObstacle() {
        // Keep the navigation path and the steered yaw. Only remove the physical
        // velocity for this tick; the next tick retries the same path and can
        // continue immediately when the swept pre-break has opened the way.
        operation = Operation.MOVE_TO;
        statue.setSpeed(0);
        statue.setZza(0);
        statue.setDeltaMovement(0, statue.getDeltaMovement().y, 0);
    }

    @Override
    protected float rotlerp(float current, float target, float maximum) {
        return safeYaw;
    }
}
