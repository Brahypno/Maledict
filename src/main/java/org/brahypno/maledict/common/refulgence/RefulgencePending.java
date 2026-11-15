package org.brahypno.maledict.common.refulgence;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 两处「护盾已经执行、事件还没被确认」的减免量暂存区。规范第 3.3 节。
 *
 * <p>为什么按<b>事件实例身份</b>记账而不是按玩家或 tick：同一名玩家在同一 tick 里挨好几段伤害是
 * 常态（近战 + 追加通道 + 药水），按玩家+tick 会把前一段覆盖掉。这里用
 * {@link IdentityHashMap}，一个事件对应一份待确认记录，MONITOR 取走即删。
 *
 * <p>每个事件最多提交一次；事件被取消时丢弃记录，但记录<b>不会</b>因此泄漏到下一 tick。
 * 确认只说明「本阶段结束时这个事件没被取消」，不证明后面别的阶段不会再取消它 —— 反照充的是护盾
 * 已经执行的减免，不要求最后真的掉血。
 *
 * <p>这里只看「该不该记」：被打的玩家主手确实带着反照载体才记 —— 没有载体就没账可充，连一条
 * 待确认记录都不必留下。这个判断走 {@link RefulgenceAbility}（载体标签 + Tetra 暴露渠道），
 * <b>不是</b>「没装 Tetra 就别记」：标签渠道的载体在没有 Tetra 的服上同样会充能。
 */
public final class RefulgencePending {
    /** Mixin：Malum {@code setAmount} 之前的事件量。 */
    private static final Map<LivingHurtEvent, Float> WARD_BEFORE = new IdentityHashMap<>();
    /** Mixin：Malum 那一记的减免量，等 MONITOR 确认。 */
    private static final Map<LivingHurtEvent, Absorbed> WARD = new IdentityHashMap<>();
    /** 逐光扬弃那一记的减免量，等 MONITOR 确认。 */
    private static final Map<LivingDamageEvent, Absorbed> ABANDONMENT = new IdentityHashMap<>();

    /**
     * Mixin 在 Malum 调用 {@code setAmount} 之前记下当时的量。
     *
     * <p>这一刻正好能顺手判断主手：没有反照载体就什么都不记，后面的取差价自然拿不到东西。
     * 主手载体在整条伤害链里不会中途改变，所以这里判断一次就够，不必在确认处重判。
     */
    public static void observeWardBefore(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || !RefulgenceAbility.holds(player)) {
            return;
        }
        WARD_BEFORE.put(event, event.getAmount());
    }

    /**
     * Mixin 在 {@code setAmount} 之后取差价，登进待确认表。
     *
     * <p>差价 = 这次调用本身造成的减少量。若别的响应物品在 Malum 方法内部先改过同一事件的 amount，
     * 这个差价只覆盖 {@code setAmount} 这一下，不会把它算成护盾吸收。
     */
    public static void observeWardAfter(LivingHurtEvent event) {
        Float before = WARD_BEFORE.remove(event);
        if (before == null) {
            return;
        }
        float reduction = before - event.getAmount();
        if (reduction <= 0.0F || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        WARD.put(event, new Absorbed(player, reduction, event.getSource()));
    }

    /** 逐光扬弃在它自己 {@code setAmount} 之后直接登记（没走 Mixin，不需要前后取差价）。 */
    public static void recordAbandonment(LivingDamageEvent event, ServerPlayer player, float absorbed,
                                         DamageSource source) {
        if (absorbed <= 0.0F) {
            return;
        }
        ABANDONMENT.put(event, new Absorbed(player, absorbed, source));
    }

    @Nullable
    public static Absorbed takeWard(LivingHurtEvent event) {
        return WARD.remove(event);
    }

    @Nullable
    public static Absorbed takeAbandonment(LivingDamageEvent event) {
        return ABANDONMENT.remove(event);
    }

    /** 服务端停止时清空，避免把玩家引用带进下一次启动。 */
    public static void clear() {
        WARD_BEFORE.clear();
        WARD.clear();
        ABANDONMENT.clear();
    }

    /** 一笔已执行的减免：谁被扣了护盾、扣了多少、原始伤害来源是谁打来的。 */
    public record Absorbed(ServerPlayer player, float amount, DamageSource source) {
    }

    private RefulgencePending() {
    }
}
