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

/** A Blighted Star awakens the statue once; combat attacks remain unauthored. */
public final class DemiurgeStatueEntity extends VicissitudeBossEntity {
    private static final EntityDataAccessor<Long> AWAKENING_START = SynchedEntityData.defineId(
            DemiurgeStatueEntity.class, EntityDataSerializers.LONG);
    private float currentHeight = 4.2F;
    private boolean roarPlayed;
    private boolean clientRoarPending;
    private final DemiurgeTrail trail = new DemiurgeTrail();
    private double slitherClock;
    private double previousSlitherClock;
    private double horizontalStep;

    public DemiurgeStatueEntity(EntityType<? extends DemiurgeStatueEntity> type, Level level) {
        super(type, level);
        setMaxUpStep(1.0F);
        moveControl = new DemiurgeMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MOVEMENT_SPEED, 0.12D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new DemiurgeNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(5, new DemiurgePatrolGoal(this));
    }

    public boolean canPatrol() {
        return DemiurgeSlither.canPatrol(awakeningAge(0), isAlive(), getTarget() != null && getTarget().isAlive());
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(AWAKENING_START, tag.contains("AwakeningStart") ? tag.getLong("AwakeningStart") : -1L);
        roarPlayed = tag.getBoolean("AwakeningRoarPlayed");
        currentHeight = DemiurgeSlither.height(awakeningAge(0));
        refreshDimensions();
    }

    @Override
    public void travel(Vec3 input) {
        super.travel(canPatrol() ? input : Vec3.ZERO);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        // Rendering includes the trailing body; the ordinary collision box is
        // the raised front. Combat multipart hitboxes remain a separate task.
        return DemiurgeSlither.groundBlend(awakeningAge(0)) > 0
                ? getBoundingBox().inflate(24, .2, 24) : getBoundingBox();
    }
}
