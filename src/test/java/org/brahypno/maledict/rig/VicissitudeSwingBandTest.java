package org.brahypno.maledict.rig;

import org.brahypno.maledict.rig.VicissitudeRig.Action;
import org.brahypno.maledict.rig.VicissitudeRig.SwingBand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    /** A local forward point has to land where the rig's own world transform puts it. */
    @Test
    void facingMatchesTheRigWorldTransform() {
        for (float yaw : new float[]{0.0F, 45.0F, 90.0F, -64.77F, 180.0F, -135.0F}) {
            VicissitudeRig.V3 forward = VicissitudeRig.toWorld(0.0D, 0.0D, 0.0D, yaw,
                                                               new VicissitudeRig.V3(0.0F, 0.0F, 1.0F));
            assertEquals(forward.x(), VicissitudeRig.facingX(yaw), 1.0E-4D, "facing x at yaw " + yaw);
            assertEquals(forward.z(), VicissitudeRig.facingZ(yaw), 1.0E-4D, "facing z at yaw " + yaw);
        }
    }

    /**
     * Replays the play-test log: boss at (-45.13, 49.98) facing yaw -64.77, target standing on the
     * heavy attack's reach straight ahead. It has to read as a hit.
     */
    @Test
    void aTargetInFrontOfTheBladeIsInTheBand() {
        float yaw = -64.77F;
        double bossX = -45.13D;
        double bossZ = 49.98D;
        double reach = Action.HEAVY_ATTACK.bladeForwardReach();
        double targetX = bossX + VicissitudeRig.facingX(yaw) * reach;
        double targetZ = bossZ + VicissitudeRig.facingZ(yaw) * reach;
        double forward = SwingBand.forward(yaw, bossX, bossZ, targetX, targetZ);
        double lateral = SwingBand.lateral(yaw, bossX, bossZ, targetX, targetZ);
        // Yaw is a float while the facing maths is double, so the recovered distance carries the
        // float rounding of the angle (about 1e-5 blocks at this reach).
        assertEquals(reach, forward, 1.0E-3D, "a target on the facing reads as fully forward");
        assertEquals(0.0D, lateral, 1.0E-3D, "a target on the facing has no lateral offset");
        assertTrue(SwingBand.of(yaw, BODY_WIDTH, reach, HALF_WIDTH).contains(forward, lateral),
                   "in front of the blade must be inside the band");
    }

    /** A target merely near the blade but off the facing axis stays out. */
    @Test
    void theBandFollowsTheFacingNotTheBladeLine() {
        float yaw = -64.77F;
        double reach = Action.HEAVY_ATTACK.bladeForwardReach();
        SwingBand band = SwingBand.of(yaw, BODY_WIDTH, reach, HALF_WIDTH);
        // Directly behind the boss: the blade's own line passes near it, the band must not.
        assertFalse(band.contains(SwingBand.forward(yaw, 0.0D, 0.0D, -1.0D, 0.0D),
                                  SwingBand.lateral(yaw, 0.0D, 0.0D, -1.0D, 0.0D)),
                    "behind the boss must never be in the band");
    }

    /** The band is one contiguous interval from the inner edge out to the reach. */
    @Test
    void theBandHasNoDeadSpot() {
        double reach = Action.SLASH_HORIZONTAL.bladeForwardReach();
        SwingBand band = SwingBand.of(0.0F, BODY_WIDTH, reach, HALF_WIDTH);
        assertEquals(VicissitudeRig.SWING_MIN_DEPTH, band.minimumBlocks(), 1.0E-9D);
        int inside = 0;
        int total = 0;
        for (double forward = 0.0D; forward <= reach; forward += 0.01D) {
            total++;
            if (band.contains(forward, 0.0D)) {
                inside++;
            }
        }
        // Only the part inside the boss' own body is excluded; everything from the inner edge out
        // to the reach has to connect.
        assertEquals(total - 60, inside, 1, "the band must be one contiguous interval");
        assertTrue(band.contains(0.6D, 0.0D), "the inner edge itself is inside");
        assertTrue(band.contains(reach, 0.0D), "the reach itself is inside");
        assertFalse(band.contains(0.59D, 0.0D), "inside the boss' own body");
    }

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
