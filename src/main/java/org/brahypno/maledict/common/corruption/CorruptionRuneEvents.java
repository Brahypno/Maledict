package org.brahypno.maledict.common.corruption;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.RuneOfTheFallenItem;
import org.brahypno.maledict.registry.MaledictMobEffects;
import org.jetbrains.annotations.Nullable;
import team.lodestar.lodestone.helpers.EntityHelper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 腐败符文的两个出手点，另外半边在 {@link CorruptionState}（账目与净化）。
 *
 * <ol>
 *   <li><b>回血记账</b>（{@code LivingHealEvent}）：目标正在 {@code Fallen} 窗口里时，把**有效**回血量
 *       折算成休眠腐败记下来；治疗本身一分不减（这是这枚符文的身份，见原文 §3.1）。</li>
 *   <li><b>命中收账</b>（{@code LivingDamageEvent}）：攻击方戴着符文时，先收走活性腐败、再落下/刷新
 *       {@code Fallen}、最后把账夹进这一记打完之后的血量里。</li>
 * </ol>
 *
 * <p>为什么两个都挂在 {@code LivingDamageEvent} 这一档、以及为什么收在写血之前：{@code LivingDamageEvent}
 * 发在护甲、附魔、吸收都算完之后、**写血之前**，所以在这里直写血量得到的顺序天然是「先收后打」——
 * 我们改完血量，原版紧接着 {@code setHealth(getHealth() - f1)}，读到的是收完之后的值。
 *
 * <p>被取消的命中不结算：注解上的 {@code receiveCanceled} 默认 {@code false}，被取消的事件根本进不来
 * （见 {@code AGENTS.md} 的 Confirmed mechanics）。这是用户裁定的「不管他」。
 *
 * <p>收用直写血量而不是另发 {@code DamageSource}：另发一记会走 {@code hurt}，被无敌帧挡、被吸收吃掉、
 * 被别的模组减伤/取消、会触发图腾、也会递归触发我们自己。代价与抑郁符文的延迟池一样：不吃图腾、
 * 不续无敌帧、没有受伤演出 —— 而「收不致死」保证了这个代价不会变成击杀。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CorruptionRuneEvents {

    /** 一记有效回血折算成多少腐败（点）。先按 1:1 起手，进游戏再调。 */
    public static final float CORRUPTION_PER_HEAL = 1.0F;

    /**
     * 单次收的上限（点）。
     *
     * <p>没有它，「治疗攒满 + 收一下」就是对任何会自愈的目标的一击处决：账夹到多少就能收多少。
     */
    public static final float MAX_COLLAPSE_PER_HIT = 6.0F;

    /** 收给目标留下的最低血量：收永远不致死，真正致命的是紧接着落地的那一记普通攻击。 */
    public static final float MIN_SURVIVABLE_HEALTH = 1.0F;

    // ---- 堕落标记（Fallen）的预设值：效果本身没有逻辑，所以没有自己的类，注册处是匿名子类 ----

    /** 每次命中把窗口刷新多久（tick）。 */
    public static final int FALLEN_DURATION_TICKS = 200;

    /** 最多 10 级；游戏里的「等级」= amplifier + 1。 */
    public static final int FALLEN_MAX_LEVEL = 10;

    public static final int FALLEN_MAX_AMPLIFIER = FALLEN_MAX_LEVEL - 1;

    /** 每级给目标多少 {@code malum:malignant_conversion}（恶念转化，0.02 = 2%）。 */
    public static final double FALLEN_CONVERSION_PER_LEVEL = 0.02D;

    /** 效果图标底色：暗紫。 */
    public static final int FALLEN_COLOR = 0x5B2A6E;

    /**
     * 恶念转化那条修饰符的 id。
     *
     * <p>改它等于把旧存档里已经挂上的修饰符作废（旧的那条会一直留在实体属性上），别改。
     * 原版 {@code addAttributeModifier} 收的是它的**字符串**形式（内部 {@code UUID.fromString}）。
     */
    public static final UUID FALLEN_MODIFIER_ID =
            UUID.nameUUIDFromBytes("maledict:fallen/malignant_conversion".getBytes(StandardCharsets.UTF_8));

    private static final int COLLAPSE_PARTICLES = 12;

    /** 回血记账：治疗照给，只把「有效的那一份」记成休眠腐败。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHeal(LivingHealEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || event.isCanceled()) {
            return;
        }
        if (!target.hasEffect(MaledictMobEffects.FALLEN.get())) {
            return;
        }
        float effective = effectiveHeal(event.getAmount(), target.getHealth(), target.getMaxHealth());
        if (effective <= 0.0F) {
            return;
        }
        CorruptionData data = CorruptionData.get(target);
        if (data == null) {
            return;
        }
        long now = target.level().getGameTime();
        CorruptionState state = data.state();
        state.purify(now);
        state.gain(effective * CORRUPTION_PER_HEAL, now);
        state.clamp(target.getHealth());
    }

    /** 命中：先收、再落标记、最后夹取。顺序见类注释。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.isCanceled()) {
            return;
        }
        CorruptionData data = CorruptionData.get(victim);
        if (data == null) {
            return;
        }
        LivingEntity attacker = attackerOf(event.getSource());
        boolean withRune = attacker != null && attacker != victim && RuneOfTheFallenItem.isEquipped(attacker);
        CorruptionState state = data.stateIfPresent();
        // 没戴符文的这一记如果打在没账目的目标身上，就与我们完全无关：直接走人。
        if (!withRune && (state == null || state.isEmpty())) {
            return;
        }

        long now = victim.level().getGameTime();
        float health = victim.getHealth();
        if (state != null) {
            state.purify(now);
        }

        if (withRune) {
            if (state != null) {
                health = collapse(victim, state, health);
            }
            mark(victim);
        }

        // 这一记接下来真会从血量里扣掉的量：原版在事件之后才写血量。
        if (state != null) {
            state.clamp(health - event.getAmount());
        }
    }

    /**
     * 落下/刷新 {@code Fallen}：第一次是 I 级，已经在身上就涨一级、并把窗口刷新。
     *
     * <p>涨级与续时都借 Lodestone 的 {@code EntityHelper}：它直接改实例上的 amplifier/duration 字段
     * （那两个字段是私有的，我们这边靠 AT 才能碰），最后调 {@code syncEffect} —— 而
     * {@code syncEffect} 走的是原版 {@code LivingEntity#onEffectUpdated(effect, true, entity)}，
     * 于是**属性修饰符会跟着换成新等级的量**，客户端也拿到新等级。封顶由 helper 的
     * {@code maxAmplifier} 参数负责（既不会超过 {@link #FALLEN_MAX_AMPLIFIER}，也不会把等级压下去）。
     */
    private static void mark(LivingEntity victim) {
        MobEffectInstance current = victim.getEffect(MaledictMobEffects.FALLEN.get());
        if (current == null) {
            victim.addEffect(new MobEffectInstance(MaledictMobEffects.FALLEN.get(),
                                                   FALLEN_DURATION_TICKS, 0, false, true));
            return;
        }
        EntityHelper.amplifyEffect(current, victim, 1, FALLEN_MAX_AMPLIFIER);
        EntityHelper.extendEffect(current, victim, FALLEN_DURATION_TICKS, FALLEN_DURATION_TICKS);
    }

    /**
     * 有效回血量。
     *
     * <p>{@code LivingHealEvent} 发在 {@code heal} 的**第一行**：满血、已死、被取消时它照样发，
     * 所以不能直接拿 {@code getAmount()} 记账 —— 满血连喝三瓶药水会凭空长出一堆腐败。
     * {@code heal} 只在血量 &gt; 0 时才写血，而 {@code setHealth} 夹到生命上限，于是有效量就是
     * {@code min(amount, maxHealth - health)}。
     */
    private static float effectiveHeal(float amount, float health, float maximum) {
        if (amount <= 0.0F || health <= 0.0F) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(amount, maximum - health));
    }

    /** 收一笔，返回收完之后目标的血量。收不动就原样返回。 */
    private static float collapse(LivingEntity victim, CorruptionState state, float health) {
        float taken = state.harvest(MAX_COLLAPSE_PER_HIT, health, MIN_SURVIVABLE_HEALTH);
        if (taken <= 0.0F) {
            return health;
        }
        float left = health - taken;
        victim.setHealth(left);
        if (victim.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                                victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                                COLLAPSE_PARTICLES, 0.3D, 0.4D, 0.3D, 0.02D);
        }
        return left;
    }

    /** 造成这一记的人；箭、火球之类取直接实体，都没有就返回 {@code null}。 */
    @Nullable
    private static LivingEntity attackerOf(DamageSource source) {
        Entity entity = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        return entity instanceof LivingEntity living ? living : null;
    }

    private CorruptionRuneEvents() {
    }
}
