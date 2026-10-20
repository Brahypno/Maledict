package org.brahypno.maledict.common.entity.raven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RavenPreeningTest {
    @Test
    void rollsOnceAfterThreeSecondsAndCanRollAgainNextGathering() {
        RavenPreening first = new RavenPreening();
        RavenPreening second = new RavenPreening();
        for (int tick = 1; tick < 60; tick++) {
            assertFalse(first.tick(true));
            assertFalse(second.tick(true));
        }
        assertTrue(first.tick(true));
        assertTrue(second.tick(true), "Each bird gets its own roll");
        for (int tick = 0; tick < 400; tick++) {
            assertFalse(first.tick(true), "A failed or successful roll cannot be retried this gathering");
        }
        first.tick(false);
        for (int tick = 0; tick < 60; tick++) {
            assertFalse(first.tick(true), "Leaving and returning must not grant another roll");
        }
        first.reset();
        for (int tick = 1; tick < 60; tick++) {
            assertFalse(first.tick(true));
        }
        assertTrue(first.tick(true), "A new gathering gets a fresh roll");
    }

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
