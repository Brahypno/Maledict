package org.brahypno.maledict.common.raven;

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

}
