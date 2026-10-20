package org.brahypno.maledict.common.curio.rune;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.rune.RuneOfBlissItem;
import org.jetbrains.annotations.Nullable;

/** SharedFate multiplies Incoming damage once; only a canceled Incoming receives a forced hit. */
@EventBusSubscriber(modid = Maledict.MODID)
public final class BlissRuneEvents {

    /**
     * 正在替被打断的一记补打。
     *
     * <p>补打走的是探针，探针自己会再走一遍 {@code hurt} 与 Incoming 事件：取消方多半会照旧取消，
     * 于是「取消 → 补打 → 又取消 → 再补打」可以无限递归下去。这一段里我们一律不出手，
     * 爬完剩下的梯级是探针自己的事（medium 档会继续往 {@code setHealth} 那一档找路）。
     *
     * <p>顺带解决乘两遍：补打的那一记**已经乘过倍率**，闸门一放我们也不会再乘第二次。
     *
     * <p>代价写在明处：补打期间若**别的**生物也挨了一下，那一记不享受符文。伤害事件在服务端
     * 单线程上跑，一个布尔就够，不用按实体记账。
     */
    private static boolean forcing;

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Float multiplier = multiplierOf(victim, event.getSource());
        if (multiplier == null) return;
        float amount = event.getAmount() * multiplier;
        if (event.isCanceled()) force(victim, event.getSource(), amount);
        else event.setAmount(amount);
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
