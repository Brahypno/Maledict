package org.brahypno.maledict.common.weapon.incursus;

import org.brahypno.maledict.common.weapon.incursus.IncursusBladeItem.DamageTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 无忧符文对神侵恶刃的抬档：一级一级往上，到顶不再高（用户指定的 light→medium→更高）。 */
class IncursusBladeTierTest {

    @Test
    void everyTierGoesUpExactlyOneRung() {
        assertEquals(DamageTier.MEDIUM, DamageTier.LIGHT.raised());
        assertEquals(DamageTier.FINAL, DamageTier.MEDIUM.raised());
    }

}
