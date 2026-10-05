package org.brahypno.maledict.common.entity;

/**
 * Shared altitude rules for the combat hover, the climb after a hit and ordinary travel.
 *
 * <p>Vanilla {@code MeleeAttackGoal} compares {@code (attackerWidth * 2)^2 + targetWidth} against the straight-line
 * distance from the attacker's centre to the target's <em>feet</em> ({@code distanceToSqr(x, y, z)} of the target's
 * position). A one-block-wide raven is therefore in reach of a zombie (0.6 wide) below 1.56 blocks, of a spider or
 * hoglin (1.4 wide) below 2.97 blocks and of a ravager (1.95 wide) below 4.02 blocks. Hovering
 * {@link #COMBAT_CLEARANCE} blocks over the enemy's feet is out of reach for everything that hunts a raven, and it
 * also keeps the three-dimensional hover distance above the three-block break-off, so an enemy standing right
 * underneath no longer pulls the bird back into a melee dive.
 */
public final class RavenFlightAltitude {
    /** Lift over its own ground position where a hover point is picked while the bird is still standing. */
    public static final double GROUND_LIFT = 1.25D;
    /** Climb kept over the enemy's feet while hovering and shooting feathers. */
    public static final double COMBAT_CLEARANCE = 4.0D;
    /** A hit always buys at least this much height, even from an attacker that is already below the bird. */
    public static final double DODGE_LIFT = 1.0D;
    /** Idle flight raises a walking destination by this much plus a spread of [0, {@link #IDLE_LIFT_SPREAD}). */
    public static final int IDLE_LIFT = 4;
    public static final int IDLE_LIFT_SPREAD = 3;
    /** Trusted-player following: cruise this high outside the cruise distance, drop to shoulder height inside it. */
    public static final double FOLLOW_CRUISE = 4.0D;
    public static final double FOLLOW_CLOSE = 1.0D;
    public static final double FOLLOW_CRUISE_DISTANCE_SQR = 64.0D;
    /** Vertical speed while dodging, in blocks per tick. */
    public static final double MIN_CLIMB_SPEED = 0.08D;
    public static final double MAX_CLIMB_SPEED = 0.28D;
    private static final double CLIMB_GAIN = 0.25D;

    /** Hover point Y: never lower than the bird already is, and high enough over the enemy's feet. */
    public static double hoverY(double currentY, boolean onGround, double enemyFeetY) {
        return Math.max(currentY + (onGround ? GROUND_LIFT : 0.0D), enemyFeetY + COMBAT_CLEARANCE);
    }

    /** Dodge ceiling after a hit from the attacker standing at {@code attackerFeetY}. */
    public static double dodgeY(double currentY, double attackerFeetY) {
        return Math.max(currentY + DODGE_LIFT, attackerFeetY + COMBAT_CLEARANCE);
    }

    /** Climb speed for this tick: proportional to the missing height, never below or above the limits. */
    public static double climbSpeed(double currentY, double targetY) {
        return Math.min(MAX_CLIMB_SPEED, Math.max(MIN_CLIMB_SPEED, (targetY - currentY) * CLIMB_GAIN));
    }

    /** Lift added to a walking destination to make an idle flight leg. */
    public static double idleFlightLift(int spread) {
        return IDLE_LIFT + spread;
    }

    /** Height of the flight path while following a trusted player at the given horizontal distance. */
    public static double followY(double playerY, double horizontalDistanceSqr) {
        return playerY + (horizontalDistanceSqr > FOLLOW_CRUISE_DISTANCE_SQR ? FOLLOW_CRUISE : FOLLOW_CLOSE);
    }

    private RavenFlightAltitude() {
    }
}
