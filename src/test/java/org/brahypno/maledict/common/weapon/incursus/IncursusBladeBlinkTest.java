package org.brahypno.maledict.common.weapon.incursus;

import org.brahypno.maledict.common.weapon.incursus.IncursusBladeBlink.Gaze;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 神侵恶刃在物品栏里的眨眼：睁着眼，隔一阵闭一下，越被盯着眨得越勤。 */
class IncursusBladeBlinkTest {

    /** 采样窗口，40 000 tick ≈ 33 分钟，够把三档节奏的差距拉开。 */
    private static final int SAMPLE_TICKS = 40_000;

    private static final int SEED = 0x5EED_1234;
    private static final int OTHER_SEED = 0x0BAD_C0DE;

    @Test
    void differentStacksBlinkToTheirOwnRhythm() {
        int firstSeedBlinks = 0;
        int secondSeedBlinks = 0;
        int bothClosed = 0;
        for (int tick = 0; tick < SAMPLE_TICKS; tick++) {
            boolean first = IncursusBladeBlink.isClosed(SEED, Gaze.IDLE, tick);
            boolean second = IncursusBladeBlink.isClosed(OTHER_SEED, Gaze.IDLE, tick);
            if (first) {
                firstSeedBlinks++;
            }
            if (second) {
                secondSeedBlinks++;
            }
            if (first && second) {
                bothClosed++;
            }
        }

        assertTrue(firstSeedBlinks > 100 && secondSeedBlinks > 100);
        assertTrue(bothClosed * 3 < Math.min(firstSeedBlinks, secondSeedBlinks),
                "两把刃闭眼的时刻重合太多（" + bothClosed + " 次），看着像同步的");
    }

}
