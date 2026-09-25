package org.brahypno.maledict.common.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 「生灵之祝」的份数表：一份就是原版这一 tick 实际回掉的血量，每级再补一份。 */
class BlessedRegenerationTest {

    private static final float EPSILON = 0.0001F;

    @Test
    void eachLevelAddsOneMoreShare() {
        assertEquals(1.0F, BlessedRegeneration.extraHeal(1.0F, 0), EPSILON);
        assertEquals(2.0F, BlessedRegeneration.extraHeal(1.0F, 1), EPSILON);
        assertEquals(0.5F, BlessedRegeneration.extraHeal(0.5F, 0), EPSILON);
        assertEquals(1.5F, BlessedRegeneration.extraHeal(0.5F, 2), EPSILON);
        assertEquals(0.3333F, BlessedRegeneration.extraHeal(0.3333F, 0), EPSILON);
    }

    @Test
    void aTickWithNoHealingGivesNothing() {
        assertEquals(0.0F, BlessedRegeneration.extraHeal(0.0F, 0), EPSILON);
        assertEquals(0.0F, BlessedRegeneration.extraHeal(-3.0F, 4), EPSILON);
    }

    @Test
    void anInvalidAmplifierGivesNothing() {
        assertEquals(0.0F, BlessedRegeneration.extraHeal(1.0F, -2), EPSILON);
    }
}
