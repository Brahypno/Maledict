package org.brahypno.maledict.common.combat;

import com.sammy.malum.core.helpers.ParticleHelper;
import com.sammy.malum.registry.common.DamageTypeRegistry;
import com.sammy.malum.registry.common.ParticleEffectTypeRegistry;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import org.brahypno.maledict.common.item.IncursusBladeItem;
import org.brahypno.maledict.registry.MaledictSounds;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;

import java.util.Comparator;
import java.util.List;

/**
 * 神侵恶刃的伤害计算与近战挥砍。
 */
public final class IncursusBladeAttack {
    private static final String LAST_ATTACK_TICK = "maledict:last_incursus_blade_attack_tick";
    private static final int NO_PENDING_TARGET = -1;

    /** 腰环斩弧的分段数；每段的精灵弧约 120°，8 段足以首尾相扣成一圈。 */
    private static final int RING_SEGMENTS = 8;
    /** 弧心所在的半径：贴住身体但留在碰撞箱外，正好是镰刀够得到的距离。 */
    private static final double RING_RADIUS = 0.5;
    /** 与 {@code spawnForwardSlashingParticle} 同源的弧心偏移，让每段的弧沿着环的走向摆开。 */
    private static final double RING_ARC_OFFSET = 0.3;
    /** 环的高度，取身高的比例，落在腰上。 */
    private static final double RING_HEIGHT = 0.5;

    /**
     * 正在结算的那次近战命中打的是谁、有没有走到 {@link IncursusBladeItem#hurtEvent}；只在服务端
     * 主线程用，不需要同步，窗口由 {@link #applyScytheMeleeDamage} 成对开关。
     */
    private static int pendingMeleeTargetId = NO_PENDING_TARGET;
    private static boolean pendingMeleeHurtEventSeen;

    public static void perform(ServerPlayer player) {
        ItemStack weapon = player.getMainHandItem();
        if (player.isSpectator() || !(weapon.getItem() instanceof IncursusBladeItem)){
            return;
        }

        long gameTime = player.level().getGameTime();
        if (player.getPersistentData().contains(LAST_ATTACK_TICK)
            && player.getPersistentData().getLong(LAST_ATTACK_TICK) == gameTime){
            return;
        }
        player.getPersistentData().putLong(LAST_ATTACK_TICK, gameTime);

        int sweepingLevel = weapon.getEnchantmentLevel(Enchantments.SWEEPING_EDGE);
        double reach = player.getAttributeValue(ForgeMod.ENTITY_REACH.get()) + 0.5
                       + sweepingLevel * 0.25;
        Vec3 attackOffset = player.getLookAngle().scale(0.4);
        Vec3 attackOrigin = player.position().add(attackOffset);
        AABB searchArea = player.getBoundingBox().move(attackOffset).inflate(reach);
        List<Entity> targets = player.level().getEntities(
                player,
                searchArea,
                target -> isValidTarget(player, target, attackOrigin, reach));
        targets.sort(Comparator.comparingDouble(player::distanceToSqr));

        player.swing(InteractionHand.MAIN_HAND, true);
        playSlashEffect(player);

        float attackStrength = player.getAttackStrengthScale(0.5f);
        DamageChannels channels = damageChannels(player, weapon);
        boolean attacked = false;
        boolean critical = false;
        for (Entity target : targets) {
            Swing swing = calculateDamage(player, weapon, target, channels.attack(), attackStrength, sweepingLevel);
            float damage = swing.damage();
            if (damage > 0.0f){
                boolean reachedHurtEvent = applyScytheMeleeDamage(player, weapon, target, damage);
                if (target instanceof LivingEntity livingTarget){
                    weapon.hurtEnemy(livingTarget, player);
                    IncursusBladeEffects.applySacredEffect(player, livingTarget, weapon);
                    if (!reachedHurtEvent){
                        IncursusBladeItem.applyTieredDamage(
                                weapon,
                                livingTarget,
                                DamageTypeHelper.create(player.level(), DamageTypes.MAGIC, player),
                                channels.magic());
                    }
                    // 细雪通道跟主伤害一起结算，不取决于事件链。
                    IncursusBladeItem.applyTieredDamage(
                            weapon,
                            livingTarget,
                            DamageTypeHelper.create(player.level(), DamageTypes.FREEZE, player),
                            channels.frozen());
                }
                attacked = true;
                critical |= swing.critical();
            }
        }
        if (critical){
            playCriticalSound(player);
        }
        if (attacked){
            IncursusBladeEffects.applyAerialEffect(player, weapon);
        }
        player.resetAttackStrengthTicker();
    }

