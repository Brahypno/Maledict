package org.brahypno.maledict.common.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;

/** Shared Forge damage resolution and same-tick maximum for capped boss hits. */
public final class BossDamage {
    private long tick = Long.MIN_VALUE;
    private float largestLoss;

    public boolean isCurrentTick(long now) {
        return tick == now;
    }

    public void beginTick(long now) {
        if (!isCurrentTick(now)) {
            tick = now;
            largestLoss = 0.0F;
        }
    }

    /** Each channel resolves fully; only the increase in the final maximum is debited. */
    public float additionalLoss(float cappedLoss) {
        float additional = Math.max(0.0F, cappedLoss - largestLoss);
        largestLoss = Math.max(largestLoss, cappedLoss);
        return additional;
    }

    /** Forge 1.20.1 actuallyHurt order, stopping before the entity's own cap and health write. */
    public static float resolve(LivingEntity victim, DamageSource source, float amount, Mitigation mitigation) {
        amount = ForgeHooks.onLivingHurt(victim, source, amount);
        if (!Float.isFinite(amount) || amount <= 0.0F) return 0.0F;
        amount = mitigation.apply(amount);
        float finalAmount = Math.max(amount - victim.getAbsorptionAmount(), 0.0F);
        victim.setAbsorptionAmount(victim.getAbsorptionAmount() - (amount - finalAmount));
        float absorbed = amount - finalAmount;
        if (absorbed > 0.0F && absorbed < 3.4028235E37F && source.getEntity() instanceof ServerPlayer player) {
            player.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(absorbed * 10.0F));
        }
        finalAmount = ForgeHooks.onLivingDamage(victim, source, finalAmount);
        return Float.isFinite(finalAmount) && finalAmount > 0.0F ? finalAmount : 0.0F;
    }

    /** The entity supplies its protected armor and magic mitigation overrides. */
    @FunctionalInterface
    public interface Mitigation {
        float apply(float amount);
    }
}
