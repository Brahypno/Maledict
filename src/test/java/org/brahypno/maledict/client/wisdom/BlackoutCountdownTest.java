package org.brahypno.maledict.client.wisdom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BlackoutCountdownTest {

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

}
