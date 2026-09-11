package org.brahypno.maledict.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * Base for Vicissitude bosses; concrete entities supply registration, AI and visuals.
 *
 * ChangeLib 1.1's DamageUtil entry point is DamageProbe. Its hurt/actuallyHurt,
 * setHealth, numeric-field, entity-data and NBT probes are checked against a
 * UUID-bound world ledger. Do not call DamageProbe on this entity from hurt(): its basic
 * handler calls hurt again. Normal accepted hits retain Forge damage/death hooks.
 * This resists those generic probes, not arbitrary reflection or world removal.
 */
public abstract class VicissitudeBossEntity extends PathfinderMob {
    public static final MobType VICISSITUDE = new MobType();
    private static final EntityDataAccessor<Float> DISPLAY_VITALITY =
            SynchedEntityData.defineId(VicissitudeBossEntity.class, EntityDataSerializers.FLOAT);
    // Transient combat transaction state is separate from the immutable committed values.
    private final VitalityState vitality = new VitalityState();

    protected VicissitudeBossEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DISPLAY_VITALITY, 1.0F);
    }

    @Override
    public final MobType getMobType() {
        return VICISSITUDE;
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (level() instanceof ServerLevel serverLevel && !vitality.joined) {
            vitality.identity = getUUID();
            vitality.ledger = VicissitudeVitalityLedger.get(serverLevel);
            vitality.capability = getCapability(VicissitudeVitalityCapability.CAPABILITY)
                    .orElseThrow(() -> new IllegalStateException("Missing Vicissitude vitality capability"));
            VicissitudeVitality saved = vitality.ledger.find(vitality.identity);
            if (saved == null) {
                // Capture all extra maximum modifiers once for this UUID. Reloading or
                // changing dimensions must not heal wounds or recapture altered attributes.
                float maximum = getMaxHealth();
                saved = VicissitudeVitality.initial(
                        Float.isFinite(maximum) && maximum > 0.0F ? maximum : 1000.0F);
            }
            vitality.joined = true;
            vitality.deathHandled = saved.current() <= 0.0F;
            commitVitality(saved);
        }
    }

    @Override
    public final float getHealth() {
        // LivingEntity's constructor invokes virtual methods before our holder exists.
        if (vitality == null) {
            return super.getHealth();
        }
        if (level().isClientSide) {
            return entityData.get(DISPLAY_VITALITY);
        }
        return vitality.joined ? verifiedVitality().current() : super.getHealth();
    }

    /** Direct setters never become an alternate damage/healing authority. */
    @Override
    public final void setHealth(float value) {
        if (vitality == null) {
            super.setHealth(value);
        } else if (!level().isClientSide && vitality.joined && !Float.isNaN(value)) {
            if (vitality.writing) {
                VicissitudeVitality state = verifiedVitality();
                float loss = Math.min(vitality.budget, Math.max(0.0F, state.current() - value));
                if (loss > 0.0F) {
                    vitality.budget -= loss;
                    commitVitality(state.afterDamage(loss, level().getGameTime(), getVitalityHitInterval()));
                }
            }
        }
    }

    /** Maximum loss per accepted hit, independently of incoming damage magnitude. */
    protected float getVitalityDamageLimit() {
        return verifiedVitality().maximum() * 0.01F;
    }

    protected int getVitalityHitInterval() {
        return 20;
    }

    /** Maximum gap between consecutive strong-kill attempts; duplicates in one tick count once. */
    protected int getKillAttemptWindow() {
        return 100;
    }

    /** Called for a valid server-side damage attempt before immunity/cooldown checks. */
    protected void onIncomingAttack(DamageSource source, float amount) {
    }

    /** Phase implementations can reject an otherwise valid hit without losing attacker information. */
    protected boolean isDamageImmune(DamageSource source) {
        return false;
    }

    @Override
    public final boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !vitality.joined || vitality.receiving
                || Float.isNaN(amount) || amount <= 0.0F) {
            return false;
        }
        onIncomingAttack(source, amount);
        if (isDamageImmune(source)) {
            return false;
        }
        VicissitudeVitality state = verifiedVitality();
        if (state.current() <= 0.0F) {
            return false;
        }
        if (source.is(DamageTypes.GENERIC_KILL)) {
            recordKillAttempt(source);
            return getHealth() <= 0.0F;
        }
        if (level().getGameTime() < state.nextHit()) {
            return false;
        }
        float limit = getVitalityDamageLimit();
        if (!Float.isFinite(limit) || limit <= 0.0F) {
            return false;
        }
        vitality.receiving = true;
        vitality.budget = Math.min(amount, limit);
        try {
            return super.hurt(source, vitality.budget);
        } finally {
            vitality.receiving = false;
            vitality.writing = false;
            vitality.budget = 0.0F;
            publishVitality();
        }
    }

    @Override
    protected final void actuallyHurt(DamageSource source, float amount) {
        // Reflectively invoking actuallyHurt outside an accepted hit cannot deduct vitality.
        if (!vitality.receiving || vitality.writing) {
            return;
        }
        vitality.writing = true;
        try {
            super.actuallyHurt(source, amount);
        } finally {
            vitality.writing = false;
        }
    }

    @Override
    public final void heal(float amount) {
        if (level().isClientSide || !vitality.joined || getHealth() <= 0.0F
                || vitality.receiving || !Float.isFinite(amount) || amount <= 0.0F) {
            return;
        }
        amount = ForgeEventFactory.onLivingHeal(this, amount);
        if (Float.isFinite(amount) && amount > 0.0F) {
            VicissitudeVitality state = verifiedVitality();
            if (state.current() > 0.0F) {
                commitVitality(state.withCurrent(Math.min(state.maximum(), state.current() + amount)));
            }
        }
    }

    @Override
    public final void die(DamageSource source) {
        if (level().isClientSide || vitality == null || !vitality.joined) {
            return;
        }
        // Direct die() is not a kill attempt and cannot open the bypass.
        if (getHealth() <= 0.0F && !vitality.finishing && !vitality.deathHandled) {
            vitality.finishing = true;
            try {
                // Probes can set the vanilla flag without running death processing.
                dead = false;
                super.die(source);
                vitality.deathHandled = dead;
            } finally {
                vitality.finishing = false;
            }
        }
    }

    @Override
    protected final void tickDeath() {
        if (getHealth() <= 0.0F) {
            super.tickDeath();
        }
    }

    @Override
    protected final void dropAllDeathLoot(DamageSource source) {
        // DamageProbe's post-zero finalizer also tries a reflective loot fallback.
        // Only the genuine death transaction may issue rewards, once.
        if (vitality.finishing && !vitality.lootHandled && getHealth() <= 0.0F) {
            vitality.lootHandled = true;
            super.dropAllDeathLoot(source);
        }
    }

    @Override
    public final void kill() {
        recordKillAttempt(damageSources().genericKill());
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && getHealth() > 0.0F
                && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            if (reason == RemovalReason.KILLED) {
                recordKillAttempt(lastKillSource());
            }
            // The third attempt starts the normal death transaction before any deletion.
            return;
        }
        // Chunk unloads and dimension transfers remain valid lifecycle operations.
        super.remove(reason);
    }

    @Override
    public void tick() {
        if (!level().isClientSide && vitality.joined) {
            if (getHealth() > 0.0F) {
                dead = false;
                deathTime = 0;
            }
            publishVitality();
        }
        super.tick();
    }

    private void publishVitality() {
        float current = verifiedVitality().current();
        entityData.set(DISPLAY_VITALITY, current);
        // Vanilla health is only a display mirror, never read back into authority.
        super.setHealth(current);
    }

    /** The independent world record wins; entity NBT and ForgeCaps are not independent votes. */
    private VicissitudeVitality verifiedVitality() {
        VicissitudeVitality authority = Objects.requireNonNull(vitality.ledger.find(vitality.identity),
                "Missing committed Vicissitude vitality");
        if (!authority.equals(vitality.snapshot)) {
            vitality.snapshot = authority;
        }
        if (!authority.equals(vitality.capability.snapshot())) {
            vitality.capability.synchronize(authority);
        }
        return vitality.snapshot;
    }

    private void commitVitality(VicissitudeVitality state) {
        vitality.ledger.commit(vitality.identity, state);
        vitality.snapshot = state;
        vitality.capability.synchronize(state);
        publishVitality();
    }

    private DamageSource lastKillSource() {
        DamageSource source = getLastDamageSource();
        return source == null ? damageSources().genericKill() : source;
    }

    private void recordKillAttempt(DamageSource source) {
        if (level().isClientSide || !vitality.joined || vitality.finishing) {
            return;
        }
        VicissitudeVitality state = verifiedVitality();
        if (state.current() > 0.0F) {
            commitVitality(state.killAttempt(level().getGameTime(), getKillAttemptWindow()));
        }
        if (getHealth() <= 0.0F) {
            die(source);
        }
    }

    @Override
    public final void setUUID(UUID identity) {
        // Allow normal construction/loading, then bind this instance to its original record.
        super.setUUID(vitality != null && vitality.joined ? vitality.identity : identity);
    }

    /** Vicissitude bosses must remain functional even if NoAI is supplied through commands or NBT. */
    @Override
    public final void setNoAi(boolean noAi) {
        super.setNoAi(false);
    }

    @Override
    public final boolean isNoAi() {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (vitality.joined) {
            // Keep live NBT loading outside the active damage setter transaction as well.
            tag = tag.copy();
            tag.putFloat("Health", Math.max(1.0F, getHealth()));
            super.setUUID(vitality.identity);
        }
        super.readAdditionalSaveData(tag);
        if (vitality.joined) {
            if (getHealth() > 0.0F) {
                dead = false;
                deathTime = 0;
            }
            publishVitality();
        }
    }

    private static final class VitalityState {
        private UUID identity;
        private VicissitudeVitalityLedger ledger;
        private VicissitudeVitalityCapability capability;
        private VicissitudeVitality snapshot;
        private float budget;
        private boolean joined;
        private boolean receiving;
        private boolean writing;
        private boolean finishing;
        private boolean deathHandled;
        private boolean lootHandled;
    }
}
