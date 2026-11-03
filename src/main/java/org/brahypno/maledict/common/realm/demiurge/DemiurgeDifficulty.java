package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.brahypno.changelib.DamageHelper.DamageOptions;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import org.brahypno.maledict.config.MaledictConfig;

/**
 * Configurable proportional damage; the same final amount applies to players and other living targets.
 */
public final class DemiurgeDifficulty {
    public static float maxHealth(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL, EASY -> 400;
            case NORMAL -> 600;
            case HARD -> 800;
        };
    }

    public static float damage(double base, Difficulty difficulty) {
        double multiplier = switch (difficulty) {
            case PEACEFUL -> 0;
            case EASY -> MaledictConfig.DEMIURGE_EASY_DAMAGE_MULTIPLIER.get();
            case NORMAL -> MaledictConfig.DEMIURGE_NORMAL_DAMAGE_MULTIPLIER.get();
            case HARD -> MaledictConfig.DEMIURGE_HARD_DAMAGE_MULTIPLIER.get();
        };
        return (float) (base * multiplier);
    }

    public static DamageSource source(DamageSource original) {
        // Player#hurt would otherwise multiply our already-scaled amount again.
        return new DamageSource(original.typeHolder(), original.getDirectEntity(), original.getEntity(), original.sourcePositionRaw()) {
            @Override
            public boolean scalesWithDifficulty() {return false;}
        };
    }

    public static void hurt(LivingEntity victim, DamageSource original, double base) {
        hurt(victim, original, base, victim.level().getDifficulty());
    }

    static void hurt(LivingEntity victim, DamageSource original, double base, Difficulty difficulty) {
        // The caller merges overlapping body contacts; ChangeLib owns delivery
        // and cooldown handling without a separate i-frame filter here.
        float amount = damage(base, difficulty);
        if (amount <= 0)
            return;
        DamageSource source = source(original);
        switch (difficulty) {
            case EASY -> DamageProbe.lighterDamageMethod(victim, source, amount);
            case NORMAL -> DamageProbe.mediumDamageMethod(victim, source, amount, false);
            case HARD -> DamageProbe.finalDamageMethod(victim, source, amount, DamageOptions.finalNoRemove(), false);
            case PEACEFUL -> {}
        }
    }

    private DemiurgeDifficulty() {}
}
