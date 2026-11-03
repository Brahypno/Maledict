package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.brahypno.maledict.common.vicissitude.VicissitudeBossEntity;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.registry.MaledictSounds;
import org.brahypno.maledict.registry.MaledictEntities;
import com.sammy.malum.registry.common.MobEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.Difficulty;
import org.brahypno.maledict.config.MaledictConfig;

/** A star awakens the statue in a rain of tears; the raised serpent then fights with three casts. */
public final class DemiurgeStatueEntity extends VicissitudeBossEntity {
    private static final double BASE_MOVEMENT_SPEED = .30;
    private static final EntityDataAccessor<Long> AWAKENING_START = SynchedEntityData.defineId(
            DemiurgeStatueEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(
            DemiurgeStatueEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ATTACK_START = SynchedEntityData.defineId(
            DemiurgeStatueEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Vector3f> BEAM_DIRECTION = SynchedEntityData.defineId(
            DemiurgeStatueEntity.class, EntityDataSerializers.VECTOR3);
    private float currentHeight = 4.2F;
    private boolean roarPlayed;
    private boolean clientRoarPending;
    private final DemiurgeTrail trail = new DemiurgeTrail();
    private double slitherClock;
    private double previousSlitherClock;
    private double horizontalStep;
    private int combatDifficulty = -1;
    private Vec3 awakeningRainOrigin;
    private Vec3 attackRainOrigin;
    private final DemiurgeRig.Pose pose = DemiurgeRig.INSTANCE.pose();
    private final DemiurgePart[] parts = new DemiurgePart[DemiurgeRig.INSTANCE.size];

    public DemiurgeStatueEntity(EntityType<? extends DemiurgeStatueEntity> type, Level level) {
        super(type, level);
        setMaxUpStep(1.0F);
        moveControl = new DemiurgeMoveControl(this);
        for (int i = 0; i < parts.length; i++) parts[i] = new DemiurgePart(this);
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        updateParts();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MAX_HEALTH, 600).add(Attributes.ARMOR, 20)
                .add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED).add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override public void onAddedToWorld() {
        if (!level().isClientSide) {
            if (combatDifficulty < 0) combatDifficulty = level().getDifficulty().getId();
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(DemiurgeDifficulty.maxHealth(Difficulty.byId(combatDifficulty)));
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(BASE_MOVEMENT_SPEED);
        }
        super.onAddedToWorld();
    }

    @Override protected float initialVitalityMaximum() {
        return DemiurgeDifficulty.maxHealth(Difficulty.byId(combatDifficulty));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new DemiurgeNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new DemiurgeCombatGoal(this));
        goalSelector.addGoal(5, new DemiurgePatrolGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> canFight()));
    }

    public boolean canPatrol() {
        return attack() == DemiurgeAttack.NONE
                && DemiurgeSlither.canPatrol(awakeningAge(0), isAlive(), getTarget() != null && getTarget().isAlive());
    }

    public boolean canFight() {
        return isAlive() && awakeningAge(0) >= DemiurgeAwakening.DURATION + DemiurgeSlither.TRANSITION_TICKS;
    }

    boolean canNavigate() {
        return canPatrol() || canFight() && attack() == DemiurgeAttack.NONE && getTarget() != null;
    }

    public DemiurgeAttack attack() { return DemiurgeAttack.values()[entityData.get(ATTACK)]; }

    public float attackAge(float partialTick) {
        return attack() == DemiurgeAttack.NONE ? -1 : Math.max(0, level().getGameTime() - entityData.get(ATTACK_START) + partialTick);
    }

    void beginAttack(DemiurgeAttack attack, LivingEntity target) {
        if (!canFight() || attack == DemiurgeAttack.NONE) return;
        if (attack == DemiurgeAttack.ROAR && !canStartRain(target)) return;
        getNavigation().stop();
        attackRainOrigin = attack == DemiurgeAttack.ROAR ? headCenter() : null;
        entityData.set(ATTACK_START, level().getGameTime());
        entityData.set(ATTACK, attack.ordinal());
        setBeamDirection(target.getBoundingBox().getCenter().subtract(eyeOrigin(0)).normalize());
    }

