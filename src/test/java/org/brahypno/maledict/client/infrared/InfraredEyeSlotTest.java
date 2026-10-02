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

    /** 还没挣到视界槽：一个都没有，自然不亮。 */
    @Test
    void noSlotMeansOff() {
        assertFalse(InfraredEyeSlot.isOn(0, List.of()));
    }

    /** 空着的槽默认是亮的，所以什么都不戴也能用。 */
    @Test
    void anEmptySlotIsStillOn() {
        assertTrue(InfraredEyeSlot.isOn(1, List.of(true)));
    }

    /** 玩家自己把那只眼睛点灭就是关：这一条与槽里有没有东西无关。 */
    @Test
    void toggledOffSlotMeansOff() {
        assertFalse(InfraredEyeSlot.isOn(1, List.of(false)));
    }

    /** 多出来的格不参与判断：第一格灭着，后面几格再亮也不开。 */
    @Test
    void onlyTheFirstSlotCounts() {
        assertTrue(InfraredEyeSlot.isOn(2, List.of(true, false)));
        assertFalse(InfraredEyeSlot.isOn(2, List.of(false, true)));
        assertFalse(InfraredEyeSlot.isOn(2, List.of(false, false)));
    }

    /** 格数从 0 变成 1 才算有：格数为 0 时，残留的开关值不作数。 */
    @Test
    void slotCountDecidesWhetherThereIsADeviceAtAll() {
        assertFalse(InfraredEyeSlot.isOn(0, List.of(true)));
    }

    /** 开关列表没同步到位（比格数短）时按 Curios 的默认值算：亮。 */
    @Test
    void missingRenderFlagsFallBackToCuriosDefault() {
        assertTrue(InfraredEyeSlot.isOn(1, List.of()));
        assertTrue(InfraredEyeSlot.isOn(2, List.of()));
    }
}
