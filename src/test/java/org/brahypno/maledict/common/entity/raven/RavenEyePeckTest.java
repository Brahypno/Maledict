package org.brahypno.maledict.common.entity.raven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 渡鸦取眼的判定：八格起手、十六格放弃、鸦眼到人眼 1.6 格下嘴，以及四项否决
 * （成年、仪式印记、信任关系、能不能挨打）。
 */
class RavenEyePeckTest {
    private static final double EIGHT_BLOCKS = RavenEyePeck.SIGHT_RADIUS * RavenEyePeck.SIGHT_RADIUS;
    private static final double SIXTEEN_BLOCKS = RavenEyePeck.ABANDON_RADIUS * RavenEyePeck.ABANDON_RADIUS;
    private static final double EYE_REACH = RavenEyePeck.REACH * RavenEyePeck.REACH;

    @Test
    void onlyAnOfferedUntrustedPlayerWithinEightBlocksIsPeckedAndTheEyeIsTheOnlyReach() {
        assertTrue(RavenEyePeck.maySeek(true, true, false, true, EIGHT_BLOCKS, true));
        assertFalse(RavenEyePeck.maySeek(true, true, false, true, EIGHT_BLOCKS + 0.1D, true));
        assertFalse(RavenEyePeck.maySeek(true, true, false, true, 1.0D, false));
        assertFalse(RavenEyePeck.maySeek(false, true, false, true, 1.0D, true));
        assertFalse(RavenEyePeck.maySeek(true, false, false, true, 1.0D, true));
        assertFalse(RavenEyePeck.maySeek(true, true, true, true, 1.0D, true));
        assertFalse(RavenEyePeck.maySeek(true, true, false, false, 1.0D, true));

        assertTrue(RavenEyePeck.mayContinue(true, true, false, true, 1.0D));
        assertTrue(RavenEyePeck.mayContinue(true, true, false, true, SIXTEEN_BLOCKS));
        assertFalse(RavenEyePeck.mayContinue(true, true, false, true, SIXTEEN_BLOCKS + 0.1D));

        assertTrue(RavenEyePeck.inReach(EYE_REACH));
        assertFalse(RavenEyePeck.inReach(EYE_REACH + 0.01D));
    }
}
