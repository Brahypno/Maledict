package org.brahypno.maledict.common.entity;

import com.sammy.malum.registry.common.MalumDamageTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.registry.MaledictEntities;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.systems.rendering.trail.TrailPointBuilder;

import javax.annotation.Nullable;

/**
 * The boss' own boomerang scythe: Malum's projectile is bound to player slots, so the boss uses
 * this dedicated entity carrying only the weapon's visual tier and attack strength.
 */
public final class VicissitudeScytheProjectileEntity extends Projectile {
    private static final EntityDataAccessor<Byte> DATA_WEAPON_TIER =
            SynchedEntityData.defineId(VicissitudeScytheProjectileEntity.class,
                    EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING =
            SynchedEntityData.defineId(VicissitudeScytheProjectileEntity.class,
                    EntityDataSerializers.BOOLEAN);
    public static final double OUTBOUND_SPEED = 0.8D;
    public static final double RETURN_SPEED = 1.1D;
    public static final int MAX_OUTBOUND_TICKS = 20;
    public static final double MAX_OUTBOUND_DISTANCE = 16.0D;
    public static final int MAX_LIFETIME = 100;

    /** Client-side trail history: 16 points, one sample per tick. */
    public final TrailPointBuilder trail = TrailPointBuilder.create(16);

    private float damage = 8.0F;
    private int outboundTicks;
    private double travelled;
    private int life;
    private boolean struck;
    private boolean forceRecall;

    public VicissitudeScytheProjectileEntity(EntityType<? extends VicissitudeScytheProjectileEntity> type,
                                             Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public VicissitudeScytheProjectileEntity(ServerLevel level, LivingEntity owner, Vec3 position,
                                             Vec3 direction, FirstVicissitudeBossEntity.BossDifficulty difficulty, float damage) {
        this(MaledictEntities.VICISSITUDE_SCYTHE.get(), level);
        setOwner(owner);
        setPos(position);
        entityData.set(DATA_WEAPON_TIER, (byte) difficulty.ordinal());
        this.damage = damage;
        setDeltaMovement(direction.normalize().scale(OUTBOUND_SPEED));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WEAPON_TIER, (byte) 0);
        builder.define(DATA_RETURNING, false);
    }

    public ItemStack getItem() {
        return FirstVicissitudeBossEntity.createWeaponForDifficulty(
                FirstVicissitudeBossEntity.BossDifficulty.byId(entityData.get(DATA_WEAPON_TIER)));
    }

    public boolean isReturning() {
        return entityData.get(DATA_RETURNING);
    }

    private void setReturning(boolean value) {
        entityData.set(DATA_RETURNING, value);
    }

    /** Server-side recall request; return starts as soon as possible. */
    public void recall() {
        forceRecall = true;
        if (!level().isClientSide) {
            setReturning(true);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            trail.addTrailPoint(position());
            trail.tickTrailPoints();
            return;
        }
        if (++life > MAX_LIFETIME) {
            finishReturn(false);
            return;
        }
        Entity owner = getOwner();
        if (!(owner instanceof FirstVicissitudeBossEntity boss) || !boss.isAlive()
            || !boss.ownsFlyingScythe(getUUID())) {
            // An invalid owner/token ends the visual projectile without dropping a weapon.
            discard();
            return;
        }
        if (!isReturning() && (forceRecall || outboundTicks >= MAX_OUTBOUND_TICKS
                               || travelled >= MAX_OUTBOUND_DISTANCE)) {
            setReturning(true);
        }
        if (isReturning()) {
            tickReturn(boss);
        } else {
            tickOutbound(boss);
        }
    }

    private void tickOutbound(FirstVicissitudeBossEntity boss) {
        outboundTicks++;
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());
        travelled += from.distanceTo(to);
        BlockHitResult blockHit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, from, to,
                getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.7D),
                candidate -> candidate != boss && candidate instanceof LivingEntity
                             && boss.isValidCombatParticipant(candidate));
        HitResult hit = entityHit != null && (blockHit.getType() == HitResult.Type.MISS
                || from.distanceToSqr(entityHit.getLocation()) <= from.distanceToSqr(blockHit.getLocation()))
                ? entityHit : blockHit;
        if (hit.getType() == HitResult.Type.BLOCK) {
            setPos(hit.getLocation());
            setReturning(true);
            return;
        }
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityResult) {
            Entity target = entityResult.getEntity();
            if (!struck && target instanceof LivingEntity living) {
                struck = true;
                boss.hurtParticipant(living,
                        DamageTypeHelper.create(level(), MalumDamageTypes.SCYTHE_SWEEP, this, boss),
                        damage);
            }
            setReturning(true);
            return;
        }
        setPos(to);
    }

    private void tickReturn(FirstVicissitudeBossEntity boss) {
        setReturning(true);
        Vec3 hand = boss.handAnchorWorldPosition();
        Vec3 offset = hand.subtract(position());
        if (offset.length() < 1.0D) {
            setPos(hand);
            finishReturn(true);
            return;
        }
        Vec3 motion = offset.normalize().scale(Math.min(RETURN_SPEED, offset.length()));
        setDeltaMovement(motion);
        setPos(position().add(motion));
    }

    private void finishReturn(boolean caught) {
        if (level() instanceof ServerLevel && getOwner() instanceof FirstVicissitudeBossEntity boss) {
            boss.onScytheReturned(this, caught);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("WeaponTier", entityData.get(DATA_WEAPON_TIER));
        tag.putFloat("ScytheDamage", damage);
        tag.putInt("OutboundTicks", outboundTicks);
        tag.putDouble("Travelled", travelled);
        tag.putInt("ScytheLife", life);
        tag.putBoolean("Struck", struck);
        tag.putBoolean("Returning", isReturning());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("WeaponTier")) {
            entityData.set(DATA_WEAPON_TIER, tag.getByte("WeaponTier"));
        } else if (getOwner() instanceof FirstVicissitudeBossEntity boss) {
            entityData.set(DATA_WEAPON_TIER, (byte) boss.getBossDifficulty().ordinal());
        }
        damage = tag.getFloat("ScytheDamage");
        outboundTicks = tag.getInt("OutboundTicks");
        travelled = tag.getDouble("Travelled");
        life = tag.getInt("ScytheLife");
        struck = tag.getBoolean("Struck");
        setReturning(tag.getBoolean("Returning"));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    /** Boss weapons are never collectable by players. */
    @Override
    public void playerTouch(Player player) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0D * 128.0D;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return null;
    }
}
