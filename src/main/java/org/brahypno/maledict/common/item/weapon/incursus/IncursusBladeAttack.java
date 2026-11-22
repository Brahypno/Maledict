package org.brahypno.maledict.common.item.weapon.incursus;

import com.sammy.malum.registry.common.MalumDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import org.brahypno.changelib.DamageHelper.LivingTargetHelper;
import org.brahypno.maledict.common.item.equipment.lightward.LightwardSet;
import org.brahypno.maledict.network.MaledictNetwork;
import org.brahypno.maledict.registry.MaledictSounds;
import org.jetbrains.annotations.Nullable;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;
import team.lodestar.lodestone.registry.common.LodestoneAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 神侵恶刃的伤害计算与近战挥砍。
 */
public final class IncursusBladeAttack {
    private static final String LAST_ATTACK_TICK = "maledict:last_incursus_blade_attack_tick";
    private static final int NO_PENDING_TARGET = -1;

    /**
     * 正在结算的那次近战命中打的是谁、有没有走到 {@link IncursusBladeItem#outgoingDamageEvent}；只在服务端
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

        int sweepingLevel = org.brahypno.maledict.registry.MaledictEnchantments.level(weapon, Enchantments.SWEEPING_EDGE);
        // 这一次挥砍唯一的范围：球心在腰高、半径按 entity_reach 推并封顶 6 格。
        // 判定与搜索箱都读它，见 IncursusSwingReach。
        double reach = IncursusSwingReach.radiusOf(
                player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), sweepingLevel);
        Vec3 swingCenter = IncursusSwingReach.centerOf(player);
        List<Entity> targets = player.level().getEntities(
                player,
                IncursusSwingReach.searchBox(swingCenter, reach),
                target -> isValidTarget(player, target, swingCenter, reach));
        targets.sort(Comparator.comparingDouble(player::distanceToSqr));
        // 多部位 Boss 会把本体与每个动画部件都暴露成独立实体。一次挥砍仍然只对活体根结算一次主伤害，
        // 否则每个部件各开一次探测、霜冻与奥术的属性请求也会按部件数各派发一轮。
        targets = uniqueLivingTargets(targets);

        float attackStrength = player.getAttackStrengthScale(0.5f);
        player.swing(InteractionHand.MAIN_HAND, true);
        // 腰环半径就是这一次挥砍的伤害半径，蓄力只压薄厚与收尾：画出来的圈就是真正能打到的圈。
        playSlashEffect(player, reach, attackStrength);

        DamageChannels channels = damageChannels(player, weapon);
        // 5 级「武器攻击更加真实」：整次挥砍只算一次，同一个门禁同时管轻档探针补满与认主连带。
        boolean weaponTruth = LightwardSet.unlocksWeaponTopUp(player, weapon);
        boolean attacked = false;
        boolean critical = false;
        for (Entity target : targets) {
            Swing swing = calculateDamage(player, weapon, target, channels.attack(), attackStrength, sweepingLevel);
            float damage = swing.damage();
            if (damage > 0.0f){
                boolean reachedHurtEvent = applyScytheMeleeDamage(player, weapon, target, damage, weaponTruth);
                if (target instanceof LivingEntity livingTarget){
                    weapon.hurtEnemy(livingTarget, player);
                    IncursusBladeEffects.applySacredEffect(player, livingTarget, weapon);
                    applyChannelDamage(player, weapon, livingTarget, channels, reachedHurtEvent);
                }
                if (weaponTruth){
                    // 认主的生物连主人一起挨这一刀：同一把刀、同一个补满开关、同一个伤害方法，
                    // 不需要另起伤害类型或另一条派发路径。
                    LivingEntity master = ownerOf(target);
                    if (master != null){
                        boolean masterHurtEvent =
                                applyScytheMeleeDamage(player, weapon, master, damage, weaponTruth);
                        // 奥术与碧水两条属性通道都由 ChangeLib 的 per-probe flush 跟着这次 probe 自己派发
                        // （ProbeScope → AttributeDamageDispatcher），主人也不例外；这里只补主伤害没走进
                        // hurt 事件时奥术少掉的那一发。
                        applyChannelDamage(player, weapon, master, channels, masterHurtEvent);
                    }
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
     * 奥术那一跳由 ChangeLib 的 per-probe flush 负责（{@code ProbeScope#close} →
     * {@code AttributeDamageDispatcher#dispatch}，见 {@code MaledictAttributes#registerAttributeDamage}）。
     * 这里只在主伤害没走进 {@code LivingDamageEvent.Pre} 时（无敌帧、免疫、兜底写血）补一发，
     * 因为那种情况下 flush 里也没有可派发的命中记录。
     *
     * <p>碧水（冻结）通道不在这里：它同样是属性（{@code maledict:powder_snow_damage}），由同一次
     * flush 派发，所以主目标与认主连带的主人各自被 probe 打中时都会自动带上，不需要第二处判断。
     */
    private static void applyChannelDamage(
            ServerPlayer player, ItemStack weapon, LivingEntity victim,
            DamageChannels channels, boolean reachedHurtEvent) {
        if (!reachedHurtEvent && !victim.isDeadOrDying()){
            IncursusBladeItem.applyTieredDamage(
                    weapon,
                    victim,
                    DamageTypeHelper.create(player.level(), DamageTypes.MAGIC, player),
                    channels.magic() * player.getAttackStrengthScale(0.0F));
        }
    }

