package org.brahypno.maledict.common.combat.refulgence;

import org.brahypno.maledict.common.combat.refulgence.RefulgenceCharge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 反照储能的算术与时限，对应实现规范第 1.2、5.2 与 6.1 节。
 * 护盾减免本身（Malum 与逐光扬弃两处）只能在游戏里验证，这里只钉住记账规则。
 */
class RefulgenceChargeTest {

    private static final double DELTA = 1.0E-6D;

    /**
     * 20 点原始伤害、倍率 0.3 → 减免 14 点 → 储能 3.5。
     */
    @Test
    void aQuarterOfTheAbsorbedDamageIsStored() {
        RefulgenceCharge charge = RefulgenceCharge.empty();
        assertEquals(3.5F, charge.add(14.0F, 10.0D, 100L), DELTA);
        assertEquals(3.5F, charge.charge(), DELTA);
    }

    /**
     * 上限是武器自身基础攻击伤害的 80%，贴顶之后多出来的吸收不再计入。
     */
    @Test
    void theCapIsEightyPercentOfTheWeaponBaseAttack() {
        RefulgenceCharge charge = RefulgenceCharge.empty();
        charge.add(100.0F, 10.0D, 100L);
        assertEquals(8.0F, charge.charge(), DELTA);
        assertEquals(0.0F, charge.add(100.0F, 10.0D, 101L), DELTA);
    }

    /**
     * 120 tick 之后到期；时钟倒退或存档恢复这类不连续一律按过期处理。
     */
    @Test
    void theWindowExpiresAfterOneHundredTwentyTicks() {
        RefulgenceCharge charge = RefulgenceCharge.empty();
        charge.add(4.0F, 10.0D, 100L);
        assertFalse(charge.isExpired(219L));
        assertTrue(charge.isExpired(220L));
        assertTrue(charge.isExpired(99L));
    }

    /**
     * 释放只扣掉实际存着的那点，不会多扣，也不会留下负数。
     */
    @Test
    void takingOnlyConsumesWhatIsStored() {
        RefulgenceCharge charge = RefulgenceCharge.empty();
        charge.add(4.0F, 10.0D, 100L);
        assertEquals(1.0F, charge.take(5.0F), DELTA);
        assertTrue(charge.isEmpty());
        assertEquals(0.0F, charge.take(5.0F), DELTA);
    }
}
