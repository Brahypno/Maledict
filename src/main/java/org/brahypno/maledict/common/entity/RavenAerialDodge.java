package org.brahypno.maledict.common.entity;

/**
 * A bounded side/back dodge. Only damage known to come from below adds height; further hits in the same dodge
 * keep its destination and deadline, so a burst cannot send the bird ever higher or hold it in dodge mode.
 */
public final class RavenAerialDodge {
    /** Longest a single dodge lasts, so a ceiling cannot pin the bird in dodge mode. */
    public static final int DURATION_TICKS = 30;
    public static final double DISTANCE = 4.0D;
    private static final double LIFT = 1.25D;
    private static final double SIDE_ANGLE = Math.PI / 6.0D;
    private static final double ARRIVAL_SQR = 0.25D;

    private int remaining;
    private double targetX;
    private double targetY;
    private double targetZ;

    public boolean onHit(double x, double y, double z, double awayX, double awayZ,
                         boolean fromBelow, boolean left) {
        if (active()) {
            return false;
        }
        double length = Math.hypot(awayX, awayZ);
        if (length < 1.0E-6D) {
            awayX = 1.0D;
            awayZ = 0.0D;
            length = 1.0D;
        }
        double angle = left ? SIDE_ANGLE : -SIDE_ANGLE;
        targetX = x + DISTANCE * (awayX * Math.cos(angle) - awayZ * Math.sin(angle)) / length;
        targetZ = z + DISTANCE * (awayX * Math.sin(angle) + awayZ * Math.cos(angle)) / length;
        targetY = y + (fromBelow ? LIFT : 0.0D);
        remaining = DURATION_TICKS;
        return true;
    }

    public static boolean fromBelow(double birdFeetY, double sourceY) {
        return sourceY < birdFeetY - 0.25D;
    }

    public boolean active() {
        return remaining > 0;
    }

    /** Ends on arrival or when the timer runs out; only meaningful while {@link #active()}. */
    public void tick(double x, double y, double z) {
        if (remaining <= 0) {
            return;
        }
        if (reached(x, y, z) || --remaining <= 0) {
            remaining = 0;
        }
    }

    public double targetY() {
        return targetY;
    }

    public double targetX() {
        return targetX;
    }

    public double targetZ() {
        return targetZ;
    }

    public boolean reached(double x, double y, double z) {
        return Math.pow(x - targetX, 2) + Math.pow(y - targetY, 2) + Math.pow(z - targetZ, 2) <= ARRIVAL_SQR;
    }

    public void finish() {
        remaining = 0;
    }
}
