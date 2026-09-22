package org.brahypno.maledict.rig;

import org.brahypno.maledict.rig.VicissitudeRigData.Joint;

import java.util.ArrayList;
import java.util.List;

/**
 * Pose evaluation and forward kinematics for the First Vicissitude skeleton.
 *
 * <p>The client model and the server-side attack geometry both consume this class, so the
 * rendered wing tips and authoritative release points share the same pose equations. Everything here
 * is expressed in the authoring space documented on {@link VicissitudeRigData}; convert to
 * entity-local blocks with {@link #toEntityLocal} and to world space with
 * {@link #toWorld(double, double, double, float)}.
 */
public final class VicissitudeRig {
    public static final int JOINT_COUNT = Joint.values().length;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    /**
     * Idle spin of the broken halo, in degrees per tick.
     */
    public static final float HALO_SPIN_PER_TICK = 0.6F;
    /**
     * The chest ring answers the halo at the same speed in the opposite direction.
     */
    public static final float CHEST_RING_SPIN_PER_TICK = -0.6F;
    /**
     * Hub of the broken chest ring, relative to the torso pivot (model units). The ring lies in
     * the chest plane, so only X and Y define the axis; the depth comes from the mesh itself.
     */
    public static final float CHEST_RING_HUB_X = 0.0F;
    public static final float CHEST_RING_HUB_Y = 1.0F;
    /**
     * The three surviving arcs of the chest ring.
     */
    public static final Joint[] CHEST_RING_ARCS = {
            Joint.CHEST_RING_LEFT, Joint.CHEST_RING_RIGHT, Joint.CHEST_RING_BOTTOM};
    /**
     * Ticks the off hand, the wings and the lower body trail the weapon by.
     */
    private static final float FOLLOW_LAG = 4.0F;
    /**
     * How much of a body turn the wing pair is allowed to lag behind, and the absolute ceiling in
     * degrees. Small on purpose: the strike curve crosses the whole turn in a couple of ticks, so
     * anything larger reads as the wings flapping on their own.
     */
    private static final float WING_DRAG_FRACTION = 0.6F;
    private static final float WING_DRAG_CAP = 30.0F;

    /**
     * Coarse hit segments; multipliers follow the design specification.
     */
    public enum Segment {
        BODY(1.0F),
        HEAD(1.25F),
        WING_LEFT_ROOT(0.75F),
        WING_LEFT_OUTER(0.5F),
        WING_RIGHT_ROOT(0.75F),
        WING_RIGHT_OUTER(0.5F);

        private final float multiplier;

        Segment(float multiplier) {
            this.multiplier = multiplier;
        }

        public float multiplier() {
            return multiplier;
        }

        public boolean isWing() {
            return this != BODY && this != HEAD;
        }
    }

    /**
     * A point in authoring space (model units).
     */
    public record V3(float x, float y, float z) {
        public V3 add(V3 other) {
            return new V3(x + other.x, y + other.y, z + other.z);
        }
    }

    /**
     * An axis aligned box in entity-local blocks.
     */
    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public boolean intersects(Box other) {
            return minX < other.maxX && maxX > other.minX
                   && minY < other.maxY && maxY > other.minY
                   && minZ < other.maxZ && maxZ > other.minZ;
        }

