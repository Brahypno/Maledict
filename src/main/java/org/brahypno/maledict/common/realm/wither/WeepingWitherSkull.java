package org.brahypno.maledict.common.realm.wither;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** Vanilla flight, charged skulls and terrain explosions, with the confirmed direct-hit skills. */
public final class WeepingWitherSkull extends WitherSkull {
    public static final int MAX_LIFETIME_TICKS = 600;
    private int lifeTicks;

    public WeepingWitherSkull(EntityType<? extends WeepingWitherSkull> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        // Expire before vanilla collision handling, so a last-tick impact cannot explode.
        if (!level().isClientSide && ++lifeTicks >= MAX_LIFETIME_TICKS) {
            discard();
            return;
        }
        super.tick();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = Mth.clamp(tag.getInt("LifeTicks"), 0, MAX_LIFETIME_TICKS);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level().isClientSide) return;
        Entity victim = hit.getEntity();
        Entity owner = getOwner();
        if (!(owner instanceof WeepingWitherEntity wither)) {
            super.onHitEntity(hit);
            return;
        }
        boolean alreadyWithered = victim instanceof LivingEntity living && living.hasEffect(MobEffects.WITHER);
        if (!victim.hurt(damageSources().witherSkull(this, wither), WeepingWitherCombat.directDamage(alreadyWithered))) return;
        if (victim.isAlive()) doEnchantDamageEffects(wither, victim);
        else wither.heal(5.0F); // The original kill heal is deliberately retained in addition to proliferation.
        wither.onEffectiveSkullHit(alreadyWithered);
        if (victim instanceof LivingEntity living) {
            int duration = level().getDifficulty() == Difficulty.HARD ? 1200
                    : level().getDifficulty() == Difficulty.NORMAL ? 300 : 0;
            if (duration > 0) living.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, 1), getEffectSource());
        }
    }
}
