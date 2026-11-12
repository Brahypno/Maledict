package org.brahypno.maledict.common.realm.wither;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.brahypno.changelib.DamageHelper.DamageStage;
import org.brahypno.changelib.HealthHelper.CombatProtection;
import org.brahypno.changelib.protection.ai.AiProtection;
import org.brahypno.changelib.protection.ai.AiProtectionDefinition;
import org.brahypno.changelib.protection.ai.ProtectedAiEntity;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.common.combat.BossDamage;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.EnumSet;
import org.joml.Vector3f;

public class WeepingWitherEntity extends WitherBoss implements ProtectedAiEntity, CombatProtection.OwnedHealthEntity {
    public static final float SCALE = 1.2F;
    private static final DustParticleOptions SEEP_DUST = new DustParticleOptions(new Vector3f(0.25F, 0.21F, 0.29F), 0.6F);
    private static final DustParticleOptions BONE_DUST = new DustParticleOptions(new Vector3f(0.55F, 0.53F, 0.54F), 0.5F);
    private static final EntityDataAccessor<Boolean> REFORMING =
            SynchedEntityData.defineId(WeepingWitherEntity.class, EntityDataSerializers.BOOLEAN);
    private static final CombatProtection.HealthAccess HEALTH_ACCESS = new CombatProtection.HealthAccess() {
        @Override
        public float read(LivingEntity entity) {
            return ((WeepingWitherEntity) entity).authorityHealth;
        }

        @Override
        public void write(LivingEntity entity, float health) {
            WeepingWitherEntity wither = (WeepingWitherEntity) entity;
            wither.authorityHealth = wither.isReforming() ? Math.max(1.0F, health) : health;
        }

        @Override
        public boolean hurt(LivingEntity entity, DamageSource source, float amount) {
            return entity.hurt(source, amount);
        }
    };
    private final WeepingWitherCombat combat = new WeepingWitherCombat();
    private final WeepingWitherSkillController skills = new WeepingWitherSkillController(this);
    private static final TargetingConditions SIDE_HEAD_TARGETS = TargetingConditions.forCombat().range(20.0D)
            .selector(target -> target.getMobType() != MobType.UNDEAD && target.attackable());
    private final int[] nextSideShot = new int[2];
    private final int[] idleSideShots = new int[2];
    private int finalHeadCooldown;
    private int destroyBlocksTicks;
    private final BossDamage damageBatch = new BossDamage();
    private float tickLargestHurt;
    @Nullable private BlockPos altarHome;
    private boolean returningHome;
    @Nullable private AiProtection.Controller aiController;
    @Nullable private CombatProtection.Controller healthController;
    private float authorityHealth;
    // The parent's private bar cannot be updated while its attack AI is paused.
    private final ServerBossEvent encounterBar = (ServerBossEvent) new ServerBossEvent(
            getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS).setDarkenScreen(true);

