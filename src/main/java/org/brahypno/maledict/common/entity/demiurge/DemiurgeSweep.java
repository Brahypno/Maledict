package org.brahypno.maledict.common.entity.demiurge;

import net.minecraft.world.phys.AABB;

/**
 * Authored lateral sweep profile. The gesture itself only braces the head; the actual body motion
 * is added to the connected body-chain guide, so it survives the chain solve and is shared by the
 * render pose, the hitboxes and the server-side damage test.
 *
 * <p>Times follow the confirmed timeline: 0-15 ticks coil to the opposite side, 16-27 sweep
 * through the target side, 28-49 retract. The profile is kept separate from the attack enum so the
 * same values drive the client and server without sending per-tick offsets.</p>
 */
public final class DemiurgeSweep {
    public static final int WINDUP_END = 16;
    public static final int ACTIVE_END = 28;
    public static final int RETRACT_END = 50;
    /**
     * Fraction of the full lateral travel coiled away before the strike.
     */
    public static final float WINDUP = -.38F;
    /**
     * Starting lateral travel of the last retained joint, in blocks.
     */
    public static final float MAX_AMPLITUDE = 4.0F;
    /**
     * A shortened body still sweeps, but never below this fraction of the full range.
     */
    public static final float MIN_LENGTH_SCALE = .35F;
    /**
     * The connected forebody and raised head stay steady while the mid/rear body swings.
     */
    public static final int STABLE_GUIDE_POINTS = 4;

    static AABB contactBounds(AABB previous, AABB current) {
        return previous.minmax(current).inflate(.35, .2, .35);
    }

    public record State(int side, float age, float amplitude, float limit) {
        public static State none() {
            return new State(0, -1, 0, 1);
        }

        public boolean active() {
            return side != 0 && age >= 0;
        }

        /**
         * Signed lateral offset at the tail end of the guide, before the per-joint weight.
         */
        public float tailOffset() {
            float swing = fraction(age);
            if (age >= ACTIVE_END)
                swing *= Math.max(0, limit);
            return side * Math.min(swing, limit) * amplitude;
        }
    }

    public static float fraction(float age) {
        if (age < 0 || age >= RETRACT_END)
            return 0;
        if (age < WINDUP_END)
            return WINDUP * smooth(age / WINDUP_END);
        if (age < ACTIVE_END)
            return WINDUP + (1 - WINDUP) * smooth((age - WINDUP_END) / (ACTIVE_END - WINDUP_END));
        return 1 - smooth((age - ACTIVE_END) / (float) (RETRACT_END - 1 - ACTIVE_END));
    }

    public static float amplitude(int bodyCut) {
        float full = DemiurgeRig.INSTANCE.bodyEndDistance(0);
        if (full <= .0001F)
            return MAX_AMPLITUDE;
        float scale = Math.max(MIN_LENGTH_SCALE, Math.min(1, DemiurgeRig.INSTANCE.bodyEndDistance(bodyCut) / full));
        return MAX_AMPLITUDE * scale;
    }

    /**
     * Weight for one guide point, counted from the head end. The first four points are the
     * connected forebody; the weight then grows smoothly to the tail.
     */
    public static float guideWeight(int guide, int retainedJoints) {
        if (retainedJoints <= STABLE_GUIDE_POINTS)
            return 0;
        float x = Math.max(0, Math.min(1, (guide - STABLE_GUIDE_POINTS)
                                          / (float) (retainedJoints - STABLE_GUIDE_POINTS)));
        return x * x * (3 - 2 * x);
    }

    private static float smooth(float x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    private DemiurgeSweep() {}
}
