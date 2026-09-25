package org.brahypno.maledict.common.curio;

import com.sammy.malum.common.entity.activator.SpiritCollectionActivatorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.AgeOfEnlightenmentItem;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.registry.MaledictMobEffects;
import team.lodestar.lodestone.helpers.EntityHelper;
import team.lodestar.lodestone.helpers.RandomHelper;

import java.util.List;
import javax.annotation.Nullable;

/**
 * 启蒙之年的玩法钩子。常驻加速、半血刷魂息虚空、击杀续效果都要求护符在饰品栏里；传染与必暴击只看启蒙之年这个效果本身。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AgeOfEnlightenmentEvents {

    private static final double DEFAULT_COOLDOWN_SPEED = 2.0D;

    private static final float VOID_SPREAD = 0.4F;

    private static final float VOID_UPWARD_MIN = 0.05F;
    private static final float VOID_UPWARD_MAX = 0.06F;

    private static final int VOID_COUNT_PLAYER = 5;
    private static final int VOID_COUNT_OTHER = 2;

    private static final int EFFECT_DURATION_TICKS = 200;

    /** 原版暴击的伤害倍率。 */
    private static final float CRIT_DAMAGE_MULTIPLIER = 1.5F;

    /** 半径（格）；击杀以玩家为心，传染以受击者为心。 */
    private static final double DARKNESS_RADIUS = 16.0D;

    /** 常驻冷却加速：每个服务端 tick 让玩家身上的冷却多走一格，之后新进入冷却的物品同样享受。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide()) {
            return;
        }
        CooldownSpeed.apply(player, cooldownSpeed());
    }

    /** 配置可能在处理器启动前被读取，读不到就退回默认倍率。 */
    private static double cooldownSpeed() {
        try {
            return MaledictConfig.ENLIGHTENMENT_COOLDOWN_SPEED.get();
        } catch (RuntimeException exception) {
            return DEFAULT_COOLDOWN_SPEED;
        }
    }

    /** 攻击半血生物时刷魂息虚空；{@code LivingHurtEvent} 里 {@code getHealth()} 还是受伤前的值，正是出手时目标剩的血。 */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        if (!HalfHealth.isAtOrBelowHalf(target.getHealth(), target.getMaxHealth())) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof Player wearer)) {
            return;
        }
        if (!AgeOfEnlightenmentItem.isEquipped(wearer)) {
            return;
        }
        if (!SpiritVoidCooldownCapability.tryUse(target)) {
            return;
        }

        spawnSpiritVoid(target, wearer);
    }

    /**
     * 传染：黑暗年代的携带者被「有启蒙之年」的伤害源打中时，那份黑暗年代跳到附近最近的另一名敌人身上；
     * 携带者自己那份不动，等级取伤害源的启蒙之年而不是受击者的。
     */
    @SubscribeEvent
    public static void onDarknessBearerHurt(LivingHurtEvent event) {
        LivingEntity carrier = event.getEntity();
        if (carrier.level().isClientSide()) {
            return;
        }
        if (carrier.getEffect(MaledictMobEffects.AGE_OF_DARKNESS.get()) == null) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity source)) {
            return;
        }
        MobEffectInstance enlightenment = source.getEffect(MaledictMobEffects.AGE_OF_ENLIGHTENMENT.get());
        if (enlightenment == null) {
            return;
        }
        if (!(carrier.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity next = nearestEnemy(serverLevel, carrier, source);
        if (next != null) {
            // 夹一次负数：等级乘数是 amplifier + 1，负等级会把减益翻成增益。
            int level = Math.max(EnlightenmentLevel.FALLBACK, enlightenment.getAmplifier());
            grant(next, MaledictMobEffects.AGE_OF_DARKNESS.get(), level);
        }
    }

    /** 击杀：佩戴者续上启蒙之年，附近最近的敌人收下黑暗年代，等级都取自护符 NBT。 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (!AgeOfEnlightenmentItem.isEquipped(player)) {
            return;
        }

        int level = AgeOfEnlightenmentItem.equippedLevel(player);
        grant(player, MaledictMobEffects.AGE_OF_ENLIGHTENMENT.get(), level);
        if (victim.level() instanceof ServerLevel serverLevel) {
            markNearestEnemy(serverLevel, player, victim, level);
        }
    }

    /**
     * 身上挂着启蒙之年时，出手必定暴击。走 {@link CriticalHitEvent}：{@code ALLOW} 拿到的是真暴击，
     * 音效与粒子照常，而不是悄悄把伤害翻倍。
     *
     * <p>副作用：原版暴击与横扫互斥，所以效果期间镰刀的横扫会被压掉。
     */
    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        if (!(event.getTarget() instanceof LivingEntity)) {
            return;
        }
        Player player = event.getEntity();
        if (!player.hasEffect(MaledictMobEffects.AGE_OF_ENLIGHTENMENT.get())) {
            return;
        }

        event.setResult(Event.Result.ALLOW);
        event.setDamageModifier(Math.max(event.getDamageModifier(), CRIT_DAMAGE_MULTIPLIER));
    }

    /**
     * 挂效果或续时，等级只升不降。抬级时传入的时间必须长于剩余时间，否则原版会把旧实例存成
     * {@code hiddenEffect}，结束时再把等级降回去；等级不高于现有的时候走 {@link EntityHelper#extendEffect}。
     */
    private static void grant(LivingEntity target, MobEffect effect, int amplifier) {
        MobEffectInstance active = target.getEffect(effect);
        if (active == null) {
            target.addEffect(new MobEffectInstance(effect, EFFECT_DURATION_TICKS, amplifier));
            return;
        }
        if (amplifier > active.getAmplifier()) {
            target.addEffect(new MobEffectInstance(effect,
                    active.getDuration() + EFFECT_DURATION_TICKS, amplifier));
            return;
        }
        EntityHelper.extendEffect(active, target, EFFECT_DURATION_TICKS);
    }

    private static void markNearestEnemy(ServerLevel level, LivingEntity player, LivingEntity victim,
                                         int amplifier) {
        LivingEntity nearest = nearestEnemy(level, player, victim);
        if (nearest != null) {
            grant(nearest, MaledictMobEffects.AGE_OF_DARKNESS.get(), amplifier);
        }
    }

    /**
     * 半径内最近的「敌人」，要求活着且受影响于药水。<b>不要改用 {@code canAttack}：它问的是目标（玩家）能不能被当成敌人，创造模式或和平难度下会把所有候选一起判否。</b>
     * {@code center} 必须排除，否则最近的「敌人」永远是玩家自己。
     */
    private static @Nullable LivingEntity nearestEnemy(ServerLevel level, LivingEntity center,
                                                       LivingEntity... excluded) {
        AABB area = center.getBoundingBox().inflate(DARKNESS_RADIUS);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != center
                        && !isExcluded(excluded, entity)
                        && isEnemy(entity)
                        && entity.isAlive()
                        && entity.isAffectedByPotions());

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double distance = candidate.distanceToSqr(center);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    /** 敌人 = 原版 {@link Enemy} 标记，或 {@link MobCategory#MONSTER} 分类（无常 Boss 注册为后者但不是 {@code Enemy}）。 */
    private static boolean isEnemy(LivingEntity entity) {
        return entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER;
    }

    private static boolean isExcluded(LivingEntity[] excluded, LivingEntity entity) {
        for (LivingEntity candidate : excluded) {
            if (candidate == entity) {
                return true;
            }
        }
        return false;
    }

    /** 在目标胸口刷一圈魂息虚空并当场收取；构造参数里的 UUID 让 {@code collect()} 把精魂算给佩戴者。 */
    private static void spawnSpiritVoid(LivingEntity target, Player wearer) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        int amount = target instanceof Player ? VOID_COUNT_PLAYER : VOID_COUNT_OTHER;
        Vec3 origin = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D);
        for (int i = 0; i < amount; i++) {
            SpiritCollectionActivatorEntity spiritVoid = new SpiritCollectionActivatorEntity(
                    level,
                    wearer.getUUID(),
                    origin.x,
                    origin.y,
                    origin.z,
                    RandomHelper.randomBetween(level.getRandom(), -VOID_SPREAD, VOID_SPREAD),
                    RandomHelper.randomBetween(level.getRandom(), VOID_UPWARD_MIN, VOID_UPWARD_MAX),
                    RandomHelper.randomBetween(level.getRandom(), -VOID_SPREAD, VOID_SPREAD));
            level.addFreshEntity(spiritVoid);
            spiritVoid.collect();
        }
    }

    private AgeOfEnlightenmentEvents() {
    }
}
