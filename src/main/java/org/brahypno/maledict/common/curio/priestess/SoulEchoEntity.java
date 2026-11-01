package org.brahypno.maledict.common.curio.priestess;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkHooks;
import team.lodestar.lodestone.registry.common.tag.LodestoneDamageTypeTags;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stationary spell target whose gameplay state belongs to the body. Remains a LivingEntity so
 * spells which search for living targets can hit it; a PartEntity would be omitted by those searches.
 * Only the body runs living ticks, effect callbacks, equipment updates, and the damage pipeline.
 */
public final class SoulEchoEntity extends LivingEntity {
    public static final int LIFETIME_TICKS = 60;
    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(SoulEchoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT =
            SynchedEntityData.defineId(SoulEchoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> EXPIRES =
            SynchedEntityData.defineId(SoulEchoEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> BODY_ID =
            SynchedEntityData.defineId(SoulEchoEntity.class, EntityDataSerializers.INT);

    private UUID targetUuid;
    private boolean forwarding;
    private AttributeMap bodyAttributes;

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
        entityData.define(BODY_ID, -1);
    }

    void refresh(LivingEntity target) {
        targetUuid = target.getUUID();
        entityData.set(BODY_ID, target.getId());
        entityData.set(WIDTH, target.getBbWidth());
        entityData.set(HEIGHT, target.getBbHeight());
        entityData.set(EXPIRES, level().getGameTime() + LIFETIME_TICKS);
        refreshDimensions();
        // The body still occupies the anchor when a direct hit creates or refreshes it.
        double angle = random.nextDouble() * Math.PI * 2;
        double offset = 0.5 + random.nextDouble() * 0.3;
        moveTo(target.getX() + Math.cos(angle) * offset, target.getY(),
                target.getZ() + Math.sin(angle) * offset, target.getYRot(), 0);
    }

    public int remainingTicks() { return (int) Math.max(0, entityData.get(EXPIRES) - level().getGameTime()); }
    public UUID targetUuid() { return targetUuid; }
    boolean isForwarding() { return forwarding; }

    /** The tracked runtime ID lets the client reuse the body's renderer, skin, and equipment. */
    public LivingEntity visualBody() {
        Entity entity = level().getEntity(entityData.get(BODY_ID));
        return entity instanceof LivingEntity living && !(living instanceof SoulEchoEntity) ? living : null;
    }

    private LivingEntity body() {
        if (level() instanceof ServerLevel level && targetUuid != null) {
            Entity entity = level.getEntity(targetUuid);
            return entity instanceof LivingEntity living && !(living instanceof SoulEchoEntity) ? living : null;
        }
        return visualBody();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || forwarding || remainingTicks() == 0 || amount <= 0
                || !source.is(LodestoneDamageTypeTags.IS_MAGIC)) return false;
        LivingEntity target = body();
        if (target == null || !target.isAlive()) {
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
    public void die(DamageSource source) {
        if (level().isClientSide() || isRemoved() || forwarding) return;
        LivingEntity target = body();
        if (target == null || target.getHealth() > 0.0F) return;
        // A damage probe can reach setHealth without finishing hurt(), then call die on its
        // original target. Health was written to the body, so its death hooks and loot must run too.
        forwarding = true;
        try {
            if (source.getEntity() instanceof LivingEntity attacker) target.setLastHurtByMob(attacker);
            if (source.getEntity() instanceof Player player) target.setLastHurtByPlayer(player);
            target.die(source);
        } finally {
            forwarding = false;
        }
    }

    @Override
    public void tick() {
        // Do not call LivingEntity.tick/baseTick: Forge's LivingTickEvent would tick the shared
        // capabilities again, and equipment detection could reapply modifiers to the body.
        if (!level().isClientSide()) {
            LivingEntity target = body();
            if (remainingTicks() == 0 || target == null || !target.isAlive()) discard();
        }
    }

    @Override
    public AttributeMap getAttributes() {
        if (body() == null) return super.getAttributes(); // LivingEntity construction / unbound spawn.
        if (bodyAttributes == null) bodyAttributes = new SoulEchoAttributes(this::body);
        return bodyAttributes;
    }

    @Override public float getHealth() {
        LivingEntity target = body();
        return target == null ? super.getHealth() : target.getHealth();
    }

    @Override public void setHealth(float health) {
        LivingEntity target = body();
        if (target == null) super.setHealth(health);
        else target.setHealth(health);
    }

    @Override public void heal(float amount) {
        LivingEntity target = body();
        if (target != null) target.heal(amount);
    }

    @Override public float getAbsorptionAmount() {
        LivingEntity target = body();
        return target == null ? super.getAbsorptionAmount() : target.getAbsorptionAmount();
    }

    @Override public void setAbsorptionAmount(float amount) {
        LivingEntity target = body();
        if (target == null) super.setAbsorptionAmount(amount);
        else target.setAbsorptionAmount(amount);
    }

    @Override public Collection<MobEffectInstance> getActiveEffects() {
        return getActiveEffectsMap().values();
    }

    @Override public Map<MobEffect, MobEffectInstance> getActiveEffectsMap() {
        LivingEntity target = body();
        return target == null ? super.getActiveEffectsMap() : target.getActiveEffectsMap();
    }

    @Override public boolean hasEffect(MobEffect effect) { return getEffect(effect) != null; }

    @Override public MobEffectInstance getEffect(MobEffect effect) {
        LivingEntity target = body();
        return target == null ? null : target.getEffect(effect);
    }

    @Override public boolean canBeAffected(MobEffectInstance effect) {
        LivingEntity target = body();
        return target != null && target.canBeAffected(effect);
    }

    @Override public boolean addEffect(MobEffectInstance effect, @Nullable Entity source) {
        LivingEntity target = body();
        return target != null && target.addEffect(effect, source);
    }

    @Override public void forceAddEffect(MobEffectInstance effect, @Nullable Entity source) {
        LivingEntity target = body();
        if (target != null) target.forceAddEffect(effect, source);
    }

    @Override public MobEffectInstance removeEffectNoUpdate(@Nullable MobEffect effect) {
        LivingEntity target = body();
        return target == null ? null : target.removeEffectNoUpdate(effect);
    }

    @Override public boolean removeEffect(MobEffect effect) {
        LivingEntity target = body();
        return target != null && target.removeEffect(effect);
    }

    @Override public boolean removeAllEffects() {
        LivingEntity target = body();
        return target != null && target.removeAllEffects();
    }

    @Override public boolean curePotionEffects(ItemStack curativeItem) {
        LivingEntity target = body();
        return target != null && target.curePotionEffects(curativeItem);
    }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        LivingEntity target = body();
        return target == null ? super.getCapability(capability, side) : target.getCapability(capability, side);
    }

    @Override public CompoundTag getPersistentData() {
        LivingEntity target = body();
        return target == null ? super.getPersistentData() : target.getPersistentData();
    }

    @Override public MobType getMobType() {
        LivingEntity target = body();
        return target == null ? super.getMobType() : target.getMobType();
    }

    @Override public boolean isInvisible() {
        LivingEntity target = body();
        return target != null && target.isInvisible();
    }

    @Override public void setInvisible(boolean invisible) {
        LivingEntity target = body();
        if (target != null) target.setInvisible(invisible);
    }

    @Override public boolean fireImmune() {
        LivingEntity target = body();
        return target == null ? super.fireImmune() : target.fireImmune();
    }

    @Override public boolean canFreeze() {
        LivingEntity target = body();
        return target != null && target.canFreeze();
    }

    @Override public Team getTeam() {
        LivingEntity target = body();
        return target == null ? super.getTeam() : target.getTeam();
    }

    @Override public boolean isInvulnerableTo(DamageSource source) {
        LivingEntity target = body();
        return target == null ? super.isInvulnerableTo(source) : target.isInvulnerableTo(source);
    }

    @Override public int getRemainingFireTicks() {
        LivingEntity target = body();
        return target == null ? super.getRemainingFireTicks() : target.getRemainingFireTicks();
    }

    @Override public void setRemainingFireTicks(int ticks) {
        LivingEntity target = body();
        if (target == null) super.setRemainingFireTicks(ticks);
        else target.setRemainingFireTicks(ticks);
    }

    @Override public boolean isOnFire() {
        LivingEntity target = body();
        return target != null && target.isOnFire();
    }

    @Override public void setSecondsOnFire(int seconds) {
        LivingEntity target = body();
        if (target != null) target.setSecondsOnFire(seconds);
    }

    @Override public int getTicksFrozen() {
        LivingEntity target = body();
        return target == null ? super.getTicksFrozen() : target.getTicksFrozen();
    }

    @Override public void setTicksFrozen(int ticks) {
        LivingEntity target = body();
        if (target == null) super.setTicksFrozen(ticks);
        else target.setTicksFrozen(ticks);
    }

    @Override public int getAirSupply() {
        LivingEntity target = body();
        return target == null ? super.getAirSupply() : target.getAirSupply();
    }

    @Override public void setAirSupply(int air) {
        LivingEntity target = body();
        if (target == null) super.setAirSupply(air);
        else target.setAirSupply(air);
    }

    @Override public void aiStep() {} // No movement, collision pushes, or independent combat simulation.
    @Override public boolean isPushable() { return false; }
    @Override public void push(double x, double y, double z) {}
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public void kill() { discard(); }
    @Override public Iterable<ItemStack> getHandSlots() {
        LivingEntity target = body();
        return target == null ? List.of() : target.getHandSlots();
    }
    @Override public Iterable<ItemStack> getArmorSlots() {
        LivingEntity target = body();
        return target == null ? List.of() : target.getArmorSlots();
    }
    @Override public ItemStack getItemBySlot(EquipmentSlot slot) {
        LivingEntity target = body();
        return target == null ? ItemStack.EMPTY : target.getItemBySlot(slot);
    }
    @Override public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        LivingEntity target = body();
        if (target != null) target.setItemSlot(slot, stack);
    }
    @Override public HumanoidArm getMainArm() {
        LivingEntity target = body();
        return target == null ? HumanoidArm.RIGHT : target.getMainArm();
    }

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
