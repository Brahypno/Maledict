package org.brahypno.maledict.common.curio.eggshell;

/**
 * One wearer's gradient window. Deliberately free of Minecraft, Curios and item types so the
 * timing rules can be unit tested and reused if a second item ever needs them.
 *
 * <p>An opening batch is allowed at full damage. Once one of its hits actually lands, the window
 * starts and every later tick is scaled by {@code elapsed / duration}; the window never refreshes
 * from inside itself. Hits that share a tick are one batch and only the largest raw amount is
 * counted.
 */
final class EggshellWindow {
    private static final long NONE = Long.MIN_VALUE;

    private long windowStart = NONE;
    private long windowUntil = NONE;
    private long batchTick = NONE;
    private float batchMax;
    private float batchFactor = 1.0F;
    private boolean openingBatch;
    private boolean openedThisBatch;

    /**
     * Applies the window to one incoming amount for {@code now}. The returned amount is what may
     * remain after this hit; zero means the hit contributes nothing beyond the batch's maximum.
     */
    float apply(long now, float amount, int durationTicks) {
        if (!Float.isFinite(amount) || amount <= 0.0F) {
            return amount;
        }
        if (batchTick != now) {
            batchTick = now;
            batchMax = 0.0F;
            openingBatch = now >= windowUntil;
            batchFactor = openingBatch ? 1.0F : factor(now, durationTicks);
            openedThisBatch = false;
        }
        float delta = amount - batchMax;
        batchMax = Math.max(batchMax, amount);
        if (!Float.isFinite(delta) || delta <= 0.0F) {
            return 0.0F;
        }
        return delta * batchFactor;
    }

    /**
     * Called after a hit actually removes health. Only the first landed hit of an opening batch
     * starts the window; window hits never refresh it.
     */
    void land(long now, int durationTicks) {
        if (batchTick != now || !openingBatch || openedThisBatch) {
            return;
        }
        openedThisBatch = true;
        windowStart = now;
        windowUntil = now + Math.max(1, durationTicks);
    }

    /** True when a new hit would be a full opening hit rather than a window hit. */
    boolean isOpen(long now) {
        return now >= windowUntil;
    }

    long windowStart() {
        return windowStart;
    }

    long windowUntil() {
        return windowUntil;
    }

    private float factor(long now, int durationTicks) {
        if (durationTicks <= 0 || now <= windowStart) {
            return 0.0F;
        }
        long elapsed = now - windowStart;
        if (elapsed >= durationTicks) {
            return 1.0F;
        }
        return (float) ((double) elapsed / (double) durationTicks);
    }
}
