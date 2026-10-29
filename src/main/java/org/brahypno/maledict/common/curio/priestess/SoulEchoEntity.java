package org.brahypno.maledict.common.curio.priestess;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import team.lodestar.lodestone.registry.common.tag.LodestoneDamageTypeTags;

import java.util.List;
import java.util.UUID;

/** Stationary, temporary spell target. Damage is handled by the body, never by a second hurt pipeline. */
public final class SoulEchoEntity extends LivingEntity {
    public static final int LIFETIME_TICKS = 60;
    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(SoulEchoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT =
            SynchedEntityData.defineId(SoulEchoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> EXPIRES =
            SynchedEntityData.defineId(SoulEchoEntity.class, EntityDataSerializers.LONG);

    private UUID targetUuid;
    private boolean forwarding;

    public SoulEchoEntity(EntityType<? extends SoulEchoEntity> type, net.minecraft.world.level.Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(WIDTH, 0.6F);
        entityData.define(HEIGHT, 1.8F);
        entityData.define(EXPIRES, 0L);
    }

    void refresh(LivingEntity target) {
        targetUuid = target.getUUID();
        entityData.set(WIDTH, target.getBbWidth());
        entityData.set(HEIGHT, target.getBbHeight());
        entityData.set(EXPIRES, level().getGameTime() + LIFETIME_TICKS);
        refreshDimensions();
        moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), 0);
    }

    public int remainingTicks() { return (int) Math.max(0, entityData.get(EXPIRES) - level().getGameTime()); }
    public UUID targetUuid() { return targetUuid; }
    boolean isForwarding() { return forwarding; }

    private LivingEntity body() {
        if (!(level() instanceof ServerLevel level) || targetUuid == null) return null;
        Entity entity = level.getEntity(targetUuid);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || forwarding || remainingTicks() == 0 || amount <= 0
                || !source.is(LodestoneDamageTypeTags.IS_MAGIC)) return false;
        LivingEntity target = body();
        if (target == null) {
            discard();
            return false;
        }
        forwarding = true;
        try {
            return target.hurt(source, amount);
        } finally {
            forwarding = false;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (remainingTicks() == 0 || body() == null)) discard();
    }

    @Override public void aiStep() {} // No movement, collision pushes, or independent combat simulation.
    @Override public boolean isPushable() { return false; }
    @Override public void push(double x, double y, double z) {}
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return false; }
    @Override public void kill() { discard(); }
    @Override public Iterable<ItemStack> getArmorSlots() { return List.of(); }
    @Override public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }
    @Override public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}
    @Override public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(entityData.get(WIDTH), entityData.get(HEIGHT));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (WIDTH.equals(key) || HEIGHT.equals(key)) refreshDimensions();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide()) SoulDisplacementEvents.forget(this);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
