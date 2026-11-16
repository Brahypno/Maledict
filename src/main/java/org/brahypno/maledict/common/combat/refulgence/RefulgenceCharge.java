package org.brahypno.maledict.common.combat.refulgence;

/**
 * 一件镶嵌武器的反照储能，规范见 {@code refracted_eutectic_refulgence_impl.md} 第 1.2 与 5.2 节。
 *
 * <p>纯算术，不碰物品与实体：护盾减免量的 {@link #CONVERSION}（25%）存进来，上限是武器自身基础
 * 攻击伤害的 {@link #CAP_SHARE}（80%），最后一次充能之后 {@link #LIFETIME_TICKS}（120 tick）到期清零。
 *
 * <p>上限只看武器本身：药水、饰品、临时攻击力加成都不能抬高它，否则一件临时增伤就能把武器变
 * 成大号蓄电池。
 */
public final class RefulgenceCharge {
    /**
     * 护盾减免量转入储能的份额。
     */
    public static final double CONVERSION = 0.25D;

    /**
     * 储能上限占武器基础攻击伤害的份额。
     */
    public static final double CAP_SHARE = 0.80D;

    /**
     * 有效时长：最后一次成功充能之后 120 tick（6 秒）。
     */
    public static final int LIFETIME_TICKS = 120;

    /**
     * NBT 里的格式版本，将来改布局时不至于把旧数据读成垃圾。
     */
    public static final int VERSION = 1;

    private float charge;
    private long lastChargeTick;

    public RefulgenceCharge(float charge, long lastChargeTick) {
        this.charge = Math.max(0.0F, charge);
        this.lastChargeTick = lastChargeTick;
    }

    public static RefulgenceCharge empty() {
        return new RefulgenceCharge(0.0F, 0L);
    }

    /**
     * 充能：{@code charge += absorbed × 25%}，夹在上限里，并刷新有效时间。
     *
     * @return 实际充进去的量（贴上限时会小于按比例算出的量）
     */
    public float add(float absorbed, double weaponAttackDamage, long tick) {
        if (absorbed <= 0.0F){
            return 0.0F;
        }
        float cap = (float) Math.max(0.0D, weaponAttackDamage * CAP_SHARE);
        float before = charge;
        charge = Math.min(cap, charge + (float) (absorbed * CONVERSION));
        lastChargeTick = tick;
        return charge - before;
    }

    /**
     * 过期判定。时钟倒退或存档恢复导致前后不连续时一律算过期 —— 拿不准的储能按清零处理，
     * 不留下一个能无限期挂着的电池。
     */
    public boolean isExpired(long tick) {
        long elapsed = tick - lastChargeTick;
        return elapsed < 0L || elapsed >= LIFETIME_TICKS;
    }

    /**
     * 扣掉一笔储能，返回实际扣到的量；不够就扣到 0 为止。
     */
    public float take(float amount) {
        float taken = Math.min(charge, Math.max(0.0F, amount));
        charge -= taken;
        return taken;
    }

    public boolean isEmpty() {
        return charge <= 0.0F;
    }

    public float charge() {
        return charge;
    }

    public long lastChargeTick() {
        return lastChargeTick;
    }
}
