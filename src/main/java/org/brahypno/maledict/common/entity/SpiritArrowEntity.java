package org.brahypno.maledict.common.entity;

import com.sammy.malum.core.handlers.SoulDataHandler;
import com.sammy.malum.registry.common.DamageTypeRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import org.brahypno.maledict.common.item.SpiritArrowType;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.registry.MaledictItems;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.systems.rendering.trail.TrailPoint;
import team.lodestar.lodestone.systems.rendering.trail.TrailPointBuilder;

public class SpiritArrowEntity extends AbstractArrow {
    private static final EntityDataAccessor<Integer> ARROW_TYPE =
            SynchedEntityData.defineId(SpiritArrowEntity.class, EntityDataSerializers.INT);
    public final TrailPointBuilder trailPointBuilder = TrailPointBuilder.create(12);
    public final TrailPointBuilder spinningTrailPointBuilder = TrailPointBuilder.create(6);
    private final float spinOffset = random.nextFloat() * (float) (Math.PI * 2.0D);

    public SpiritArrowEntity(EntityType<? extends SpiritArrowEntity> entityType, Level level) {
        super(entityType, level);
    }

    public SpiritArrowEntity(Level level, LivingEntity shooter, SpiritArrowType arrowType) {
        super(MaledictEntities.SPIRIT_ARROW.get(), shooter, level);
        setArrowType(arrowType);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ARROW_TYPE, SpiritArrowType.SACRED.ordinal());
    }

    public SpiritArrowType getArrowType() {
        return SpiritArrowType.byId(entityData.get(ARROW_TYPE));
    }

    private void setArrowType(SpiritArrowType arrowType) {
        entityData.set(ARROW_TYPE, arrowType.ordinal());
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        float adjustedVelocity = getArrowType() == SpiritArrowType.AERIAL ? velocity * 1.5F : velocity;
        super.shoot(x, y, z, adjustedVelocity, inaccuracy);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide){
            return;
        }
        if (!inGround && !isRemoved()){
            for (int i = 0; i < 2; i++) {
                float partialTick = i * 0.5F;
                var position = getPosition(partialTick);
                trailPointBuilder.addTrailPoint(position);

                double spinAngle = spinOffset + (tickCount + partialTick) / 2.0D;
                spinningTrailPointBuilder.addTrailPoint(new TrailPoint(
                        position.add(Math.cos(spinAngle) * 0.15D, 0.0D,
                                     Math.sin(spinAngle) * 0.15D),
                        i));
            }
        }
        trailPointBuilder.tickTrailPoints();
        spinningTrailPointBuilder.tickTrailPoints();
    }

    @Override
    public boolean isInWater() {
        return getArrowType() != SpiritArrowType.AQUEOUS && super.isInWater();
    }

    @Override
    public boolean isNoGravity() {
        return super.isNoGravity()
               || getArrowType() == SpiritArrowType.AQUEOUS
                  && (super.isInWater() || isInFluidType());
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        double originalBaseDamage = getBaseDamage();
        if (hasConditionalDamageBonus(hitResult.getEntity())){
            setBaseDamage(originalBaseDamage * 1.5D);
        }
        try {
            if (hitResult.getEntity() instanceof LivingEntity le)
                SoulDataHandler.exposeSoul(le);
            super.onHitEntity(hitResult);
        }
        finally {
            setBaseDamage(originalBaseDamage);
        }
    }

    private boolean hasConditionalDamageBonus(Entity target) {
        if (!(target instanceof LivingEntity livingTarget)){
            return false;
        }
        boolean undead = livingTarget.getMobType() == MobType.UNDEAD;
        return getArrowType() == SpiritArrowType.SACRED && undead
               || getArrowType() == SpiritArrowType.WICKED && !undead;
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        switch (getArrowType()) {
            case INFERNAL -> target.addEffect(
                    new MobEffectInstance(MobEffects.GLOWING, 200), getEffectSource());
            case ARCANE -> dealMagicDamage(target, 3.0F);
            case ELDRITCH -> {
                float projectileDamage = (float) Math.ceil(getDeltaMovement().length() * getBaseDamage());
                dealMagicDamage(target, projectileDamage * 0.5F);
            }
        }
    }

    private void dealMagicDamage(LivingEntity target, float amount) {
        if (!level().isClientSide && amount > 0.0F){
            DamageProbe.mediumDamageMethod(target, DamageTypeHelper.create(level(), DamageTypeRegistry.VOODOO, this, getOwner()), amount);
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(MaledictItems.getSpiritArrow(getArrowType()).get());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SpiritArrowType", getArrowType().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SpiritArrowType")){
            setArrowType(SpiritArrowType.byId(tag.getInt("SpiritArrowType")));
        }
    }
}
