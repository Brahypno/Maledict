package org.brahypno.maledict.common.entity;

/** Confirmed formula: difficulty IDs 0/1/2/3; undead resistance has no hit counter. */
public final class RavenCombat {
    public static float attackDamage(float base, int difficulty, boolean undead) {
        return (base + difficulty * 2.0F) * (undead ? 2.0F : 1.0F);
    }

    public static float incomingDamage(float amount, boolean undead) {
        return amount * (undead ? 0.5F : 1.0F);
    }

    private RavenCombat() {
    }
}