    /**
     * 一次攻击的三条伤害通道：攻击力与魔法伤害来自实体属性，冻结来自物品 NBT 的碧水等级。
     */
    public record DamageChannels(float attack, float magic, float frozen) {
        public DamageChannels scaled(float factor) {
            if (factor == 1.0f){
                return this;
            }
            return new DamageChannels(attack * factor, magic * factor, frozen * factor);
        }
    }

    /** 一次结算算出来的伤害和它算不算暴击；暴击音效按挥砍补，不按目标补。 */
    private record Swing(float damage, boolean critical) {
    }

    public static DamageChannels damageChannels(LivingEntity attacker, ItemStack weapon) {
        return new DamageChannels(
                (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE),
                (float) attacker.getAttributeValue(LodestoneAttributeRegistry.MAGIC_DAMAGE.get()),
                (float) IncursusBladeItem.getStat(weapon, IncursusBladeItem.POWDER_SNOW_DAMAGE));
    }

    /**
     * 结算近战主伤害并探测它有没有走进 {@link IncursusBladeItem#hurtEvent}；探针走回退路径
     * （无敌帧、免疫、setHealth 兜底）时事件没发生，返回 false。
     */
    private static boolean applyScytheMeleeDamage(
            ServerPlayer player, ItemStack weapon, Entity target, float damage) {
        pendingMeleeTargetId = target.getId();
        pendingMeleeHurtEventSeen = false;
        try {
            IncursusBladeItem.applyTieredDamage(
                    weapon,
                    target,
                    DamageTypeHelper.create(player.level(), DamageTypeRegistry.SCYTHE_MELEE, player),
                    damage);
            return pendingMeleeHurtEventSeen;
        }
        finally {
            pendingMeleeTargetId = NO_PENDING_TARGET;
            pendingMeleeHurtEventSeen = false;
        }
    }

    /**
     * 由 {@link IncursusBladeItem#hurtEvent} 调用。只认「同一个目标 + scythe_melee」，
     * Lodestone 补打的 {@code minecraft:magic} 伤害不会把自己算成主伤害。
     */
    public static void markMeleeHurtEvent(LivingEntity target, DamageSource source) {
        if (pendingMeleeTargetId == target.getId() && source.is(DamageTypeRegistry.SCYTHE_MELEE)){
            pendingMeleeHurtEventSeen = true;
        }
    }

    private static Swing calculateDamage(
            ServerPlayer player, ItemStack weapon, Entity target,
            float baseDamage, float attackStrength, int sweepingLevel) {
        MobType mobType = target instanceof LivingEntity livingTarget
                          ? livingTarget.getMobType()
                          : MobType.UNDEFINED;
        float enchantmentDamage = EnchantmentHelper.getDamageBonus(weapon, mobType);
        baseDamage *= 0.2f + attackStrength * attackStrength * 0.8f;
        enchantmentDamage *= attackStrength;
        float sweepingDamageMultiplier = 1.0f + (float) sweepingLevel / (sweepingLevel + 4.0f);
        float damage = (baseDamage + enchantmentDamage) * sweepingDamageMultiplier;

        boolean vanillaCritical = attackStrength > 0.9f
                                  && player.fallDistance > 0.0f
                                  && !player.onGround()
                                  && !player.onClimbable()
                                  && !player.isInWater()
                                  && !player.hasEffect(MobEffects.BLINDNESS)
                                  && !player.isPassenger()
                                  && !player.isSprinting();
        CriticalHitEvent criticalHit =
                ForgeHooks.getCriticalHit(player, target, vanillaCritical, vanillaCritical ? 1.5f : 1.0f);
        if (criticalHit != null){
            damage *= criticalHit.getDamageModifier();
            player.crit(target);
        }
        return new Swing(damage, criticalHit != null);
    }

