package org.brahypno.maledict.common.entity;

/** Ground destinations use separate takeoff and landing distances to avoid repeated mode switches. */
public final class RavenGroundTravel {
    public enum Action { HOP, TAKE_OFF, FLY, LAND }

    public static Action next(boolean flying, double horizontalDistanceSquared) {
        if (flying) {
            return horizontalDistanceSquared <= 4.0D ? Action.LAND : Action.FLY;
        }
        return horizontalDistanceSquared > 16.0D ? Action.TAKE_OFF : Action.HOP;
    }

    private RavenGroundTravel() {
    }
}
