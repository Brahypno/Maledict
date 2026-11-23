package org.brahypno.maledict.common.item.curio.priestess;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.fluids.FluidType;
import org.brahypno.maledict.common.item.curio.priestess.SoulDisplacementEvents;
import org.jetbrains.annotations.Nullable;
import team.lodestar.lodestone.registry.common.tag.LodestoneDamageTypeTags;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 静止的法术靶子：<b>游戏状态属于本体</b>。仍然是 {@link LivingEntity}，因为寻找活体目标的法术
 * 才会选中它；{@code PartEntity} 会被这类搜索直接跳过。真正在跑的只有本体——活体 tick、效果回调、
 * 装备更新与伤害管线都只在本体那一份上发生，回声这边只把问题转给本体。
 *
 * <p>1.20.1 的这一层在移植时整段丢了，只剩 {@code hurt} 的魔法重定向：给回声上效果什么都不做、
 * 以写血结算的探针写了回声自己的池又立刻被同步抹掉、{@code die()} 不再跑本体的死亡钩子。
 * 这里按 1.20.1 逐条补回，并做几处 1.21 的必然调整：
 *
 * <ul>
 *   <li>{@code getMobType()} 在 1.21 不存在（{@code MobType} 随 1.20.5 移除，亡灵判定改成
 *       {@code EntityTypeTags}）。能转的只剩 {@link #isInvertedHealAndHarm()}（瞬间治疗/伤害反转），
 *       而 Smite 这类按实体类型标签判定的附魔无从穿透。</li>
 *   <li>Capability 转发没有了：{@code Entity#getCapability} 与 {@code AttachmentHolder#getData}
 *       在 NeoForge 21.1 都是 {@code final}，能力改为按注册的 provider 查询，不能按实体覆写。
 *       Malum 1.8.2 自身也已从 capability 换成 attachment，同样读不到回声头上的转发。</li>
 *   <li>{@code curePotionEffects} 随 1.21 的牛奶改动消失，{@link #removeAllEffects()} 覆盖同一个效果。</li>
 *   <li>两个写入口变成了 {@code final}，但都留着可覆写的落点：吸收值拦
 *       {@link #internalSetAbsorptionAmount(float)}，点火拦 {@link #igniteForTicks(int)}。</li>
 * </ul>
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WIDTH, 0.6F);
        builder.define(HEIGHT, 1.8F);
        builder.define(EXPIRES, 0L);
        builder.define(BODY_ID, -1);
    }

    public void refresh(LivingEntity target) {
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
    public boolean isForwarding() { return forwarding; }

    /** The tracked runtime ID lets the client reuse the body's renderer, skin, and equipment. */
    public LivingEntity visualBody() {
        Entity entity = level().getEntity(entityData.get(BODY_ID));
        return entity instanceof LivingEntity living && !(living instanceof SoulEchoEntity) ? living : null;
    }

    /** 服务端按 UUID 找本体；客户端只能靠同步下来的运行时 ID（渲染、血量条都走这条）。 */
    private LivingEntity body() {
        if (!(level() instanceof ServerLevel level) || targetUuid == null) return visualBody();
        Entity entity = level.getEntity(targetUuid);
        return entity instanceof LivingEntity living && !(living instanceof SoulEchoEntity) && living.isAlive()
               ? living : null;
    }

    // ------------------------------------------------------------------ 伤害与死亡

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

    // ------------------------------------------------------------------ 属性与生命

    @Override
    public AttributeMap getAttributes() {
        // LivingEntity 的构造器会先建好自己的属性表；未绑定本体时（构造期、无主生成）照常用它。
        if (body() == null) return super.getAttributes();
        if (bodyAttributes == null) bodyAttributes = new SoulEchoAttributes(this::body);
        return bodyAttributes;
    }

    @Override
    public float getHealth() {
        LivingEntity target = body();
        return target == null ? super.getHealth() : target.getHealth();
    }

    @Override
    public void setHealth(float health) {
        LivingEntity target = body();
        if (target == null) super.setHealth(health);
        else target.setHealth(health);
    }

    @Override
    public void heal(float amount) {
        LivingEntity target = body();
        if (target != null) target.heal(amount);
    }

    @Override
    public float getAbsorptionAmount() {
        LivingEntity target = body();
        return target == null ? super.getAbsorptionAmount() : target.getAbsorptionAmount();
    }

    /**
     * 吸收值只有写入口能转：1.21 的 {@code setAbsorptionAmount} 是 {@code final}，
     * 它夹取之后落到这个 protected 钩子，所以覆写这里等于拦下所有写入；读口
     * {@link #getAbsorptionAmount()} 本来就是普通方法。
     */
    @Override
    protected void internalSetAbsorptionAmount(float amount) {
        LivingEntity target = body();
        if (target == null) super.internalSetAbsorptionAmount(amount);
        else target.setAbsorptionAmount(amount);
    }

    // ------------------------------------------------------------------ 状态效果

    @Override
    public Collection<MobEffectInstance> getActiveEffects() {
        LivingEntity target = body();
        return target == null ? super.getActiveEffects() : target.getActiveEffects();
    }

    @Override
    public Map<Holder<MobEffect>, MobEffectInstance> getActiveEffectsMap() {
        LivingEntity target = body();
        return target == null ? super.getActiveEffectsMap() : target.getActiveEffectsMap();
    }

    @Override
    public boolean hasEffect(Holder<MobEffect> effect) {
        LivingEntity target = body();
        return target == null ? super.hasEffect(effect) : target.hasEffect(effect);
    }

    @Override
    public MobEffectInstance getEffect(Holder<MobEffect> effect) {
        LivingEntity target = body();
        return target == null ? null : target.getEffect(effect);
    }

    @Override
    @SuppressWarnings("deprecation") // Override-only hook, single call site MobEffectEvent.Applicable.
    public boolean canBeAffected(MobEffectInstance effect) {
        LivingEntity target = body();
        return target != null && target.canBeAffected(effect);
    }

    @Override
    public boolean addEffect(MobEffectInstance effect, @Nullable Entity source) {
        LivingEntity target = body();
        return target != null && target.addEffect(effect, source);
    }

    @Override
    public void forceAddEffect(MobEffectInstance effect, @Nullable Entity source) {
        LivingEntity target = body();
        if (target != null) target.forceAddEffect(effect, source);
    }

    @Override
    public MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> effect) {
        LivingEntity target = body();
        return target == null ? null : target.removeEffectNoUpdate(effect);
    }

    @Override
    public boolean removeEffect(Holder<MobEffect> effect) {
        LivingEntity target = body();
        return target != null && target.removeEffect(effect);
    }

    @Override
    public boolean removeAllEffects() {
        LivingEntity target = body();
        return target != null && target.removeAllEffects();
    }

    // ------------------------------------------------------------------ 其余归属本体的状态

    /** 1.21 里 {@code MobType} 没了；亡灵式的治疗/伤害反转只能从这个钩子转过去。 */
    @Override
    public boolean isInvertedHealAndHarm() {
        LivingEntity target = body();
        return target == null ? super.isInvertedHealAndHarm() : target.isInvertedHealAndHarm();
    }

    @Override
    public PlayerTeam getTeam() {
        LivingEntity target = body();
        return target == null ? super.getTeam() : target.getTeam();
    }

    @Override
    public CompoundTag getPersistentData() {
        LivingEntity target = body();
        return target == null ? super.getPersistentData() : target.getPersistentData();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        LivingEntity target = body();
        return target == null ? super.isInvulnerableTo(source) : target.isInvulnerableTo(source);
    }

    @Override
    public boolean isInvisible() {
        LivingEntity target = body();
        return target != null && target.isInvisible();
    }

    @Override
    public void setInvisible(boolean invisible) {
        LivingEntity target = body();
        if (target != null) target.setInvisible(invisible);
    }

    @Override
    public boolean fireImmune() {
        LivingEntity target = body();
        return target != null && target.fireImmune();
    }

    @Override
    public boolean canFreeze() {
        LivingEntity target = body();
        return target != null && target.canFreeze();
    }

    @Override
    public int getRemainingFireTicks() {
        LivingEntity target = body();
        return target == null ? super.getRemainingFireTicks() : target.getRemainingFireTicks();
    }

    @Override
    public void setRemainingFireTicks(int ticks) {
        LivingEntity target = body();
        if (target == null) super.setRemainingFireTicks(ticks);
        else target.setRemainingFireTicks(ticks);
    }

    @Override
    public boolean isOnFire() {
        LivingEntity target = body();
        return target != null && target.isOnFire();
    }

    /**
     * 点火的唯一可覆写入口：1.21 的 {@code igniteForSeconds} 是 {@code final}，它落到
     * {@code igniteForTicks}；转发给本体之后，燃烧时间倍率由本体自己的属性算。
     */
    @Override
    public void igniteForTicks(int ticks) {
        LivingEntity target = body();
        if (target == null) super.igniteForTicks(ticks);
        else target.igniteForTicks(ticks);
    }

    @Override
    public int getTicksFrozen() {
        LivingEntity target = body();
        return target == null ? super.getTicksFrozen() : target.getTicksFrozen();
    }

    @Override
    public void setTicksFrozen(int ticks) {
        LivingEntity target = body();
        if (target == null) super.setTicksFrozen(ticks);
        else target.setTicksFrozen(ticks);
    }

    @Override
    public int getAirSupply() {
        LivingEntity target = body();
        return target == null ? super.getAirSupply() : target.getAirSupply();
    }

    @Override
    public void setAirSupply(int air) {
        LivingEntity target = body();
        if (target == null) super.setAirSupply(air);
        else target.setAirSupply(air);
    }

    // ------------------------------------------------------------------ 装备

    @Override
    public Iterable<ItemStack> getHandSlots() {
        LivingEntity target = body();
        return target == null ? List.of() : target.getHandSlots();
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        LivingEntity target = body();
        return target == null ? List.of() : target.getArmorSlots();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        LivingEntity target = body();
        return target == null ? ItemStack.EMPTY : target.getItemBySlot(slot);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        LivingEntity target = body();
        if (target != null) target.setItemSlot(slot, stack);
    }

    @Override
    public HumanoidArm getMainArm() {
        LivingEntity target = body();
        return target == null ? HumanoidArm.RIGHT : target.getMainArm();
    }

    // ------------------------------------------------------------------ 生命周期

    @Override
    public void tick() {
        // 不调用 super.tick()：LivingEntity 的 baseTick 会照着转发给本体的空气/燃烧/冻结值再 tick 一遍，
        // 等于把本体的这些计时器推两倍。效果表本身用的是自己的字段（空的），不会双 tick，但代价为零；
        // 回声这里要跑的只有存活期限与本体校验。
        if (!level().isClientSide()) {
            LivingEntity target = body();
            if (remainingTicks() == 0 || target == null) discard();
        }
    }

    @Override public void aiStep() {} // No movement, collision pushes, or independent combat simulation.
    @Override public boolean isPushable() { return false; }
    @Override public void push(double x, double y, double z) {}
    // NeoForge deprecates the no-arg form for callers; its default type-sensitive implementation is
    // `isPushedByFluid() && type.canPushEntity(this)`. Entity#updateFluidHeightAndDoFluidPushing asks
    // this overload once per fluid block, and the no-arg read it also performs lands in a local that is
    // never used, so this single override covers the whole fluid push pass.
    @Override public boolean isPushedByFluid(FluidType type) { return false; }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public void kill() { discard(); }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
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
}
