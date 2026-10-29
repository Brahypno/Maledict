package org.brahypno.maledict.common.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 「熟成之赐」的四分之一表：整数部分照发，小数部分按掷骰进位。 */
class RipeningBonusTest {

    @Test
    void theRemainderRidesOnTheRoll() {
        assertEquals(2, RipeningBonus.ripened(1, 0, 0.24F));
        assertEquals(1, RipeningBonus.ripened(1, 0, 0.25F));
    }

}
