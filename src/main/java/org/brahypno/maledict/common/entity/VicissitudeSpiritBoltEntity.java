package org.brahypno.maledict.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.systems.rendering.trail.TrailPointBuilder;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * A straight Vicissitude bolt. Phase one bolts press the target to a single hit point, phase two
 * bolts deal ordinary damage; the firing phase is decided by the boss and stored per projectile
 * so a mid flight phase change cannot silently convert one into the other.
 */
public final class VicissitudeSpiritBoltEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> DATA_PRESS_HEALTH =
            SynchedEntityData.defineId(VicissitudeSpiritBoltEntity.class, EntityDataSerializers.BOOLEAN);
    /** Client side trail history; 12 points at most, one sample per tick. */
    public final TrailPointBuilder trail = TrailPointBuilder.create(12);

    private float damage = 6.0F;
    private int life;
    private int maximumLife = 80;
    private boolean removed;

    public VicissitudeSpiritBoltEntity(EntityType<? extends VicissitudeSpiritBoltEntity> type,
                                       Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public VicissitudeSpiritBoltEntity(ServerLevel level, LivingEntity owner, Vec3 position,
                                       Vec3 motion, boolean pressHealth, float damage,
                                       int maximumLife) {
        this(boltType(), level);
        setOwner(owner);
        setPos(position);
        setDeltaMovement(motion);
        setPressHealth(pressHealth);
        this.damage = damage;
        this.maximumLife = maximumLife;
    }

    private static EntityType<? extends VicissitudeSpiritBoltEntity> boltType() {
        return org.brahypno.maledict.registry.MaledictEntities.VICISSITUDE_SPIRIT_BOLT.get();
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_PRESS_HEALTH, true);
    }

    public boolean pressesHealth() {
        return entityData.get(DATA_PRESS_HEALTH);
    }

    private void setPressHealth(boolean value) {
        entityData.set(DATA_PRESS_HEALTH, value);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            trail.addTrailPoint(position());
            trail.tickTrailPoints();
            return;
        }
        if (++life > maximumLife) {
            discard();
            return;
        }
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());
        BlockHitResult blockHit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, from, to,
                getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.6D),
                candidate -> candidate != getOwner() && candidate.isAlive()
                             && isCombatParticipant(candidate)
                             && candidate instanceof LivingEntity);
        HitResult hit = entityHit != null && (blockHit.getType() == HitResult.Type.MISS
                || from.distanceToSqr(entityHit.getLocation()) <= from.distanceToSqr(blockHit.getLocation()))
                ? entityHit : blockHit;
        if (hit.getType() != HitResult.Type.MISS) {
            setPos(hit.getLocation());
            onImpact(hit);
            return;
        }
        setPos(to);
    }

    private void onImpact(HitResult hit) {
        if (hit instanceof EntityHitResult entityHit
            && entityHit.getEntity() instanceof LivingEntity living) {
            if (shouldPressHealth()) {
                living.setHealth(Math.min(living.getHealth(), 1.0F));
            } else {
                Entity owner = getOwner();
                living.invulnerableTime = 0;
                living.hurt(owner instanceof LivingEntity livingOwner
                        ? damageSources().mobProjectile(this, livingOwner)
                        : damageSources().magic(), damage);
            }
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    getX(), getY(), getZ(), 4, 0.1D, 0.1D, 0.1D, 0.01D);
        }
        discard();
    }

    /**
     * Phase one bolts always press the target down to a single hit point.
     *
     * <p>The flag is set at release time, which is what {@code 07} asks for, but the owning
     * boss' current stage is checked as well: a bolt released during phase one can never turn
     * into ordinary damage, whatever happens to the flag while it flies.
     */
    private boolean shouldPressHealth() {
        if (pressesHealth()) {
            return true;
        }
        return getOwner() instanceof FirstVicissitudeBossEntity boss && boss.isPhaseOneStage();
    }

    /** Bolts never scratch bystanders: only this fight's registered participants are valid. */
    private boolean isCombatParticipant(Entity candidate) {
        return !(getOwner() instanceof FirstVicissitudeBossEntity boss)
               || boss.isValidCombatParticipant(candidate);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PressHealth", pressesHealth());
        tag.putFloat("BoltDamage", damage);
        tag.putInt("BoltLife", life);
        tag.putInt("BoltMaxLife", maximumLife);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPressHealth(tag.getBoolean("PressHealth"));
        damage = tag.getFloat("BoltDamage");
        life = tag.getInt("BoltLife");
        maximumLife = tag.getInt("BoltMaxLife");
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0D * 96.0D;
    }


}
