package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.world.entity.ai.control.MoveControl;

/** Ground steering checks the full trailing corridor before advancing the raised front. */
final class DemiurgeMoveControl extends MoveControl {
    private final DemiurgeStatueEntity statue;
    private float safeYaw;

    DemiurgeMoveControl(DemiurgeStatueEntity statue) {
        super(statue);
        this.statue = statue;
    }

    @Override
    public void tick() {
        safeYaw = statue.getYRot();
        if (hasWanted() && statue.canNavigate()) {
            float targetYaw = (float) Math.toDegrees(Math.atan2(statue.getX() - wantedX, wantedZ - statue.getZ()));
            var turn = DemiurgePatrolSteering.steer(statue.position(), statue.getYRot(), targetYaw,
                    statue.horizontalStep(), statue.patrolBody());
            if (turn.blocked()) {
                stopAtObstacle();
                return;
            }
            safeYaw = turn.yaw();
            if (!DemiurgeBlockBreaking.clearAhead(statue, safeYaw)) {
                stopAtObstacle();
                return;
            }
        }
        super.tick();
    }

    private void stopAtObstacle() {
        operation = Operation.WAIT;
        statue.getNavigation().stop();
        statue.setSpeed(0);
        statue.setZza(0);
        statue.setDeltaMovement(0, statue.getDeltaMovement().y, 0);
    }

    @Override
    protected float rotlerp(float current, float target, float maximum) {
        return safeYaw;
    }
}
