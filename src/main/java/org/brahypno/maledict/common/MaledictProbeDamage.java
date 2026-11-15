package org.brahypno.maledict.common;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.brahypno.changelib.DamageHelper.DamagePolicy;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import org.brahypno.changelib.DamageHelper.DamageProbeResult;
import org.brahypno.changelib.DamageHelper.DamageStage;

/**
 * 沉辉与反照共用的伤害派发口：走 ChangeLib 的伤害探针，但<b>只到 D0</b>（原版 {@code hurt}）。
 *
 * <p>为什么用探针：原版 {@code hurt} 的返回值不可靠 —— 有的实体重写了 {@code hurt}、有的把伤害
 * 吃掉仍返回 true、有的经 NBT/血量直写结算。探针给出的是「真实扣掉了多少」（含伤害吸收），
 * 释放到底打进去没有不用再猜。
 *
 * <p>为什么只到 D0：两个规范都写明了这是普通 {@code hurt} 流程，不是 ChangeLib 式特殊攻击，
 * 不得暗中换成直写血量。所以策略是 {@link DamageStage#D0_VANILLA_HURT} + 尊重目标保护声明 +
 * 不改防御 + 不补偿缺口 —— 进不去就如实返回 0，不绕过护甲，也不为凑数做外科手术。
 */
public final class MaledictProbeDamage {
    /** 只允许原版 hurt 一个阶段；不穿透保护、不改防御、不补偿缺口。 */
    private static final DamagePolicy HURT_ONLY = new DamagePolicy(
            DamageStage.D0_VANILLA_HURT,
            DamageProbeResult.Penetration.RESPECT,
            false,
            false);

    /**
     * 普通一记：完全遵守目标自己的伤害节奏（无敌帧照常拦）。
     *
     * @return 真实扣掉的量（含吸收），{@code 0} 表示没打进真实血量
     */
    public static float hurtOnly(Entity victim, DamageSource source, float amount) {
        return DamageProbe.damage(victim, source, amount, HURT_ONLY).totalDealt();
    }

    /**
     * 同一 tick 的追加一击：只把目标的无敌帧在这一次调用里隔离掉，之后原样还原。
     *
     * <p>用于反照那种「本次攻击的附加通道」—— 主伤害刚把无敌帧拉满，不隔离就只会被当成重复命中
     * 吞掉；而隔离后再还原，我们这一记也不会反过来延长目标的无敌时间。
     *
     * @return 真实扣掉的量（含吸收），{@code 0} 表示没打进真实血量
     */
    public static float hurtOnlyIsolated(Entity victim, DamageSource source, float amount) {
        return DamageProbe.damageThroughHurt(victim, source, amount, HURT_ONLY).totalDealt();
    }

    private MaledictProbeDamage() {
    }
}
