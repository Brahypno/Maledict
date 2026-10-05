package org.brahypno.maledict.common.entity;

/** Healing takes precedence over breeding, including the meal that reaches full health. */
public final class RavenFeeding {
    public static final float HEAL_AMOUNT = 2.0F;

    public record Meal(float healthAfter, boolean consumed, boolean grow, boolean breed) {
    }

    public static Meal evaluate(float health, float maximum, int age, boolean canLove) {
        if (health < maximum) {
            return new Meal(Math.min(maximum, health + HEAL_AMOUNT), true, age < 0, false);
        }
        if (age < 0) {
            return new Meal(health, true, true, false);
        }
        return new Meal(health, age == 0 && canLove, false, age == 0 && canLove);
    }

    private RavenFeeding() {
    }
}
