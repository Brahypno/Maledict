package org.brahypno.maledict.common.raven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RavenPreeningTest {

    @Test
    void interruptedPreeningMustStartTheThreeSecondWaitAgain() {
        RavenPreening preening = new RavenPreening();
        for (int tick = 0; tick < 59; tick++) {
            assertFalse(preening.tick(true));
        }
        assertFalse(preening.tick(false));
        for (int tick = 1; tick < 60; tick++) {
            assertFalse(preening.tick(true));
        }
        assertTrue(preening.tick(true));
    }
}
