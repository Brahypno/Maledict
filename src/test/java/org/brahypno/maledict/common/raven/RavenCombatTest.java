package org.brahypno.maledict.common.raven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RavenCombatTest {

    @Test
    void successiveUndeadHitsAreHalvedIndependentlyBeforeArmor() {
        // Undead 8 -> 4, undead 8 -> 4, ordinary 8 -> 8, undead 8 -> 4.
        assertEquals(4, RavenCombat.incomingDamage(8, true));
        assertEquals(4, RavenCombat.incomingDamage(8, true));
        assertEquals(8, RavenCombat.incomingDamage(8, false));
        assertEquals(4, RavenCombat.incomingDamage(8, true));
    }
}
