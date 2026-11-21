package org.brahypno.maledict.common.item.weapon.incursus;

import com.sammy.malum.registry.common.DamageTypeRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.OwnableEntity;
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
import org.brahypno.changelib.DamageHelper.LivingTargetHelper;
import org.brahypno.maledict.common.item.equipment.lightward.LightwardSet;
import org.brahypno.maledict.network.MaledictNetwork;
import org.brahypno.maledict.registry.MaledictSounds;
import org.jetbrains.annotations.Nullable;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;

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
        // Multipart bosses expose the body plus every animated part as separate entities. One
        // swing must still be one main damage per living root, otherwise each part opens its own
        // probe and the freeze/magic attribute requests would fire once per part.
        targets = uniqueLivingTargets(targets);

        float attackStrength = player.getAttackStrengthScale(0.5f);
        player.swing(InteractionHand.MAIN_HAND, true);
        playSlashEffect(player, attackStrength);

        DamageChannels channels = damageChannels(player);
        // 5 级「武器攻击更加真实」：同一个门禁同时管两个读数 —— 轻档探针改成补满，以及这一刀顺着
        // 从属的契约落到主人身上。整次挥砍只算一次。
        boolean weaponTruth = LightwardSet.unlocksWeaponTopUp(player, weapon);
        boolean attacked = false;
        boolean critical = false;
        for (Entity target : targets) {
            Swing swing = calculateDamage(player, weapon, target, channels.attack(), attackStrength, sweepingLevel);
            float damage = swing.damage();
            if (damage > 0.0f){
                applyScytheMeleeDamage(player, weapon, target, damage, weaponTruth);
                if (weaponTruth){
                    // 认主的生物连主人一起挨这一刀：同一把刀、同一个补满开关、同一个伤害方法，
                    // 不需要另起伤害类型或另一条派发路径。
                    LivingEntity owner = ownerOf(target);
                    if (owner != null){
                        applyScytheMeleeDamage(player, weapon, owner, damage, weaponTruth);
                    }
                }
                if (target instanceof LivingEntity livingTarget){
                    weapon.hurtEnemy(livingTarget, player);
                    IncursusBladeEffects.applySacredEffect(player, livingTarget, weapon);
                    // 冻结/魔法都是 ChangeLib 注册的属性请求；每个主伤害 probe 结束后立即按顺序
                    // 派发，不等到整次挥砍结束。
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
     * 投掷/幻影用的攻击渠道快照。近战主伤害只读 {@code attack}；魔法与冻结由
     * ChangeLib 注册的 attribute 请求在主伤害 probe 之后派发。
     */
    public record DamageChannels(float attack, float magic) {
        public DamageChannels scaled(float factor) {
            if (factor == 1.0f){
                return this;
            }
            return new DamageChannels(attack * factor, magic * factor);
        }
    }

    /**
     * 一次结算算出来的伤害和它算不算暴击；暴击音效按挥砍补，不按目标补。
     */
    private record Swing(float damage, boolean critical) {
    }

    public static DamageChannels damageChannels(LivingEntity attacker) {
        return new DamageChannels(
                (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE),
                (float) attacker.getAttributeValue(LodestoneAttributeRegistry.MAGIC_DAMAGE.get()));
    }

    /**
     * Collapses Forge multipart targets onto their living root, keeping the nearest hitbox per
     * root. This keeps one swing to one main-damage probe per boss even when every part is a
     * separate {@link net.minecraftforge.entity.PartEntity}.
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
     * 结算近战主伤害。冻结/魔法属性请求由 ChangeLib 的 per-probe flush 在主伤害 probe 之后
     * 依次派发，不再由这里探测或补偿。
     */
    private static void applyScytheMeleeDamage(
            ServerPlayer player, ItemStack weapon, Entity target, float damage,
            boolean compensateDamage) {
        IncursusBladeItem.applyTieredDamage(
                weapon,
                target,
                DamageTypeHelper.create(player.level(), DamageTypeRegistry.SCYTHE_MELEE, player),
                damage,
                compensateDamage);
    }

    /**
     * 被打者的主人：只认 {@link OwnableEntity#getOwnerUUID()}，并且只在被打者所在的维度里按 UUID 现查。
     * 原版驯服的宠物与其他模组认主的随从一视同仁；跨维度不追，主人已死或已经离场就算没有。
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
        if (level.getEntity(identity) instanceof LivingEntity owner
            && owner != target
            && owner.isAlive()
            && !owner.isRemoved()){
            return owner;
        }
        return null;
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

    /**
     * 蓄力 0% → 30%、50% → 65%、100% → 100%；只影响视觉，不使用伤害的平方曲线。
     */
    static float slashScale(float attackStrength) {
        return 0.3f + 0.7f * attackStrength;
    }

    private static void playSlashEffect(ServerPlayer player, float attackStrength) {
        SoundHelper.playSound(
                player,
                MaledictSounds.INCURSUS_BLADE_SLASH.get(),
                1.0f,
                RandomHelper.randomBetween(player.getRandom(), 0.9f, 1.1f));
        MaledictNetwork.sendIncursusSlash(player, slashScale(attackStrength));
    }

    /**
     * 一次挥砍里至少打出一个暴击才补一层；逐个目标各放一次的话，横扫五只就是五声叠在一起。
     */
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
