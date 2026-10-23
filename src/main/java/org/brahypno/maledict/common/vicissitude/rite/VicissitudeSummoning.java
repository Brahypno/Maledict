package org.brahypno.maledict.common.vicissitude.rite;

/** Shared timing for the server's descent and the client's portal/post-processing. */
public final class VicissitudeSummoning {
    public static final int OPEN_TICKS = 30;
    public static final int ARRIVAL_TICK = 130;
    public static final int DURATION = 160;
    public static final float RISE = 9.0F;
    public static final float PORTAL_RADIUS = 3.8F;
    /** Covers the 4.17-block rift rim and the folded/unfolded wing envelope (under 4.41). */
    public static final double CLEARANCE_RADIUS = 4.5D;
    /** Wing tips reach above the body's 4.375-block collision box (under 5.19). */
    public static final double CLEARANCE_HEIGHT = 5.25D;
    /** Cloth can hang slightly below the body's feet. */
    public static final double CLEARANCE_BELOW = 0.25D;

    private VicissitudeSummoning() {
    }

    public static float descent(float ticks) {
        return smooth((ticks - OPEN_TICKS) / (ARRIVAL_TICK - OPEN_TICKS));
    }

    public static float aperture(float ticks) {
        return smooth(ticks / OPEN_TICKS)
                * (1.0F - smooth((ticks - ARRIVAL_TICK) / (DURATION - ARRIVAL_TICK)));
    }

    public static float grayscale(float ticks) {
        return aperture(ticks);
    }

    public static float wingFold(float ticks) {
        return 1.0F - smooth((ticks - 85.0F) / (ARRIVAL_TICK - 85.0F));
    }

    /** The top layer disappears at tick 10, then one layer every 15 ticks, base last. */
    public static int totemLayersConsumed(float ticks, int layerCount) {
        if (ticks < 10) return 0;
        return Math.min(layerCount, 1 + (int) ((ticks - 10) / 15));
    }

    private static float smooth(float value) {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }
}
