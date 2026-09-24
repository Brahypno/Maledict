package org.brahypno.maledict.common.item;

import org.brahypno.maledict.common.item.IncursusBladeBlink.Gaze;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 神侵恶刃在物品栏里的眨眼：睁着眼，隔一阵闭一下。
 *
 * <p>钉住的是规律本身（{@code IncursusBladeBlink} 是纯换算，不碰世界）：同一 tick 永远同一个
 * 答案、单次闭眼不超过 {@link IncursusBladeBlink#MAX_BLINK_TICKS} tick（跨轮也不许粘起来）、
 * 越被盯着眨得越勤、不同的堆各眨各的。至于「看上去像不像活物」，只能进游戏用眼睛验。
 */
class IncursusBladeBlinkTest {

    /** 采样窗口，40 000 tick ≈ 33 分钟，够把三档节奏的差距拉开。 */
    private static final int SAMPLE_TICKS = 40_000;

    private static final int SEED = 0x5EED_1234;
    private static final int OTHER_SEED = 0x0BAD_C0DE;

    /**
     * 同一个 tick 问多少次都是同一个答案。
     *
     * <p>渲染一秒要问六十次；答案要是跟着调用次数变，物品栏里就会闪成一片。
     */
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

    /**
     * 一次闭眼就是一次眨眼：1 到 {@link IncursusBladeBlink#MAX_BLINK_TICKS} tick，不能更长。
     *
     * <p>「不能更长」同时管住了跨轮粘连：某轮的闭眼贴着轮边界、下一轮的又贴着另一边的话，
     * 两段会连成一段长闭眼——那看起来就不是眨眼，是闭眼休息。
     */
    @Test
    void everyBlinkIsShort() {
        for (int seed = 0; seed < 64; seed++) {
            for (Gaze gaze : Gaze.values()) {
                List<Integer> lengths = blinkLengths(seed, gaze, SAMPLE_TICKS);
                assertFalse(lengths.isEmpty(), gaze + " 在 33 分钟里一次都没眨");
                for (int length : lengths) {
                    assertTrue(length >= IncursusBladeBlink.MIN_BLINK_TICKS
                                    && length <= IncursusBladeBlink.MAX_BLINK_TICKS,
                            gaze + " 眨了一次 " + length + " tick 的眼");
                }
            }
        }
    }

    /**
     * 越被盯着眨得越勤：快捷栏选中 &gt; 鼠标指着 &gt; 只是躺在背包里。
     *
     * <p>这就是「根据是否选中采取不同的贴图交换速度」那一条：三档之间要拉开足够大的差距，
     * 不然玩家根本看不出选中与否有区别。
     */
    @Test
    void theMoreAttentionTheMoreItBlinks() {
        int selected = blinkLengths(SEED, Gaze.SELECTED, SAMPLE_TICKS).size();
        int hovered = blinkLengths(SEED, Gaze.HOVERED, SAMPLE_TICKS).size();
        int idle = blinkLengths(SEED, Gaze.IDLE, SAMPLE_TICKS).size();

        assertTrue(idle > 100, "没人理的时候也得偶尔眨一下，实际 " + idle + " 次");
        assertTrue(hovered > idle * 1.5, "悬停该比躺着明显勤：" + hovered + " vs " + idle);
        assertTrue(selected > hovered * 1.5, "选中该比悬停明显勤：" + selected + " vs " + hovered);
    }

    /** 两把一模一样的刃各眨各的：不是复制粘贴，也不是同一套节奏平移。 */
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

    /** 一段窗口里每一次闭眼各占多少 tick，按发生顺序排好。 */
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
