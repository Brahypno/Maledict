package org.brahypno.maledict.common.entity.wither;

/**
 * Confirmed hit order: inspect existing wither, damage, heal if eligible, then apply wither.
 */
public final class WeepingWitherCombat {
    public static final int REFORMATION_TICKS = 60;
    public static final int HEAL_COOLDOWN = 40;
    private boolean reformationUsed;
    private int reformationTicks;
    private int healCooldown;

    /**
     * The withered-target rider is one percent of the victim's own cap instead of a flat heart, so
     * the same skull keeps its bite against a large pool without overpaying against a small one.
     */
    public static float directDamage(boolean alreadyWithered, float targetMaxHealth) {
        float base = 8.0F * 1.25F;
        if (alreadyWithered)
            return base + Math.min(base * 0.2F, 4.0F) + targetMaxHealth * 0.01F;
        return base + 2.0F;
    }

    public boolean recordEffectiveHit(boolean alreadyWithered) {
        if (!alreadyWithered || healCooldown > 0 || isReforming())
            return false;
        healCooldown = HEAL_COOLDOWN;
        return true;
    }

    public boolean beginReformation() {
        if (reformationUsed)
            return false;
        reformationUsed = true;
        reformationTicks = REFORMATION_TICKS;
        return true;
    }

    /**
     * Returns true exactly on the tick that reformation finishes.
     */
    public boolean tick() {
        if (healCooldown > 0)
            healCooldown--;
        return reformationTicks > 0 && --reformationTicks == 0;
    }

    public boolean isReforming() {
        return reformationTicks > 0;
    }

    public boolean reformationUsed() {
        return reformationUsed;
    }

    public int reformationTicks() {
        return reformationTicks;
    }

    public int healCooldown() {
        return healCooldown;
    }

    public void restore(boolean used, int remaining, int cooldown) {
        reformationTicks = Math.max(0, Math.min(remaining, REFORMATION_TICKS));
        reformationUsed = used || reformationTicks > 0;
        healCooldown = Math.max(0, Math.min(cooldown, HEAL_COOLDOWN));
    }
}
