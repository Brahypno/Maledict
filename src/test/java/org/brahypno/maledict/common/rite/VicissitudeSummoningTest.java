package org.brahypno.maledict.common.rite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VicissitudeSummoningTest {
    @Test
    void threePolesDisappearTopDownBeforeTheBaseWithoutRepeatingLayers() {
        int[] ticks = {0, 9, 10, 24, 25, 39, 40, 54, 55, 130, 160};
        int[] expected = {0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4};
        for (int i = 0; i < ticks.length; i++) {
            assertEquals(expected[i], VicissitudeSummoning.totemLayersConsumed(ticks[i], 4));
        }
    }

    @Test
    void everyRecipeConsumesItsBaseBeforeArrivalEvenWhenSummoningTicksAreSkipped() {
        for (int layers = 4; layers <= 6; layers++) {
            int previous = 0;
            for (int tick : new int[]{0, 12, 31, 67, 90, 130, 160, 10000}) {
                int consumed = VicissitudeSummoning.totemLayersConsumed(tick, layers);
                assertTrue(consumed >= previous && consumed <= layers);
                previous = consumed;
            }
            assertEquals(layers, VicissitudeSummoning.totemLayersConsumed(90, layers));
        }
    }

    @Test
    void wingsFinishOpeningAndHandOverToTheUnfoldedIdlePose() {
        assertEquals(1, VicissitudeSummoning.wingFold(85));
        float previous = 1;
        for (float tick = 85; tick <= VicissitudeSummoning.ARRIVAL_TICK; tick += .25F) {
            float fold = VicissitudeSummoning.wingFold(tick);
            assertTrue(fold >= 0 && fold <= previous);
            previous = fold;
        }
        for (int tick : new int[]{VicissitudeSummoning.ARRIVAL_TICK, 159, 160, 10000}) {
            assertEquals(0, VicissitudeSummoning.wingFold(tick));
        }
    }

    @Test
    void bossWaitsForTheOpeningAndArrivesBeforeTheRiftCloses() {
        for (int tick = 0; tick <= VicissitudeSummoning.OPEN_TICKS; tick++) {
            assertEquals(0, VicissitudeSummoning.descent(tick));
        }
        assertEquals(1, VicissitudeSummoning.aperture(VicissitudeSummoning.OPEN_TICKS));
        for (int tick = VicissitudeSummoning.OPEN_TICKS; tick <= VicissitudeSummoning.ARRIVAL_TICK; tick++) {
            assertEquals(1, VicissitudeSummoning.aperture(tick));
        }
        assertEquals(1, VicissitudeSummoning.descent(VicissitudeSummoning.ARRIVAL_TICK));
    }

    @Test
    void descentNeverReversesOrOvershootsEvenAcrossSkippedTicks() {
        float previous = 0;
        for (float tick = -20; tick <= 200; tick += 0.37F) {
            float descent = VicissitudeSummoning.descent(tick);
            assertTrue(descent >= previous && descent >= 0 && descent <= 1);
            previous = descent;
        }
    }

    @Test
    void colorAlwaysReturnsAfterTheSummoningIncludingLateTracking() {
        assertEquals(0, VicissitudeSummoning.grayscale(0));
        assertEquals(1, VicissitudeSummoning.grayscale(VicissitudeSummoning.OPEN_TICKS));
        assertTrue(VicissitudeSummoning.grayscale(145) > 0);
        assertTrue(VicissitudeSummoning.grayscale(145) < 1);
        for (int tick : new int[]{VicissitudeSummoning.DURATION, 180, 10000}) {
            assertEquals(0, VicissitudeSummoning.grayscale(tick));
            assertEquals(0, VicissitudeSummoning.aperture(tick));
            assertEquals(1, VicissitudeSummoning.descent(tick));
        }
    }
}
