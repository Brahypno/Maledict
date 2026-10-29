package org.brahypno.maledict.client.infrared;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 眼槽开关的验收：<b>第一格</b>视界槽在、那一格的渲染开关亮着，就算开着——槽空着也算。
 *
 * <p>这里证明不了饰品真的会出现在眼槽里、眼睛按钮真的点得动，那些只能在游戏里验。
 */
class InfraredEyeSlotTest {

    /** 多出来的格不参与判断：第一格灭着，后面几格再亮也不开。 */
    @Test
    void onlyTheFirstSlotCounts() {
        assertTrue(InfraredEyeSlot.isOn(2, List.of(true, false)));
        assertFalse(InfraredEyeSlot.isOn(2, List.of(false, true)));
        assertFalse(InfraredEyeSlot.isOn(2, List.of(false, false)));
    }

}
