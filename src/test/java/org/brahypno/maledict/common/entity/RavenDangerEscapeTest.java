package org.brahypno.maledict.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RavenDangerEscapeTest {
    @Test
    void standingInFireEscapesThenResumesEvenWhileResidualFireTicksContinue() {
        RavenDangerEscape escape = new RavenDangerEscape();
        assertTrue(escape.shouldEscape(true, true));
        escape.begin(true);
        assertFalse(RavenDangerEscape.reachedSafety(true, 16));
        assertFalse(RavenDangerEscape.reachedSafety(false, 4));
        assertTrue(RavenDangerEscape.reachedSafety(false, 9));
        escape.finish();
        escape.onDamage(true, true);
        assertFalse(escape.shouldEscape(false, true));
        assertTrue(escape.shouldEscape(true, true));
    }

    @Test
    void extinguishingThenCatchingFireAgainStartsAnotherEscape() {
        RavenDangerEscape escape = new RavenDangerEscape();
        escape.begin(true);
        escape.finish();
        escape.updateBurning(false);
        assertFalse(escape.shouldEscape(false, false));
        assertTrue(escape.shouldEscape(false, true));
    }

    @Test
    void environmentalDamageRequestsEscapeButMobAttacksKeepNormalRetaliation() {
        RavenDangerEscape escape = new RavenDangerEscape();
        escape.onDamage(false, false);
        assertFalse(escape.shouldEscape(false, false));
        escape.onDamage(true, false);
        assertTrue(escape.shouldEscape(false, false));
        escape.begin(false);
        escape.onDamage(true, false);
        escape.finish();
        assertFalse(escape.shouldEscape(false, false));
    }
}
