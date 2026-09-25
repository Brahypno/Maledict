package org.brahypno.maledict.common.item;

import org.brahypno.maledict.common.item.IncursusBladeBlink.Gaze;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 神侵恶刃在物品栏里的眨眼：睁着眼，隔一阵闭一下，越被盯着眨得越勤。 */
class IncursusBladeBlinkTest {

    /** 采样窗口，40 000 tick ≈ 33 分钟，够把三档节奏的差距拉开。 */
    private static final int SAMPLE_TICKS = 40_000;

    private static final int SEED = 0x5EED_1234;
    private static final int OTHER_SEED = 0x0BAD_C0DE;

    @Test
    void theSameTickAlwaysAnswersTheSame() {
        for (Gaze gaze : Gaze.values()) {
            for (int tick = 0; tick < 500; tick++) {
                boolean first = IncursusBladeBlink.isClosed(SEED, gaze, tick);
                for (int repeat = 0; repeat < 4; repeat++) {
                    assertEquals(first, IncursusBladeBlink.isClosed(SEED, gaze, tick),
                            gaze + " 在 tick " + tick + " 上前后的答案不一致");
                }
            }
        }
    }

    /** 单次闭眼是 1 到 3 tick（真人眨眼约 100–150 毫秒），跨轮也不许粘成一段。 */
    @Test
    void everyBlinkIsShort() {
        for (int seed = 0; seed < 64; seed++) {
            for (Gaze gaze : Gaze.values()) {
                List<Integer> lengths = blinkLengths(seed, gaze, SAMPLE_TICKS);
                assertFalse(lengths.isEmpty(), gaze + " 在 33 分钟里一次都没眨");
                for (int length : lengths) {
                    assertTrue(length >= 1 && length <= 3, gaze + " 眨了一次 " + length + " tick 的眼");
                }
            }
        }
    }

    /** 快捷栏选中 &gt; 鼠标指着 &gt; 只是躺在背包里。 */
    @Test
    void theMoreAttentionTheMoreItBlinks() {
        int selected = blinkLengths(SEED, Gaze.SELECTED, SAMPLE_TICKS).size();
        int hovered = blinkLengths(SEED, Gaze.HOVERED, SAMPLE_TICKS).size();
        int idle = blinkLengths(SEED, Gaze.IDLE, SAMPLE_TICKS).size();

        assertTrue(idle > 100, "没人理的时候也得偶尔眨一下，实际 " + idle + " 次");
        assertTrue(hovered > idle * 1.5, "悬停该比躺着明显勤：" + hovered + " vs " + idle);
        assertTrue(selected > hovered * 1.5, "选中该比悬停明显勤：" + selected + " vs " + hovered);
    }

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

    private static List<Integer> blinkLengths(int seed, Gaze gaze, int ticks) {
        List<Integer> lengths = new ArrayList<>();
        int current = 0;
        for (int tick = 0; tick < ticks; tick++) {
            if (IncursusBladeBlink.isClosed(seed, gaze, tick)) {
                current++;
            } else if (current > 0) {
                lengths.add(current);
                current = 0;
            }
        }
        if (current > 0) {
            lengths.add(current);
        }
        return lengths;
    }
}
