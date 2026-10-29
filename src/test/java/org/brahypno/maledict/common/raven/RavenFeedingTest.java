package org.brahypno.maledict.common.raven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RavenFeedingTest {
    @Test
    void injuredAdultNeedsTwoHealingMealsThenASeparateBreedingMeal() {
        RavenFeeding.Meal first = RavenFeeding.evaluate(6, 10, 0, true);
        assertEquals(8, first.healthAfter());
        assertTrue(first.consumed());
        assertFalse(first.breed());
        RavenFeeding.Meal second = RavenFeeding.evaluate(first.healthAfter(), 10, 0, true);
        assertEquals(10, second.healthAfter());
        assertFalse(second.breed());
        assertTrue(RavenFeeding.evaluate(second.healthAfter(), 10, 0, true).breed());
    }

}
