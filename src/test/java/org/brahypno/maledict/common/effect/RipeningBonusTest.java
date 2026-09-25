package org.brahypno.maledict.common.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 「熟成之赐」的四分之一表：整数部分照发，小数部分按掷骰进位。 */
class RipeningBonusTest {

    @Test
    void eachLevelAddsItsRatio() {
        assertEquals(5, RipeningBonus.ripened(4, 0, 0.0F));
        assertEquals(5, RipeningBonus.ripened(4, 0, 0.99F));
        assertEquals(12, RipeningBonus.ripened(8, 1, 0.99F));
        assertEquals(6, RipeningBonus.ripened(3, 2, 0.0F));
        assertEquals(5, RipeningBonus.ripened(3, 2, 1.0F));
    }

    @Test
    void theRemainderRidesOnTheRoll() {
        assertEquals(2, RipeningBonus.ripened(1, 0, 0.24F));
        assertEquals(1, RipeningBonus.ripened(1, 0, 0.25F));
    }

    @Test
    void nothingToGiveOrTakeIsLeftAlone() {
        assertEquals(0, RipeningBonus.ripened(0, 0, 0.0F));
        assertEquals(-5, RipeningBonus.ripened(-5, 0, 0.0F));
    }

    @Test
    void anInvalidAmplifierGivesNothing() {
        assertEquals(4, RipeningBonus.ripened(4, -2, 0.0F));
    }
}