    /**
     * 一次攻击的伤害通道：攻击力与魔法伤害来自实体属性；碧水（冻结）是属性，由同一次 probe 的
     * flush 派发，所以不在这张表里。
     */
    public record DamageChannels(float attack, float magic) {
        public DamageChannels scaled(float factor) {
            if (factor == 1.0f){
                return this;
            }
            return new DamageChannels(attack * factor, magic * factor);
        }
    }

    /** 一次结算算出来的伤害和它算不算暴击；暴击音效按挥砍补，不按目标补。 */
    private record Swing(float damage, boolean critical) {
    }

    public static DamageChannels damageChannels(LivingEntity attacker, ItemStack weapon) {
        return new DamageChannels(
                (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE),
                (float) attacker.getAttributeValue(LodestoneAttributes.MAGIC_DAMAGE));
    }

    /**
     * 结算近战主伤害并探测它有没有走进 {@link IncursusBladeItem#outgoingDamageEvent}；探针走回退路径
     * （无敌帧、免疫、setHealth 兜底）时事件没发生，返回 false。
     *
     * <p>{@code compensateDamage} 是 5 级「武器攻击更加真实」：认主连带的那一记必须与被打者用同一个
     * 开关，否则主人与宠物会在防御结算上各走一条路。
     */
    private static boolean applyScytheMeleeDamage(
            ServerPlayer player, ItemStack weapon, Entity target, float damage, boolean compensateDamage) {
        pendingMeleeTargetId = target.getId();
        pendingMeleeHurtEventSeen = false;
        try {
            IncursusBladeItem.applyTieredDamage(
                    weapon,
                    target,
                    DamageTypeHelper.create(player.level(), MalumDamageTypes.SCYTHE_MELEE, player),
                    damage,
                    compensateDamage);
            return pendingMeleeHurtEventSeen;
        }
        finally {
            pendingMeleeTargetId = NO_PENDING_TARGET;
            pendingMeleeHurtEventSeen = false;
        }
    }

