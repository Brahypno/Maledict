package org.brahypno.maledict.common.curio;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.RuneOfBlissItem;
import org.jetbrains.annotations.Nullable;

/**
 * 「无忧符文」的出手点：把一对打得起来的人绑在同一条命运上。
 *
 * <p>两件事：
 *
 * <ol>
 *   <li><b>增伤</b>：{@code 1 + 0.70 × |攻方血量比 − 守方血量比|}，攻守两侧同一个倍率（见
 *       {@link SharedFate}）。只乘一次 —— 两边都戴着不会变成平方，因为这是「这一对关系」的属性，
 *       不是每一方各自的效果。</li>
 *   <li><b>强制命中</b>：{@code LivingAttackEvent} / {@code LivingHurtEvent} / {@code LivingDamageEvent}
 *       三个事件挂在**最低优先级**，接到已经**被取消**的那一个，就替这一记补打一次
 *       {@link DamageProbe#lighterDamageMethod}：medium 档、但**不要求打满**（只要真掉血就收手）。</li>
 * </ol>
 *
 * <p>为什么挂在最低优先级：这一符文要的是「最后说话」。别人的减伤、免疫、闪避都已经表过态，
 * 到我们这里还剩下多少就是多少 —— 增伤乘的是**别人处理完之后**的量，取消也才看得见
 * （{@code EventPriority.LOWEST} 是最后一个跑，此时 {@code isCanceled()} 才是最终答案）。
 *
 * <p><b>为什么必须写 {@code receiveCanceled = true}：</b>{@code ASMEventHandler#invoke} 是
 * {@code if (!event.isCanceled() || subInfo.receiveCanceled())} 才把事件交到方法里，
 * 而注解上的 {@code receiveCanceled} 默认 {@code false}。不写这一条，被取消的事件**根本不会进这三个
 * 方法**，「接到已经取消的那一个就补打」这一段就成了死代码 —— 优先级排得再后也没用。
 *
 * <p>「双方」必须是两个生物：没有攻击者的伤害（摔落、火、毒）没有可比的血量比，也没有可言的
 * 「攻击命中」，一律不介入 —— 倍率是 {@code 1}，取消也不补打。自己打自己同理（比值为 0）。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlissRuneEvents {

    /**
     * 正在替被打断的一记补打。
     *
     * <p>补打走的是探针，探针自己会再走一遍 {@code hurt} 与这三个事件：取消方多半会照旧取消，
     * 于是「取消 → 补打 → 又取消 → 再补打」可以无限递归下去。这一段里我们一律不出手，
     * 爬完剩下的梯级是探针自己的事（medium 档会继续往 {@code setHealth} 那一档找路）。
     *
     * <p>顺带解决乘两遍：补打的那一记**已经乘过倍率**，闸门一放我们也不会再乘第二次。
     *
     * <p>代价写在明处：补打期间若**别的**生物也挨了一下，那一记不享受符文。伤害事件在服务端
     * 单线程上跑，一个布尔就够，不用按实体记账。
     */
    private static boolean forcing;

    /**
     * 攻击在门口就被挡下（另一方的取消、无敌、闪避、阶段免疫）——把这一记补进去。
     *
     * <p>没被取消时这里什么都不做：本事件只有量、没有落点，增伤留给随后的 {@code LivingHurtEvent}，
     * 免得同一记乘两遍。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!event.isCanceled()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        Float multiplier = multiplierOf(victim, event.getSource());
        if (multiplier == null) {
            return;
        }
        force(victim, event.getSource(), event.getAmount() * multiplier);
    }

    /** 正常一记在这里乘倍率；被取消的这一记连倍率一起补进去。 */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Float multiplier = multiplierOf(victim, event.getSource());
        if (multiplier == null) {
            return;
        }
        float amount = event.getAmount() * multiplier;
        if (event.isCanceled()) {
            force(victim, event.getSource(), amount);
            return;
        }
        event.setAmount(amount);
    }

    /**
     * 护甲、附魔、吸收都算完之后（{@code actuallyHurt} 里、写血量之前）被打断 ——补进去。
     *
     * <p>这里**不再乘倍率**：能走到这一步说明 {@code LivingHurtEvent} 已经放过行，倍率乘过了。
     * 同时也意味着这一记走的是原版正路，补的也是已经减过防的量。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!event.isCanceled()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (multiplierOf(victim, event.getSource()) == null) {
            return;
        }
        force(victim, event.getSource(), event.getAmount());
    }

    /**
     * 这一记的倍率；**不成对**返回 {@code null}。
     *
     * <p>不成对的四种情形：正在补打、客户端、没有攻击者（或攻击者不是生物）、自己打自己；
     * 以及最要紧的一条 —— 攻守两边**都没戴**这枚符文。
     */
    @Nullable
    private static Float multiplierOf(LivingEntity victim, DamageSource source) {
        if (forcing || victim.level().isClientSide()) {
            return null;
        }
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker) || attacker == victim) {
            return null;
        }
        if (!RuneOfBlissItem.isEquipped(attacker) && !RuneOfBlissItem.isEquipped(victim)) {
            return null;
        }
        return SharedFate.multiplier(
                SharedFate.healthRatio(attacker.getHealth(), attacker.getMaxHealth()),
                SharedFate.healthRatio(victim.getHealth(), victim.getMaxHealth()));
    }

    /**
     * 补一记被打断的伤害：medium 档、落地即止。
     *
     * <p><b>不要求打满</b>是刻意的：这一符文的承诺是「打得中」，不是「打得动」。所以用
     * {@link DamageProbe#lighterDamageMethod}（medium 档的梯级、拿下第一笔真实掉血就收手），
     * 而不是会一路补到指定数值的 {@code mediumDamageMethod} —— 后者会去撬防御，
     * 对一个「让攻击能命中」的效果来说过头了。
     */
    private static void force(LivingEntity victim, DamageSource source, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        forcing = true;
        try {
            DamageProbe.lighterDamageMethod(victim, source, amount);
        } finally {
            forcing = false;
        }
    }

    private BlissRuneEvents() {
    }
}
