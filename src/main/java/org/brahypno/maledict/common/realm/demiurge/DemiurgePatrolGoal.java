package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.phys.Vec3;

/** Occasional ground patrol; a live enemy immediately ends this idle movement. */
final class DemiurgePatrolGoal extends WaterAvoidingRandomStrollGoal {
    private final DemiurgeStatueEntity statue;

    DemiurgePatrolGoal(DemiurgeStatueEntity statue) {
        super(statue, 0.8D);
        this.statue = statue;
    }

    @Override
    public boolean canUse() {
        return statue.canPatrol() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return statue.canPatrol() && super.canContinueToUse();
    }

    @Override
    protected Vec3 getPosition() {
        // Vanilla's waypoint tolerance grows with mob width. A destination
        // inside this five-block statue is already "arrived" and never moves it.
        double minimum = statue.getBbWidth() + .5;
        var body = statue.patrolBody();
        for (int i = 0; i < 16; i++) {
            Vec3 point = super.getPosition();
            if (point != null && point.subtract(statue.position()).horizontalDistanceSqr() >= minimum * minimum
                    && DemiurgePatrolSteering.clearDestination(point, statue.position(), statue.getYRot(), body)) return point;
        }
        return null;
    }
}
