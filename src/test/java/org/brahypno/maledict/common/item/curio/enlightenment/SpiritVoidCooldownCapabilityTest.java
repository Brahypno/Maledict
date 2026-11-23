package org.brahypno.maledict.common.item.curio.enlightenment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 魂息虚空的每目标限流：一次占用挂满 {@link SpiritVoidCooldownCapability#COOLDOWN_TICKS} tick，
 * 恰好到 0 才能再占一次，空的计数器 tick 下去也不该减成负数。
 *
 * <p>这里只证明计数器本身；「半血目标才刷」那半边要目标实体与伤害来源，属 GameTest 的范围。
 */
class SpiritVoidCooldownCapabilityTest {

    @Test
    void oneUseHoldsForTheFullCooldownAndOnlyThenComesBack() {
        SpiritVoidCooldownCapability capability = new SpiritVoidCooldownCapability();

        assertTrue(capability.tryUse());
        assertEquals(SpiritVoidCooldownCapability.COOLDOWN_TICKS, capability.cooldown());

        for (int tick = 1; tick < SpiritVoidCooldownCapability.COOLDOWN_TICKS; tick++) {
            capability.tickDown();
            assertFalse(capability.tryUse());
        }
        assertEquals(1, capability.cooldown());

        capability.tickDown();
        assertEquals(0, capability.cooldown());
        assertTrue(capability.tryUse());

        for (int tick = 0; tick < SpiritVoidCooldownCapability.COOLDOWN_TICKS * 3; tick++) {
            capability.tickDown();
        }
        assertEquals(0, capability.cooldown());
    }
}
