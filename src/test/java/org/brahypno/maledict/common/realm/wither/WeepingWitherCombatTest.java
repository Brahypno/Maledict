package org.brahypno.maledict.common.realm.wither;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeepingWitherCombatTest {
    @Test
    void confirmedHitOrderGlobalCooldownAndSingleReformationSurviveRestore() {
        var combat = new WeepingWitherCombat();
        assertEquals(12.0F, WeepingWitherCombat.directDamage(false));
        assertFalse(combat.recordEffectiveHit(false));
        assertEquals(14.0F, WeepingWitherCombat.directDamage(true));
        assertTrue(combat.recordEffectiveHit(true));
        assertFalse(combat.recordEffectiveHit(true));
        assertFalse(combat.recordEffectiveHit(true));
        for (int tick = 0; tick < 39; tick++) combat.tick();
        assertFalse(combat.recordEffectiveHit(true));
        combat.tick();
        assertTrue(combat.recordEffectiveHit(true));
        assertTrue(combat.beginReformation());
        assertFalse(combat.recordEffectiveHit(true));
        for (int tick = 0; tick < 10; tick++) assertFalse(combat.tick());
        var loaded = new WeepingWitherCombat();
        loaded.restore(combat.reformationUsed(), combat.reformationTicks(), combat.healCooldown());
        assertFalse(loaded.beginReformation());
        for (int tick = 0; tick < 49; tick++) assertFalse(loaded.tick());
        assertTrue(loaded.isReforming());
        assertTrue(loaded.tick());
        assertFalse(loaded.isReforming());
        assertFalse(loaded.tick());
        assertFalse(loaded.beginReformation());
    }
}
