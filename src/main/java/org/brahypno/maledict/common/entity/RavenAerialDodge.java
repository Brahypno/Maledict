package org.brahypno.maledict.common.entity;

/**
 * A hit from a living attacker buys altitude: the bird rises out of melee reach, then the ordinary goals resume with
 * the retaliation target untouched. Environmental damage never starts a dodge, and touching danger still wins.
 */
public final class RavenAerialDodge {
    /** Longest a single dodge lasts, so a ceiling cannot pin the bird in dodge mode. */
    public static final int DURATION_TICKS = 40;
    private static final double ARRIVAL = 0.05D;

    private int remaining;
    private double targetY;

    /** The first hit of a dodge sets the ceiling; a further hit only extends the timer and never lowers it. */
    public void onHit(double currentY, double attackerFeetY) {
        double wanted = RavenFlightAltitude.dodgeY(currentY, attackerFeetY);
        targetY = remaining > 0 ? Math.max(targetY, wanted) : wanted;
        remaining = DURATION_TICKS;
    }

    public boolean active() {
        return remaining > 0;
    }

    /** Ends on arrival or when the timer runs out; only meaningful while {@link #active()}. */
    public void tick(double currentY) {
        if (remaining <= 0) {
            return;
        }
        if (reached(currentY) || --remaining <= 0) {
            remaining = 0;
        }
    }

    public double targetY() {
        return targetY;
    }

    public boolean reached(double currentY) {
        return currentY >= targetY - ARRIVAL;
    }

    public void finish() {
        remaining = 0;
    }
}
