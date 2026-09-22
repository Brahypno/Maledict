package org.brahypno.maledict.rig;

/** Deterministic, synchronized transition clock; no per-render particle spawning. */
public final class VicissitudeFeatherShed {
    public static final float FALL_TICKS = 24.0F;
    private VicissitudeFeatherShed() {}
    public static float elapsed(float transitionTick, float delay) {
        return Math.max(0, transitionTick - delay);
    }
    public static float scale(float elapsed) {
        return Math.max(0, Math.min(1, (FALL_TICKS - elapsed) / 7.0F));
    }
    public static float drop(float elapsed) {
        return .028F * elapsed * elapsed;
    }
}