        public boolean contains(double x, double y, double z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        public double centerX() {
            return (minX + maxX) * 0.5D;
        }

        public double centerY() {
            return (minY + maxY) * 0.5D;
        }

        public double centerZ() {
            return (minZ + maxZ) * 0.5D;
        }

        public Box inflate(double amount) {
            return new Box(minX - amount, minY - amount, minZ - amount,
                           maxX + amount, maxY + amount, maxZ + amount);
        }

        public Box expandTowards(double dx, double dy, double dz) {
            return new Box(
                    Math.min(minX, minX + dx), Math.min(minY, minY + dy), Math.min(minZ, minZ + dz),
                    Math.max(maxX, maxX + dx), Math.max(maxY, maxY + dy), Math.max(maxZ, maxZ + dz));
        }

        public static Box of(double x1, double y1, double z1, double x2, double y2, double z2) {
            return new Box(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                           Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
        }
    }

    /**
     * Mutable per-frame pose; reuse one instance per entity.
     */
    public static final class Pose {
        private boolean bareWings;
        private final float[] rotX = new float[JOINT_COUNT];
        private final float[] rotY = new float[JOINT_COUNT];
        private final float[] rotZ = new float[JOINT_COUNT];
        private final float[] offX = new float[JOINT_COUNT];
        private final float[] offY = new float[JOINT_COUNT];
        private final float[] offZ = new float[JOINT_COUNT];
        private final float[] matrices = new float[16 * JOINT_COUNT];

        private Pose() {
        }

        public void reset() {
            bareWings = false;
            java.util.Arrays.fill(rotX, 0.0F);
            java.util.Arrays.fill(rotY, 0.0F);
            java.util.Arrays.fill(rotZ, 0.0F);
            java.util.Arrays.fill(offX, 0.0F);
            java.util.Arrays.fill(offY, 0.0F);
            java.util.Arrays.fill(offZ, 0.0F);
        }

        public float rotX(Joint joint) {
            return rotX[joint.index()];
        }

        public float rotY(Joint joint) {
            return rotY[joint.index()];
        }

        public float rotZ(Joint joint) {
            return rotZ[joint.index()];
        }

        public float offX(Joint joint) {
            return offX[joint.index()];
        }

        public float offY(Joint joint) {
            return offY[joint.index()];
        }

        public float offZ(Joint joint) {
            return offZ[joint.index()];
        }

        public void setRotation(Joint joint, float x, float y, float z) {
            int index = joint.index();
            rotX[index] = x;
            rotY[index] = y;
            rotZ[index] = z;
        }

        public void addRotation(Joint joint, float x, float y, float z) {
            int index = joint.index();
            rotX[index] += x;
            rotY[index] += y;
            rotZ[index] += z;
        }

        public void setOffset(Joint joint, float x, float y, float z) {
            int index = joint.index();
            offX[index] = x;
            offY[index] = y;
            offZ[index] = z;
        }

        public void addOffset(Joint joint, float x, float y, float z) {
            int index = joint.index();
            offX[index] += x;
            offY[index] += y;
            offZ[index] += z;
        }

        /**
         * Copies every channel and solved matrix of another pose.
         */
        public void copyFrom(Pose other) {
            bareWings = other.bareWings;
            System.arraycopy(other.rotX, 0, rotX, 0, JOINT_COUNT);
            System.arraycopy(other.rotY, 0, rotY, 0, JOINT_COUNT);
            System.arraycopy(other.rotZ, 0, rotZ, 0, JOINT_COUNT);
            System.arraycopy(other.offX, 0, offX, 0, JOINT_COUNT);
            System.arraycopy(other.offY, 0, offY, 0, JOINT_COUNT);
            System.arraycopy(other.offZ, 0, offZ, 0, JOINT_COUNT);
            System.arraycopy(other.matrices, 0, matrices, 0, matrices.length);
        }

        /**
         * Moves this pose a fraction of the way towards the target pose. Blending authored poses
         * is what keeps action changes continuous instead of snapping between keyframes.
         */
        public void interpolate(Pose target, float delta) {
            bareWings = target.bareWings;
            float amount = Math.max(0.0F, Math.min(1.0F, delta));
            for (int index = 0; index < JOINT_COUNT; index++) {
                rotX[index] += (target.rotX[index] - rotX[index]) * amount;
                rotY[index] += (target.rotY[index] - rotY[index]) * amount;
                rotZ[index] += (target.rotZ[index] - rotZ[index]) * amount;
                offX[index] += (target.offX[index] - offX[index]) * amount;
                offY[index] += (target.offY[index] - offY[index]) * amount;
                offZ[index] += (target.offZ[index] - offZ[index]) * amount;
            }
        }
    }

    private VicissitudeRig() {
    }

    public static Pose newPose() {
        return new Pose();
    }

    /**
     * Builds the full pose for one frame. All inputs are already interpolated by the caller.
     *
     * @param phaseTwoBlend 0 for the phase-one silhouette, 1 for the phase-two silhouette
     * @param hurtTicks     remaining ticks of the hurt reaction, 0 when unhurt
     * @param idleTicks     continuous animation clock (world game time based)
     * @param wingFold      0 = wings fully spread, 1 = wings folded against the body
     * @param deathTicks    ticks since death started, negative while alive
     */
    public static void compute(
            Pose pose, boolean phaseTwo, float phaseTwoBlend, Action action,
            float actionTicks, boolean actionLeft, float hurtTicks,
            float idleTicks, float wingFold, float deathTicks) {
        pose.reset();
        pose.bareWings = phaseTwo || phaseTwoBlend >= 1.0F;
        applyStage(pose, phaseTwoBlend);
        applyIdle(pose, idleTicks, phaseTwoBlend);
        applyAction(pose, action, actionTicks, actionLeft);
        applyFold(pose, wingFold);
        applyHurt(pose, hurtTicks);
        applyDeath(pose, deathTicks);
        solve(pose);
    }

    /**
     * Stage silhouettes: high idol versus forward-leaning executor.
     */
    private static void applyStage(Pose pose, float blend) {
        float torsoLean = lerp(0.0F, 15.0F, blend);
        float headDown = lerp(8.0F, 15.0F, blend);
        float armSpread = lerp(24.0F, 14.0F, blend);
        float wingRaise = lerp(30.0F, 8.0F, blend);
        float shoulderForward = lerp(0.0F, 12.0F, blend);

        pose.addRotation(Joint.BODY, torsoLean * 0.35F, 0.0F, 0.0F);
        pose.addRotation(Joint.TORSO, torsoLean, 0.0F, 0.0F);
        pose.addRotation(Joint.HEAD_ROOT, headDown, 0.0F, 0.0F);
        pose.addRotation(Joint.ARM_LEFT, 0.0F, 0.0F, -armSpread);
        pose.addRotation(Joint.ARM_RIGHT, -shoulderForward, 0.0F, armSpread);
        pose.addRotation(Joint.WING_LEFT_ROOT, 0.0F, 0.0F, -wingRaise);
        pose.addRotation(Joint.WING_RIGHT_ROOT, 0.0F, 0.0F, wingRaise);
        // The lower body trails further behind once the boss leans into the fight.
        pose.addRotation(Joint.LOWER_ROOT, lerp(0.0F, 10.0F, blend), 0.0F, 0.0F);
        pose.addRotation(Joint.SPINE_TAIL_1, lerp(2.0F, 8.0F, blend), 0.0F, 0.0F);
        pose.addRotation(Joint.SPINE_TAIL_2, lerp(2.0F, 8.0F, blend), 0.0F, 0.0F);
        pose.addRotation(Joint.SPINE_TAIL_3, lerp(2.0F, 8.0F, blend), 0.0F, 0.0F);
        // Bind rotations for the four broken halo arcs; the gaps between them stay open.
        pose.setRotation(Joint.HALO_FRAGMENT_1, 0.0F, 0.0F, -30.0F);
        pose.setRotation(Joint.HALO_FRAGMENT_2, 0.0F, 0.0F, 99.0F);
        pose.setRotation(Joint.HALO_FRAGMENT_3, 0.0F, 0.0F, 163.0F);
        pose.setRotation(Joint.HALO_FRAGMENT_4, 0.0F, 0.0F, -108.0F);
    }

    /**
     * Floating idle: body bob, feather micro motion, counter-rotating rings and delayed spine sway.
     */
    private static void applyIdle(Pose pose, float ticks, float phaseTwoBlend) {
        float bob = (float) Math.sin(ticks * (Math.PI * 2.0D / 80.0D));
        pose.addOffset(Joint.BODY, 0.0F, -1.28F * bob, 0.0F);
        pose.addRotation(Joint.BODY, bob * 1.5F, bob, 0.0F);
        // The halo lies in model XY. Spin about its normal (Z), not vertical Y; the chest ring
        // turns the other way so the two broken rings read as one linked mechanism.
        pose.addRotation(Joint.HALO_ROOT, 0.0F, 0.0F, ticks * HALO_SPIN_PER_TICK);
        spinChestRing(pose, ticks * CHEST_RING_SPIN_PER_TICK);
        for (int i = 1; i <= 4; i++) {
            Joint left = Joint.valueOf("WING_LEFT_FEATHER_" + i);
            Joint right = Joint.valueOf("WING_RIGHT_FEATHER_" + i);
            float phase = ticks * (float) (Math.PI * 2.0D / 40.0D) + i * 0.7F;
            float sway = (float) Math.sin(phase) * 3.0F;
            pose.addRotation(left, sway, sway * 0.5F, 0.0F);
            pose.addRotation(right, sway, -sway * 0.5F, 0.0F);
        }
        for (int i = 0; i < 3; i++) {
            float phase = (ticks - i * 6.0F) * (float) (Math.PI * 2.0D / 80.0D);
            float sway = (float) Math.sin(phase) * 2.0F;
            pose.addRotation(Joint.values()[Joint.SPINE_TAIL_1.index() + i], sway, 0.0F, sway * 0.5F);
        }
        pose.addRotation(Joint.HALO_FRAGMENT_1, 0.0F, 2.0F * bob, 0.0F);
        pose.addRotation(Joint.HALO_FRAGMENT_3, 0.0F, -2.0F * bob, 0.0F);
        // Arms drift on their own slow clock and out of phase with each other, so the silhouette
        // keeps moving while the boss hovers and waits. Phase two drops the left arm lower: the
        // design asks for an independent left hand once the executor silhouette takes over.
        float drift = (float) Math.sin(ticks * (float) (Math.PI * 2.0D / 120.0D));
        float counter = (float) Math.sin(ticks * (float) (Math.PI * 2.0D / 120.0D) + 1.9F);
        pose.addRotation(Joint.ARM_LEFT, drift * 2.6F, 0.0F,
                         -drift * 2.2F - 4.0F * phaseTwoBlend);
        pose.addRotation(Joint.FOREARM_LEFT, drift * 2.2F + 3.0F * phaseTwoBlend, 0.0F, 0.0F);
        pose.addRotation(Joint.ARM_RIGHT, counter * 2.2F, 0.0F, counter * 1.8F);
        pose.addRotation(Joint.FOREARM_RIGHT, counter * 1.6F, 0.0F, 0.0F);
        // The wing roots breathe on a slower clock than the body bob, and slightly unevenly:
        // a pair of wings that moves as one rigid plate is the fastest way to look like a prop.
        float breathe = (float) Math.sin(ticks * (float) (Math.PI * 2.0D / 160.0D));
        float uneven = (float) Math.sin(ticks * (float) (Math.PI * 2.0D / 160.0D) + 0.8F);
        pose.addRotation(Joint.WING_LEFT_ROOT, breathe * 2.4F, breathe * 3.2F, -breathe * 4.5F);
        pose.addRotation(Joint.WING_RIGHT_ROOT, uneven * 2.4F, -uneven * 3.2F, uneven * 4.5F);
        pose.addRotation(Joint.WING_LEFT_OUTER, 0.0F, breathe * 2.6F, -uneven * 3.0F);
        pose.addRotation(Joint.WING_RIGHT_OUTER, 0.0F, -uneven * 2.6F, breathe * 3.0F);
        pose.addRotation(Joint.WING_LEFT_LOWER, 0.0F, uneven * 3.0F, 0.0F);
        pose.addRotation(Joint.WING_RIGHT_LOWER, 0.0F, -breathe * 3.0F, 0.0F);
    }

    /**
     * Spins the whole broken chest ring around its hub.
     *
     * <p>The three arcs are separate joints whose pivots sit on the ring itself, so a per joint
     * rotation would only spin each arc in place. Moving every pivot along the circle the arcs
     * travel turns the fragments into one rigid ring again, which is how the halo already moves.
     */
    private static void spinChestRing(Pose pose, float degrees) {
        float radians = degrees * DEG_TO_RAD;
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        for (Joint arc : CHEST_RING_ARCS) {
            float dx = arc.localX() - CHEST_RING_HUB_X;
            float dy = arc.localY() - CHEST_RING_HUB_Y;
            // The hub only defines the axis: a spin in the model XY plane never moves the arcs
            // away from the chest, so Z is left exactly where it was authored.
            pose.addOffset(arc, cos * dx - sin * dy - dx, sin * dx + cos * dy - dy, 0.0F);
            pose.addRotation(arc, 0.0F, 0.0F, degrees);
        }
    }

    /**
     * Attack poses. Timings match the combat specification tables.
     */
    private static void applyAction(Pose pose, Action action, float t, boolean left) {
        if (action == null || action == Action.NONE || t < 0.0F){
            return;
        }
        float total = Math.max(1.0F, action.duration());
        ActionSample lead = sampleAction(action, t, 0.0F);
        float swing = lead.swing();
        float wind = lead.wind();
        // Trailing samples of the same curves. The weapon leads and everything else follows a
        // few ticks later, which is what turns one rigid arm swing into a whole body motion.
        ActionSample follow = sampleAction(action, t, FOLLOW_LAG);
        float swingLag = follow.swing();
        float windLag = follow.wind();
        float trail = swing - swingLag;
        switch (action) {
            case WING_RANGED -> {
                Joint root = left ? Joint.WING_LEFT_ROOT : Joint.WING_RIGHT_ROOT;
                Joint tip = left ? Joint.WING_LEFT_TIP : Joint.WING_RIGHT_TIP;
                // Positive yaw sweeps the left wing forward; the right wing mirrors it.
                float sweep = left ? 1.0F : -1.0F;
                pose.addRotation(root, 0.0F, sweep * (-18.0F * wind + 26.0F * swing), 0.0F);
                pose.addRotation(tip, 0.0F, sweep * 15.0F * wind, 0.0F);
                pose.addRotation(Joint.TORSO, 0.0F, sweep * 9.0F * swing, 0.0F);
                // Both hands brace in front of the chest; the far wing folds back to clear the shot.
                pose.addRotation(Joint.ARM_LEFT, -10.0F * wind + 14.0F * swingLag, 0.0F,
                                 10.0F * wind + 6.0F * swing);
                pose.addRotation(Joint.ARM_RIGHT, -10.0F * wind + 14.0F * swingLag, 0.0F,
                                 -10.0F * wind - 6.0F * swing);
                pose.addRotation(Joint.FOREARM_LEFT, 16.0F * wind - 20.0F * swingLag, 0.0F, 0.0F);
                pose.addRotation(Joint.FOREARM_RIGHT, 16.0F * wind - 20.0F * swingLag, 0.0F, 0.0F);
                float far = left ? -1.0F : 1.0F;
                pose.addRotation(Joint.WING_RIGHT_LOWER, 0.0F, far * 9.0F * wind, 0.0F);
                pose.addRotation(Joint.WING_LEFT_LOWER, 0.0F, far * -9.0F * wind, 0.0F);
                dragWingsYaw(pose, sweep * 9.0F * (swing - swingLag));
            }
            case WING_BARRAGE -> {
                float pulse = 0.0F;
                for (int wave = 0; wave < 3; wave++) {
                    float waveTick = 20.0F + wave * 8.0F;
                    pulse += bell(t, waveTick, waveTick + 12.0F) * (1.0F - wave * 0.15F);
                }
                float load = clamp(t / 20.0F);
                float settle = t > 36.0F ? clamp(1.0F - (t - 36.0F) / 20.0F) : 1.0F;
                pose.addRotation(Joint.WING_LEFT_ROOT,
                                 -6.0F * load * settle, -14.0F * load * settle + 26.0F * pulse, -8.0F * pulse);
                pose.addRotation(Joint.WING_RIGHT_ROOT,
                                 -6.0F * load * settle, 14.0F * load * settle - 26.0F * pulse, 8.0F * pulse);
                pose.addRotation(Joint.WING_LEFT_OUTER, 0.0F, 10.0F * pulse, 0.0F);
                pose.addRotation(Joint.WING_RIGHT_OUTER, 0.0F, -10.0F * pulse, 0.0F);
                pose.addRotation(Joint.TORSO, -4.0F * load * settle, 0.0F, 0.0F);
                // The arms pump once per volley instead of hanging while the wings do the work.
                pose.addRotation(Joint.ARM_LEFT, 12.0F * load * settle - 18.0F * pulse, 0.0F,
                                 14.0F * load * settle + 10.0F * pulse);
                pose.addRotation(Joint.ARM_RIGHT, 12.0F * load * settle - 18.0F * pulse, 0.0F,
                                 -14.0F * load * settle - 10.0F * pulse);
                pose.addRotation(Joint.FOREARM_LEFT, 20.0F * load * settle - 26.0F * pulse, 0.0F, 0.0F);
                pose.addRotation(Joint.FOREARM_RIGHT, 20.0F * load * settle - 26.0F * pulse, 0.0F, 0.0F);
                pose.addRotation(Joint.LOWER_ROOT, 6.0F * load * settle, 0.0F, 0.0F);
            }
            case CAST_FROM_CHEST -> {
                float lean = 10.0F * wind - 16.0F * swing;
                float leanLag = 10.0F * windLag - 16.0F * swingLag;
                pose.addRotation(Joint.TORSO, lean, 0.0F, 0.0F);
                pose.addRotation(Joint.CHEST_RING_LEFT, 0.0F, 0.0F, -38.0F * wind + 58.0F * swing);
                pose.addRotation(Joint.CHEST_RING_RIGHT, 0.0F, 0.0F, 38.0F * wind - 58.0F * swing);
                pose.addRotation(Joint.CHEST_RING_BOTTOM, 40.0F * swing, 0.0F, 0.0F);
                pose.addRotation(Joint.CHEST_SHELL_LEFT, 0.0F, 0.0F, -12.0F * swing);
                pose.addRotation(Joint.CHEST_SHELL_RIGHT, 0.0F, 0.0F, 12.0F * swing);
                pose.addRotation(Joint.HEAD_ROOT, -8.0F * wind, 0.0F, 0.0F);
                // Both hands cup the hollow chest while it charges, then throw wide on the release.
                pose.addRotation(Joint.ARM_LEFT, 46.0F * wind - 66.0F * swingLag, 0.0F,
                                 22.0F * wind - 30.0F * swing);
                pose.addRotation(Joint.ARM_RIGHT, 46.0F * wind - 66.0F * swingLag, 0.0F,
                                 -22.0F * wind + 30.0F * swing);
                pose.addRotation(Joint.FOREARM_LEFT, 34.0F * wind - 48.0F * swing, 0.0F, 0.0F);
                pose.addRotation(Joint.FOREARM_RIGHT, 34.0F * wind - 48.0F * swing, 0.0F, 0.0F);
                dragWingsPitch(pose, lean - leanLag);
                pose.addRotation(Joint.SPINE_TAIL_1, 6.0F * windLag - 10.0F * swingLag, 0.0F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_2, 7.0F * windLag - 12.0F * swingLag, 0.0F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_3, 8.0F * windLag - 14.0F * swingLag, 0.0F, 0.0F);
            }
            case CAST_FROM_HALO -> {
                pose.addRotation(Joint.HEAD_ROOT, -14.0F * wind + 10.0F * swing, 0.0F, 0.0F);
                pose.addRotation(Joint.HALO_ROOT, 0.0F, 0.0F, 48.0F * wind - 115.0F * swing);
                pose.addRotation(Joint.HALO_FRAGMENT_1, 0.0F, 0.0F, -18.0F * swing);
                pose.addRotation(Joint.HALO_FRAGMENT_2, 0.0F, 0.0F, 22.0F * swing);
                pose.addRotation(Joint.HALO_FRAGMENT_3, 0.0F, 0.0F, -26.0F * swing);
                pose.addRotation(Joint.HALO_FRAGMENT_4, 0.0F, 0.0F, 16.0F * swing);
                pose.addRotation(Joint.BODY, 0.0F, 12.0F * swing, 0.0F);
                // The right hand reaches up into the ring; the left arm holds the balance low and wide.
                pose.addRotation(Joint.ARM_RIGHT, 96.0F * wind - 128.0F * swingLag, 0.0F,
                                 14.0F * wind - 8.0F * swing);
                pose.addRotation(Joint.FOREARM_RIGHT, 26.0F * wind - 34.0F * swingLag, 0.0F, 0.0F);
                pose.addRotation(Joint.ARM_LEFT, -22.0F * wind + 30.0F * swingLag, 0.0F,
                                 -26.0F * wind - 12.0F * swing);
                pose.addRotation(Joint.FOREARM_LEFT, -14.0F * wind, 0.0F, 0.0F);
                dragWingsYaw(pose, 12.0F * (swing - swingLag));
            }
            case SLASH_HORIZONTAL -> {
                // Windup turns the body to its right, the release sweeps through to the left.
                float bodyYaw = 30.0F * wind - 62.0F * swing;
                pose.addRotation(Joint.BODY, 0.0F, bodyYaw, 0.0F);
                pose.addRotation(Joint.TORSO, 0.0F, 16.0F * wind - 36.0F * swing, 0.0F);
                pose.addRotation(Joint.ARM_RIGHT, 44.0F * wind - 104.0F * swing, 0.0F, 26.0F * wind);
                pose.addRotation(Joint.FOREARM_RIGHT, 52.0F * wind - 78.0F * swing, 0.0F, 0.0F);
                // The free arm is the counterweight: it lifts against the windup and whips out
                // the other way while the blade crosses, four ticks behind the weapon.
                pose.addRotation(Joint.ARM_LEFT, 34.0F * windLag - 52.0F * swingLag, 0.0F,
                                 -20.0F * wind + 30.0F * swing);
                pose.addRotation(Joint.FOREARM_LEFT, 20.0F * windLag - 30.0F * swingLag, 0.0F, 0.0F);
                dragWingsYaw(pose, bodyYaw - (30.0F * windLag - 62.0F * swingLag));
                pose.addRotation(Joint.LOWER_ROOT, 0.0F, -bodyYaw * 0.22F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_1, 0.0F, -bodyYaw * 0.10F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_2, 0.0F, -bodyYaw * 0.14F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_3, 0.0F, -bodyYaw * 0.18F, 0.0F);
            }
            case SLASH_VERTICAL -> {
                float chop = -12.0F * wind + 28.0F * swing;
                pose.addRotation(Joint.BODY, chop, 0.0F, 0.0F);
                pose.addRotation(Joint.ARM_RIGHT, 118.0F * wind - 172.0F * swing, 0.0F, 12.0F * wind);
                pose.addRotation(Joint.FOREARM_RIGHT, 62.0F * wind - 82.0F * swing, 0.0F, 0.0F);
                pose.addRotation(Joint.TORSO, -9.0F * wind + 18.0F * swing, 0.0F, 0.0F);
                pose.addRotation(Joint.ARM_LEFT, 36.0F * windLag - 62.0F * swingLag, 0.0F,
                                 -14.0F * wind - 8.0F * swing);
                pose.addRotation(Joint.FOREARM_LEFT, 22.0F * windLag - 34.0F * swingLag, 0.0F, 0.0F);
                dragWingsPitch(pose, chop - (-12.0F * windLag + 28.0F * swingLag));
                pose.addRotation(Joint.WING_LEFT_ROOT, 0.0F, 0.0F, -8.0F * swing);
                pose.addRotation(Joint.WING_RIGHT_ROOT, 0.0F, 0.0F, 8.0F * swing);
                pose.addRotation(Joint.LOWER_ROOT, 10.0F * windLag - 18.0F * swingLag, 0.0F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_1, 8.0F * trail, 0.0F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_2, 10.0F * trail, 0.0F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_3, 12.0F * trail, 0.0F, 0.0F);
            }
            case HEAVY_ATTACK -> {
                float slam = -20.0F * wind + 36.0F * swing;
                pose.addRotation(Joint.BODY, slam, 0.0F, 0.0F);
                pose.addRotation(Joint.TORSO, -14.0F * wind + 28.0F * swing, 0.0F, 0.0F);
                pose.addRotation(Joint.ARM_RIGHT, 152.0F * wind - 208.0F * swing, 0.0F, 18.0F * wind);
                pose.addRotation(Joint.FOREARM_RIGHT, 72.0F * wind - 92.0F * swing, 0.0F, 0.0F);
                pose.addRotation(Joint.HEAD_ROOT, -14.0F * wind + 26.0F * swing, 0.0F, 0.0F);
                pose.addRotation(Joint.WING_LEFT_ROOT, 0.0F, 0.0F, -24.0F * wind + 36.0F * swing);
                pose.addRotation(Joint.WING_RIGHT_ROOT, 0.0F, 0.0F, 24.0F * wind - 36.0F * swing);
                // The whole body commits: the off arm drives back, the tail drags, the wings snap.
                pose.addRotation(Joint.ARM_LEFT, 44.0F * windLag - 78.0F * swingLag, 0.0F,
                                 -26.0F * wind - 12.0F * swing);
                pose.addRotation(Joint.FOREARM_LEFT, 30.0F * windLag - 44.0F * swingLag, 0.0F, 0.0F);
                dragWingsPitch(pose, slam - (-20.0F * windLag + 36.0F * swingLag));
                pose.addRotation(Joint.WING_LEFT_OUTER, 0.0F, 0.0F, -14.0F * swing);
                pose.addRotation(Joint.WING_RIGHT_OUTER, 0.0F, 0.0F, 14.0F * swing);
                pose.addRotation(Joint.LOWER_ROOT, 18.0F * windLag - 30.0F * swingLag, 0.0F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_1, 12.0F * trail + 6.0F * wind, 0.0F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_2, 15.0F * trail + 7.0F * wind, 0.0F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_3, 18.0F * trail + 8.0F * wind, 0.0F, 0.0F);
            }
            case DASH -> {
                float lunge = clamp((t - 20.0F) / 12.0F);
                float brake = clamp((t - 32.0F) / 18.0F);
                float push = lunge * (1.0F - brake);
                pose.addRotation(Joint.BODY, -14.0F * wind + 30.0F * push, 0.0F, 0.0F);
                pose.addRotation(Joint.TORSO, -10.0F * wind + 18.0F * push, 0.0F, 0.0F);
                pose.addRotation(Joint.WING_LEFT_ROOT, 0.0F, -20.0F * wind + 30.0F * push, 14.0F * wind);
                pose.addRotation(Joint.WING_RIGHT_ROOT, 0.0F, 20.0F * wind - 30.0F * push, -14.0F * wind);
                pose.addRotation(Joint.ARM_RIGHT, 20.0F * wind - 35.0F * push, 0.0F, 0.0F);
                // The off arm reaches back through the charge and swings forward when it brakes.
                pose.addRotation(Joint.ARM_LEFT, 46.0F * wind - 74.0F * push - 24.0F * brake, 0.0F,
                                 -10.0F * wind);
                pose.addRotation(Joint.FOREARM_LEFT, 24.0F * wind - 30.0F * push, 0.0F, 0.0F);
                pose.addRotation(Joint.WING_LEFT_OUTER, 0.0F, 12.0F * push, 0.0F);
                pose.addRotation(Joint.WING_RIGHT_OUTER, 0.0F, -12.0F * push, 0.0F);
                pose.addRotation(Joint.LOWER_ROOT, 20.0F * wind - 26.0F * push, 0.0F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_1, 10.0F * push, 0.0F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_2, 12.0F * push, 0.0F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_3, 14.0F * push, 0.0F, 0.0F);
            }
            case SCYTHE_THROW -> {
                float bodyYaw = 34.0F * wind - 64.0F * swing;
                pose.addRotation(Joint.BODY, 0.0F, bodyYaw, 0.0F);
                pose.addRotation(Joint.ARM_RIGHT, 126.0F * wind - 184.0F * swing, 0.0F, 0.0F);
                pose.addRotation(Joint.FOREARM_RIGHT, 58.0F * wind - 74.0F * swing, 0.0F, 0.0F);
                pose.addRotation(Joint.ARM_LEFT, 30.0F * windLag - 46.0F * swingLag, 0.0F,
                                 -18.0F * swing);
                pose.addRotation(Joint.FOREARM_LEFT, 18.0F * windLag, 0.0F, 0.0F);
                pose.addRotation(Joint.WING_LEFT_ROOT, 0.0F, -12.0F * wind + 18.0F * swingLag, 0.0F);
                pose.addRotation(Joint.WING_RIGHT_ROOT, 0.0F, 12.0F * wind - 18.0F * swingLag, 0.0F);
                dragWingsYaw(pose, bodyYaw - (34.0F * windLag - 64.0F * swingLag));
                pose.addRotation(Joint.LOWER_ROOT, 0.0F, -bodyYaw * 0.25F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_2, 0.0F, -bodyYaw * 0.12F, 0.0F);
                pose.addRotation(Joint.SPINE_TAIL_3, 0.0F, -bodyYaw * 0.16F, 0.0F);
            }
            case SCYTHE_RECOVER -> {
                float ease = 1.0F - clamp(t / total);
                pose.addRotation(Joint.ARM_RIGHT, -25.0F * ease, 0.0F, 0.0F);
                pose.addRotation(Joint.FOREARM_RIGHT, -20.0F * ease, 0.0F, 0.0F);
                pose.addRotation(Joint.ARM_LEFT, -14.0F * ease, 0.0F, 6.0F * ease);
                pose.addRotation(Joint.FOREARM_LEFT, -12.0F * ease, 0.0F, 0.0F);
                pose.addRotation(Joint.WING_LEFT_ROOT, 0.0F, 0.0F, -6.0F * ease);
                pose.addRotation(Joint.WING_RIGHT_ROOT, 0.0F, 0.0F, 6.0F * ease);
            }
            case RANGED_FALLBACK -> {
                pose.addRotation(Joint.WING_LEFT_ROOT, -8.0F * wind, -24.0F * wind, -14.0F * swing);
                pose.addRotation(Joint.WING_RIGHT_ROOT, -8.0F * wind, 24.0F * wind, 14.0F * swing);
                pose.addRotation(Joint.TORSO, -6.0F * wind + 10.0F * swing, 0.0F, 0.0F);
                pose.addRotation(Joint.ARM_LEFT, 14.0F * wind - 20.0F * swingLag, 0.0F,
                                 12.0F * wind);
                pose.addRotation(Joint.ARM_RIGHT, 14.0F * wind - 20.0F * swingLag, 0.0F,
                                 -12.0F * wind);
                pose.addRotation(Joint.FOREARM_LEFT, 18.0F * wind - 24.0F * swingLag, 0.0F, 0.0F);
                pose.addRotation(Joint.FOREARM_RIGHT, 18.0F * wind - 24.0F * swingLag, 0.0F, 0.0F);
            }
            default -> {
            }
        }
    }

    /**
     * Wing drag for the whole span, following a body yaw: the pair lags behind by a fraction of
     * the turn the torso just made, so the wings read as mass instead of being welded to the
     * spine.
     *
     * <p>The argument is the body's own turn in degrees. The strike curve crosses most of that
     * turn in two or three ticks, so this only ever takes {@link #WING_DRAG_FRACTION} of it and
     * caps the result: a wing that swings further than the torso it hangs from looks broken, not
     * heavy.
     *
     * <p>Both wings take the same sign: this is the pair lagging behind a rotation, not a
     * symmetric fold, so mirroring the sign would cancel the effect out.
     */
    private static void dragWingsYaw(Pose pose, float bodyTurn) {
        float drag = clampDrag(bodyTurn * WING_DRAG_FRACTION);
        pose.addRotation(Joint.WING_LEFT_ROOT, 0.0F, drag, 0.0F);
        pose.addRotation(Joint.WING_RIGHT_ROOT, 0.0F, drag, 0.0F);
        pose.addRotation(Joint.WING_LEFT_OUTER, 0.0F, drag * 0.6F, 0.0F);
        pose.addRotation(Joint.WING_RIGHT_OUTER, 0.0F, drag * 0.6F, 0.0F);
        pose.addRotation(Joint.WING_LEFT_FEATHERS, 0.0F, drag * 0.4F, 0.0F);
        pose.addRotation(Joint.WING_RIGHT_FEATHERS, 0.0F, drag * 0.4F, 0.0F);
    }

    /**
     * Wing drag for a forward lean: the pair lifts against the pitch and settles after it.
     */
    private static void dragWingsPitch(Pose pose, float bodyLean) {
        float drag = clampDrag(bodyLean * WING_DRAG_FRACTION);
        pose.addRotation(Joint.WING_LEFT_ROOT, drag, 0.0F, 0.0F);
        pose.addRotation(Joint.WING_RIGHT_ROOT, drag, 0.0F, 0.0F);
        pose.addRotation(Joint.WING_LEFT_OUTER, drag * 0.7F, 0.0F, 0.0F);
        pose.addRotation(Joint.WING_RIGHT_OUTER, drag * 0.7F, 0.0F, 0.0F);
        pose.addRotation(Joint.WING_LEFT_FEATHERS, drag * 0.5F, 0.0F, 0.0F);
        pose.addRotation(Joint.WING_RIGHT_FEATHERS, drag * 0.5F, 0.0F, 0.0F);
    }

    private static float clampDrag(float degrees) {
        return Math.max(-WING_DRAG_CAP, Math.min(WING_DRAG_CAP, degrees));
    }

    /**
     * Collision driven fold: the wings collapse towards the body when space is tight.
     */
    private static void applyFold(Pose pose, float fold) {
        if (fold <= 0.0F){
            return;
        }
        float amount = clamp(fold);
        pose.addRotation(Joint.WING_LEFT_ROOT, 0.0F, 18.0F * amount, 30.0F * amount);
        pose.addRotation(Joint.WING_RIGHT_ROOT, 0.0F, -18.0F * amount, -30.0F * amount);
        pose.addRotation(Joint.WING_LEFT_OUTER, 0.0F, 26.0F * amount, 12.0F * amount);
        pose.addRotation(Joint.WING_RIGHT_OUTER, 0.0F, -26.0F * amount, -12.0F * amount);
        pose.addRotation(Joint.WING_LEFT_LOWER, 0.0F, 34.0F * amount, 0.0F);
        pose.addRotation(Joint.WING_RIGHT_LOWER, 0.0F, -34.0F * amount, 0.0F);
        pose.addRotation(Joint.ARM_LEFT, 0.0F, 0.0F, -10.0F * amount);
        pose.addRotation(Joint.ARM_RIGHT, 0.0F, 0.0F, 10.0F * amount);
    }

    /**
     * Six ticks of small divergence; shells and feathers lag behind the core.
     */
    private static void applyHurt(Pose pose, float hurtTicks) {
        if (hurtTicks <= 0.0F){
            return;
        }
        float strength = clamp(hurtTicks / 6.0F);
        float jitter = (float) Math.sin(hurtTicks * 2.3F);
        pose.addOffset(Joint.BODY, jitter * 1.2F * strength, 0.0F, jitter * 0.8F * strength);
        pose.addRotation(Joint.CHEST_SHELL_LEFT, 0.0F, 0.0F, -4.0F * strength);
        pose.addRotation(Joint.CHEST_SHELL_RIGHT, 0.0F, 0.0F, 4.0F * strength);
        pose.addRotation(Joint.HEAD_SHELL_LEFT, 0.0F, 0.0F, -5.0F * strength);
        pose.addRotation(Joint.HEAD_SHELL_RIGHT, 0.0F, 0.0F, 5.0F * strength);
        for (int i = 1; i <= 4; i++) {
            float lag = strength * (2.0F + i);
            pose.addRotation(Joint.valueOf("WING_LEFT_FEATHER_" + i), lag, 0.0F, 0.0F);
            pose.addRotation(Joint.valueOf("WING_RIGHT_FEATHER_" + i), lag, 0.0F, 0.0F);
        }
    }

    /**
     * Eighty tick death: ring stops, shell opens, wings fail, core goes out.
     */
    private static void applyDeath(Pose pose, float ticks) {
        if (ticks < 0.0F){
            return;
        }
        float shell = clamp((ticks - 16.0F) / 19.0F);
        float wings = clamp((ticks - 36.0F) / 23.0F);
        float core = clamp((ticks - 60.0F) / 19.0F);
        float stop = clamp(ticks / 15.0F);

        pose.addRotation(Joint.HEAD_SHELL_LEFT, 0.0F, 0.0F, -34.0F * shell);
        pose.addRotation(Joint.HEAD_SHELL_RIGHT, 0.0F, 0.0F, 34.0F * shell);
        pose.addRotation(Joint.HEAD_SHELL_TOP, -40.0F * shell, 0.0F, 0.0F);
        pose.addOffset(Joint.HEAD_SHELL_TOP, 0.0F, -3.0F * shell, 0.0F);
        pose.addRotation(Joint.CHEST_RING_LEFT, 0.0F, 0.0F, -55.0F * shell);
        pose.addRotation(Joint.CHEST_RING_RIGHT, 0.0F, 0.0F, 55.0F * shell);
        pose.addRotation(Joint.CHEST_RING_BOTTOM, 60.0F * shell, 0.0F, 0.0F);
        pose.addRotation(Joint.CHEST_SHELL_LEFT, 0.0F, 0.0F, -22.0F * shell);
        pose.addRotation(Joint.CHEST_SHELL_RIGHT, 0.0F, 0.0F, 22.0F * shell);
        pose.addRotation(Joint.CHEST_SHELL_BACK, 0.0F, 0.0F, 12.0F * shell);
        pose.addRotation(Joint.HALO_ROOT, 0.0F, 0.0F, -30.0F * stop);
        pose.addRotation(Joint.HALO_FRAGMENT_2, 0.0F, 0.0F, 24.0F * shell);
        pose.addRotation(Joint.HALO_FRAGMENT_4, 0.0F, 0.0F, -28.0F * shell);
        pose.addRotation(Joint.BODY, 24.0F * wings, 0.0F, 0.0F);
        pose.addRotation(Joint.WING_LEFT_ROOT, 0.0F, 0.0F, 46.0F * wings);
        pose.addRotation(Joint.WING_RIGHT_ROOT, 0.0F, 0.0F, -46.0F * wings);
        pose.addRotation(Joint.WING_LEFT_OUTER, 0.0F, 0.0F, 30.0F * wings);
        pose.addRotation(Joint.WING_RIGHT_OUTER, 0.0F, 0.0F, -30.0F * wings);
        pose.addRotation(Joint.WING_LEFT_LOWER, 0.0F, 0.0F, 26.0F * wings);
        pose.addRotation(Joint.WING_RIGHT_LOWER, 0.0F, 0.0F, -26.0F * wings);
        for (int i = 1; i <= 4; i++) {
            float drop = -wings * (10.0F + i * 6.0F);
            pose.addRotation(Joint.valueOf("WING_LEFT_FEATHER_" + i), drop, 0.0F, drop * 0.4F);
            pose.addRotation(Joint.valueOf("WING_RIGHT_FEATHER_" + i), drop, 0.0F, -drop * 0.4F);
        }
        pose.addRotation(Joint.ARM_LEFT, 0.0F, 0.0F, 26.0F * wings);
        pose.addRotation(Joint.ARM_RIGHT, 0.0F, 0.0F, -26.0F * wings);
        pose.addOffset(Joint.BODY, 0.0F, 10.0F * core, 0.0F);
        pose.addRotation(Joint.SPINE_TAIL_1, 14.0F * wings, 0.0F, 0.0F);
        pose.addRotation(Joint.SPINE_TAIL_2, 16.0F * wings, 0.0F, 0.0F);
        pose.addRotation(Joint.SPINE_TAIL_3, 18.0F * wings, 0.0F, 0.0F);
        pose.addOffset(Joint.LOWER_FRAGMENT_LEFT, -3.0F * wings, 2.0F * wings, 0.0F);
        pose.addOffset(Joint.LOWER_FRAGMENT_RIGHT, 3.0F * wings, 2.0F * wings, 0.0F);
    }

    /**
     * Composes every joint matrix: M(joint) = M(parent) * T(local) * Rz * Ry * Rx.
     */
    public static void solve(Pose pose) {
        for (Joint joint : Joint.values()) {
            int index = joint.index() * 16;
            identity(pose.matrices, index);
            // The helpers post-multiply, so rotations come first and the pivot translation
            // last: a joint's own rotation must never move its own pivot.
            rotateX(pose.matrices, index, pose.rotX(joint));
            rotateY(pose.matrices, index, pose.rotY(joint));
            rotateZ(pose.matrices, index, pose.rotZ(joint));
            translate(pose.matrices, index,
                      joint.localX() + pose.offX(joint),
                      joint.localY() + pose.offY(joint),
                      joint.localZ() + pose.offZ(joint));
            Joint parent = joint.parent();
            if (parent != null){
                multiply(pose.matrices, index, parent.index() * 16, index);
            }
        }
    }

    private static void identity(float[] target, int offset) {
        java.util.Arrays.fill(target, offset, offset + 16, 0.0F);
        target[offset] = 1.0F;
        target[offset + 5] = 1.0F;
        target[offset + 10] = 1.0F;
        target[offset + 15] = 1.0F;
    }

    // Column-major 4x4 matrices: element (row r, column c) lives at index c * 4 + r, so the
    // translation sits in indices 12..14 and transform() applies M * p exactly like OpenGL.

    /**
     * M = T * M.
     */
    private static void translate(float[] matrix, int offset, float x, float y, float z) {
        matrix[offset + 12] += x;
        matrix[offset + 13] += y;
        matrix[offset + 14] += z;
    }

    /**
     * M = Rx * M.
     */
    private static void rotateX(float[] matrix, int offset, float degrees) {
        if (degrees == 0.0F){
            return;
        }
        float radians = degrees * DEG_TO_RAD;
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        for (int column = 0; column < 4; column++) {
            float y = matrix[offset + column * 4 + 1];
            float z = matrix[offset + column * 4 + 2];
            matrix[offset + column * 4 + 1] = cos * y - sin * z;
            matrix[offset + column * 4 + 2] = sin * y + cos * z;
        }
    }

    /**
     * M = Ry * M.
     */
    private static void rotateY(float[] matrix, int offset, float degrees) {
        if (degrees == 0.0F){
            return;
        }
        float radians = degrees * DEG_TO_RAD;
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        for (int column = 0; column < 4; column++) {
            float x = matrix[offset + column * 4];
            float z = matrix[offset + column * 4 + 2];
            matrix[offset + column * 4] = cos * x + sin * z;
            matrix[offset + column * 4 + 2] = -sin * x + cos * z;
        }
    }

    /**
     * M = Rz * M.
     */
    private static void rotateZ(float[] matrix, int offset, float degrees) {
        if (degrees == 0.0F){
            return;
        }
        float radians = degrees * DEG_TO_RAD;
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        for (int column = 0; column < 4; column++) {
            float x = matrix[offset + column * 4];
            float y = matrix[offset + column * 4 + 1];
            matrix[offset + column * 4] = cos * x - sin * y;
            matrix[offset + column * 4 + 1] = sin * x + cos * y;
        }
    }

    /**
     * target = left * right, where all three are column-major 4x4 blocks.
     */
    private static void multiply(float[] matrix, int targetOffset, int leftOffset, int rightOffset) {
        float[] result = new float[16];
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0.0F;
                for (int k = 0; k < 4; k++) {
                    sum += matrix[leftOffset + k * 4 + row] * matrix[rightOffset + column * 4 + k];
                }
                result[column * 4 + row] = sum;
            }
        }
        System.arraycopy(result, 0, matrix, targetOffset, 16);
    }

    /**
     * Transforms a point expressed in the joint's own frame into authoring space.
     */
    public static V3 transform(Pose pose, Joint joint, float x, float y, float z) {
        int offset = joint.index() * 16;
        float[] m = pose.matrices;
        return new V3(
                m[offset] * x + m[offset + 4] * y + m[offset + 8] * z + m[offset + 12],
                m[offset + 1] * x + m[offset + 5] * y + m[offset + 9] * z + m[offset + 13],
                m[offset + 2] * x + m[offset + 6] * y + m[offset + 10] * z + m[offset + 14]);
    }

    public static V3 jointOrigin(Pose pose, Joint joint) {
        return transform(pose, joint, 0.0F, 0.0F, 0.0F);
    }

    /**
     * Authoring space to entity-local blocks (x left, y up, z forward).
     */
    public static V3 toEntityLocal(V3 modelPoint) {
        return new V3(modelPoint.x / VicissitudeRigData.UNITS_PER_BLOCK,
                      (VicissitudeRigData.MODEL_ORIGIN_Y - modelPoint.y) / VicissitudeRigData.UNITS_PER_BLOCK,
                      -modelPoint.z / VicissitudeRigData.UNITS_PER_BLOCK);
    }

    /**
     * Entity-local blocks to world space for the supplied entity position and yaw.
     */
    public static V3 toWorld(double entityX, double entityY, double entityZ, float yawDegrees, V3 local) {
        float yaw = yawDegrees * DEG_TO_RAD;
        float cos = (float) Math.cos(yaw);
        float sin = (float) Math.sin(yaw);
        // local.yRot(-yaw): x' = x cos - z sin, z' = x sin + z cos
        return new V3(
                (float) entityX + local.x * cos - local.z * sin,
                (float) entityY + local.y,
                (float) entityZ + local.x * sin + local.z * cos);
    }

    public static V3 worldPoint(
            Pose pose, Joint joint, float x, float y, float z,
            double entityX, double entityY, double entityZ, float yawDegrees) {
        return toWorld(entityX, entityY, entityZ, yawDegrees, toEntityLocal(transform(pose, joint, x, y, z)));
    }

    /**
     * Coarse, pose-following hit and blocking volumes: body, head and three wing segments per
     * side. The chest cavity has no box of its own, and range effects use the highest multiplier
     * they overlap without stacking.
     */
    public static List<SegmentVolume> segmentVolumes(
            Pose pose, double entityX, double entityY,
            double entityZ, float yawDegrees) {
        List<SegmentVolume> volumes = new ArrayList<>(8);
        volumes.add(new SegmentVolume(Segment.BODY, volumeBox(pose, Joint.TORSO,
                                                              -9.0F, -3.0F, -6.0F, 9.0F, 14.0F, 7.0F, 0.0F,
                                                              entityX, entityY, entityZ, yawDegrees)));
        volumes.add(new SegmentVolume(Segment.HEAD, volumeBox(pose, Joint.HEAD_ROOT,
                                                              -6.0F, -14.0F, -6.0F, 6.0F, 2.0F, 7.0F, 0.0F,
                                                              entityX, entityY, entityZ, yawDegrees)));
        // Blender exports one bound per articulated wing mesh group, including each feather.
        for (var bounds : pose.bareWings ? VicissitudeMeshGeometry.BONE_WINGS : VicissitudeMeshGeometry.WINGS) {
            boolean left = bounds.joint().name().startsWith("WING_LEFT_");
            boolean root = bounds.joint() == Joint.WING_LEFT_UPPER
                           || bounds.joint() == Joint.WING_RIGHT_UPPER;
            Segment segment = left ? (root ? Segment.WING_LEFT_ROOT : Segment.WING_LEFT_OUTER)
                                   : (root ? Segment.WING_RIGHT_ROOT : Segment.WING_RIGHT_OUTER);
            double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
            double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
            // Transform all eight corners: opposite corners alone lose rotated extents.
            for (int corner = 0; corner < 8; corner++) {
                V3 point = worldPoint(pose, bounds.joint(),
                                      (corner & 1) == 0 ? bounds.minX() : bounds.maxX(),
                                      (corner & 2) == 0 ? bounds.minY() : bounds.maxY(),
                                      (corner & 4) == 0 ? bounds.minZ() : bounds.maxZ(),
                                      entityX, entityY, entityZ, yawDegrees);
                minX = Math.min(minX, point.x);
                minY = Math.min(minY, point.y);
                minZ = Math.min(minZ, point.z);
                maxX = Math.max(maxX, point.x);
                maxY = Math.max(maxY, point.y);
                maxZ = Math.max(maxZ, point.z);
            }
            volumes.add(new SegmentVolume(segment, new Box(minX, minY, minZ, maxX, maxY, maxZ)));
        }
        return volumes;
    }

    /**
     * Hit record for one segment.
     */
    public record SegmentVolume(Segment segment, Box box) {
    }

    private static Box volumeBox(
            Pose pose, Joint joint, float x1, float y1, float z1,
            float x2, float y2, float z2, float radiusModelUnits,
            double entityX, double entityY, double entityZ,
            float yawDegrees) {
        V3 a = worldPoint(pose, joint, x1, y1, z1, entityX, entityY, entityZ, yawDegrees);
        V3 b = worldPoint(pose, joint, x2, y2, z2, entityX, entityY, entityZ, yawDegrees);
        Box box = Box.of(a.x, a.y, a.z, b.x, b.y, b.z);
        return radiusModelUnits <= 0.0F
               ? box : box.inflate(radiusModelUnits / VicissitudeRigData.UNITS_PER_BLOCK);
    }

    private static Box wingCapsule(
            Pose pose, Joint from, Joint to, float radiusModelUnits,
            double entityX, double entityY, double entityZ,
            float yawDegrees) {
        V3 a = toWorld(entityX, entityY, entityZ, yawDegrees, toEntityLocal(jointOrigin(pose, from)));
        V3 b = toWorld(entityX, entityY, entityZ, yawDegrees, toEntityLocal(jointOrigin(pose, to)));
        return Box.of(a.x, a.y, a.z, b.x, b.y, b.z)
                  .inflate(radiusModelUnits / VicissitudeRigData.UNITS_PER_BLOCK);
    }

    /**
     * Range attacks take the highest multiplier they overlap; single hits take the nearest.
     */
    public static Segment highestMultiplierSegment(List<SegmentVolume> volumes, Box area) {
        Segment best = Segment.BODY;
        float bestMultiplier = -1.0F;
        for (SegmentVolume volume : volumes) {
            if (volume.box().intersects(area) && volume.segment().multiplier() > bestMultiplier){
                bestMultiplier = volume.segment().multiplier();
                best = volume.segment();
            }
        }
        return best;
    }

    public static Segment nearestSegment(List<SegmentVolume> volumes, double x, double y, double z) {
        Segment best = Segment.BODY;
        double bestDistance = Double.MAX_VALUE;
        for (SegmentVolume volume : volumes) {
            Box box = volume.box();
            double dx = Math.max(0.0D, Math.max(box.minX() - x, x - box.maxX()));
            double dy = Math.max(0.0D, Math.max(box.minY() - y, y - box.maxY()));
            double dz = Math.max(0.0D, Math.max(box.minZ() - z, z - box.maxZ()));
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance){
                bestDistance = distance;
                best = volume.segment();
            }
        }
        return best;
    }

    /**
     * Highest multiplier among the given body sample points (used by area damage).
     */
    public static float multiplierAt(List<SegmentVolume> volumes, Box area) {
        return highestMultiplierSegment(volumes, area).multiplier();
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float lerp(float from, float to, float delta) {
        return from + (to - from) * clamp(delta);
    }

    private static float smooth(float value) {
        float clamped = clamp(value);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    /** Stateless action channels; follow delays are measured in ticks, never rendered frames. */
    public record ActionSample(float wind, float swing) { }

    public static ActionSample sampleAction(Action action, float ticks, float delay) {
        if (action == null || action == Action.NONE || ticks < 0.0F) {
            return new ActionSample(0.0F, 0.0F);
        }
        float t = Math.max(0.0F, ticks - delay);
        float total = Math.max(1.0F, action.duration());
        return new ActionSample(ramp(t, action.releaseTick(), total),
                strike(t, action.releaseTick(), total));
    }

    /**
     * Ramps to 1 at {@code hit} and stays there until {@code total}.
     */
    private static float ramp(float t, float hit, float total) {
        if (t <= hit){
            return smooth(hit <= 0.0F ? 1.0F : t / hit);
        }
        return 1.0F;
    }

    /**
     * 0 -> 1 at {@code hit} -> 0 at {@code total}.
     */
    private static float bell(float t, float hit, float total) {
        if (t <= hit){
            return smooth(hit <= 0.0F ? 1.0F : t / hit);
        }
        float span = Math.max(1.0F, total - hit);
        return 1.0F - smooth((t - hit) / span);
    }

    /**
     * Strike curve used by every attack: anticipation eases up to exactly 1.0 on the release
     * frame, then snaps through with a short overshoot, holds the impact for a few ticks and
     * follows through. The overshoot and hold are what give the swings weight instead of the
     * weightless in/out bell.
     */
    private static float strike(float t, float hit, float total) {
        if (t <= hit){
            return smooth(hit <= 0.0F ? 1.0F : t / hit);
        }
        float after = t - hit;
        if (after < 2.0F){
            return 1.0F + 0.28F * (after / 2.0F);
        }
        if (after < 6.0F){
            return 1.28F - 0.18F * ((after - 2.0F) / 4.0F);
        }
        float span = Math.max(1.0F, total - hit - 6.0F);
        return 1.10F * (1.0F - smooth((after - 6.0F) / span));
    }

    /**
     * Combat actions with their authored timings (ticks).
     */
    public enum Action {
        NONE(0, 0, false),
        WING_RANGED(20, 12, true),
        WING_BARRAGE(56, 20, true),
        CAST_FROM_CHEST(50, 30, true),
        CAST_FROM_HALO(60, 40, true),
        SLASH_HORIZONTAL(20, 8, false),
        SLASH_VERTICAL(30, 14, false),
        HEAVY_ATTACK(50, 24, false),
        DASH(50, 20, false),
        SCYTHE_THROW(28, 16, false),
        SCYTHE_RECOVER(10, 0, false),
        RANGED_FALLBACK(28, 16, false);

        private final int duration;
        private final int releaseTick;
        private final boolean phaseOne;

        Action(int duration, int releaseTick, boolean phaseOne) {
            this.duration = duration;
            this.releaseTick = releaseTick;
            this.phaseOne = phaseOne;
        }

        public int duration() {
            return duration;
        }

        public int releaseTick() {
            return releaseTick;
        }

        /** The last part of the tell commits to a direction, leaving time to sidestep. */
        public int aimLockTick() {
            return switch (this) {
                case SLASH_HORIZONTAL -> 4;
                case SLASH_VERTICAL -> 8;
                case HEAVY_ATTACK -> 14;
                case DASH -> 14;
                case WING_RANGED -> 8;
                case WING_BARRAGE -> 14;
                case SCYTHE_THROW, RANGED_FALLBACK -> 10;
                case CAST_FROM_CHEST, CAST_FROM_HALO -> 10;
                default -> 0;
            };
        }

        public boolean isPhaseOneSkill() {
            return phaseOne;
        }

        public boolean isMelee() {
            return this == SLASH_HORIZONTAL || this == SLASH_VERTICAL || this == HEAVY_ATTACK;
        }

        public static Action byId(int id) {
            Action[] values = values();
            return id >= 0 && id < values.length ? values[id] : NONE;
        }
    }
}
