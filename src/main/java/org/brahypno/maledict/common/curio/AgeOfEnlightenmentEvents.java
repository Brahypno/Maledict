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
 * 启蒙之年的玩法。五块内容各自独立：
 * <ul>
 *   <li><b>常驻</b>：{@link CooldownSpeed} 每 tick 给佩戴者的物品冷却加速，戴着就有，
 *       与伤害、击杀都无关（见 {@link #onPlayerTick}）；</li>
 *   <li><b>半血触发</b>：攻击半血生物时刷魂息虚空，目标身上带一道
 *       {@link SpiritVoidCooldownCapability} 限流（见 {@link #onLivingHurt}）；</li>
 *   <li><b>击杀</b>：刷新启蒙之年，并给附近一名敌人黑暗年代（见 {@link #onLivingDeath}）；</li>
 *   <li><b>传染</b>：身上带着黑暗年代的生物被「有启蒙之年」的伤害源打中时，那份黑暗年代
 *       跳到它附近另一名敌人身上（见 {@link #onDarknessBearerHurt}）；</li>
 *   <li><b>必暴击</b>：身上有启蒙之年时出手必定暴击，且效果不会被这一击消耗
 *       （见 {@link #onCriticalHit}）。</li>
 * </ul>
 *
 * <p>常驻、半血触发、击杀这三块的成立条件是「护符在饰品栏里」；传染与必暴击只看启蒙之年
 * 这个效果本身：它是护符击杀时给的，最长能独立活 10 秒，那段时间里摘掉护符也照样暴击、
 * 照样传染。
 *
 * <p>击杀给出的两种效果都从护符的 NBT 上读等级（见 {@link EnlightenmentLevel}），
 * 读不到就是 0 级，也就是与加等级之前完全一样的行为。
 *
 * <p>「刷魂息虚空」沿用 Malum 监视者项链的做法：召唤
 * {@link SpiritCollectionActivatorEntity}——也就是 Malum 里名为 {@code pneuma_void}
 * （魂息虚空）的那个实体——让它们从目标身上飘出来。监视者项链在攻击满血生物时刷，
 * 这里改成攻击半血生物时刷。
 *
 * <p>只挂在 {@link Mod.EventBusSubscriber.Bus#FORGE} 上，不注册客户端逻辑，
 * 所以专用服务器同样能跑。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AgeOfEnlightenmentEvents {

    /** {@code cooldownSpeed} 配置读不到时的退路：两倍速。 */
    private static final double DEFAULT_COOLDOWN_SPEED = 2.0D;

    /** 魂息虚空的初速度上限，与监视者项链一致。 */
    private static final float VOID_SPREAD = 0.4F;

    private static final float VOID_UPWARD_MIN = 0.05F;
    private static final float VOID_UPWARD_MAX = 0.06F;

    /** 玩家身上刷 5 个，非玩家目标刷 2 个，同样是监视者项链的配额。 */
    private static final int VOID_COUNT_PLAYER = 5;
    private static final int VOID_COUNT_OTHER = 2;

    /** 启蒙之年持续 10 秒；每次击杀再续 10 秒。 */
    private static final int EFFECT_DURATION_TICKS = 200;

    /** 必暴击打出的伤害倍率：原版暴击就是 1.5 倍。 */
    private static final float CRIT_DAMAGE_MULTIPLIER = 1.5F;

    /** 黑暗年代的撒播半径（格）：击杀时丢给玩家附近最近的一名敌人，传染时以受击者为心。 */
    private static final double DARKNESS_RADIUS = 16.0D;

    /**
     * 佩戴时常驻的冷却加速。
     *
     * <p>这不是伤害或击杀触发的，只要护符在饰品栏里就一直在跑。每个服务端 tick 让玩家身上
     * 正在走的冷却多走一格——所以是「速度」而不是「一次性砍短」，之后新进入冷却的物品自动享受。
     *
     * <p>{@code PlayerTickEvent} 只在服务端有意义：原版把冷却记在 {@code ServerPlayer} 上，
     * 客户端那边只是一份播放用的副本。
     */
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

    /**
     * 加速倍率。
     *
     * <p>配置读取本身也包一层：配置文件在极早期就被读，而这个处理器可能比它先跑起来，
     * 与其因为一次配置时机问题丢掉常驻效果，不如退回默认的两倍速。
     */
    private static double cooldownSpeed() {
        try {
            return MaledictConfig.ENLIGHTENMENT_COOLDOWN_SPEED.get();
        } catch (RuntimeException exception) {
            return DEFAULT_COOLDOWN_SPEED;
        }
    }

    /**
     * 攻击半血生物时触发——与监视者项链的「攻击满血生物」相对。
     *
     * <p>用 {@code LivingHurtEvent}，与监视者项链一致。Forge 的时序是
     * 「onLivingHurt（原始伤害）→ 护甲/附魔/吸收减免 → onLivingDamage（最终伤害）→ setHealth」，
     * 两个事件里 {@code getHealth()} 都还是**受伤前**的值，正好就是「出手时它剩多少血」。
     *
     * <p>目标身上那道 {@link SpiritVoidCooldownCapability} 是照着监视者项链配的限流，
     * 不需要写进 tooltip——那边也没提自己有冷却。
     */
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
     * 黑暗年代的传染：身上带着黑暗年代的生物，被「有启蒙之年」的伤害源打中时，
     * 那份黑暗年代跳到它附近最近的另一名敌人身上。
     *
     * <p>这是「启蒙之年把黑暗推给别人」这件事的实指：击杀只丢出去一次，之后每打一下那个
     * 被标记的敌人，黑暗就再往前挪一格。挨打的那只自己身上的黑暗不动，它只是个搬运工——
     * 所以一直打同一只，黑暗就一直在人群里换目标。
     *
     * <p>等级取<b>伤害源启蒙之年的等级</b>，不是受击者身上那份的：那是这件饰品 NBT 上写的值
     * （见 {@link EnlightenmentLevel}），传染既然是启蒙之年干的活，就按推它的那个人算。
     * 这也延续了 {@link #onCriticalHit} 「只看效果、不看护符还在不在」的做法——效果还挂着，
     * 传播就照常发生。
     *
     * <p>伤害源是生物就行，没有写死成玩家：现在能带上启蒙之年的只有玩家，但没必要现在就把
     * 这条规则锁死。致命一击会同时走到 {@link #onLivingDeath}（那边按护符的等级再丢一次），
     * 两边的目标通常就是同一只，{@link #grant} 把时间叠上去，不会挂出两份效果。
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
            // 夹一次负数：等级乘数走的是 amount * (amplifier + 1)，负等级会把黑暗年代的
            // 减益翻成增益。护符那条路已经在 EnlightenmentLevel 里夹过，这里是第二道。
            int level = Math.max(EnlightenmentLevel.FALLBACK, enlightenment.getAmplifier());
            grant(next, MaledictMobEffects.AGE_OF_DARKNESS.get(), level);
        }
    }

    /**
     * 击杀：佩戴者续上启蒙之年，附近最近的一名敌人收下黑暗年代。
     *
     * <p>两种效果的等级是同一个数——护符 NBT 上那个，见
     * {@link AgeOfEnlightenmentItem#equippedLevel}。
     */
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
     * 身上挂着启蒙之年时，出手必定暴击。
     *
     * <p>用 Forge 的 {@link CriticalHitEvent} 而不是像 Malum 那样在 {@code LivingHurtEvent} 里
     * 把伤害乘二。它的事件文档写得很直白：{@code ALLOW} 就是「this attack is forced to be critical」，
     * 于是拿到的是一记**真暴击**——原版在 {@code Player.attack} 里看到暴击标记后会把
     * {@code PLAYER_ATTACK_CRIT} 音效和 {@code crit()} 粒子一并放出来，Malum 那种翻倍则是静默的。
     *
     * <p>三处刻意的取舍：
     * <ul>
     *   <li><b>只看效果，不看护符还在不在。</b>效果是护符击杀时给的，最长能独立活 10 秒；
     *       这段时间里摘掉护符也照样暴击。问「戴着没有」是护符的事，这里问的是效果的事。</li>
     *   <li><b>只作用于生物。</b>原版的暴击条件里带 {@code target instanceof LivingEntity}，
     *       对展示框、盔甲架这类目标强行 ALLOW 只会平白放出音效。</li>
     *   <li><b>不消耗效果。</b>这是与 Malum 的 grim_certainty 最大的区别——那边命中后
     *       {@code removeEffect}，这里让它一直挂到自然结束，靠击杀续时。</li>
     * </ul>
     *
     * <p>副作用要认：原版里「是否暴击」与「是否横扫」是互斥的（{@code flag && !flag2} 才走横扫），
     * 所以效果期间镰刀的横扫攻击会被压掉。这是「必定暴击」这个说法本身带来的，不是实现失误。
     *
     * <p>倍率取 {@code max}：如果别的 mod 已经把这一击标成暴击并且给了更高的倍率，
     * 这里不该把它压回 1.5。事件处理器之间没有顺序保证，取大值让结果与顺序无关。
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
     * 给目标挂上效果；已经挂着就续时，等级只升不降。
     *
     * <p>没有效果时直接 {@code addEffect}。已有的情况下分两种：
     * <ul>
     *   <li><b>新等级更高</b>：原版没有改 amplifier 的接口，但 {@code addEffect} 内部那条
     *       「新效果时间更长才覆盖旧效果」的规则（{@code MobEffectInstance#update}）在新等级
     *       更高时会直接采用新实例的等级和时间，所以把剩余时间加上一个
     *       {@link #EFFECT_DURATION_TICKS} 再喂回去，就等于「续时并抬级」。传进去的时间
     *       必须比剩余时间长：短了原版会把旧的那份存成 {@code hiddenEffect}，
     *       等新效果快结束时把等级降回去。</li>
     *   <li><b>等级不高于现有的</b>：走 {@link EntityHelper#extendEffect}——Lodestone 给的
     *       处理方式，它直接加 duration 并把变化同步给客户端，比 {@code addEffect} 再读回来
     *       稳妥，也不会被原版那条规则吃掉。</li>
     * </ul>
     *
     * <p>不降级：一段已经存在的黑暗不会因为伤害源换了枚低等级护符而变弱，启蒙之年同理。
     * 战斗中护符等级不会变，这条只在「中途换了另一枚护符」这类边缘情况下起作用，
     * 取高的那个更不容易出意外。
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

    /** 击杀路径：把黑暗年代丢给玩家附近最近的一名敌人，等级跟着护符走。 */
    private static void markNearestEnemy(ServerLevel level, LivingEntity player, LivingEntity victim,
                                         int amplifier) {
        LivingEntity nearest = nearestEnemy(level, player, victim);
        if (nearest != null) {
            grant(nearest, MaledictMobEffects.AGE_OF_DARKNESS.get(), amplifier);
        }
    }

    /**
     * 以 {@code center} 为心 {@link #DARKNESS_RADIUS} 格内最近的「敌人」，判定见
     * {@link #isEnemy}。另外要求活着、受影响于药水（排掉盔甲架这类
     * {@code isAffectedByPotions()} 为 false 的实体）。按距离取最近的一只，
     * 找不到就是 {@code null}。
     *
     * <p><b>为什么不用 {@code entity.canAttack(player)}</b>：那个方法名读着像「这只怪能不能打
     * 玩家」，实现却是 {@code target.canBeSeenAsEnemy()}——它问的是<b>目标</b>（玩家）能不能被
     * 当成敌人，跟候选怪本身毫无关系。而 {@code Player.canBeSeenAsEnemy()} 在创造模式下必定为
     * false（{@code abilities.invulnerable}），和平难度下 {@code canAttack} 也直接返回 false：
     * 两种情况下所有候选会一起被判否，一只怪都选不出来——恰好是测试新饰品的常规环境。
     *
     * <p>代价是 PvP 里别的玩家不算敌人。这件饰品丢的是「附近的一名敌人」，指的就是怪。
     *
     * <p>{@code center} 一定要排掉：玩家自己在框里、距离为 0，不排掉的话「最近的一名敌人」
     * 永远是他本人。{@code excluded} 是给调用方补的（击杀时那只刚死的怪虽然已经不满足
     * {@code isAlive()}，仍然显式排一次；传染时要排掉伤害源）。
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

    /**
     * 这只生物算不算「敌人」。两条取并集：
     * <ul>
     *   <li>原版 {@link Enemy} 标记——僵尸、骷髅、苦力怕、史莱姆、恶魂、末影龙、
     *       灾厄村民、猪灵这一系「与玩家为敌」的生物；</li>
     *   <li>{@link MobCategory#MONSTER} 分类——补上没实现 {@code Enemy} 的怪。最直接的一例
     *       是本模组自己的无常 Boss：{@code VicissitudeBossEntity} 继承 {@code PathfinderMob}，
     *       注册时用的却是 {@code MONSTER} 分类，只认标记就会把它漏掉。</li>
     * </ul>
     *
     * <p>两条都不看玩家自己的状态，所以创造模式、和平难度下照样选得出目标——这正是
     * {@code canAttack} 那套判定栽跟头的地方，见 {@link #nearestEnemy}。狼、马、村民这类
     * 既没有标记也不是 MONSTER 分类，不会被当成敌人。
     */
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

    /**
     * 在目标胸口刷一圈魂息虚空，并当场收取。
     *
     * <p>数量、初速、上扬，以及「挂上去之后调 {@code collect()}」，全部照抄监视者项链，
     * 唯一区别是触发条件。{@code collect()} 会把精魂算到 {@code owner} 头上——也就是佩戴者，
     * 它的构造参数里传的是 {@code target.getUUID()}，那是定位用的，不是归属。
     * 这一步才是 tooltip 里「收获精魂」四个字的实指。
     */
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
