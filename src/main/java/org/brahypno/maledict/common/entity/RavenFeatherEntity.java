package org.brahypno.maledict.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.registry.MaledictEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

/** A single-hit feather, with the firing raven's damage and trust captured at release. */
public final class RavenFeatherEntity extends ThrowableProjectile {
    private float baseDamage = 2.0F;
    private int difficulty;
    private int life;
    @Nullable
    private UUID intendedTarget;
    private final List<UUID> trustedPlayers = new ArrayList<>(2);

    public RavenFeatherEntity(EntityType<? extends RavenFeatherEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public RavenFeatherEntity(RavenEntity owner, Vec3 origin, LivingEntity target) {
        this(MaledictEntities.RAVEN_FEATHER.get(), owner.level());
        setOwner(owner);
        setPos(origin);
        baseDamage = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
        difficulty = owner.level().getDifficulty().getId();
        trustedPlayers.addAll(owner.getTrustedPlayers());
        intendedTarget = target.getUUID();
        Vec3 aim = target.getBoundingBox().getCenter();
        Vec3 direction = aim.subtract(origin);
        shoot(direction.x, direction.y, direction.z, 1.5F, 0.0F);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && ++life >= 60) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity instanceof LivingEntity living && super.canHitEntity(entity)
                && RavenShotSafety.mayHit(living.getMobType() == MobType.UNDEAD,
                entity.getUUID().equals(intendedTarget), entity instanceof RavenEntity,
                trustedPlayers.contains(entity.getUUID()));
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!level().isClientSide && hit.getEntity() instanceof LivingEntity target) {
            LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
            float damage = RavenCombat.attackDamage(baseDamage, difficulty, target.getMobType() == MobType.UNDEAD);
            if (target.hurt(damageSources().mobProjectile(this, owner), damage) && owner instanceof RavenEntity raven) {
                raven.onFeatherHit(target);
            }
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide) {
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("BaseDamage", baseDamage);
        tag.putInt("Difficulty", difficulty);
        tag.putInt("Life", life);
        if (intendedTarget != null) {
            tag.putUUID("IntendedTarget", intendedTarget);
        }
        ListTag trusted = new ListTag();
        for (UUID player : trustedPlayers) {
            trusted.add(NbtUtils.createUUID(player));
        }
        tag.put("Trusted", trusted);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        baseDamage = tag.getFloat("BaseDamage");
        difficulty = tag.getInt("Difficulty");
        life = tag.getInt("Life");
        intendedTarget = tag.hasUUID("IntendedTarget") ? tag.getUUID("IntendedTarget") : null;
        trustedPlayers.clear();
        ListTag trusted = tag.getList("Trusted", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < trusted.size(); i++) {
            trustedPlayers.add(NbtUtils.loadUUID(trusted.get(i)));
        }
        setNoGravity(true);
    }
}
