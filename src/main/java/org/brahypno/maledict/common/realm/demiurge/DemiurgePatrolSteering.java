package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Reserves the trailing body's corridor before selecting a patrol destination or steering arc. */
public final class DemiurgePatrolSteering {
    public static final double TURN_RADIUS = 6;
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
        if (Math.abs(Mth.wrapDegrees(bearing - yaw)) > maximumBearing) return false;
        for (double distance = .25; distance <= length; distance += .25) {
            if (occupied(origin.add(offset.scale(distance / length)), body)) return false;
        }
        return !occupied(destination, body);
    }

    public static Turn steer(Vec3 origin, float yaw, float targetYaw, double step, List<Vec3> body) {
        float maximum = (float) Math.min(2, Math.toDegrees(step / TURN_RADIUS));
        float wanted = Mth.clamp(Mth.wrapDegrees(targetYaw - yaw), -maximum, maximum);
        float[] choices = {wanted, 0, -maximum, maximum};
        for (float change : choices) {
            if (clearArc(origin, yaw, change, step, body)) return new Turn(yaw + change, false);
        }
        return new Turn(yaw, true);
    }

    private static boolean clearArc(Vec3 origin, float yaw, float change, double step, List<Vec3> body) {
        double curvature = step > .00001 ? Math.toRadians(change) / step : 0;
        Vec3 point = origin;
        for (double distance = .25; distance <= LOOK_AHEAD; distance += .25) {
            float heading = yaw + (float) Math.toDegrees(curvature * (distance - .125));
            point = point.add(Vec3.directionFromRotation(0, heading).scale(.25));
            if (occupied(point, body)) return false;
        }
        return true;
    }

    private static boolean occupied(Vec3 point, List<Vec3> body) {
        for (Vec3 section : body) {
            double dx = point.x - section.x, dz = point.z - section.z;
            if (dx * dx + dz * dz < BODY_CLEARANCE * BODY_CLEARANCE) return true;
        }
        return false;
    }
}