    private static void playSlashEffect(ServerPlayer player) {
        SoundHelper.playSound(
                player,
                MaledictSounds.INCURSUS_BLADE_SLASH.get(),
                1.0f,
                RandomHelper.randomBetween(player.getRandom(), 0.9f, 1.1f));
        spawnSlashRing(player);
    }

    /**
     * 绕身一圈的腰环斩弧。{@code HEAVY_SLASH} 的精灵只在贴图左上方画了一道约 120° 的弧，
     * 弧心离粒子中心约 0.3×scale，所以单发粒子读起来是「正前方一道横斩」。要绕满一圈就得把
     * 弧心摆到一个圆上、并让每段的弧对着自己那一段的方向：
     *
     * <p>Malum 把这套约定封在 {@code spawnForwardSlashingParticle} 里——方向 {@code d} 的
     * 左右轴 {@code left = (-cos yaw, 0, sin yaw)}、上轴 {@code up = left × d}，粒子落在
     * {@code position + d×0.4 + up×(-0.3)}，自旋角取 {@code atan2(d.x, d.z)}。这里照抄同一套
     * 基，只是把弧心绕玩家轴转一圈，于是每段的弧都朝外、连成一条闭合的腰环。
     */
    private static void spawnSlashRing(ServerPlayer player) {
        for (int i = 0; i < RING_SEGMENTS; i++) {
            RingSegment segment = ringSegment(i);
            Vec3 direction = segment.direction();
            // 弧心沿半径外移；再按 Malum 那套基横向让开，让每段的弧顺着环的走向摆，而不是挤在一点。
            Vec3 left = new Vec3(-direction.z, 0.0, direction.x);
            Vec3 offset = direction.scale(RING_RADIUS)
                                  .add(left.cross(direction).scale(-RING_ARC_OFFSET));
            Vec3 position = player.position()
                                  .add(offset.x, player.getBbHeight() * RING_HEIGHT, offset.z);
            ParticleHelper.createSlashingEffect(ParticleEffectTypeRegistry.SCYTHE_SLASH)
                          .setSpiritType(SpiritTypeRegistry.UMBRAL_SPIRIT)
                          .setSlashAngle(segment.slashAngle())
                          .spawnSlashingParticle(player.level(), position, direction);
        }
    }

    /**
     * 第 {@code index} 段腰环：方向沿半径向外，自旋角用 Malum 的刀光约定
     * {@code atan2(direction.x, direction.z)}，于是这一段的弧正好铺在它自己那格上。
     * 从 +Z 起按 {@link #RING_SEGMENTS} 等分整圈。
     */
    static RingSegment ringSegment(int index) {
        float yaw = (float) Math.toRadians(index * (360.0f / RING_SEGMENTS));
        Vec3 direction = new Vec3(Math.sin(yaw), 0.0, Math.cos(yaw));
        return new RingSegment(direction, (float) Mth.atan2(direction.x, direction.z));
    }

    /** 腰环上一段斩弧的朝向和自旋角。 */
    record RingSegment(Vec3 direction, float slashAngle) {
    }

    /** 一次挥砍里至少打出一个暴击才补一层；逐个目标各放一次的话，横扫五只就是五声叠在一起。 */
    private static void playCriticalSound(ServerPlayer player) {
        SoundHelper.playSound(
                player,
                MaledictSounds.INCURSUS_BLADE_CRIT.get(),
                1.2f,
                RandomHelper.randomBetween(player.getRandom(), 0.95f, 1.05f));
    }

    private static boolean isValidTarget(
            ServerPlayer player, Entity target, Vec3 attackOrigin, double reach) {
        if (target == player || !target.isAlive() || !target.isAttackable() || target.skipAttackInteraction(player) || !player.hasLineOfSight(target)){
            return false;
        }
        if (target instanceof Player otherPlayer && !player.canHarmPlayer(otherPlayer)){
            return false;
        }
        double inclusiveReach = reach + target.getBbWidth() * 0.5;
        return attackOrigin.distanceToSqr(target.position()) <= inclusiveReach * inclusiveReach;
    }

    private IncursusBladeAttack() {
    }
}