    /**
     * 把一批命中目标收敛成「每个活体根一个」：多部位实体的每个 {@code PartEntity} 都属于同一个活体根，
     * 一次挥砍只该对它开一次主探测。
     */
    public static List<Entity> uniqueLivingTargets(List<Entity> targets) {
        Map<Entity, Entity> unique = new LinkedHashMap<>();
        for (Entity target : targets) {
            LivingEntity living = LivingTargetHelper.getLivingTarget(target);
            Entity root = living != null ? living : target;
            unique.putIfAbsent(root, root);
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * 被打者的主人：只认 {@link OwnableEntity#getOwnerUUID()}，并且只在被打者所在的维度里按 UUID 现查。
     * 原版驯服的宠物与其他模组认主的随从一视同仁；跨维度不追，主人已死或已经离场就算没有。
     *
     * <p>没有「自己打自己的宠物」的豁免——主人正好是攻击者本人时，这一刀照样打在自己身上；
     * 主人那一记也不会重新进 {@code perform}，所以认主链不会级联。
     */
    @Nullable
    private static LivingEntity ownerOf(Entity target) {
        if (!(target instanceof OwnableEntity ownable) || !(target.level() instanceof ServerLevel level)){
            return null;
        }
        UUID identity = ownable.getOwnerUUID();
        if (identity == null){
            return null;
        }
        if (level.getEntity(identity) instanceof LivingEntity master
            && master != target
            && master.isAlive()
            && !master.isRemoved()){
            return master;
        }
        return null;
    }

    /**
     * 由 {@code IncursusBladeItem#outgoingDamageEvent(LivingDamageEvent.Pre, …)}（Pre 那一半）调用。
     * 只认「同一个目标 + scythe_melee」，Lodestone 补打的 {@code minecraft:magic} 伤害不会把自己算成主伤害。
     */
    public static void markMeleeHurtEvent(LivingEntity target, DamageSource source) {
        if (pendingMeleeTargetId == target.getId() && source.is(MalumDamageTypes.SCYTHE_MELEE)){
            pendingMeleeHurtEventSeen = true;
        }
    }

    private static Swing calculateDamage(
            ServerPlayer player, ItemStack weapon, Entity target,
            float baseDamage, float attackStrength, int sweepingLevel) {
        float enchantmentDamage = EnchantmentHelper.modifyDamage(
                player.serverLevel(), weapon, target,
                DamageTypeHelper.create(player.level(), MalumDamageTypes.SCYTHE_MELEE, player), baseDamage) - baseDamage;
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
                CommonHooks.fireCriticalHit(player, target, vanillaCritical, vanillaCritical ? 1.5f : 1.0f);
        if (criticalHit.isCriticalHit()){
            damage *= criticalHit.getDamageMultiplier();
            player.crit(target);
        }
        return new Swing(damage, criticalHit.isCriticalHit());
    }

    /**
     * 腰环半径就是这一次挥砍的伤害半径。蓄力只改薄厚与收尾，不改范围——轻点与满蓄力的环一样大、
     * 一样远，玩家看到的圈就是真正能打到的圈。
     */
    private static void playSlashEffect(ServerPlayer player, double reach, float charge) {
        SoundHelper.playSound(
                player,
                MaledictSounds.INCURSUS_BLADE_SLASH.get(),
                1.0f,
                RandomHelper.randomBetween(player.getRandom(), 0.9f, 1.1f));
        MaledictNetwork.sendIncursusSlash(player, reach, charge);
    }

    /** 一次挥砍里至少打出一个暴击才补一层；逐个目标各放一次的话，横扫五只就是五声叠在一起。 */
    private static void playCriticalSound(ServerPlayer player) {
        SoundHelper.playSound(
                player,
                MaledictSounds.INCURSUS_BLADE_CRIT.get(),
                1.2f,
                RandomHelper.randomBetween(player.getRandom(), 0.95f, 1.05f));
    }

    /**
     * 目标过滤：原版的那些闸照旧，范围提问交给 {@link IncursusSwingReach#touches}——
     * 与碰撞箱最近点比距离，而不是拿脚底点凑数。高个 Boss、站在台阶上的怪因此不会再漏判。
     */
    private static boolean isValidTarget(
            ServerPlayer player, Entity target, Vec3 swingCenter, double reach) {
        if (target == player || !target.isAlive() || !target.isAttackable() || target.skipAttackInteraction(player) || !player.hasLineOfSight(target)){
            return false;
        }
        if (target instanceof Player otherPlayer && !player.canHarmPlayer(otherPlayer)){
            return false;
        }
        return IncursusSwingReach.touches(target.getBoundingBox(), swingCenter, reach);
    }

    private IncursusBladeAttack() {
    }
}
