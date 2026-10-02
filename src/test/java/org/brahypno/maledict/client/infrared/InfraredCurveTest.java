package org.brahypno.maledict.client.infrared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 微光视觉两段算术的验收：迟滞的两个阈值与中括号那格，以及过渡时长落在文档要求的 0.3–0.5 秒里。
 *
 * <p>这里证明不了效果真的出现在画面上——读相机、读眼槽、上传 uniform 那几段只能在游戏里验。
 */
class InfraredCurveTest {

    @Test
    void darknessEntersAtSixOrBelow() {
        assertTrue(InfraredCurve.darknessAfter(false, 0));
        assertTrue(InfraredCurve.darknessAfter(false, 6));
        assertFalse(InfraredCurve.darknessAfter(false, 7));
        assertFalse(InfraredCurve.darknessAfter(false, 15));
    }

    @Test
    void darknessLeavesAtEightOrAbove() {
        assertTrue(InfraredCurve.darknessAfter(true, 0));
        assertTrue(InfraredCurve.darknessAfter(true, 7));
        assertFalse(InfraredCurve.darknessAfter(true, 8));
        assertFalse(InfraredCurve.darknessAfter(true, 15));
    }

    /** 7 这一格两个方向都保持现状：在阈值上反复横跳不该让画面闪。 */
    @Test
    void theMiddleStepHoldsWhicheverWayYouComeFrom() {
        boolean active = false;
        active = InfraredCurve.darknessAfter(active, 5);
        assertTrue(active);
        active = InfraredCurve.darknessAfter(active, 7);
        assertTrue(active);
        active = InfraredCurve.darknessAfter(active, 8);
        assertFalse(active);
        active = InfraredCurve.darknessAfter(active, 7);
        assertFalse(active);
        active = InfraredCurve.darknessAfter(active, 6);
        assertTrue(active);
    }

    @Test
    void fadeInIsMostlyDoneInAboutFourTenthsOfASecond() {
        assertTrue(InfraredCurve.approach(0.0F, 1.0F, 0.2F) < 0.85F);
        assertTrue(InfraredCurve.approach(0.0F, 1.0F, 0.3F) > 0.9F);
        assertTrue(InfraredCurve.approach(0.0F, 1.0F, 0.5F) > 0.98F);
        assertTrue(InfraredCurve.approach(0.0F, 1.0F, 10.0F) <= 1.0F);
    }

    /** 逐帧推进与整段一次性算出来的必须一致，否则掉帧会改变淡入淡出的手感。 */
    @Test
    void frameSteppingMatchesOneBigStep() {
        float stepped = 0.0F;
        for (int frame = 0; frame < 30; frame++) {
            stepped = InfraredCurve.approach(stepped, 1.0F, 1.0F / 60.0F);
        }
        assertEquals(InfraredCurve.approach(0.0F, 1.0F, 0.5F), stepped, 0.002F);
    }

    /** 淡出期间强度必须还在 epsilon 之上，后处理才不会被提前关掉。 */
    @Test
    void fadeOutStaysVisibleForAWhileThenSnapsToZero() {
        assertTrue(InfraredCurve.approach(1.0F, 0.0F, 0.25F) > InfraredCurve.ACTIVE_EPSILON);
        assertEquals(0.0F, InfraredCurve.approach(1.0F, 0.0F, 1.0F), 0.0F);
        assertEquals(0.0F, InfraredCurve.approach(0.0F, 0.0F, 1.0F), 0.0F);
    }

    @Test
    void strengthNeverOvershootsAndNeverGoesBackwards() {
        float previous = 0.0F;
        for (int frame = 0; frame < 120; frame++) {
            float next = InfraredCurve.approach(previous, 1.0F, 1.0F / 20.0F);
            assertTrue(next >= previous);
            assertTrue(next <= 1.0F);
            previous = next;
        }
        // 负的帧时长（时钟回拨之类）不该把强度推离目标。
        assertEquals(0.5F, InfraredCurve.approach(0.5F, 1.0F, -1.0F), 0.0F);
    }
}