    void finishAttack() {
        entityData.set(ATTACK, DemiurgeAttack.NONE.ordinal());
        attackRainOrigin = null;
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    void aimAttack(LivingEntity target) {
        float age = attackAge(0);
        if ((attack() == DemiurgeAttack.CHARGE && age >= attack().release) || attack() == DemiurgeAttack.ROAR) return;
        float bearing = (float) Math.toDegrees(Math.atan2(getX() - target.getX(), target.getZ() - getZ()));
        setYRot(Mth.approachDegrees(getYRot(), bearing, 2));
        if (attack() == DemiurgeAttack.LASER) {
            Vec3 desired = target.getBoundingBox().getCenter().subtract(eyeOrigin(0)).normalize();
            setBeamDirection(beamDirection().lerp(desired, .08).normalize());
        }
    }

    private void setBeamDirection(Vec3 direction) {
        direction = DemiurgeAim.constrain(direction, getYRot());
        entityData.set(BEAM_DIRECTION, new Vector3f((float) direction.x, (float) direction.y, (float) direction.z));
    }

    public Vec3 beamDirection() {
        var direction = entityData.get(BEAM_DIRECTION);
        return new Vec3(direction.x, direction.y, direction.z);
    }

    public Vec3 eyeOrigin(int side) { return pose.eye(side, position(), getYRot()); }
    public Vec3 headCenter() { return pose.headCenter(position(), getYRot()); }
    public Vec3 headForward() { return pose.headForward(getYRot()); }

    boolean canStartRain(LivingEntity target) {
        return target instanceof Player && canAttack(target)
                && target.position().subtract(headCenter()).horizontalDistanceSqr() <= DemiurgeAttack.RAIN_RADIUS * DemiurgeAttack.RAIN_RADIUS;
    }

    public Vec3 laserEnd(Vec3 from) {
        return laserEnd(from, headForward());
    }

    public Vec3 laserEnd(Vec3 from, Vec3 direction) {
        Vec3 to = from.add(direction.scale(DemiurgeAttack.LASER_RANGE));
        return level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getLocation();
    }

    @Override public boolean canAttack(LivingEntity target) {
        return DemiurgeTearEntity.canHit(target) && super.canAttack(target);
    }

    @Override public boolean isMultipartEntity() { return true; }
    @Override public DemiurgePart[] getParts() { return parts; }
    @Override public void setId(int id) {
        super.setId(id);
        if (parts != null) for (int i = 0; i < parts.length; i++) parts[i].setId(id + i + 1);
    }

    void updateParts() {
        pose.sample(awakeningAge(0), slitherClock(1), attack(), attackAge(0), position(), getYRot(), this::trailPoint, beamDirection());
        for (int i = 0; i < parts.length; i++) parts[i].place(pose.bounds(i, position(), getYRot()));
    }

    public float slitherClock(float partialTick) {
        return (float) ((previousSlitherClock + (slitherClock - previousSlitherClock) * partialTick) % DemiurgeSlither.CYCLE_TICKS);
    }

    public DemiurgeTrail.Point trailPoint(double distance, Vec3 origin, float yaw) {
        return trail.sample(distance, origin, yaw);
    }

    double horizontalStep() {
        return horizontalStep;
    }

    java.util.List<Vec3> patrolBody() {
        return DemiurgePatrolSteering.body(position(), getYRot(), trail::sample);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(AWAKENING_START, -1L);
        entityData.define(ATTACK, 0);
        entityData.define(ATTACK_START, -1L);
        entityData.define(BEAM_DIRECTION, new Vector3f(0, 0, 1));
    }

    public float awakeningAge(float partialTick) {
        long start = entityData.get(AWAKENING_START);
        return start < 0 ? -1 : Math.max(0, level().getGameTime() - start + partialTick);
    }

    public boolean isAwake() {
        return awakeningAge(0) >= DemiurgeAwakening.DURATION;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!stack.is(MaledictItems.BLIGHTED_STAR.get()) || player.isSpectator()) {
            return super.mobInteract(player, hand);
        }
        if (!isAlive() || entityData.get(AWAKENING_START) >= 0) {
            return InteractionResult.FAIL;
        }
        if (!level().isClientSide) {
            updateParts();
            awakeningRainOrigin = headCenter();
            entityData.set(AWAKENING_START, level().getGameTime());
            // Explicitly costs one star, including creative mode. Both hands
            // and repeated packets share the already-started server state.
            stack.shrink(1);
            playSound(SoundEvents.GRINDSTONE_USE, 0.9F, 0.55F);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void tick() {
        Vec3 before = position();
        AABB[] previousBounds = new AABB[parts.length];
        for (int i = 0; i < parts.length; i++) previousBounds[i] = parts[i].getBoundingBox();
        previousSlitherClock = slitherClock;
        super.tick();
        double distance = position().subtract(before).horizontalDistance();
        horizontalStep = distance;
        slitherClock += distance * DemiurgeSlither.CYCLE_TICKS / DemiurgeSlither.BLOCKS_PER_CYCLE;
        float age = awakeningAge(0);
        // The ground body must not follow airborne positions from spawning or
        // awakening. An empty trail already extends behind the current feet.
        if (age >= DemiurgeAwakening.DURATION + DemiurgeSlither.TRANSITION_TICKS && onGround()) {
            trail.record(position(), getYRot());
        }
        float height = DemiurgeSlither.height(age);
        if (height != currentHeight) {
            currentHeight = height;
            refreshDimensions();
        }
        if (!level().isClientSide && isAlive() && !roarPlayed && age >= DemiurgeAwakening.ROAR_TICK) {
            roarPlayed = true;
            if (age < DemiurgeAwakening.ROAR_TICK + 20) {
                playSound(MaledictSounds.DEMIURGE_AWAKENING_ROAR.get(), 3.0F, 1.0F);
                level().broadcastEntityEvent(this, DemiurgeAwakening.ROAR_EVENT);
            }
        }
        updateParts();
        if (!level().isClientSide && isAlive()) tickAttacks(age, previousBounds);
    }

    private void tickAttacks(float awakeningAge, AABB[] previousBounds) {
        float age = attackAge(0);
        var attack = attack();
        if (attack == DemiurgeAttack.CHARGE && attack.active(age)) {
            Set<LivingEntity> contacts = new HashSet<>();
            for (int i = 0; i < parts.length; i++) {
                var box = parts[i].getBoundingBox().minmax(previousBounds[i]);
                if (box.getSize() < .01) continue;
                contacts.addAll(level().getEntitiesOfClass(LivingEntity.class, box, this::canAttack));
            }
            for (var victim : contacts) {
                // Merge overlapping parts before invoking hurt, and let vanilla's
                // equal-damage i-frame window govern sustained contact.
                DemiurgeDifficulty.hurt(victim, damageSources().mobAttack(this), MaledictConfig.DEMIURGE_CHARGE_DAMAGE.get());
            }
        }
        if (attack == DemiurgeAttack.LASER && attack.active(age)
                && ((int) age - attack.release) % DemiurgeAttack.LASER_INTERVAL == 0) fireLasers();
        if (attack == DemiurgeAttack.ROAR && (int) age == attack.release) {
            playSound(MaledictSounds.DEMIURGE_AWAKENING_ROAR.get(), 3, 1);
            level().broadcastEntityEvent(this, DemiurgeAwakening.ROAR_EVENT);
        }
        if (awakeningAge >= 0 && awakeningAge < DemiurgeAwakening.DURATION) {
            if (awakeningRainOrigin == null) awakeningRainOrigin = headCenter();
            releaseRain(awakeningRainOrigin);
        } else if (attack == DemiurgeAttack.ROAR && attack.active(age) && attackRainOrigin != null) {
            releaseRain(attackRainOrigin);
        }
    }

    void fireLasers() {
        Set<UUID> hit = new HashSet<>();
        for (int side = 0; side < 2; side++) {
            Vec3 from = eyeOrigin(side), to = laserEnd(from);
            for (var victim : level().getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(.18), this::canAttack)) {
                var box = victim.getBoundingBox().inflate(.14);
                if ((box.contains(from) || box.clip(from, to).isPresent()) && hit.add(victim.getUUID())) {
                    DemiurgeDifficulty.hurt(victim, damageSources().indirectMagic(this, this), MaledictConfig.DEMIURGE_LASER_DAMAGE.get());
                    victim.addEffect(new MobEffectInstance(MobEffectRegistry.SILENCED.get(), DemiurgeAttack.SILENCE_TICKS));
                }
            }
        }
    }

    private void releaseRain(Vec3 center) {
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = Math.sqrt(random.nextDouble()) * DemiurgeAttack.RAIN_RADIUS;
            var tear = MaledictEntities.DEMIURGE_TEAR.get().create(level());
            tear.setOwner(this);
            tear.setPos(center.x + Math.cos(angle) * radius, center.y + 10 + random.nextDouble() * 3,
                    center.z + Math.sin(angle) * radius);
            tear.setDeltaMovement(0, -DemiurgeTearEntity.FALL_SPEED, 0);
            level().addFreshEntity(tear);
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(5.1F, Math.max(4.2F, currentHeight));
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == DemiurgeAwakening.ROAR_EVENT) {
            clientRoarPending = true;
        } else {
            super.handleEntityEvent(event);
        }
    }

