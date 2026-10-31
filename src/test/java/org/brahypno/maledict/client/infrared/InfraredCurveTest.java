package org.brahypno.maledict.client.infrared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 微光视觉迟滞序列：进入黑暗、经过中间亮度、退出后再次经过中间亮度。
 *
 * <p>这里证明不了效果真的出现在画面上——读相机、读眼槽、上传 uniform 那几段只能在游戏里验。
 */
class InfraredCurveTest {

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

}
