package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/** Distance-spaced movement history keeps the long body following turns instead of rotating as a plank. */
public final class DemiurgeTrail {
    public record Point(Vec3 position, float yaw) {}
    private final Deque<Point> points = new ArrayDeque<>();

    public void record(Vec3 position, float yaw) {
        if (!points.isEmpty() && points.getFirst().position.distanceToSqr(position) > 64) points.clear();
        if (points.isEmpty() || points.getFirst().position.distanceToSqr(position) >= 0.0009) {
            if (!points.isEmpty()) {
                Vec3 delta = position.subtract(points.getFirst().position);
                yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
            }
            points.addFirst(new Point(position, yaw));
            while (points.size() > 768) points.removeLast();
        }
    }

    public Point sample(double distance, Vec3 origin, float yaw) {
        Point center = samplePosition(distance, origin, yaw);
        Vec3 tangent = samplePosition(Math.max(0, distance - .5), origin, yaw).position
                .subtract(samplePosition(distance + .5, origin, yaw).position);
        float heading = (float) Math.toDegrees(Math.atan2(-tangent.x, tangent.z));
        return new Point(center.position, heading);
    }

    private Point samplePosition(double distance, Vec3 origin, float yaw) {
        Vec3 newer = origin;
        float heading = yaw;
        boolean newest = true;
        for (Point point : points) {
            Vec3 delta = newer.subtract(point.position);
            if (newest && delta.dot(Vec3.directionFromRotation(0, point.yaw)) < -.00001) {
                // The latest tick sample can be ahead of the interpolated
                // render position. Start with the preceding sample instead.
                newest = false;
                continue;
            }
            newest = false;
            double length = delta.length();
            if (length > .0001) {
                heading = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
                if (distance <= length) return new Point(newer.lerp(point.position, distance / length), heading);
                distance -= length;
            }
            newer = point.position;
        }
        return new Point(newer.add(Vec3.directionFromRotation(0, heading).scale(-distance)), heading);
    }
}
