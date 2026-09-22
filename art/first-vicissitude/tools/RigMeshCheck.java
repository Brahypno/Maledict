import org.brahypno.maledict.rig.VicissitudeRig;
import org.brahypno.maledict.rig.VicissitudeRigData.Joint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Integration check: actual exported mesh vertices stay inside posed server wing volumes. */
public final class RigMeshCheck {
    private record Sample(Joint joint, float x, float y, float z, boolean feather) {}
    public static void main(String[] args) throws Exception {
        // The ring must stay face-on to its head-local plane while it rotates over time.
        for (var action : new VicissitudeRig.Action[]{VicissitudeRig.Action.NONE, VicissitudeRig.Action.CAST_FROM_HALO}) {
            for (int tick = 0; tick <= 120; tick += 10) {
                var pose = VicissitudeRig.newPose();
                VicissitudeRig.compute(pose, false, 0, action, tick % 60, false, 0, tick, 0, -1);
                var h = VicissitudeRig.jointOrigin(pose, Joint.HEAD_ROOT);
                var n = VicissitudeRig.transform(pose, Joint.HEAD_ROOT, 0, 0, 1);
                var c = VicissitudeRig.jointOrigin(pose, Joint.HALO_ROOT);
                var p = VicissitudeRig.transform(pose, Joint.HALO_ROOT, 14, 0, 0);
                float distance = (p.x()-c.x())*(n.x()-h.x())+(p.y()-c.y())*(n.y()-h.y())
                                 +(p.z()-c.z())*(n.z()-h.z());
                if (Math.abs(distance) > .0001F) throw new AssertionError("Halo rotates out of its plane at " + tick);
            }
        }
        System.out.println("PASS: halo stays in its plane during idle and halo casting");
        checkRingSpin();
        checkActualRingMesh();
        checkWingComposure();
        List<Sample> points = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of("build/rig-tool/wing-vertices.csv"))) {
            String[] fields = line.split(",");
            points.add(new Sample(Joint.valueOf(fields[0]), Float.parseFloat(fields[1]),
                    Float.parseFloat(fields[2]), Float.parseFloat(fields[3]), fields[4].equals("1")));
        }
        long checks = 0;
        for (int phase = 0; phase < 2; phase++) {
            for (var action : VicissitudeRig.Action.values()) {
                for (int yaw : new int[]{0, 90, 180, 270}) {
                    for (float fold : new float[]{0, .8F}) {
                        var pose = VicissitudeRig.newPose();
                        VicissitudeRig.compute(pose, phase == 1, phase, action, action.releaseTick(),
                                true, 0, 13, fold, -1);
                        var volumes = VicissitudeRig.segmentVolumes(pose, 10, 64, -23, yaw);
                        for (Sample sample : points) {
                            if (phase == 1 && sample.feather) continue;
                            var p = VicissitudeRig.worldPoint(pose, sample.joint, sample.x, sample.y,
                                    sample.z, 10, 64, -23, yaw);
                            boolean inside = volumes.stream().anyMatch(v -> v.segment().isWing()
                                    && v.box().inflate(.00002).contains(p.x(), p.y(), p.z()));
                            if (!inside) throw new AssertionError("Mesh outside wing collision: "
                                    + sample + " phase=" + phase + " action=" + action + " yaw=" + yaw);
                            checks++;
                        }
                    }
                }
            }
        }
        System.out.println("PASS: " + checks + " exported vertex / action / phase / yaw / fold checks");
    }

    /**
     * The chest ring and the halo are one mechanism: the chest arcs must travel rigidly around
     * their hub, and they must turn the other way at the same rate as the ring behind the head.
     */
    private static void checkActualRingMesh() throws Exception {
        long checks = 0;
        var lines = Files.readAllLines(Path.of("build/rig-tool/chest-ring-vertices.csv"));
        if (lines.isEmpty()) throw new AssertionError("Missing chest ring mesh samples");
        for (int phase = 0; phase < 2; phase++) {
            for (int tick = 0; tick <= 600; tick += 7) {
                var pose = VicissitudeRig.newPose();
                VicissitudeRig.compute(pose, phase == 1, phase, VicissitudeRig.Action.NONE,
                        0, false, 0, tick, 0, -1);
                var hub = VicissitudeRig.transform(pose, Joint.TORSO,
                        VicissitudeRig.CHEST_RING_HUB_X, VicissitudeRig.CHEST_RING_HUB_Y, 0);
                var ax = offset(pose, Joint.TORSO, 1, 0, 0);
                var ay = offset(pose, Joint.TORSO, 0, 1, 0);
                for (String line : lines) {
                    String[] f = line.split(",");
                    var v = VicissitudeRig.transform(pose, Joint.valueOf(f[0]),
                            Float.parseFloat(f[1]), Float.parseFloat(f[2]), Float.parseFloat(f[3]));
                    float dx = v.x()-hub.x(), dy = v.y()-hub.y(), dz = v.z()-hub.z();
                    double radius = Math.hypot(dx*ax[0]+dy*ax[1]+dz*ax[2], dx*ay[0]+dy*ay[1]+dz*ay[2]);
                    if (radius < 4.4 || radius > 5.6) throw new AssertionError("Chest mesh left its socket: " + radius);
                    checks++;
                }
            }
        }
        System.out.println("PASS: " + checks + " actual chest-ring vertices stay concentric through a full idle turn in both phases");
    }

    private static void checkRingSpin() {
        float hubX = VicissitudeRig.CHEST_RING_HUB_X;
        float hubY = VicissitudeRig.CHEST_RING_HUB_Y;
        for (var action : new VicissitudeRig.Action[]{VicissitudeRig.Action.NONE,
                VicissitudeRig.Action.CAST_FROM_CHEST}) {
            float[] reference = null;
            for (int tick = 0; tick <= 240; tick += 5) {
                var pose = VicissitudeRig.newPose();
                VicissitudeRig.compute(pose, false, 0, action, tick % 50, false, 0, tick, 0, -1);
                var hub = VicissitudeRig.transform(pose, Joint.TORSO, hubX, hubY, 0);
                float[] radii = new float[VicissitudeRig.CHEST_RING_ARCS.length];
                for (int index = 0; index < radii.length; index++) {
                    radii[index] = distance(VicissitudeRig.jointOrigin(
                            pose, VicissitudeRig.CHEST_RING_ARCS[index]), hub);
                }
                if (reference == null) {
                    reference = radii;
                } else {
                    for (int index = 0; index < radii.length; index++) {
                        if (Math.abs(radii[index] - reference[index]) > .0005F) {
                            // A per joint rotation would swing each arc around its own pivot instead.
                            throw new AssertionError("Chest ring arc left its circle: "
                                    + VicissitudeRig.CHEST_RING_ARCS[index] + " at " + tick);
                        }
                    }
                }
                if (tick == 0) {
                    continue;
                }
                // Idle only: the same action pose at the same action clock is the reference, so the
                // authored fragment flare of a cast cancels and only the slow spin is left. Each
                // ring is measured against its own non spinning ancestor, which removes the body
                // bob and the torso lean as well.
                var spine = VicissitudeRig.newPose();
                VicissitudeRig.compute(spine, false, 0, action, tick % 50, false, 0, 0, 0, -1);
                float ringTurn = inPlaneAngle(pose, Joint.TORSO, Joint.CHEST_RING_LEFT, 1.0F, 0.0F)
                                 - inPlaneAngle(spine, Joint.TORSO, Joint.CHEST_RING_LEFT, 1.0F, 0.0F);
                float haloTurn = inPlaneAngle(pose, Joint.HEAD_ROOT, Joint.HALO_ROOT, 14.0F, 0.0F)
                                 - inPlaneAngle(spine, Joint.HEAD_ROOT, Joint.HALO_ROOT, 14.0F, 0.0F);
                float expected = 2.0F * tick * VicissitudeRig.CHEST_RING_SPIN_PER_TICK;
                if (Math.abs(ringTurn - haloTurn - expected) > .01F) {
                    throw new AssertionError("Rings are not counter rotating at " + tick
                            + ": ring=" + ringTurn + " halo=" + haloTurn);
                }
            }
        }
        System.out.println("PASS: chest ring travels rigidly and counter rotates against the halo");
    }

    /** Angle from a reference joint's local X to a marker vector, about that joint's local Z. */
    private static float inPlaneAngle(VicissitudeRig.Pose pose, Joint reference, Joint marker,
                                      float markerX, float markerY) {
        float[] axis = offset(pose, reference, 1.0F, 0.0F, 0.0F);
        float[] normal = offset(pose, reference, 0.0F, 0.0F, 1.0F);
        float[] vector = offset(pose, marker, markerX, markerY, 0.0F);
        float crossX = axis[1] * vector[2] - axis[2] * vector[1];
        float crossY = axis[2] * vector[0] - axis[0] * vector[2];
        float crossZ = axis[0] * vector[1] - axis[1] * vector[0];
        float dot = axis[0] * vector[0] + axis[1] * vector[1] + axis[2] * vector[2];
        float along = crossX * normal[0] + crossY * normal[1] + crossZ * normal[2];
        return (float) Math.toDegrees(Math.atan2(along, dot));
    }

    private static float[] offset(VicissitudeRig.Pose pose, Joint joint, float x, float y, float z) {
        var origin = VicissitudeRig.jointOrigin(pose, joint);
        var point = VicissitudeRig.transform(pose, joint, x, y, z);
        return new float[]{point.x() - origin.x(), point.y() - origin.y(),
                point.z() - origin.z()};
    }

    private static float distance(VicissitudeRig.V3 a, VicissitudeRig.V3 b) {
        return (float) Math.sqrt((a.x() - b.x()) * (a.x() - b.x()) + (a.y() - b.y()) * (a.y() - b.y())
                                 + (a.z() - b.z()) * (a.z() - b.z()));
    }

    /**
     * Wings must stay wings. Sweeps every action, phase, idle clock and wing fold and checks that
     * no wing joint is rotated further than the authored poses ever intend: an earlier secondary
     * motion pass scaled the wing drag by a factor twice and sent the roots through a full turn,
     * which read as the wings flapping on their own.
     */
    private static void checkWingComposure() {
        String[] wingJoints = {"WING_LEFT_ROOT", "WING_RIGHT_ROOT", "WING_LEFT_OUTER",
                "WING_RIGHT_OUTER", "WING_LEFT_LOWER", "WING_RIGHT_LOWER",
                "WING_LEFT_FEATHERS", "WING_RIGHT_FEATHERS"};
        // Authored envelope per joint, roughly fifteen percent above the largest pose the sweep
        // actually produces, so any accidental extra multiplier trips the check immediately.
        float[][] limits = {{14.0F, 70.0F, 66.0F}, {14.0F, 70.0F, 66.0F}, {10.0F, 56.0F, 24.0F},
                {10.0F, 56.0F, 24.0F}, {8.0F, 52.0F, 10.0F}, {8.0F, 52.0F, 10.0F},
                {12.0F, 12.0F, 10.0F}, {12.0F, 12.0F, 10.0F}};
        float[] peak = new float[wingJoints.length * 3];
        long samples = 0;
        for (int phase = 0; phase < 2; phase++) {
            for (var action : VicissitudeRig.Action.values()) {
                for (int tick = 0; tick <= action.duration() + 8; tick++) {
                    for (float fold : new float[]{0.0F, 0.5F, 1.0F}) {
                        var pose = VicissitudeRig.newPose();
                        VicissitudeRig.compute(pose, phase == 1, phase, action, tick, true, 3.0F,
                                tick * 3.0F, fold, -1.0F);
                        for (int index = 0; index < wingJoints.length; index++) {
                            Joint joint = Joint.valueOf(wingJoints[index]);
                            float[] rotations = {pose.rotX(joint), pose.rotY(joint), pose.rotZ(joint)};
                            for (int axis = 0; axis < 3; axis++) {
                                float magnitude = Math.abs(rotations[axis]);
                                peak[index * 3 + axis] = Math.max(peak[index * 3 + axis], magnitude);
                                if (magnitude > limits[index][axis]) {
                                    throw new AssertionError("Wing joint over rotated: " + joint
                                            + " axis " + axis + " = " + magnitude + " at "
                                            + action + " tick " + tick + " phase " + phase
                                            + " fold " + fold);
                                }
                            }
                        }
                        samples++;
                    }
                }
            }
        }
        StringBuilder report = new StringBuilder();
        for (int index = 0; index < wingJoints.length; index++) {
            report.append(index == 0 ? "" : ", ").append(wingJoints[index]).append('=')
                    .append(String.format("%.1f/%.1f/%.1f", peak[index * 3], peak[index * 3 + 1],
                            peak[index * 3 + 2]));
        }
        System.out.println("PASS: " + samples + " wing composure samples within limits ("
                           + report + ")");
    }
}
