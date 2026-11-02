package org.brahypno.maledict.common.realm.demiurge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** One exported rig drives visible bones, multipart bounds and the two laser origins. */
public final class DemiurgeRig {
    public static final DemiurgeRig INSTANCE = load();
    private record Bone(Vector3f position, Quaternionf rotation) {}
    private final Bone[][] awakening, transition, slither;
    private final Bone[][][] attacks = new Bone[DemiurgeAttack.values().length][][];
    private final List<List<Vector3f>> vertices = new ArrayList<>();
    private final DemiurgeBodyChain chain;
    private final int head;
    private final Vector3f[] eyes = new Vector3f[2];
    public final int size;

    private static DemiurgeRig load() {
        try (var input = Objects.requireNonNull(DemiurgeRig.class.getResourceAsStream(
                "/assets/maledict/models/entity/demiurge_statue.mesh.json"));
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return new DemiurgeRig(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot load Demiurge combat rig", exception);
        }
    }

    private DemiurgeRig(JsonObject root) {
        var animation = root.getAsJsonObject("animation");
        var names = animation.getAsJsonArray("bones");
        size = names.size();
        int headIndex = -1;
        for (int i = 0; i < size; i++) {
            vertices.add(new ArrayList<>());
            if (names.get(i).getAsString().equals("head")) headIndex = i;
        }
        head = headIndex;
        awakening = frames(animation.getAsJsonArray("frames"));
        transition = frames(animation.getAsJsonArray("ground_transition_frames"));
        slither = frames(animation.getAsJsonArray("slither_frames"));
        for (var attack : DemiurgeAttack.values()) {
            if (attack == DemiurgeAttack.NONE) continue;
            var clip = animation.getAsJsonObject("attacks").getAsJsonObject(attack.name().toLowerCase(java.util.Locale.ROOT));
            if (clip.get("release").getAsInt() != attack.release
                    || clip.get("recovery").getAsInt() != attack.recovery
                    || clip.getAsJsonArray("frames").size() != attack.duration + 1) {
                throw new IllegalStateException("Demiurge attack asset and server timeline differ: " + attack);
            }
            attacks[attack.ordinal()] = frames(clip.getAsJsonArray("frames"));
        }
        for (var value : root.getAsJsonArray("quads")) {
            var quad = value.getAsJsonObject();
            for (var v : quad.getAsJsonArray("v")) vertices.get(quad.get("bone").getAsInt()).add(vector(v.getAsJsonArray()));
        }
        var joints = new ArrayList<DemiurgeBodyChain.Joint>();
        for (var value : animation.getAsJsonArray("body_chain")) {
            var joint = value.getAsJsonObject();
            var corners = new ArrayList<Vector3f>();
            for (var v : joint.getAsJsonArray("corners")) corners.add(vector(v.getAsJsonArray()));
            joints.add(new DemiurgeBodyChain.Joint(joint.get("bone").getAsInt(),
                    vector(joint.getAsJsonArray("front")), vector(joint.getAsJsonArray("back")), corners));
        }
        chain = new DemiurgeBodyChain(joints);
        for (int i = 0; i < 2; i++) eyes[i] = vector(animation.getAsJsonArray("eyes").get(i).getAsJsonArray());
    }

    private static Vector3f vector(JsonArray array) {
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private Bone[][] frames(JsonArray frames) {
        Bone[][] result = new Bone[frames.size()][size];
        for (int tick = 0; tick < result.length; tick++) for (int bone = 0; bone < size; bone++) {
            var transform = frames.get(tick).getAsJsonArray().get(bone).getAsJsonObject();
            var q = transform.getAsJsonArray("q");
            result[tick][bone] = new Bone(vector(transform.getAsJsonArray("p")), new Quaternionf(
                    q.get(0).getAsFloat(), q.get(1).getAsFloat(), q.get(2).getAsFloat(), q.get(3).getAsFloat()));
        }
        return result;
    }

    public Pose pose() { return new Pose(); }

    public final class Pose {
        public final Vector3f[] positions = new Vector3f[size];
        public final Quaternionf[] rotations = new Quaternionf[size];

        private Pose() {
            for (int i = 0; i < size; i++) {
                positions[i] = new Vector3f();
                rotations[i] = new Quaternionf();
            }
        }

        public void sample(float age, float clock, DemiurgeAttack attack, float attackAge,
                           Vec3 origin, float yaw, DemiurgeBodyChain.Path path) {
            Bone[][] base = age < 100 ? awakening : age < 140 ? transition : slither;
            float time = age < 100 ? Math.max(0, age) : age < 140 ? age - 100 : clock % DemiurgeSlither.CYCLE_TICKS;
            float blend = attack.blend(attackAge);
            for (int bone = 0; bone < size; bone++) {
                interpolate(base, time, bone, 1);
                if (blend > 0) interpolate(attacks[attack.ordinal()], attackAge, bone, blend);
            }
            float stance = DemiurgeSlither.groundBlend(age);
            if (stance > 0) chain.follow(positions, rotations, origin, yaw, stance, path);
        }

        private void interpolate(Bone[][] clip, float time, int bone, float blend) {
            float clamped = Math.max(0, Math.min(clip.length - 1, time));
            int key = (int) clamped;
            var a = clip[key][bone];
            var b = clip[Math.min(key + 1, clip.length - 1)][bone];
            var p = new Vector3f(a.position).lerp(b.position, clamped - key);
            var q = new Quaternionf(a.rotation).slerp(b.rotation, clamped - key);
            positions[bone].lerp(p, blend);
            rotations[bone].slerp(q, blend);
        }

        public Vec3 eye(int side, Vec3 origin, float yaw) {
            return world(rotations[head].transform(eyes[side], new Vector3f()).add(positions[head]), origin, yaw);
        }

        public AABB bounds(int bone, Vec3 origin, float yaw) {
            double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
            double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
            for (var corner : vertices.get(bone)) {
                var point = world(rotations[bone].transform(corner, new Vector3f()).add(positions[bone]), origin, yaw);
                minX = Math.min(minX, point.x); minY = Math.min(minY, point.y); minZ = Math.min(minZ, point.z);
                maxX = Math.max(maxX, point.x); maxY = Math.max(maxY, point.y); maxZ = Math.max(maxZ, point.z);
            }
            return vertices.get(bone).isEmpty() ? new AABB(origin, origin) : new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    private static Vec3 world(Vector3f point, Vec3 origin, float yaw) {
        return new Vec3(point.x, point.y, point.z).yRot((float) Math.toRadians(-yaw)).add(origin);
    }
}
