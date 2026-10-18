package org.brahypno.maledict.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.EventHooks;
import org.brahypno.changelib.HealthHelper.HealthProtectionLevel;
import org.brahypno.changelib.HealthHelper.SetHealthProtection;

import java.util.Objects;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Base for Vicissitude bosses; concrete entities supply registration, AI and visuals.
 *
 * Do not call ChangeLib's DamageProbe from hurt(): its basic handler calls hurt again.
 */
public abstract class VicissitudeBossEntity extends PathfinderMob {
    /** Default MAX_HEALTH; difficulty tables express their pool as a delta against this. */
    public static final double BASE_MAX_HEALTH = 1000.0D;
    private static final EntityDataAccessor<Float> DISPLAY_VITALITY =
            SynchedEntityData.defineId(VicissitudeBossEntity.class, EntityDataSerializers.FLOAT);
    private final VitalityState vitality = new VitalityState();
    // Each channel resolves completely; only the increase in the final maximum is debited.
    private float tickLargestLoss;
    private long tickHitAt = Long.MIN_VALUE;

    protected VicissitudeBossEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BASE_MAX_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISPLAY_VITALITY, 1.0F);
    }


    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (level() instanceof ServerLevel serverLevel && !vitality.joined) {
            vitality.identity = getUUID();
            vitality.ledger = VicissitudeVitalityLedger.get(serverLevel);
            vitality.capability = getData(org.brahypno.maledict.registry.MaledictAttachments.VICISSITUDE_VITALITY);
            VicissitudeVitality saved = vitality.ledger.find(vitality.identity);
            if (saved == null) {
                // Captured once per UUID; reloading must not heal wounds or recapture attributes.
                float maximum = initialVitalityMaximum();
                saved = VicissitudeVitality.initial(
                        Float.isFinite(maximum) && maximum > 0.0F ? maximum : 1000.0F);
            }
            vitality.joined = true;
            commitVitality(saved);
            SetHealthProtection.protect(this, HealthProtectionLevel.DEFENSE_STATE);
        }
    }

    /** Initial authoritative pool; vanilla's ranged attribute may cap a larger authored pool. */
    protected float initialVitalityMaximum() {
        return getMaxHealth();
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

    /** Direct setters are never an alternate damage/healing authority. */
    @Override
    public final void setHealth(float value) {
        if (vitality == null) {
            super.setHealth(value);
        }
    }

    /** 全身的单次命中基准上限，也就是身体（权重 1.0）那一条。 */
    protected float getVitalityDamageLimit() {
        return getVitalityMaximum() * 0.01F;
    }

    /** 这一次命中的上限 = 基准上限 × 落点部位权重（1.0 即身体）。权重只乘这一层，乘第二遍等于同一个优势算两次。 */
    protected float getVitalityDamageLimit(float weight) {
        float base = getVitalityDamageLimit();
        return weight > 0.0F ? base * weight : base;
    }

    protected final float getVitalityMaximum() {
        return verifiedVitality().maximum();
    }

    /** 我们自己那扇门的宽度。它只在原版 invulnerableTime 放行之后才被问到，两扇门是串联的。 */
    protected int getVitalityHitInterval() {
        return 20;
    }

    /** Maximum gap between consecutive strong-kill attempts; duplicates in one tick count once. */
    protected int getKillAttemptWindow() {
        return 100;
    }

    /** Called for a valid server-side damage attempt, before immunity/cooldown checks. */
    protected void onIncomingAttack(DamageSource source, float amount) {
    }

    /** Called after an incoming hit has passed immunity/cooldown checks and was accepted. */
    protected void onDamageAccepted(DamageSource source, float amount) {
    }

    /** 这一击的落点权重，用来缩放单次上限；它决定上限有多高，不决定伤害有多大。 */
    protected float incomingHitWeight(DamageSource source) {
        return 1.0F;
    }

    protected boolean isDamageImmune(DamageSource source) {
        return false;
    }

    /** 逐击修正（部位加成、适应、衰减等）。它在两道门之后、单次上限之前生效，越不过任何一道。 */
    protected float modifyIncomingDamage(DamageSource source, float amount) {
        return amount;
    }

    /** Two serial gates; nested/same-tick channels share the maximum final health loss. */
    @Override
    public final boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !vitality.joined
                || Float.isNaN(amount) || amount <= 0.0F) {
            return false;
        }
        onIncomingAttack(source, amount);
        if (isDamageImmune(source)) {
            return false;
        }
        long now = level().getGameTime();
        // 同一 tick 的追加伤害并入本批；两道门对本批只判一次。
        boolean sameTickBatch = tickHitAt == now;
        if (!sameTickBatch && invulnerableTime > 0) {
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
        if (!sameTickBatch && state.gateClosed(now)) {
            return false;
        }
        // 过了两道门就算挨到：适应在这里记账，早于上限与批内取最大的判定。
        amount = modifyIncomingDamage(source, amount);
        float limit = getVitalityDamageLimit(incomingHitWeight(source));
        if (!Float.isFinite(limit) || limit <= 0.0F) {
            return false;
        }
        float cappedAmount = Math.min(amount, limit);
        if (!Float.isFinite(cappedAmount) || cappedAmount <= 0.0F) {
            return false;
        }
        if (!sameTickBatch) {
            tickHitAt = now;
            tickLargestLoss = 0.0F;
        }
        DamageFrame frame = new DamageFrame(limit);
        vitality.frames.push(frame);
        boolean accepted;
        // 清零让 super.hurt 全额落地，而不是只算超出上一击的差额。
        invulnerableTime = 0;
        lastHurt = 0.0F;
        try {
            accepted = super.hurt(source, cappedAmount);
        } finally {
            vitality.frames.pop();
            publishVitality();
        }
        // Vanilla hurt() can return true even when LivingHurtEvent/LivingDamageEvent was canceled.
        accepted = accepted && frame.finalLoss > 0.0F;
        if (accepted) {
            commitVitality(verifiedVitality().afterAcceptedHit(now, getVitalityHitInterval()));
            onDamageAccepted(source, frame.finalLoss);
        }
        return accepted;
    }

    @Override
    protected final void actuallyHurt(DamageSource source, float amount) {
        DamageFrame frame = vitality.frames.peek();
        if (frame == null || frame.resolved || isInvulnerableTo(source) || getHealth() <= 0.0F) {
            return;
        }
        frame.resolved = true;
        // Native NeoForge order, with health committed to the authoritative vitality ledger.
        var container = damageContainers.peek();
        container.setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ARMOR,
                container.getNewDamage() - getDamageAfterArmorAbsorb(source, container.getNewDamage()));
        getDamageAfterMagicAbsorb(source, container.getNewDamage());
        float damage = CommonHooks.onLivingDamagePre(this, container);
        if (!Float.isFinite(damage)) return;
        container.setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION,
                Math.min(getAbsorptionAmount(), damage));
        float absorbed = Math.min(damage, container.getReduction(
                net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION));
        setAbsorptionAmount(Math.max(0.0F, getAbsorptionAmount() - absorbed));
        if (absorbed > 0.0F && absorbed < 3.4028235E37F && source.getEntity() instanceof ServerPlayer player) {
            player.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(absorbed * 10.0F));
        }
        float finalAmount = container.getNewDamage();
        if (!Float.isFinite(finalAmount) || finalAmount < 0.0F) return;
        frame.finalLoss = Math.min(frame.limit, finalAmount);
        float additionalLoss = Math.max(0.0F, frame.finalLoss - tickLargestLoss);
        tickLargestLoss = Math.max(tickLargestLoss, frame.finalLoss);
        if (additionalLoss > 0.0F) {
            float loss = Math.min(getHealth(), additionalLoss);
            if (loss > 0.0F) {
                getCombatTracker().recordDamage(source, loss);
                commitVitality(verifiedVitality().afterDamage(loss));
                gameEvent(GameEvent.ENTITY_DAMAGE);
                onDamageTaken(container);
            }
        }
        container.setNewDamage(additionalLoss);
        CommonHooks.onLivingDamagePost(this, container);
    }

    @Override
    public final void heal(float amount) {
        if (level().isClientSide || !vitality.joined || getHealth() <= 0.0F
                || !vitality.frames.isEmpty() || !Float.isFinite(amount) || amount <= 0.0F) {
            return;
        }
        amount = EventHooks.onLivingHeal(this, amount);
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
                if (dead) {
                    onFinalDeath(source);
                } else {
                    commitVitality(verifiedVitality().afterCanceledDeath());
                    deathTime = 0;
                }
            } finally {
                vitality.finishing = false;
            }
        }
    }

    /** Called once when the genuine death transaction has started. */
    protected void onFinalDeath(DamageSource source) {
    }

    /** Length of the authored death sequence; the entity is removed when it finishes. */
    protected int getDeathDurationTicks() {
        return 20;
    }

    /** Per tick death update for the authored death sequence, on both sides. */
    protected void onDeathTick(int ticks) {
    }

    @Override
    protected final void tickDeath() {
        if (getHealth() > 0.0F || (!level().isClientSide && !vitality.deathHandled)) {
            return;
        }
        deathTime++;
        onDeathTick(deathTime);
        if (deathTime >= Math.max(1, getDeathDurationTicks())
            && !level().isClientSide && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected final void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        // DamageProbe's post-zero finalizer also tries a reflective loot fallback.
        // Only the genuine death transaction may issue rewards, once.
        if (vitality.finishing && !vitality.lootHandled && getHealth() <= 0.0F) {
            vitality.lootHandled = true;
            super.dropAllDeathLoot(level, source);
        }
    }

    @Override
    public final void kill() {
        recordKillAttempt(damageSources().genericKill());
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && vitality.joined && getHealth() <= 0.0F
            && !vitality.deathHandled && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            die(lastKillSource());
            return;
        }
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
        if (isPassenger()) {
            stopRiding();
        }
        if (isVehicle()) {
            ejectPassengers();
        }
        if (!level().isClientSide && vitality.joined) {
            if (getHealth() > 0.0F) {
                dead = false;
                deathTime = 0;
            } else if (!vitality.deathHandled) {
                die(lastKillSource());
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

    /**
     * Body contact cannot displace the boss. Vanilla asks {@link #isPushable()} for both sides of a contact
     * ({@code EntitySelector#pushableBy} filters the candidates, {@code Entity#push} guards each shove) and then
     * adds that shove straight to the victim's delta movement, once per tick per overlapping entity -- which our
     * own movement blends into instead of overwriting, so leaning on the boss would walk it around. Only the
     * boss's own movement code may change its velocity; players touching it still get the usual separation.
     */
    @Override
    public final boolean isPushable() {
        return false;
    }

    /**
     * Ram attacks and other mods hand out impulses through this method directly, bypassing
     * {@link #isPushable()}: the ender dragon, ravager, hoglin charge, warden sonic boom and moving minecarts all
     * call it on their victim. None of them may move the boss either.
     */
    @Override
    public final void push(double x, double y, double z) {
    }

    /** Boss movement cannot be disabled by boats, minecarts, mounts or forced passengers. */
    @Override
    public final boolean startRiding(net.minecraft.world.entity.Entity vehicle, boolean force) {
        return false;
    }

    @Override
    protected final boolean canAddPassenger(net.minecraft.world.entity.Entity passenger) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected final boolean couldAcceptPassenger() {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (vitality.joined) {
            // Live NBT loading cannot replace the bound identity or health mirror.
            tag = tag.copy();
            tag.putFloat("Health", Math.max(1.0F, getHealth()));
            super.setUUID(vitality.identity);
        }
        super.readAdditionalSaveData(tag);
        if (!vitality.joined) {
            vitality.deathHandled = tag.getBoolean("VicissitudeDeathHandled");
            vitality.lootHandled = tag.getBoolean("VicissitudeLootHandled");
        }
        if (vitality.joined) {
            if (getHealth() > 0.0F) {
                dead = false;
                deathTime = 0;
            }
            publishVitality();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("VicissitudeDeathHandled", vitality.deathHandled);
        tag.putBoolean("VicissitudeLootHandled", vitality.lootHandled);
    }

    private static final class DamageFrame {
        private final float limit;
        private float finalLoss;
        private boolean resolved;

        private DamageFrame(float limit) {
            this.limit = limit;
        }
    }

    private static final class VitalityState {
        private UUID identity;
        private VicissitudeVitalityLedger ledger;
        private VicissitudeVitalityCapability capability;
        private VicissitudeVitality snapshot;
        private boolean joined;
        private final Deque<DamageFrame> frames = new ArrayDeque<>();
        private boolean finishing;
        private boolean deathHandled;
        private boolean lootHandled;
    }
}
