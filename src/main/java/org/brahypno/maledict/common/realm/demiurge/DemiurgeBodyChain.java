package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Places rigid sections in order along one guide, advancing by each section's fixed span. */
public final class DemiurgeBodyChain {
    public record Joint(int bone, Vector3f front, Vector3f back, List<Vector3f> corners) {}

    @FunctionalInterface
    public interface Path {
        DemiurgeTrail.Point sample(double distance, Vec3 origin, float yaw);
    }

    private final List<Joint> joints;

    public DemiurgeBodyChain(List<Joint> joints) {
        this.joints = List.copyOf(joints);
    }

    public void follow(Vector3f[] positions, Quaternionf[] rotations, Vec3 origin, float yaw, float blend, Path path) {
        Quaternionf turn = new Quaternionf();
        Vector3f target = new Vector3f();
        Vector3f endpoint = new Vector3f();
        List<Vector2f> guide = new ArrayList<>(joints.size() + 1);
        float[] heights = new float[joints.size()];
        int index = 0;
        for (Joint joint : joints) {
            Vector3f position = positions[joint.bone];
            Quaternionf rotation = rotations[joint.bone];
            float minimum = Float.POSITIVE_INFINITY;
            for (var corner : joint.corners) {
                minimum = Math.min(minimum, rotation.transform(corner, target).y + position.y);
            }
            if (minimum < .025F) position.y += (.025F - minimum) * blend;
            map(position, target, origin, yaw, path);
            heights[index++] = target.y;
            if (guide.isEmpty()) {
                rotation.transform(joint.front, endpoint).add(position);
                map(endpoint, target, origin, yaw, path);
                guide.add(new Vector2f(target.x, target.z));
            }
            rotation.transform(joint.back, endpoint).add(position);
            map(endpoint, target, origin, yaw, path);
            guide.add(new Vector2f(target.x, target.z));
        }

        Guide cursor = new Guide(guide);
        Vector2f front = new Vector2f(guide.get(0)), back = new Vector2f();
        Vector3f direction = new Vector3f();
        index = 0;
        for (Joint joint : joints) {
            Vector3f position = positions[joint.bone];
            Quaternionf rotation = rotations[joint.bone];
            rotation.transform(direction.set(joint.back).sub(joint.front));
            float span = (float) Math.hypot(direction.x, direction.z);
            cursor.advance(span, back);
            float angle = (float) (Math.atan2(back.x - front.x, back.y - front.y)
                    - Math.atan2(direction.x, direction.z));
            turn.rotationY(angle).mul(rotation);
            turn.transform(joint.front, target).negate();
            target.x += front.x;
            target.z += front.y;
            target.y = heights[index++];
            position.lerp(target, blend);
            rotation.slerp(turn, blend);
            front.set(back);
        }
    }

    private static void map(Vector3f point, Vector3f target, Vec3 origin, float yaw, Path path) {
        if (point.z >= 0) {
            target.set(point);
            return;
        }
        var trail = path.sample(-point.z, origin, yaw);
        Vec3 lateral = new Vec3(point.x, 0, 0).yRot((float) Math.toRadians(-trail.yaw()));
        Vec3 local = trail.position().add(lateral).add(0, point.y, 0)
                .subtract(origin).yRot((float) Math.toRadians(yaw));
        target.set((float) local.x, (float) local.y, (float) local.z);
    }

    /** Walk forward to the next circle/guide intersection; never aim back at an already passed target. */
    private static final class Guide {
        private final List<Vector2f> points;
        private final Vector2f cursor;
        private int next = 1;

        Guide(List<Vector2f> points) {
            this.points = points;
            cursor = new Vector2f(points.get(0));
        }

        void advance(float span, Vector2f result) {
            if (span < .0001F) {
                result.set(cursor);
                return;
            }
            Vector2f front = new Vector2f(cursor);
            double radiusSquared = (double) span * span;
            while (next < points.size()) {
                Vector2f end = points.get(next);
                if (end.distanceSquared(front) >= radiusSquared) {
                    intersect(front, end.x - cursor.x, end.y - cursor.y, radiusSquared, result);
                    return;
                }
                cursor.set(end);
                next++;
            }
            Vector2f last = points.get(points.size() - 1), before = points.get(points.size() - 2);
            intersect(front, last.x - before.x, last.y - before.y, radiusSquared, result);
        }

        private void intersect(Vector2f front, double dx, double dz, double radiusSquared, Vector2f result) {
            double x = cursor.x - front.x, z = cursor.y - front.y;
            double a = dx * dx + dz * dz;
            double b = x * dx + z * dz;
            double c = x * x + z * z - radiusSquared;
            double t = (-b + Math.sqrt(Math.max(0, b * b - a * c))) / a;
            cursor.add((float) (dx * t), (float) (dz * t));
            result.set(cursor);
        }
    }
}
