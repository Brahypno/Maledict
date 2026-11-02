package org.brahypno.maledict.common.realm.demiurge;

/** Confirmed transition: finish awakening, lower to a raised ground stance, then patrol. */
public final class DemiurgeSlither {
    public static final int TRANSITION_TICKS = 40;
    public static final int CYCLE_TICKS = 60;
    public static final float BLOCKS_PER_CYCLE = 8;
    public static final float HEIGHT = 6.4F;

    public static float groundBlend(float awakeningAge) {
        float x = Math.max(0, Math.min(1, (awakeningAge - DemiurgeAwakening.DURATION) / TRANSITION_TICKS));
        return x * x * (3 - 2 * x);
    }

    public static boolean canPatrol(float age, boolean alive, boolean hasEnemy) {
        return alive && !hasEnemy && age >= DemiurgeAwakening.DURATION + TRANSITION_TICKS;
    }

    public static float height(float age) {
        float standing = DemiurgeAwakening.height(age);
        return standing + (HEIGHT - standing) * groundBlend(age);
    }

    private DemiurgeSlither() {}
}
