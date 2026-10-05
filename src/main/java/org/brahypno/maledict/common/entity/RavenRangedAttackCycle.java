package org.brahypno.maledict.common.entity;

/** Continuous hovering: ten ticks to aim, then sixty ticks between feather releases. */
public final class RavenRangedAttackCycle {
    private static final int WINDUP_TICKS = 10;
    private static final int SHOT_INTERVAL = 60;
    private int windup;
    private long nextChargeAt;

    public static boolean canHover(double distanceSquared, boolean visible) {
        return visible && distanceSquared >= 9.0D && distanceSquared <= 144.0D;
    }

    public boolean tick(boolean ready, long gameTime) {
        if (!ready || gameTime < nextChargeAt) {
            cancelCharge();
            return false;
        }
        if (++windup < WINDUP_TICKS) {
            return false;
        }
        cancelCharge();
        nextChargeAt = gameTime + SHOT_INTERVAL - WINDUP_TICKS + 1;
        return true;
    }

    public void cancelCharge() {
        windup = 0;
    }
}
