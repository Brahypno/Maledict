package org.brahypno.maledict.common.item;

import org.brahypno.maledict.common.item.IncursusBladeItem.DamageTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 无忧符文对神侵恶刃的抬档：一级一级往上，到顶不再高（用户指定的 light→medium→更高）。 */
class IncursusBladeTierTest {

    @Test
    void everyTierGoesUpExactlyOneRung() {
        assertEquals(DamageTier.MEDIUM, DamageTier.LIGHT.raised());
        assertEquals(DamageTier.FINAL, DamageTier.MEDIUM.raised());
    }

    /** 终档没有更高的地方可去，抬了还是终档（不溢出、不回绕）。 */
    @Test
    void theTopTierStaysAtTheTop() {
        assertEquals(DamageTier.FINAL, DamageTier.FINAL.raised());
    }
}
