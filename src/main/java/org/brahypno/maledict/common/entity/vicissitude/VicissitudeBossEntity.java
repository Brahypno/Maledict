package org.brahypno.maledict.common.entity.vicissitude;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.ForgeEventFactory;
import org.brahypno.changelib.DamageHelper.DamageStage;
import org.brahypno.changelib.HealthHelper.CombatProtection;
import org.brahypno.changelib.protection.ai.AiProtection;
import org.brahypno.changelib.protection.ai.AiProtectionDefinition;
import org.brahypno.changelib.protection.ai.ProtectedAiEntity;
import org.brahypno.maledict.common.combat.BossDamage;
import org.brahypno.maledict.common.combat.BossRecovery;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Base for Vicissitude bosses; concrete entities supply registration, AI and visuals.
 * <p>
 * Do not call ChangeLib's DamageProbe from hurt(): its basic handler calls hurt again.
 */
public abstract class VicissitudeBossEntity extends PathfinderMob
        implements ProtectedAiEntity, CombatProtection.OwnedHealthEntity {
    public static final MobType VICISSITUDE = new MobType();
    /**
     * Default MAX_HEALTH; difficulty tables express their pool as a delta against this.
     */
    public static final double BASE_MAX_HEALTH = 1000.0D;
    private static final float MAXIMUM_HIT_WEIGHT = 1.25F;
    private static final float INITIAL_DISPLAY_VITALITY = 1.0F;
    private static final EntityDataAccessor<Float> DISPLAY_VITALITY =
            SynchedEntityData.defineId(VicissitudeBossEntity.class, EntityDataSerializers.FLOAT);
    /**
     * Bridges ChangeLib's defense to the authoritative vitality ledger; never calls getHealth.
     */
    private static final CombatProtection.HealthAccess VITALITY_ACCESS = new CombatProtection.HealthAccess() {
        @Override
        public float read(LivingEntity entity) {
            return ((VicissitudeBossEntity) entity).authorityHealth();
        }

        @Override
        public void write(LivingEntity entity, float health) {
            ((VicissitudeBossEntity) entity).writeAuthorityHealth(health);
        }

        @Override
        public boolean hurt(LivingEntity entity, DamageSource source, float amount) {
            return entity.hurt(source, amount);
        }
    };
    private final VitalityState vitality = new VitalityState();
    private final BossDamage damageBatch = new BossDamage();
    /**
     * Sole permission to commit the ledger; binds once, when the instance first joins.
     */
    private CombatProtection.Controller vitalityController;
    private AiProtection.Controller aiController;
    private float clientVitality = INITIAL_DISPLAY_VITALITY;
    /**
     * Game time of the last resolved loss; feeds the out-of-combat memory, where a hit with no
     * causing entity would otherwise leave no trace.
     */
    private long lastCombatTick = BossRecovery.NO_COMBAT_TICK;

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
        entityData.define(DISPLAY_VITALITY, INITIAL_DISPLAY_VITALITY);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == DISPLAY_VITALITY && level().isClientSide){
            clientVitality = entityData.get(DISPLAY_VITALITY);
        }
    }

    @Override
    public final MobType getMobType() {
        return VICISSITUDE;
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!(level() instanceof ServerLevel serverLevel)){
            return;
        }
        if (!vitality.joined){
            vitality.identity = getUUID();
            vitality.ledger = VicissitudeVitalityLedger.get(serverLevel);
            vitality.capability = getCapability(VicissitudeVitalityCapability.CAPABILITY)
                    .orElseThrow(() -> new IllegalStateException("Missing Vicissitude vitality capability"));
            VicissitudeVitality saved = vitality.ledger.find(vitality.identity);
            // An existing world record means this instance is resuming a boss that was already in the
            // world; a fresh UUID is a summon or a first spawn. Only the former gets a load grace.
            vitality.resumed = saved != null;
            if (saved == null){
                // Captured once per UUID; reloading must not heal wounds or recapture attributes.
                float maximum = initialVitalityMaximum();
                saved = VicissitudeVitality.initial(
                        Float.isFinite(maximum) && maximum > 0.0F ? maximum : 1000.0F);
            }
            vitality.joined = true;
            commitVitality(saved);
        }
        bindVitalityProtection();
        bindAiProtection();
    }

    /**
     * The owned binding checks the adapter by identity, so no external caller can bind a different
     * pool before us. This is the only place the defense is attached.
     */
    @Override
    public final boolean ownsHealthAccess(CombatProtection.HealthAccess candidate) {
        return candidate == VITALITY_ACCESS;
    }

    private void bindVitalityProtection() {
        if (vitalityController == null){
            CombatProtection.Controller controller = CombatProtection.bindOwned(
                    this, combatProtectionGrade(), VITALITY_ACCESS,
                    getVitalityDamageLimit(MAXIMUM_HIT_WEIGHT));
            // The hit rhythm is the binding's own time gate: ChangeLib stamps it where this boss
            // commits a landed loss, so our damage code and a stray write read the same cadence.
            controller.interval(getVitalityHitInterval());
            // A boss resuming an existing world record is reachable for the first time; hold the same
            // gate so the first hit of that moment is refused instead of free, like a player's join
            // i-frames. A summon is not a resume: its rite already carries its own immunity.
            if (vitality.resumed){
                controller.hold(getLoadGraceTicks());
            }
            vitalityController = controller;
        }
    }

    /**
     * Health defense grade for this boss. Read once when the instance binds; a subclass may override
     * it so different bosses carry different protection levels.
     */
    protected DamageStage combatProtectionGrade() {
        return DamageStage.D4_CONSISTENCY_WRITE;
    }

    /**
     * One definition for every Vicissitude boss; only the target rule is entity-specific.
     */
    @Override
    public final AiProtectionDefinition aiProtectionDefinition() {
        return new AiProtectionDefinition(true, true, true,
                                          (self, candidate) -> canHoldAiTarget(candidate));
    }

    /**
     * Overridable part of the AI target rule; a candidate must pass this to become the mob target.
     */
    protected boolean canHoldAiTarget(LivingEntity candidate) {
        return candidate != this && !candidate.isRemoved() && candidate.isAlive()
               && candidate.level() == level();
    }

    private void bindAiProtection() {
        if (level().isClientSide || aiController != null){
            return;
        }
        aiController = AiProtection.bind(this);
    }

    /**
     * The only path by which a boss may change its own target once AI protection is bound. External
     * {@code setTarget} calls are rejected by ChangeLib; this keeps Forge from seeing a redundant
     * target event when the value did not change.
     */
    protected final void selectAiTarget(@Nullable LivingEntity target) {
        if (getTarget() == target){
            return;
        }
        AiProtection.Controller controller = aiController;
        if (controller == null){
            super.setTarget(target);
        }else {
            controller.selectTarget(target);
        }
    }

    private float authorityHealth() {
        return verifiedVitality().current();
    }

    private void writeAuthorityHealth(float health) {
        if (!Float.isFinite(health)){
            return;
        }
        VicissitudeVitality current = verifiedVitality();
        float clamped = Math.max(0.0F, Math.min(current.maximum(), health));
        if (clamped == current.current()){
            return;
        }
        writeAuthority(current.withCurrent(clamped));
    }

    /**
     * Initial authoritative pool; vanilla's ranged attribute may cap a larger authored pool.
     */
    protected float initialVitalityMaximum() {
        return getMaxHealth();
    }

    /**
     * Pins the first-capture pool to the larger of the difficulty table and the final {@code MAX_HEALTH}
     * that is already in effect when the instance joins the world. The base class reads it in
     * {@link #onAddedToWorld()} after each boss has applied its own difficulty attribute, so a spawn
     * modifier from another mod (L2Hostility and friends) lands first and is absorbed here; a
     * reduction cannot drag the pool below the difficulty value. Captured exactly once per UUID: an
     * existing world record is never recaptured, and later attribute changes never resize the pool.
     */
    protected final float initialVitalityMaximum(float difficultyMaximum) {
        return VicissitudeVitality.initialMaximum(difficultyMaximum, getMaxHealth());
    }

    @Override
    public final float getHealth() {
        // LivingEntity's constructor invokes virtual methods before our holder exists.
        if (vitality == null){
            return super.getHealth();
        }
        if (level().isClientSide){
            // The tracked display is a fallback until ChangeLib's committed health arrives.
            // Keep the fallback cached so ChangeLib cannot capture it as a getter dependency.
            return CombatProtection.healthView(this, clientVitality);
        }
        if (!vitality.joined){
            return super.getHealth();
        }
        // healthView also reconciles: an unowned raw change is repaired back to the committed value,
        // so reads cannot hand out a tampered pool.
        return CombatProtection.healthView(this, authorityHealth());
    }

    /**
     * Setters submit a damage request to ChangeLib. Below-grade requests reenter hurt(), and only
     * the resolved loss is committed; construction keeps LivingEntity's normal initialization.
     */
    @Override
    public final void setHealth(float value) {
        if (vitality == null){
            super.setHealth(value);
        }else if (!level().isClientSide && vitality.joined){
            CombatProtection.healthWrite(this, value, () -> {});
        }
    }

    /**
     * 全身的单次命中基准上限，也就是身体（权重 1.0）那一条。
     */
    protected float getVitalityDamageLimit() {
        return getVitalityMaximum() * 0.01F;
    }

    /**
     * 这一次命中的上限 = 基准上限 × 落点部位权重（1.0 即身体）。权重只乘这一层，乘第二遍等于同一个优势算两次。
     */
    protected float getVitalityDamageLimit(float weight) {
        float base = getVitalityDamageLimit();
        return weight > 0.0F ? base * weight : base;
    }

    protected final float getVitalityMaximum() {
        return verifiedVitality().maximum();
    }

    /**
     * 这一击节奏门的宽度，挂在 ChangeLib 的血量绑定上。它只在原版 invulnerableTime 放行之后才被问到，
     * 两扇门仍然是串联的；门本身不再由本类记账，见 {@link #getSpawnGraceTicks()}。
     */
    protected int getVitalityHitInterval() {
        return 20;
    }

    /**
     * 这个实例从存档里读回来时的一段无敌。节奏是活状态，不写存档：读档后的第一下应该被挡下，
     * 而不是白挨。召唤不在此列——仪式本身有自己的免疫。
     */
    protected int getLoadGraceTicks() {
        return 20;
    }

    /**
     * 为“这一击够重、能压过节奏”之类的特定场合，削掉当前无敌帧的一部分剩余量。
     *
     * <p>这是**攻击侧**的操作：附魔、技能、重击都可以对目标直接调用
     * {@link CombatProtection#shortenRhythm(net.minecraft.world.entity.LivingEntity, int)}，不需要经过
     * 这个 Boss。这里只是给子类留一个不用写类名的入口。削的是正在跑的剩余量，
     * {@link #getVitalityHitInterval()} 的配置值不动：削完之后下一次命中仍从完整窗口开始，削穿即开口。
     */
    protected final void shortenHitWindow(int ticks) {
        if (ticks > 0 && !level().isClientSide){
            CombatProtection.shortenRhythm(this, ticks);
        }
    }

    /**
     * Maximum gap between consecutive strong-kill attempts; duplicates in one tick count once.
     */
    protected int getKillAttemptWindow() {
        return 100;
    }

    /**
     * Called for a valid server-side damage attempt, before immunity/cooldown checks.
     */
    protected void onIncomingAttack(DamageSource source, float amount) {
    }

    /**
     * Called after an incoming hit has passed immunity/cooldown checks and was accepted.
     */
    protected void onDamageAccepted(DamageSource source, float amount) {
    }

    /**
     * 这一击的落点权重，用来缩放单次上限；它决定上限有多高，不决定伤害有多大。
     */
    protected float incomingHitWeight(DamageSource source) {
        return 1.0F;
    }

    protected boolean isDamageImmune(DamageSource source) {
        return false;
    }

    /**
     * 逐击修正（部位加成、适应、衰减等）。它在两道门之后、单次上限之前生效，越不过任何一道。
     */
    protected float modifyIncomingDamage(DamageSource source, float amount) {
        return amount;
    }

    /**
     * Vanilla's i-frame in series with the binding's ChangeLib time gate; nested/same-tick channels
     * share the maximum final health loss.
     */
    @Override
    public final boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || !vitality.joined
            || Float.isNaN(amount) || amount <= 0.0F){
            return false;
        }
        onIncomingAttack(source, amount);
        if (isDamageImmune(source)){
            return false;
        }
        long now = level().getGameTime();
        // 同一 tick 的追加伤害并入本批；两道门对本批只判一次。
        boolean sameTickBatch = damageBatch.isCurrentTick(now);
        if (!sameTickBatch && invulnerableTime > 0){
            return false;
        }
        VicissitudeVitality state = verifiedVitality();
        if (state.current() <= 0.0F){
            return false;
        }
        if (source.is(DamageTypes.GENERIC_KILL)){
            recordKillAttempt(source);
            return getHealth() <= 0.0F;
        }
        // ChangeLib owns the window: it is stamped where this boss commits a landed loss, so a
        // hidden write and an ordinary hit answer the same question.
        if (!sameTickBatch && CombatProtection.rhythmRemaining(this) > 0){
            return false;
        }
        // 过了两道门就算挨到：适应在这里记账，早于上限与批内取最大的判定。
        amount = modifyIncomingDamage(source, amount);
        float limit = getVitalityDamageLimit(incomingHitWeight(source));
        if (!Float.isFinite(limit) || limit <= 0.0F){
            return false;
        }
        float cappedAmount = Math.min(amount, limit);
        if (!Float.isFinite(cappedAmount) || cappedAmount <= 0.0F){
            return false;
        }
        damageBatch.beginTick(now);
        DamageFrame frame = new DamageFrame(limit);
        vitality.frames.push(frame);
        boolean accepted;
        // 清零让 super.hurt 全额落地，而不是只算超出上一击的差额。
        invulnerableTime = 0;
        lastHurt = 0.0F;
        try {
            accepted = super.hurt(source, cappedAmount);
        }
        finally {
            vitality.frames.pop();
            publishVitality();
        }
        // Vanilla hurt() can return true even when LivingHurtEvent/LivingDamageEvent was canceled.
        accepted = accepted && frame.finalLoss > 0.0F;
        if (accepted){
            // ChangeLib restarted the gate when this loss was committed; there is no second copy of
            // the cadence here, so the accepted branch only reports the hit.
            onDamageAccepted(source, frame.finalLoss);
        }
        return accepted;
    }

    @Override
    protected final void actuallyHurt(DamageSource source, float amount) {
        DamageFrame frame = vitality.frames.peek();
        // Use the authority, not getHealth(): the protected view would clamp a stale witness before
        // this hit's own verdict. Defense reconciliation belongs to external reads, not our pipeline.
        VicissitudeVitality state = verifiedVitality();
        if (frame == null || frame.resolved || isInvulnerableTo(source) || state.current() <= 0.0F){
            return;
        }
        frame.resolved = true;
        // No setter transaction is opened around mod callbacks; a callback's setHealth cannot spend it.
        float finalAmount = BossDamage.resolve(this, source, amount,
                                               damage -> getDamageAfterMagicAbsorb(source, getDamageAfterArmorAbsorb(source, damage)));
        if (finalAmount <= 0.0F)
            return;
        frame.finalLoss = Math.min(frame.limit, finalAmount);
        float additionalLoss = damageBatch.additionalLoss(frame.finalLoss);
        if (additionalLoss > 0.0F){
            float current = verifiedVitality().current();
            float loss = Math.min(current, additionalLoss);
            if (loss > 0.0F){
                getCombatTracker().recordDamage(source, loss);
                applyAuthorityDamage(current, loss);
                setAbsorptionAmount(getAbsorptionAmount() - loss);
                gameEvent(GameEvent.ENTITY_DAMAGE);
            }
        }
    }

    /**
     * Commits the resolved loss through the same grade and request allowance used by other writes.
     */
    private void applyAuthorityDamage(float current, float loss) {
        float target = Math.max(0.0F, current - loss);
        if (target >= current){
            return;
        }
        lastCombatTick = level().getGameTime();
        VicissitudeVitality next = verifiedVitality().withCurrent(target);
        vitalityController.commitDamage(() -> writeAuthority(next));
        publishVitality();
    }

    @Override
    public final void heal(float amount) {
        if (level().isClientSide || isRemoved() || !vitality.joined || getHealth() <= 0.0F
            || !vitality.frames.isEmpty() || !Float.isFinite(amount) || amount <= 0.0F){
            return;
        }
        amount = ForgeEventFactory.onLivingHeal(this, amount);
        if (Float.isFinite(amount) && amount > 0.0F){
            VicissitudeVitality state = verifiedVitality();
            if (state.current() > 0.0F){
                commitVitality(state.withCurrent(Math.min(state.maximum(), state.current() + amount)));
            }
        }
    }

    /**
     * Whether the pool may refill while the boss holds no target. A scripted beat can clear the
     * target without the fight being over, and such a beat must not hand the whole pool back.
     */
    protected boolean recoversOutOfCombat() {
        return true;
    }

    @Override
    public final void die(DamageSource source) {
        if (level().isClientSide || isRemoved() || vitality == null || !vitality.joined){
            return;
        }
        // Direct die() is not a kill attempt and cannot open the bypass.
        if (getHealth() <= 0.0F && !vitality.finishing && !vitality.deathHandled){
            vitality.finishing = true;
            try {
                // Probes can set the vanilla flag without running death processing.
                dead = false;
                super.die(source);
                vitality.deathHandled = dead;
                if (dead){
                    onFinalDeath(source);
                }else {
                    commitVitality(verifiedVitality().afterCanceledDeath());
                    deathTime = 0;
                }
            }
            finally {
                vitality.finishing = false;
            }
        }
    }

    /**
     * Called once when the genuine death transaction has started.
     */
    protected void onFinalDeath(DamageSource source) {
    }

    /**
     * Length of the authored death sequence; the entity is removed when it finishes.
     */
    protected int getDeathDurationTicks() {
        return 20;
    }

    /**
     * Per tick death update for the authored death sequence, on both sides.
     */
    protected void onDeathTick(int ticks) {
    }

    @Override
    protected final void tickDeath() {
        if (getHealth() > 0.0F || (!level().isClientSide && !vitality.deathHandled)){
            return;
        }
        deathTime++;
        onDeathTick(deathTime);
        if (deathTime >= Math.max(1, getDeathDurationTicks())
            && !level().isClientSide && !isRemoved()){
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected final void dropAllDeathLoot(DamageSource source) {
        // DamageProbe's post-zero finalizer also tries a reflective loot fallback.
        // Only the genuine death transaction may issue rewards, once.
        if (vitality.finishing && !vitality.lootHandled && getHealth() <= 0.0F){
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
        if (!level().isClientSide && vitality.joined && getHealth() <= 0.0F
            && !vitality.deathHandled && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)){
            die(lastKillSource());
            return;
        }
        if (!level().isClientSide && getHealth() > 0.0F
            && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)){
            if (reason == RemovalReason.KILLED){
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
        if (isPassenger()){
            stopRiding();
        }
        if (isVehicle()){
            ejectPassengers();
        }
        boolean clientSide = level().isClientSide;
        if (clientSide || vitality.joined){
            if (getHealth() > 0.0F){
                dead = false;
                deathTime = 0;
            }else if (!clientSide && !vitality.deathHandled){
                die(lastKillSource());
            }
            if (!clientSide){
                publishVitality();
            }
        }
        if (!clientSide && vitality.joined && recoversOutOfCombat()){
            // Out of combat: no target, no remembered attacker, no recent damage, so 5% per tick.
            BossRecovery.tick(this, getVitalityMaximum(), lastCombatTick);
        }
        super.tick();
    }

    private void publishVitality() {
        // SynchedEntityData compares the actual slot, including any change since the last publish.
        entityData.set(DISPLAY_VITALITY, verifiedVitality().current());
    }

    /**
     * The independent world record wins; entity NBT and ForgeCaps are not independent votes.
     */
    private VicissitudeVitality verifiedVitality() {
        VicissitudeVitality authority = Objects.requireNonNull(vitality.ledger.find(vitality.identity),
                                                               "Missing committed Vicissitude vitality");
        if (!authority.equals(vitality.snapshot)){
            vitality.snapshot = authority;
        }
        if (!authority.equals(vitality.capability.snapshot())){
            vitality.capability.synchronize(authority);
        }
        return vitality.snapshot;
    }

    /**
     * Commits one complete record. The controller callback must stay a raw ledger update: events,
     * rewards and display publishing belong outside it.
     */
    private void commitVitality(VicissitudeVitality state) {
        CombatProtection.Controller controller = vitalityController;
        if (controller == null){
            writeAuthority(state); // Initial commit before the instance joins and binds.
        }else {
            controller.commit(() -> writeAuthority(state));
        }
        publishVitality();
    }

    /**
     * Same-package repair hook for world-record edits.
     */
    final void restoreAuthority(VicissitudeVitality state) {
        commitVitality(state);
    }

    /**
     * Raw ledger/capability update; no publishing, guards or controller calls.
     */
    private void writeAuthority(VicissitudeVitality state) {
        vitality.ledger.commit(vitality.identity, state);
        vitality.snapshot = state;
        vitality.capability.synchronize(state);
    }

    private DamageSource lastKillSource() {
        DamageSource source = getLastDamageSource();
        return source == null ? damageSources().genericKill() : source;
    }

    private void recordKillAttempt(DamageSource source) {
        if (level().isClientSide || isRemoved() || !vitality.joined || vitality.finishing){
            return;
        }
        VicissitudeVitality state = verifiedVitality();
        if (state.current() > 0.0F){
            commitVitality(state.killAttempt(level().getGameTime(), getKillAttemptWindow()));
        }
        if (getHealth() <= 0.0F){
            die(source);
        }
    }

    @Override
    public final void setUUID(UUID identity) {
        // Allow normal construction/loading, then bind this instance to its original record.
        super.setUUID(vitality != null && vitality.joined ? vitality.identity : identity);
    }

    /**
     * Vicissitude bosses must remain functional even if NoAI is supplied through commands or NBT.
     */
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

    /**
     * Boss movement cannot be disabled by boats, minecarts, mounts or forced passengers.
     */
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
        if (vitality.joined){
            // Live NBT loading cannot replace the bound identity or health mirror.
            tag = tag.copy();
            tag.putFloat("Health", Math.max(1.0F, getHealth()));
            super.setUUID(vitality.identity);
        }
        super.readAdditionalSaveData(tag);
        if (!vitality.joined){
            vitality.deathHandled = tag.getBoolean("VicissitudeDeathHandled");
            vitality.lootHandled = tag.getBoolean("VicissitudeLootHandled");
        }
        if (vitality.joined){
            if (getHealth() > 0.0F){
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
        private boolean resumed;
        private boolean finishing;
        private boolean deathHandled;
        private boolean lootHandled;
    }
}