    public WeepingWitherEntity(EntityType<? extends WeepingWitherEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WitherBoss.createAttributes().add(Attributes.MAX_HEALTH, 400.0D)
                .add(Attributes.ARMOR, 8.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(REFORMING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Vanilla target goals write setTarget directly; replace them with controller submissions
        // before AI protection snapshots the selectors.
        targetSelector.removeAllGoals(goal -> goal instanceof HurtByTargetGoal
                || goal instanceof NearestAttackableTargetGoal<?>);
        targetSelector.addGoal(1, new WeepingWitherHurtByTargetGoal(this));
        targetSelector.addGoal(2, new WeepingWitherNearestTargetGoal(this));
        goalSelector.addGoal(1, new ReturnToAltarGoal());
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide) {
            if (aiController == null) aiController = AiProtection.bind(this);
            if (healthController == null) {
                authorityHealth = super.getHealth();
                // The hurt path owns the cap, which follows the live max-health attribute.
                healthController = CombatProtection.bindOwned(
                        this, DamageStage.D3_STORAGE_WRITE, HEALTH_ACCESS, Float.POSITIVE_INFINITY);
            }
        }
    }

    @Override
    public final boolean ownsHealthAccess(CombatProtection.HealthAccess candidate) {
        return candidate == HEALTH_ACCESS;
    }

    @Override
    public float getHealth() {
        if (level().isClientSide) {
            float health = super.getHealth();
            return isReforming() ? Math.max(1.0F, health) : health;
        }
        return healthController == null ? super.getHealth() : CombatProtection.healthView(this, authorityHealth);
    }

    private void commitHealth(float health) {
        if (healthController == null) {
            super.setHealth(health);
        } else {
            healthController.commit(() -> authorityHealth = health);
        }
    }

    @Override
    public final AiProtectionDefinition aiProtectionDefinition() {
        return new AiProtectionDefinition(true, true, true,
                (self, candidate) -> canHoldAiTarget(candidate));
    }

    private boolean canHoldAiTarget(LivingEntity candidate) {
        return candidate != this && !candidate.isRemoved() && candidate.isAlive()
                && candidate.level() == level()
                && !isAlliedTo(candidate) && canAttackType(candidate.getType())
                && canAttack(candidate);
    }

    private void selectAiTarget(@Nullable LivingEntity target) {
        if (getTarget() == target) {
            return;
        }
        AiProtection.Controller controller = aiController;
        if (controller == null) {
            super.setTarget(target);
        } else {
            controller.selectTarget(target);
        }
    }

    /** Package-private bridge for this entity's target goals; the controller itself stays private. */
    void selectTargetFromGoal(@Nullable LivingEntity target) {
        selectAiTarget(target);
    }

    public void setAltarHome(BlockPos pos) {
        altarHome = pos.immutable();
        restrictTo(altarHome, 24);
    }

    private boolean withinHome(LivingEntity entity, double radius) {
        if (altarHome == null) return true;
        double dx = entity.getX() - (altarHome.getX() + 0.5D);
        double dz = entity.getZ() - (altarHome.getZ() + 0.5D);
        return dx * dx + dz * dz <= radius * radius;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isReforming() && !returningHome && withinHome(target, 24.0D) && super.canAttack(target);
    }

    public boolean isReforming() {
        return entityData.get(REFORMING);
    }

    /** True from the end of the one-time reformation onwards; the final phase never reforms again. */
    public boolean isFinalPhase() {
        return combat.reformationUsed() && !isReforming();
    }

    public boolean isHalfHealthEmpowered() {
        return skills.isEmpowered();
    }

    public boolean isReturningHome() {
        return returningHome;
    }

    void onDangerousSkullBlockHit(BlockHitResult hit) {
        skills.onDangerousSkullBlockHit(hit);
    }

    private void clearDeathAnimation() {
        dead = false;
        deathTime = 0;
        setPose(Pose.STANDING);
    }

    private void restoreReformingState() {
        if (!level().isClientSide && getHealth() <= 0.0F) commitHealth(1.0F);
        clearDeathAnimation();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (REFORMING.equals(key)) {
            if (isReforming()) restoreReformingState();
            else if (getHealth() > 0.0F) clearDeathAnimation();
        }
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == 3 && isReforming()) {
            restoreReformingState();
            return;
        }
        super.handleEntityEvent(event);
    }

