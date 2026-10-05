package org.brahypno.maledict.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RavenCombatTest {
    @Test
    void allFourDifficultiesDoubleOnlyAgainstUndead() {
        float[] ordinary = {2, 4, 6, 8};
        float[] undead = {4, 8, 12, 16};
        for (int difficulty = 0; difficulty < 4; difficulty++) {
            assertEquals(ordinary[difficulty], RavenCombat.attackDamage(2, difficulty, false));
            assertEquals(undead[difficulty], RavenCombat.attackDamage(2, difficulty, true));
        }
    }

    @Test
    void successiveUndeadHitsAreHalvedIndependentlyBeforeArmor() {
        // Undead 8 -> 4, undead 8 -> 4, ordinary 8 -> 8, undead 8 -> 4.
        assertEquals(4, RavenCombat.incomingDamage(8, true));
        assertEquals(4, RavenCombat.incomingDamage(8, true));
        assertEquals(8, RavenCombat.incomingDamage(8, false));
        assertEquals(4, RavenCombat.incomingDamage(8, true));
    }
}