    /** Client-only presentation consumes a vanilla entity event without common-side client imports. */
    public boolean consumeRoarPulse() {
        boolean pending = clientRoarPending;
        clientRoarPending = false;
        return pending;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("AwakeningStart", entityData.get(AWAKENING_START));
        tag.putBoolean("AwakeningRoarPlayed", roarPlayed);
        if (combatDifficulty >= 0) tag.putInt("CombatDifficulty", combatDifficulty);
        if (awakeningRainOrigin != null) {
            tag.putDouble("AwakeningRainX", awakeningRainOrigin.x);
            tag.putDouble("AwakeningRainY", awakeningRainOrigin.y);
            tag.putDouble("AwakeningRainZ", awakeningRainOrigin.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(AWAKENING_START, tag.contains("AwakeningStart") ? tag.getLong("AwakeningStart") : -1L);
        roarPlayed = tag.getBoolean("AwakeningRoarPlayed");
        combatDifficulty = tag.contains("CombatDifficulty") ? tag.getInt("CombatDifficulty") : -1;
        awakeningRainOrigin = tag.contains("AwakeningRainX") ? new Vec3(tag.getDouble("AwakeningRainX"),
                tag.getDouble("AwakeningRainY"), tag.getDouble("AwakeningRainZ")) : null;
        currentHeight = DemiurgeSlither.height(awakeningAge(0));
        refreshDimensions();
        finishAttack();
        updateParts();
    }

    @Override
    public void travel(Vec3 input) {
        float chargeSpeed = attack().chargeSpeed(attackAge(0));
        if (!level().isClientSide && isAlive() && chargeSpeed > 0) {
            Vec3 forward = Vec3.directionFromRotation(0, getYRot()).scale(chargeSpeed);
            boolean clear = DemiurgeBlockBreaking.clearAhead(this, getYRot());
            setDeltaMovement(clear ? forward.x : 0, getDeltaMovement().y, clear ? forward.z : 0);
            super.travel(Vec3.ZERO);
        } else {
            if (!canNavigate()) setDeltaMovement(0, getDeltaMovement().y, 0);
            super.travel(canNavigate() ? input : Vec3.ZERO);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return DemiurgeSlither.groundBlend(awakeningAge(0)) > 0
                ? getBoundingBox().inflate(42, 18, 42) : getBoundingBox();
    }
}
