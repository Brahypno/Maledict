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
    /**
     * The attribute default every Vicissitude boss starts from. Difficulty tables express their
     * pool as a delta against this, and the ledger captures whatever the attribute ends up at.
     */
    public static final double BASE_MAX_HEALTH = 1000.0D;
    private static final EntityDataAccessor<Float> DISPLAY_VITALITY =
            SynchedEntityData.defineId(VicissitudeBossEntity.class, EntityDataSerializers.FLOAT);
    // Transient combat transaction state is separate from the immutable committed values.
    private final VitalityState vitality = new VitalityState();
    // 同一个 tick 里的一批命中只取最大的一刀，见 #hurt 与 #flushPendingHit。
    private float tickLargestHit;
    private float tickAppliedDamage;
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
            if (vitality.writing > 0) {
                VicissitudeVitality state = verifiedVitality();
                float loss = Math.min(vitality.budget, Math.max(0.0F, state.current() - value));
                if (loss > 0.0F) {
                    vitality.budget -= loss;
                    commitVitality(state.afterDamage(loss));
                }
            }
        }
    }

    /**
     * 全身的单次命中基准上限，也就是身体（权重 1.0）那一条。
     *
     * <p>具体到一次命中还要按部位缩放，见 {@link #getVitalityDamageLimit(float)}：容易打中、
     * 也容易打出伤害的部位允许吃下更多，翼这类次要部位则更低。这里只管"标准无倍率"的那个数。
     */
    protected float getVitalityDamageLimit() {
        return getVitalityMaximum() * 0.01F;
    }

    /**
     * 这一次命中适用的上限：基准上限乘上落点部位的权重。
     *
     * <p>权重由子类给出（无常那边就是 {@code Segment#capWeight()}），1.0 即身体。<b>只留这一
     * 层</b>：部位的优势全在权重里，这里钳一次就够，再在上限之上乘一档倍率等于同一个优势算两遍，
     * 上限高的部位会先被自己的倍率顶到顶，区别当场消失。
     */
    protected float getVitalityDamageLimit(float weight) {
        float base = getVitalityDamageLimit();
        return weight > 0.0F ? base * weight : base;
    }

    protected final float getVitalityMaximum() {
        return verifiedVitality().maximum();
    }

    /**
     * 我们自己那扇门的宽度：两次有效命中之间至少隔多少 tick。
     *
     * <p>它只在原版那一扇放行之后才被问到，所以这个数比原版的无敌帧短也不会更宽松——
     * 两扇门是串联的，谁都能单独拦下。
     */
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

    /** Called after an incoming hit has passed immunity/cooldown checks and was accepted. */
    protected void onDamageAccepted(DamageSource source, float amount) {
    }

    /**
     * 这一击落在哪个部位，用它的权重去缩放单次上限；返回 1.0 即按身体的基准上限算。
     *
     * <p>它决定的是"上限有多高"，不是"伤害有多大"：两者分开之后，部位差异只体现在允许吃下
     * 多少，而不是每一击都先被乘一遍。
     */
    protected float incomingHitWeight(DamageSource source) {
        return 1.0F;
    }

    /** Phase implementations can reject an otherwise valid hit without losing attacker information. */
    protected boolean isDamageImmune(DamageSource source) {
        return false;
    }

    /**
     * 部位加成、非玩家半伤、适应、距离衰减这类逐击修正。它在两道门之后、单次上限之前生效，
     * 所以再怎么改大也越不过门，也越不过上限。
     */
    protected float modifyIncomingDamage(DamageSource source, float amount) {
        return amount;
    }

    /**
     * 两扇门串起来，谁都能单独拦下这一刀：<b>原版的 {@code invulnerableTime} 先判，它可以自己
     * 拦；它放行之后，还要我们那一扇也放行</b>，两条都过才真的扣血。
     *
     * <p>两扇门都<b>只对本 tick 的第一刀设卡</b>。同一 tick 里跟着落下来的追加伤害（提尔锋的
     * voodoo、多重射击的第二支箭……）是同一刀带出来的一批，放进同一个批次处理：批内只算
     * 最大的那一刀（差额先补、下一 tick 开头再由 {@link #flushPendingHit()} 补齐），所以
     * 它们既不会被第一刀刚关上的门吞掉，也不会叠加成多份伤害。
     *
     * <p>被任何一扇挡下都是 {@code return false}，不闪红、不击退、没有音效，因为
     * {@code super.hurt} 根本不会被调用；两扇都放行的这一刀，闪红照旧由原版的
     * {@code markHurt} 给。
     */
    @Override
    public final boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !vitality.joined
                || Float.isNaN(amount) || amount <= 0.0F) {
            return false;
        }
        onIncomingAttack(source, amount);
        if (isDamageImmune(source)) {
            // 阶段免疫：连适应都不记——这一刀不属于"打进来了"。
            return false;
        }
        long now = level().getGameTime();
        // 同一 tick 的后续命中（提尔锋的 voodoo 追加、多重射击的第二支箭……）算这一批的成员：
        // 那是同一刀带出来的伤害，不该被"这一刀刚把门关上"再挡一次——批内取最大值那条规则
        // 已经把它们收成一次扣血，多余的部分不会叠加。两道门对本批的判定都只做一次。
        boolean sameTickBatch = tickHitAt == now;
        if (!sameTickBatch && invulnerableTime > 0) {
            // 被门挡下的刀不记适应：门说"这一刀不算数"，那它就没挨到。
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
        // 第二扇门：同样只对本 tick 的第一刀设卡。
        if (!sameTickBatch && state.gateClosed(now)) {
            return false;
        }
        // 记账在这一步里（{@code DamageAdaptation}），排在下面所有"要不要真的扣血"的判定之前：
        // 只要过了两道门，这一刀就算"挨到了"，哪怕后面被单次上限或批内取最大丢掉，也照样进记录。
        amount = modifyIncomingDamage(source, amount);
        float limit = getVitalityDamageLimit(incomingHitWeight(source));
        if (!Float.isFinite(limit) || limit <= 0.0F) {
            return false;
        }
        float cappedAmount = Math.min(amount, limit);
        if (!Float.isFinite(cappedAmount) || cappedAmount <= 0.0F) {
            return false;
        }
        // 同一 tick 的后续命中：只有更大的一刀才补差额，更小的直接不算。
        float appliedAmount = sameTickBatch
                              ? Math.max(0.0F, cappedAmount - tickAppliedDamage)
                              : cappedAmount;
        if (sameTickBatch && appliedAmount <= 0.0F) {
            return false;
        }
        tickHitAt = now;
        tickLargestHit = sameTickBatch ? Math.max(tickLargestHit, cappedAmount) : cappedAmount;
        tickAppliedDamage += appliedAmount;
        // 计数式事务：主刀的 super.hurt 里会触发 LivingHurtEvent，追加伤害（voodoo/freeze）
        // 会再进一次 hurt()，于是事务是嵌套的。用布尔的话，内层 finally 会把外层的状态清掉，
        // 外层那一刀的扣血就被整条跳过（血量纹丝不动就是这么来的）。所以用深度计数，
        // 只有最外层退出时才真正复位。
        vitality.receiving++;
        vitality.budget = appliedAmount;
        boolean accepted;
        // 两扇都过了才走到这里：清掉原版那两个值只是让 super.hurt 走"全额落地"那一支，
        // 而不是原版那种"只算超出上一击的差额"。
        invulnerableTime = 0;
        lastHurt = 0.0F;
        try {
            accepted = super.hurt(source, appliedAmount);
        } finally {
            if (--vitality.receiving <= 0) {
                vitality.receiving = 0;
                vitality.writing = 0;
                vitality.budget = 0.0F;
                publishVitality();
            }
        }
        if (accepted) {
            // 同一 tick 的整批命中共用一个开门时刻，所以这里重复提交也是同一个值。
            commitVitality(verifiedVitality().afterAcceptedHit(now, getVitalityHitInterval()));
            onDamageAccepted(source, appliedAmount);
        } else {
            tickAppliedDamage -= appliedAmount;
            if (tickAppliedDamage <= 0.0F) {
                tickHitAt = Long.MIN_VALUE;
                tickLargestHit = 0.0F;
            }
        }
        return accepted;
    }

    /**
     * 把这一 tick 那一批命中补齐到最大值，下一 tick 开头调用。
     *
     * <p>差额记账都在 {@link #hurt} 里做完，这里只负责收尾：如果批内最后一刀就是最大的那刀，
     * 这一步什么也不做。放在 tick 开头而不是别处，是为了让"同一 tick"这个词有一致的边界，
     * 不用去碰原版的调用顺序。
     */
    private void flushPendingHit() {
        if (tickHitAt == Long.MIN_VALUE) {
            return;
        }
        float pending = tickLargestHit - tickAppliedDamage;
        tickHitAt = Long.MIN_VALUE;
        tickLargestHit = 0.0F;
        tickAppliedDamage = 0.0F;
        if (pending <= 0.0F) {
            return;
        }
        VicissitudeVitality state = verifiedVitality();
        float loss = Math.min(pending, state.current());
        if (loss > 0.0F) {
            commitVitality(state.afterDamage(loss));
        }
    }

    @Override
    protected final void actuallyHurt(DamageSource source, float amount) {
        // Reflectively invoking actuallyHurt outside an accepted hit cannot deduct vitality.
        // 嵌套是合法的（主刀的 super.hurt 里还会发 voodoo），所以这里只看"在不在已接受的事务里"。
        if (vitality.receiving <= 0) {
            return;
        }
        vitality.writing++;
        try {
            super.actuallyHurt(source, amount);
        } finally {
            vitality.writing--;
        }
    }

    @Override
    public final void heal(float amount) {
        if (level().isClientSide || !vitality.joined || getHealth() <= 0.0F
                || vitality.receiving > 0 || !Float.isFinite(amount) || amount <= 0.0F) {
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
                if (dead) {
                    onFinalDeath(source);
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
        if (getHealth() > 0.0F) {
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
        if (isPassenger()) {
            stopRiding();
        }
        if (isVehicle()) {
            ejectPassengers();
        }
        if (!level().isClientSide && vitality.joined) {
            // 上一 tick 那一批命中到此为止，补齐到最大值。
            flushPendingHit();
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
        /** 事务深度：主刀里还会发 voodoo，所以是嵌套计数而不是布尔。 */
        private int receiving;
        private int writing;
        private boolean finishing;
        private boolean deathHandled;
        private boolean lootHandled;
    }
}