    @Override
    public void tick() {
        // LivingEntity.baseTick advances deathTime before aiStep. Repair before calling it,
        // on both sides, so a lingering zero-health client state cannot run the death animation.
        if (isReforming()) restoreReformingState();
        else if (!level().isClientSide && !dead && getHealth() <= 0.0F) {
            // A penetrating storage stage can commit zero without invoking LivingEntity.hurt.
            DamageSource source = getLastDamageSource();
            die(source != null ? source : damageSources().generic());
        }
        super.tick();
        if (!level().isClientSide) encounterBar.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide) skills.tick();
        if (isReforming()) {
            setDeltaMovement(Vec3.ZERO);
            getNavigation().stop();
            if (!level().isClientSide && combat.tick()) finishReformation();
            return;
        }
        if (!level().isClientSide) {
            combat.tick();
            if (!withinHome(this, 24.0D)) returningHome = true;
            if (returningHome && withinHome(this, 20.0D)) {
                returningHome = false;
                getNavigation().stop();
            }
            if (returningHome || getTarget() != null && !withinHome(getTarget(), 24.0D)) {
                selectAiTarget(null);
                for (int head = 0; head < 3; head++) setAlternativeTarget(head, 0);
            }
        }
        super.aiStep();
        if (level().isClientSide && isAlive()) {
            if (tickCount % 3 == 0) spawnAsh(1);
            if (tickCount % 16 == 0) spawnSeepDust(1);
            if (hurtTime == 9) {
                spawnAsh(4);
                spawnSeepDust(1);
                for (int i = 0; i < 2; i++) level().addParticle(BONE_DUST,
                        getRandomX(0.6D), getRandomY(), getRandomZ(0.6D), 0, -0.15D, 0);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        // WitherBoss's side-head factory and block timer are private. This entity owns those AI
        // steps so every head directly creates its custom projectile, retaining vanilla timings.
        if (getInvulnerableTicks() > 0) {
            int remaining = getInvulnerableTicks() - 1;
            setInvulnerableTicks(remaining);
            if (remaining == 0) {
                spawnExplosion();
            }
            if (tickCount % 10 == 0) heal(10.0F);
            return;
        }
        if (!returningHome) {
            for (int side = 0; side < 2; side++) tickSideHead(side);
            setAlternativeTarget(0, getTarget() != null ? getTarget().getId() : 0);
            tickFinalMainHead();
        }
        tickBlockDestruction();
        if (tickCount % 20 == 0 && !isFinalPhase()) heal(1.0F);
    }

    /** The final phase raises the main-head cadence from 40 to 32 ticks (about 25 percent faster). */
    private void tickFinalMainHead() {
        if (!isFinalPhase() || skills.suppressesSkullShots()) {
            finalHeadCooldown = 0;
            return;
        }
        if (finalHeadCooldown > 0) {
            finalHeadCooldown--;
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && canAttack(target) && getInvulnerableTicks() == 0
                && hasLineOfSight(target)) {
            shoot(0, aimAt(target), random.nextFloat() < mainHeadChargedChance());
            finalHeadCooldown = 32;
        }
    }

    private void spawnExplosion() {
        level().explode(this, getX(), getEyeY(), getZ(), 7.0F, false, Level.ExplosionInteraction.MOB);
        if (!isSilent()) level().globalLevelEvent(1023, blockPosition(), 0);
    }

    private void tickSideHead(int side) {
        if (skills.suppressesSkullShots()) return;
        if (tickCount < nextSideShot[side]) return;
        nextSideShot[side] = tickCount + sideHeadDelay(10, 10);
        int idleThreshold = isHalfHealthEmpowered() ? 8 : 15;
        if ((level().getDifficulty() == Difficulty.NORMAL || level().getDifficulty() == Difficulty.HARD)
                && idleSideShots[side]++ > idleThreshold) {
            shoot(side + 1, new Vec3(Mth.nextDouble(random, getX() - 10.0D, getX() + 10.0D),
                    Mth.nextDouble(random, getY() - 5.0D, getY() + 5.0D),
                    Mth.nextDouble(random, getZ() - 10.0D, getZ() + 10.0D)), true);
            idleSideShots[side] = 0;
        }
        int id = getAlternativeTarget(side + 1);
        if (id > 0) {
            if (level().getEntity(id) instanceof LivingEntity target && canAttack(target)
                    && distanceToSqr(target) <= 900.0D && hasLineOfSight(target)) {
                shoot(side + 1, aimAt(target), isHalfHealthEmpowered() && random.nextFloat() < 0.10F);
                nextSideShot[side] = tickCount + sideHeadDelay(40, 20);
                idleSideShots[side] = 0;
            } else setAlternativeTarget(side + 1, 0);
        } else {
            var targets = level().getNearbyEntities(LivingEntity.class, SIDE_HEAD_TARGETS, this,
                    getBoundingBox().inflate(20.0D, 8.0D, 20.0D));
            if (!targets.isEmpty()) setAlternativeTarget(side + 1, targets.get(random.nextInt(targets.size())).getId());
        }
    }

    /** The final phase raises every ordinary side-head rate by about 25 percent (interval times 0.8). */
    private int sideHeadDelay(int base, int spread) {
        int delay = base + random.nextInt(spread);
        return isFinalPhase() ? Math.max(1, Math.round(delay * 0.8F)) : delay;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float strength) {
        // The final phase replaces this goal-driven cadence with the faster timer below.
        if (isFinalPhase() || skills.suppressesSkullShots()) return;
        if (canAttack(target) && getInvulnerableTicks() == 0) {
            shoot(0, aimAt(target), random.nextFloat() < mainHeadChargedChance());
        }
    }

    private float mainHeadChargedChance() {
        return isHalfHealthEmpowered() ? 0.15F : 0.001F;
    }

    private static Vec3 aimAt(LivingEntity target) {
        return target.position().add(0.0D, target.getEyeHeight() * 0.5D, 0.0D);
    }

    public Vec3 headPosition(int head) {
        if (head == 0) return position().add(0.0D, 3.0D * SCALE, 0.0D);
        double yaw = Math.toRadians(yBodyRot + 180.0D * head);
        return position().add(Math.cos(yaw) * 1.3D * SCALE, 2.2D * SCALE, Math.sin(yaw) * 1.3D * SCALE);
    }

    /** Package-private bridge for the skill controller's volley. */
    void shoot(int head, Vec3 target, boolean charged) {
        WeepingWitherSkull skull = MaledictEntities.WEEPING_WITHER_SKULL.get().create(level());
        if (skull == null) return;
        Vec3 muzzle = headPosition(head);
        Vec3 power = target.subtract(muzzle).normalize().scale(0.1D);
        skull.setOwner(this);
        skull.setDangerous(charged);
        skull.setPos(muzzle);
        skull.xPower = power.x;
        skull.yPower = power.y;
        skull.zPower = power.z;
        if (!isSilent()) level().levelEvent(null, 1024, blockPosition(), 0);
        level().addFreshEntity(skull);
    }

    private void tickBlockDestruction() {
        if (destroyBlocksTicks <= 0 || --destroyBlocksTicks != 0 || !ForgeEventFactory.getMobGriefingEvent(level(), this)) return;
        boolean destroyed = false;
        for (BlockPos pos : BlockPos.betweenClosed(blockPosition().offset(-1, 0, -1), blockPosition().offset(1, 3, 1))) {
            var state = level().getBlockState(pos);
            if (state.canEntityDestroy(level(), pos, this) && ForgeEventFactory.onEntityDestroyBlock(this, pos, state)) {
                destroyed |= level().destroyBlock(pos, true, this);
            }
        }
        if (destroyed) level().levelEvent(null, 1022, blockPosition(), 0);
    }

    @Override
    public void travel(Vec3 input) {
        if (isReforming()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        // Vanilla chase steering uses hardcoded velocity, bypassing the flying-speed attribute.
        // Scale this movement step and restore momentum to avoid compounding the boost every tick.
        setDeltaMovement(getDeltaMovement().scale(1.1D));
        super.travel(input);
        setDeltaMovement(getDeltaMovement().scale(1.0D / 1.1D));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isReforming()) return false;
        int previousInvulnerableTime = invulnerableTime;
        // Additional channels in this tick resolve in full, then share the largest final loss.
        // The first hit of a later tick still goes through vanilla's existing hurt cooldown.
        if (damageBatch.isCurrentTick(level().getGameTime())) {
            invulnerableTime = 0;
            lastHurt = 0.0F;
        }
        boolean hurt = super.hurt(source, Math.min(amount, getMaxHealth() * 0.05F));
        if (damageBatch.isCurrentTick(level().getGameTime())) {
            // A smaller or immune follow-up must not weaken the next tick's vanilla cooldown.
            tickLargestHurt = Math.max(tickLargestHurt, lastHurt);
            lastHurt = tickLargestHurt;
            invulnerableTime = Math.max(invulnerableTime, previousInvulnerableTime);
        }
        if (hurt) {
            if (destroyBlocksTicks <= 0) destroyBlocksTicks = 20;
            for (int side = 0; side < 2; side++) idleSideShots[side] += 3;
        }
        return hurt;
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        if (isReforming() || isInvulnerableTo(source)) return;
        amount = CombatProtection.incoming(this, source, amount);
        long now = level().getGameTime();
        if (!damageBatch.isCurrentTick(now)) {
            damageBatch.beginTick(now);
            tickLargestHurt = 0.0F;
        }
        float finalAmount = BossDamage.resolve(this, source, amount,
                damage -> getDamageAfterMagicAbsorb(source, getDamageAfterArmorAbsorb(source, damage)));
        if (finalAmount <= 0.0F || isReforming() || isDeadOrDying()) return;
        float cappedLoss = Math.min(finalAmount, getMaxHealth() * 0.05F);
        float additionalLoss = damageBatch.additionalLoss(cappedLoss);
        if (additionalLoss > 0.0F) {
            getCombatTracker().recordDamage(source, additionalLoss);
            float remaining = Math.max(0.0F, getHealth() - additionalLoss);
            if (healthController == null) super.setHealth(remaining);
            else healthController.commitDamage(() -> authorityHealth = remaining);
            setAbsorptionAmount(getAbsorptionAmount() - additionalLoss);
            gameEvent(GameEvent.ENTITY_DAMAGE);
        }
    }

    @Override
    public void heal(float amount) {
        if (isReforming()) return;
        if (healthController == null) {
            super.heal(amount);
            return;
        }
        amount = ForgeEventFactory.onLivingHeal(this, amount);
        float health = getHealth();
        if (Float.isFinite(amount) && amount > 0.0F && health > 0.0F && !isReforming() && !isRemoved()) {
            commitHealth(Math.min(getMaxHealth(), health + amount));
        }
    }

    public void onEffectiveSkullHit(boolean alreadyWithered) {
        if (isAlive() && combat.recordEffectiveHit(alreadyWithered)) heal(3.0F);
    }

    @Override
    public void die(DamageSource source) {
        if (!CombatProtection.allowDeath(this)) return;
        if (!level().isClientSide && (combat.isReforming() || combat.beginReformation())) {
            commitHealth(1.0F);
            clearDeathAnimation();
            entityData.set(REFORMING, true);
            setDeltaMovement(Vec3.ZERO);
            selectAiTarget(null);
            for (int head = 0; head < 3; head++) setAlternativeTarget(head, 0);
            getNavigation().stop();
            return;
        }
        super.die(source);
        skills.onFinalDeath();
    }

    private void finishReformation() {
        commitHealth(Math.min(80.0F, getMaxHealth()));
        clearDeathAnimation();
        entityData.set(REFORMING, false);
        spawnExplosion();
    }

    private void spawnAsh(int count) {
        for (int particle = 0; particle < count; particle++) {
            // Vanilla ash falls under gravity; unlike portal particles it does not climb or orbit.
            level().addParticle(ParticleTypes.ASH, getRandomX(0.7D), getRandomY(), getRandomZ(0.7D), 0, 0, 0);
        }
    }

    private void spawnSeepDust(int count) {
        double yaw = Math.toRadians(yBodyRot);
        for (int particle = 0; particle < count; particle++) {
            level().addParticle(SEEP_DUST,
                    getX() + Math.sin(yaw) * 0.35D + random.nextGaussian() * 0.12D,
                    getY() + 2.0D + random.nextDouble() * 0.6D,
                    getZ() - Math.cos(yaw) * 0.35D + random.nextGaussian() * 0.12D,
                    0, -0.35D, 0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("ReformationUsed", combat.reformationUsed());
        tag.putInt("ReformationTicks", combat.reformationTicks());
        tag.putInt("ProliferationCooldown", combat.healCooldown());
        if (altarHome != null) tag.put("AltarHome", NbtUtils.writeBlockPos(altarHome));
        tag.putBoolean("ReturningHome", returningHome);
        skills.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        combat.restore(tag.getBoolean("ReformationUsed"), tag.getInt("ReformationTicks"), tag.getInt("ProliferationCooldown"));
        entityData.set(REFORMING, combat.isReforming());
        if (combat.isReforming()) {
            commitHealth(1.0F);
            clearDeathAnimation();
        } else if (combat.reformationUsed() && getHealth() > 0.0F) {
            // Repair saves written by the old implementation with positive health and stale DeathTime.
            clearDeathAnimation();
        }
        if (tag.contains("AltarHome")) setAltarHome(NbtUtils.readBlockPos(tag.getCompound("AltarHome")));
        returningHome = tag.getBoolean("ReturningHome") && altarHome != null;
        skills.restore(tag);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        encounterBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        encounterBar.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        if (encounterBar != null) encounterBar.setName(getDisplayName());
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        // WitherBoss's custom death loot separately supplies the star and its extended lifetime.
        return EntityType.WITHER.getDefaultLootTable();
    }

    private final class ReturnToAltarGoal extends Goal {
        private int nextPathAttempt;

        ReturnToAltarGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return returningHome && altarHome != null;
        }

        @Override
        public void start() {
            nextPathAttempt = tickCount;
        }

        @Override
        public void tick() {
            if (tickCount < nextPathAttempt) return;
            nextPathAttempt = tickCount + 10;
            getNavigation().moveTo(altarHome.getX() + 0.5D, altarHome.getY(), altarHome.getZ() + 0.5D, 1.0D);
        }
    }
}
