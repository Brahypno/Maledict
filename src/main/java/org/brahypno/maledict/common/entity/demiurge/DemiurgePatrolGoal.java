package org.brahypno.maledict.common.entity.demiurge;

import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * No-enemy behaviour: keep circling a local anchor instead of picking random
 * destinations in one direction. Each waypoint advances one step around a
 * ring, so the statue stays in the same area and its long body has to curl
 * through a continuous turn.
 */
final class DemiurgePatrolGoal extends WaterAvoidingRandomStrollGoal {
    private static final double ORBIT_RADIUS = 10;
    private static final double ORBIT_STEP = Math.toRadians(38);
    private static final int ORBIT_ATTEMPTS = 4;
    private static final double REANCHOR_DISTANCE = 30;
    private final DemiurgeStatueEntity statue;
    private Vec3 anchor;
    private double orbitAngle;
    private int orbitDirection = 1;

    DemiurgePatrolGoal(DemiurgeStatueEntity statue) {
        super(statue, 1.0D);
        setInterval(10);
        this.statue = statue;
    }

    @Override
    public boolean canUse() {
        if (!statue.canPatrol())
            return false;
        if (anchor == null || statue.position().distanceToSqr(anchor) > REANCHOR_DISTANCE * REANCHOR_DISTANCE) {
            resetOrbit();
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return statue.canPatrol() && super.canContinueToUse();
    }

    @Override
    protected Vec3 getPosition() {
        if (anchor == null)
            resetOrbit();
        // Vanilla's waypoint tolerance grows with mob width. A destination
        // inside this five-block statue is already "arrived" and never moves it.
        double minimum = statue.getBbWidth() + .5;
        for (int attempt = 0; attempt < ORBIT_ATTEMPTS; attempt++) {
            orbitAngle += orbitDirection * ORBIT_STEP;
            double radius = ORBIT_RADIUS + (statue.getRandom().nextDouble() - .5) * 2;
            Vec3 desired = anchor.add(Math.cos(orbitAngle) * radius, 0, Math.sin(orbitAngle) * radius);
            Vec3 point = LandRandomPos.getPosTowards(statue, 12, 3, desired);
            if (point == null || point.distanceToSqr(statue.position()) < minimum * minimum)
                continue;
            // Keep the waypoint on the local ring; a random detour would turn
            // the orbit back into a straight walk.
            if (point.distanceToSqr(anchor) > (radius + 4) * (radius + 4))
                continue;
            // Do not hand the navigator a point it cannot actually reach through
            // the current terrain. The pathfinder already knows which blocks are
            // breakable and which were verified as blocked.
            if (!statue.canReach(point))
                continue;
            return point;
        }
        resetOrbit();
        return null;
    }

    private void resetOrbit() {
        anchor = statue.position();
        orbitAngle = statue.getRandom().nextDouble() * Math.PI * 2;
        orbitDirection = statue.getRandom().nextBoolean() ? 1 : -1;
    }
}
