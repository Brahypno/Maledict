package org.brahypno.maledict.common.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link RipeningBonus} 的四分之一表。
 *
 * <p>规则：整数部分照发，小数部分按掷骰进位（掷骰小于零头就多给一点）。
 * 于是 4 点经验必定多 1 点，而 1 点经验有四分之一的概率多 1 点——单次随机，长期正好四分之一。
 */
class RipeningBonusTest {

    /** 等级 I：4 点经验正好多出 1 点，零头为 0，掷什么都一样。 */
    @Test
    void levelOneAddsAWholeQuarter() {
        assertEquals(5, RipeningBonus.ripened(4, 0, 0.0F));
        assertEquals(5, RipeningBonus.ripened(4, 0, 0.99F));
    }

    /** 1 点经验的零头是 0.25：掷到 0.24 进位，掷到 0.25 就不进位（半开区间）。 */
    @Test
    void theRemainderRidesOnTheRoll() {
        assertEquals(2, RipeningBonus.ripened(1, 0, 0.24F));
        assertEquals(1, RipeningBonus.ripened(1, 0, 0.25F));
    }

    /** 等级 II：8 点经验多出一半，4 点，与掷骰无关。 */
    @Test
    void levelTwoDoublesTheRatio() {
        assertEquals(12, RipeningBonus.ripened(8, 1, 0.99F));
    }

    /** 等级越高零头越大：3 点经验在 III 级的零头是 0.25，掷 0 就进位。 */
    @Test
    void higherLevelsRaiseTheRemainder() {
        assertEquals(6, RipeningBonus.ripened(3, 2, 0.0F));
        assertEquals(5, RipeningBonus.ripened(3, 2, 1.0F));
    }

    /** 长期看正好四分之一：100 次等距掷骰里，1 点经验多给的那次正好落在 25 次。 */
    @Test
    void aHundredRollsPayExactlyAQuarter() {
        int total = 0;
        for (int i = 0; i < 100; i++) {
            total += RipeningBonus.ripened(1, 0, i / 100.0F) - 1;
        }
        assertEquals(25, total);
    }

    /** 没经验可发时原样返回，不掷骰也不加成。 */
    @Test
    void nothingToGiveMeansNothingAdded() {
        assertEquals(0, RipeningBonus.ripened(0, 0, 0.0F));
    }

    /** 扣经验（负数）不碰：这条路上只加不乘。 */
    @Test
    void takingExperienceAwayIsLeftAlone() {
        assertEquals(-5, RipeningBonus.ripened(-5, 0, 0.0F));
    }

    /** 等级非法时不生效，免得负等级把经验反过来扣。 */
    @Test
    void anInvalidAmplifierGivesNothing() {
        assertEquals(4, RipeningBonus.ripened(4, -1, 0.0F));
    }
}
