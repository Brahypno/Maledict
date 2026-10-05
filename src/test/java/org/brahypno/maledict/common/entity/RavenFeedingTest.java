package org.brahypno.maledict.common.entity;

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

    @Test
    void feedingAChickHealsAndSpeedsGrowthWithoutBreeding() {
        RavenFeeding.Meal hurt = RavenFeeding.evaluate(9, 10, -24000, false);
        assertEquals(10, hurt.healthAfter());
        assertTrue(hurt.grow());
        assertFalse(hurt.breed());
        RavenFeeding.Meal healthy = RavenFeeding.evaluate(10, 10, -24000, false);
        assertTrue(healthy.consumed());
        assertTrue(healthy.grow());
        assertFalse(healthy.breed());
    }

    @Test
    void breedingCooldownAndExistingLoveDoNotConsumeUnneededMeat() {
        assertFalse(RavenFeeding.evaluate(10, 10, 6000, true).consumed());
        assertFalse(RavenFeeding.evaluate(10, 10, 0, false).consumed());
        assertTrue(RavenFeeding.evaluate(6, 10, 6000, false).consumed());
    }
}
