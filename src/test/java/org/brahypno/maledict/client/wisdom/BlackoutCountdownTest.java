package org.brahypno.maledict.client.wisdom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BlackoutCountdownTest {
    /** 六十 tick 总时长不变：第 59 tick 黑幕仍可见，第 60 tick 完全结束。 */
    @Test
    void blackoutEndsOnTheSixtiethTick() {
        BlackoutCountdown countdown = new BlackoutCountdown();
        assertFalse(countdown.isActive());
        countdown.start(60);
        assertTrue(countdown.isActive());
        for (int tick = 1; tick < 60; tick++) {
            countdown.tick();
            assertTrue(countdown.isActive(), "Blackout must remain active at tick " + tick);
        }
        countdown.tick();
        assertFalse(countdown.isActive());
        countdown.tick();
        assertFalse(countdown.isActive());
    }

    @Test
    void clearingStopsAnActiveBlackout() {
        BlackoutCountdown countdown = new BlackoutCountdown();
        countdown.start(60);
        countdown.tick();
        countdown.start(0);
        assertFalse(countdown.isActive());
    }

    /** 已确认：前两秒全黑，最后一秒淡出；中点半透明，结束完全透明。 */
    @Test
    void holdsBlackForTwoSecondsThenFadesDuringTheLastSecond() {
        BlackoutCountdown countdown = new BlackoutCountdown();
        countdown.start(60);
        for (int tick = 0; tick <= 40; tick++) {
            assertEquals(1.0F, countdown.opacity(0.0F), 0.0001F);
            if (tick < 40) {
                countdown.tick();
            }
        }
        for (int tick = 41; tick <= 60; tick++) {
            countdown.tick();
            assertEquals((60 - tick) / 20.0F, countdown.opacity(0.0F), 0.0001F);
        }
        assertFalse(countdown.isActive());
        assertEquals(0.0F, countdown.opacity(0.5F), 0.0001F);
    }

    @Test
    void interpolatesOpacityBetweenTicksForASmoothFade() {
        BlackoutCountdown countdown = new BlackoutCountdown();
        countdown.start(60);
        for (int tick = 0; tick < 50; tick++) {
            countdown.tick();
        }
        assertEquals(0.5F, countdown.opacity(0.0F), 0.0001F);
        assertEquals(0.475F, countdown.opacity(0.5F), 0.0001F);
        assertEquals(0.45F, countdown.opacity(1.0F), 0.0001F);
    }
}
