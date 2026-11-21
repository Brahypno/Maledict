package org.brahypno.maledict.common.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Out-of-combat recovery shared by the bosses: while a boss is not fighting anything it restores
 * {@link #HEAL_FRACTION_PER_TICK} of its own maximum pool every tick.
 * <p>
 * A boss still counts as fighting while any of these holds:
 * <ul>
 * <li>it holds a live target;</li>
 * <li>vanilla remembers an attacker through {@link LivingEntity#getLastHurtByMob()} (cleared 100
 * ticks after the hit, or as soon as that attacker dies);</li>
 * <li>it took resolved damage within the last {@link #COMBAT_MEMORY_TICKS} ticks. This covers hits
 * that never produced a target: a player shooting from beyond the boss's own reach, projectiles,
 * environment damage and scripted damage. Without it a boss being whittled down from outside its
 * aggro range would heal faster than it is being hurt.</li>
 * </ul>
 * The memory is stamped by the boss itself, because {@code lastHurtByMob} is only set for damage
 * that carries a causing entity. A boss also stamps the end of a scripted revival, which is a fight
 * beat rather than a disengagement.
 */
public final class BossRecovery {
    /**
     * Fraction of the maximum pool restored per tick while out of combat (5%: a full pool in 20 ticks).
     */
    public static final float HEAL_FRACTION_PER_TICK = 0.05F;
    /**
     * How long after the last damage a boss still counts as engaged. Matches vanilla's own
     * {@code lastHurtByMob} memory, so both combat signals expire together.
     */
    public static final int COMBAT_MEMORY_TICKS = 100;
    /**
     * Sentinel for "this boss has not been in combat yet".
     */
    public static final long NO_COMBAT_TICK = Long.MIN_VALUE;

    /**
     * Applies one tick of out-of-combat healing. {@code maximum} is the authoritative pool size,
     * which is not always {@link LivingEntity#getMaxHealth()} for the ledger-owned bosses.
     */
    public static void tick(Mob boss, float maximum, long lastCombatTick) {
        if (boss.level().isClientSide || boss.isRemoved() || !boss.isAlive()
            || isEngaged(boss, lastCombatTick)) {
            return;
        }
        float health = boss.getHealth();
        if (!Float.isFinite(maximum) || maximum <= 0.0F || health <= 0.0F || health >= maximum) {
            return;
        }
        boss.heal(maximum * HEAL_FRACTION_PER_TICK);
    }

    /**
     * True while any creature still counts as an enemy, or while combat is recent enough to mean the
     * boss is still being fought. A dead or removed target never keeps a boss engaged.
     */
    public static boolean isEngaged(Mob boss, long lastCombatTick) {
        LivingEntity target = boss.getTarget();
        if (target != null && target.isAlive() && !target.isRemoved()) {
            return true;
        }
        if (boss.getLastHurtByMob() != null) {
            return true;
        }
        return lastCombatTick != NO_COMBAT_TICK
               && boss.level().getGameTime() - lastCombatTick < COMBAT_MEMORY_TICKS;
    }

    private BossRecovery() {
    }
}
