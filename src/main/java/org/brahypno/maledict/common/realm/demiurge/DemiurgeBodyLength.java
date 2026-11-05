package org.brahypno.maledict.common.realm.demiurge;

/**
 * Confirmed body-loss model: crossing 75 %, 50 % and 25 % health severs four more trailing sections
 * each time. The loss only ever grows during one encounter, and {@link DemiurgeRig#maxBodyCut()}
 * caps it so at least {@link #MINIMUM_GROUND_LENGTH} blocks of body still lie on the floor.
 */
public final class DemiurgeBodyLength {
    /** Confirmed floor for the remaining ground body, in blocks. */
    public static final float MINIMUM_GROUND_LENGTH = 8.0F;
    /** Descending fractions of maximum health; each crossed threshold costs four sections. */
    public static final float[] THRESHOLDS = {.75F, .50F, .25F};
    public static final int SECTIONS_PER_THRESHOLD = 4;

    public static int cutFor(float healthFraction, int maximumCut) {
        int crossed = 0;
        for (float threshold : THRESHOLDS) {
            if (healthFraction <= threshold) {
                crossed++;
            }
        }
        return Math.max(0, Math.min(maximumCut, crossed * SECTIONS_PER_THRESHOLD));
    }

    private DemiurgeBodyLength() {
    }
}
