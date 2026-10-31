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
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.common.combat.BossDamage;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.EnumSet;
import org.joml.Vector3f;

public class WeepingWitherEntity extends WitherBoss {
    public static final float SCALE = 1.2F;
    private static final DustParticleOptions SEEP_DUST = new DustParticleOptions(new Vector3f(0.25F, 0.21F, 0.29F), 0.6F);
    private static final DustParticleOptions BONE_DUST = new DustParticleOptions(new Vector3f(0.55F, 0.53F, 0.54F), 0.5F);
    private static final EntityDataAccessor<Boolean> REFORMING =
            SynchedEntityData.defineId(WeepingWitherEntity.class, EntityDataSerializers.BOOLEAN);
    private final WeepingWitherCombat combat = new WeepingWitherCombat();
    private static final TargetingConditions SIDE_HEAD_TARGETS = TargetingConditions.forCombat().range(20.0D)
            .selector(target -> target.getMobType() != MobType.UNDEAD && target.attackable());
    private final int[] nextSideShot = new int[2];
    private final int[] idleSideShots = new int[2];
    private int destroyBlocksTicks;
    private final BossDamage damageBatch = new BossDamage();
    private float tickLargestHurt;
    @Nullable private BlockPos altarHome;
    private boolean returningHome;
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
        goalSelector.addGoal(1, new ReturnToAltarGoal());
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

    private void clearDeathAnimation() {
        dead = false;
        deathTime = 0;
        setPose(Pose.STANDING);
    }

    private void restoreReformingState() {
        if (getHealth() <= 0.0F) setHealth(1.0F);
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
        super.tick();
        if (!level().isClientSide) encounterBar.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void aiStep() {
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
                setTarget(null);
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
        }
        tickBlockDestruction();
        if (tickCount % 20 == 0) heal(1.0F);
    }

    private void spawnExplosion() {
        level().explode(this, getX(), getEyeY(), getZ(), 7.0F, false, Level.ExplosionInteraction.MOB);
        if (!isSilent()) level().globalLevelEvent(1023, blockPosition(), 0);
    }

    private void tickSideHead(int side) {
        if (tickCount < nextSideShot[side]) return;
        nextSideShot[side] = tickCount + 10 + random.nextInt(10);
        if ((level().getDifficulty() == Difficulty.NORMAL || level().getDifficulty() == Difficulty.HARD)
                && idleSideShots[side]++ > 15) {
            shoot(side + 1, new Vec3(Mth.nextDouble(random, getX() - 10.0D, getX() + 10.0D),
                    Mth.nextDouble(random, getY() - 5.0D, getY() + 5.0D),
                    Mth.nextDouble(random, getZ() - 10.0D, getZ() + 10.0D)), true);
            idleSideShots[side] = 0;
        }
        int id = getAlternativeTarget(side + 1);
        if (id > 0) {
            if (level().getEntity(id) instanceof LivingEntity target && canAttack(target)
                    && distanceToSqr(target) <= 900.0D && hasLineOfSight(target)) {
                shoot(side + 1, aimAt(target), false);
                nextSideShot[side] = tickCount + 40 + random.nextInt(20);
                idleSideShots[side] = 0;
            } else setAlternativeTarget(side + 1, 0);
        } else {
            var targets = level().getNearbyEntities(LivingEntity.class, SIDE_HEAD_TARGETS, this,
                    getBoundingBox().inflate(20.0D, 8.0D, 20.0D));
            if (!targets.isEmpty()) setAlternativeTarget(side + 1, targets.get(random.nextInt(targets.size())).getId());
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float strength) {
        if (canAttack(target) && getInvulnerableTicks() == 0) {
            shoot(0, aimAt(target), random.nextFloat() < 0.001F);
        }
    }

    private static Vec3 aimAt(LivingEntity target) {
        return target.position().add(0.0D, target.getEyeHeight() * 0.5D, 0.0D);
    }

    public Vec3 headPosition(int head) {
        if (head == 0) return position().add(0.0D, 3.0D * SCALE, 0.0D);
        double yaw = Math.toRadians(yBodyRot + 180.0D * head);
        return position().add(Math.cos(yaw) * 1.3D * SCALE, 2.2D * SCALE, Math.sin(yaw) * 1.3D * SCALE);
    }

    private void shoot(int head, Vec3 target, boolean charged) {
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
        if (isInvulnerableTo(source)) return;
        long now = level().getGameTime();
        if (!damageBatch.isCurrentTick(now)) {
            damageBatch.beginTick(now);
            tickLargestHurt = 0.0F;
        }
        float finalAmount = BossDamage.resolve(this, source, amount,
                damage -> getDamageAfterMagicAbsorb(source, getDamageAfterArmorAbsorb(source, damage)));
        if (finalAmount <= 0.0F) return;
        float cappedLoss = Math.min(finalAmount, getMaxHealth() * 0.05F);
        float additionalLoss = damageBatch.additionalLoss(cappedLoss);
        if (additionalLoss > 0.0F) {
            getCombatTracker().recordDamage(source, additionalLoss);
            setHealth(getHealth() - additionalLoss);
            setAbsorptionAmount(getAbsorptionAmount() - additionalLoss);
            gameEvent(GameEvent.ENTITY_DAMAGE);
        }
    }

    @Override
    public void heal(float amount) {
        if (!isReforming()) super.heal(amount);
    }

    public void onEffectiveSkullHit(boolean alreadyWithered) {
        if (isAlive() && combat.recordEffectiveHit(alreadyWithered)) heal(3.0F);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && (combat.isReforming() || combat.beginReformation())) {
            setHealth(1.0F);
            clearDeathAnimation();
            entityData.set(REFORMING, true);
            setDeltaMovement(Vec3.ZERO);
            setTarget(null);
            for (int head = 0; head < 3; head++) setAlternativeTarget(head, 0);
            getNavigation().stop();
            return;
        }
        super.die(source);
    }

    private void finishReformation() {
        setHealth(80.0F);
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        combat.restore(tag.getBoolean("ReformationUsed"), tag.getInt("ReformationTicks"), tag.getInt("ProliferationCooldown"));
        entityData.set(REFORMING, combat.isReforming());
        if (combat.isReforming()) {
            setHealth(1.0F);
            clearDeathAnimation();
        } else if (combat.reformationUsed() && getHealth() > 0.0F) {
            // Repair saves written by the old implementation with positive health and stale DeathTime.
            clearDeathAnimation();
        }
        if (tag.contains("AltarHome")) setAltarHome(NbtUtils.readBlockPos(tag.getCompound("AltarHome")));
        returningHome = tag.getBoolean("ReturningHome") && altarHome != null;
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
