package org.brahypno.maledict.common.entity.demiurge;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reserves the trailing body's corridor before selecting a patrol destination or steering arc.
 */
public final class DemiurgePatrolSteering {
    public static final double TURN_RADIUS = 6;
    /**
     * A stopped statue still needs to be able to turn. The previous tick's displacement used to
     * clamp the maximum yaw change to zero at rest, which locked it into its old heading. A
     * nominal step keeps the authored two-degree arc while the body is stationary; the arc check
     * below then follows the curve the first moving tick would take.
     */
    public static final double NOMINAL_TURN_STEP = .21;
    /**
     * Authored cap on yaw change per tick; kept public for movement tests.
     */
    public static final float MAX_TURN_PER_TICK = 2;
    private static final double BODY_CLEARANCE = 2.1;
    private static final double LOOK_AHEAD = 6;

    public record Turn(float yaw, boolean blocked) {}

    private DemiurgePatrolSteering() {}

    public static List<Vec3> body(Vec3 origin, float yaw, DemiurgeBodyChain.Path path) {
        return body(origin, yaw, path, 18);
    }

    /**
     * Reserves only the corridor the remaining body actually covers; {@code endDistance} is the
     * authored far end of the current chain, so a severed tail no longer blocks its own path.
     */
    public static List<Vec3> body(Vec3 origin, float yaw, DemiurgeBodyChain.Path path, double endDistance) {
        List<Vec3> result = new ArrayList<>();
        // The first four blocks are the connected forebody. The authored trailing extent includes
        // the tail and the sideways swing, so it shrinks together with the body cut.
        for (double distance = 4; distance <= endDistance; distance += .5) {
            result.add(path.sample(distance, origin, yaw).position());
        }
        return result;
    }

    public static boolean clearDestination(Vec3 destination, Vec3 origin, float yaw, List<Vec3> body) {
        Vec3 offset = destination.subtract(origin);
        double length = offset.horizontalDistance();
        float bearing = (float) Math.toDegrees(Math.atan2(-offset.x, offset.z));
        double maximumBearing = Math.toDegrees(Math.atan2(length, 2 * TURN_RADIUS));
        if (Math.abs(Mth.wrapDegrees(bearing - yaw)) > maximumBearing)
            return false;
        for (double distance = .25; distance <= length; distance += .25) {
            if (occupied(origin.add(offset.scale(distance / length)), body))
                return false;
        }
        return !occupied(destination, body);
    }

    public static Turn steer(Vec3 origin, float yaw, float targetYaw, double step, List<Vec3> body) {
        // Only a genuinely stationary tick needs the nominal step. While the
        // statue is actually moving, the authored six-block turn radius still
        // binds the yaw change to the real displacement; otherwise a slow-moving
        // serpent would fold its own trailing body.
        double turnStep = step > .0001 ? step : NOMINAL_TURN_STEP;
        float maximum = (float) Math.min(MAX_TURN_PER_TICK, Math.toDegrees(turnStep / TURN_RADIUS));
        float wanted = Mth.clamp(Mth.wrapDegrees(targetYaw - yaw), -maximum, maximum);
        float[] choices = {wanted, 0, -maximum, maximum};
        for (float change : choices) {
            if (clearArc(origin, yaw, change, turnStep, body))
                return new Turn(yaw + change, false);
        }
        return new Turn(yaw, true);
    }

    /**
     * A destination outside the current turning cone is not rejected outright: a point further
     * along a reachable arc is offered instead, so a target behind the statue produces a U-turn
     * through several waypoints rather than a permanent refusal to move.
     */
    @Nullable
    public static Vec3 reachableWaypoint(Vec3 origin, float yaw, Vec3 desired, List<Vec3> body) {
        Vec3 offset = desired.subtract(origin);
        double length = offset.horizontalDistance();
        if (length < .001)
            return null;
        if (clearDestination(desired, origin, yaw, body))
            return desired;
        float targetYaw = (float) Math.toDegrees(Math.atan2(-offset.x, offset.z));
        float side = Mth.wrapDegrees(targetYaw - yaw) < 0 ? -1 : 1;
        // Walk the heading around a broad arc towards the destination, with the other
        // side available when the trailing body blocks the preferred turn.
        for (float change : new float[]{15 * side, -15 * side, 30 * side, -30 * side, 45 * side, -45 * side}) {
            double distance = Math.min(12, Math.max(7, length * .55));
            Vec3 point = origin.add(Vec3.directionFromRotation(0, yaw + change).scale(distance));
            if (clearDestination(point, origin, yaw, body))
                return point;
        }
        return null;
    }

    private static boolean clearArc(Vec3 origin, float yaw, float change, double step, List<Vec3> body) {
        double curvature = step > .00001 ? Math.toRadians(change) / step : 0;
        Vec3 point = origin;
        for (double distance = .25; distance <= LOOK_AHEAD; distance += .25) {
            float heading = yaw + (float) Math.toDegrees(curvature * (distance - .125));
            point = point.add(Vec3.directionFromRotation(0, heading).scale(.25));
            if (occupied(point, body))
                return false;
        }
        return true;
    }

    private static boolean occupied(Vec3 point, List<Vec3> body) {
        for (Vec3 section : body) {
            double dx = point.x - section.x, dz = point.z - section.z;
            if (dx * dx + dz * dz < BODY_CLEARANCE * BODY_CLEARANCE)
                return true;
        }
        return false;
    }
}
