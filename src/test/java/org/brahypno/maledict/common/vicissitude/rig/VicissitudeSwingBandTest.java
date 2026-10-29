package org.brahypno.maledict.common.vicissitude.rig;

import org.brahypno.maledict.common.vicissitude.rig.VicissitudeRig.SwingBand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the melee band's geometry. The first version of it measured "forward" along the mirror of the
 * boss' facing - {@code (+sin, cos)} instead of the {@code (-sin, cos)} that {@link VicissitudeRig#toWorld}
 * and the movement code use - so a target standing right in front of the blade logged
 * {@code forward=-1.77} and was reported as a miss. These tests exist so that sign cannot flip again.
 */
class VicissitudeSwingBandTest {
    private static final double BODY_WIDTH = 1.6D;
    private static final double HALF_WIDTH = VicissitudeRig.SWING_HALF_WIDTH;

    /** Past the reach and outside the half width are both outside. */
    @Test
    void pastTheReachAndOffToTheSideAreOutside() {
        SwingBand band = SwingBand.of(0.0F, BODY_WIDTH, 4.4D, HALF_WIDTH);
        assertFalse(band.contains(4.5D, 0.0D), "past the reach");
        assertFalse(band.contains(1.0D, 2.0D), "outside the half width");
        assertTrue(band.contains(4.4D, HALF_WIDTH), "the far corner is inside");
        assertTrue(band.contains(3.0D, 1.0D), "the middle of the band is inside");
    }
}
