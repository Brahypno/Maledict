package org.brahypno.maledict.common.vitals;

/**
 * 延迟池：进池的伤害与治疗不立刻落在真实血条上，而是每 tick 按固定比例释放。
 *
 * <p>指数释放就是「天然收敛」：入队速率 R（点/tick）时池子的稳态长度是
 * {@code R / RELEASE_PER_TICK}，不会无限增长；真实血量的变化速度最终等于入队速率，只是落后
 * 一个池子长度的时间。
 *
 * <p>两个池子**先对冲再排队**：新来的伤害先吃掉攒着的治疗，新来的治疗先吃掉攒着的伤害，抵不完的
 * 才真的排进池子。所以两个池子不会同时有值 —— 一剂治疗等于先把还没落地的痛抹掉。
 *
 * <p>累计释放量收敛到初始池子大小，所以池子 ≥ 当前血量就是必死，不存在「永远差一点」。
 *
 * <p>这个类不碰 Minecraft 类型，池子的数学可以单独跑测试。
 */
public final class DelayedVitalsPool {

    /**
     * 掉血每 tick 释放的比例。半衰期 {@code ln2 / 0.025 ≈ 28} tick，约 1.4 秒。
     *
     * <p>痛得跟上战斗节奏：再慢就等于长时间无敌。
     */
    public static final float DAMAGE_RELEASE_PER_TICK = 0.025F;

    /**
     * 回血每 tick 释放的比例。半衰期 {@code ln2 / 0.01 ≈ 69} tick，约 3.5 秒。
     *
     * <p>刻意比掉血慢一倍多：一剂金苹果不该在血条上炸开，治愈本来就该拖沓。
     */
    public static final float HEAL_RELEASE_PER_TICK = 0.01F;

    /** 低于这个值就当已经结清，免得浮点尾巴让池子永远不空、同步也永远不停。 */
    public static final float EMPTY_THRESHOLD = 0.01F;

    /**
     * 每 tick 的释放地板：池子再小，每 tick 也至少放这么多。
     *
     * <p>纯指数是「前期特别快、后期特别慢」的曲线：9.87 点的池子要 273 tick（约 13.6 秒）才归零，
     * 最后半颗心要五秒多，最后那点渣还要八秒。加地板之后同一个池子约 5 秒结清，而开局那一下几乎
     * 没变（首 tick 从 0.247 只涨到 0.267）；收敛性也没丢 —— 地板的等效不动点是负的，池子一定在
     * 有限 tick 内归零。
     */
    public static final float RELEASE_FLOOR = 0.02F;

    /** 待扣除。 */
    private float pendingDamage;

    /** 待治疗。 */
    private float pendingHeal;

    /**
     * 入队一记待扣除：**先和攒着的待治疗对冲**，抵不完的才排进待扣除。
     *
     * @return 真正排进待扣除的量；{@code 0} 表示全被抵掉了
     */
    public float netDamage(float amount) {
        if (amount <= 0.0F) {
            return 0.0F;
        }
        float cancelled = Math.min(amount, pendingHeal);
        pendingHeal -= cancelled;
        float queued = amount - cancelled;
        pendingDamage += queued;
        return queued;
    }

    /**
     * 入队一记待治疗：同样先和攒着的待扣除对冲。
     *
     * @return 真正排进待治疗的量；{@code 0} 表示这一剂全拿去抵痛了
     */
    public float netHeal(float amount) {
        if (amount <= 0.0F) {
            return 0.0F;
        }
        float cancelled = Math.min(amount, pendingDamage);
        pendingDamage -= cancelled;
        float queued = amount - cancelled;
        pendingHeal += queued;
        return queued;
    }

    /**
     * 推进一 tick，返回本 tick 该从真实血量里扣掉的量。
     *
     * <p>客户端本地推演同样调用它、丢掉返回值即可：两边跑的是同一个公式，所以显示不会跑偏。
     */
    public float releaseDamage() {
        float released = releaseAmount(pendingDamage, DAMAGE_RELEASE_PER_TICK);
        pendingDamage = settle(pendingDamage - released);
        return released;
    }

    /** 推进一 tick，返回本 tick 该加回真实血量的量。客户端同样可以只拿它推进。 */
    public float releaseHeal() {
        float released = releaseAmount(pendingHeal, HEAL_RELEASE_PER_TICK);
        pendingHeal = settle(pendingHeal - released);
        return released;
    }

    /** 按比例放，但每 tick 至少 {@link #RELEASE_FLOOR}，且不会超过池子本身。 */
    private static float releaseAmount(float pending, float rate) {
        if (pending <= 0.0F) {
            return 0.0F;
        }
        return Math.min(pending, pending * rate + RELEASE_FLOOR);
    }

    private static float settle(float pending) {
        return pending < EMPTY_THRESHOLD ? 0.0F : pending;
    }

    public float pendingDamage() {
        return pendingDamage;
    }

    public float pendingHeal() {
        return pendingHeal;
    }

    public boolean isEmpty() {
        return pendingDamage <= 0.0F && pendingHeal <= 0.0F;
    }

    /** 死亡清除，或服务端与客户端对齐时直接覆盖。 */
    public void set(float damage, float heal) {
        pendingDamage = Math.max(0.0F, damage);
        pendingHeal = Math.max(0.0F, heal);
    }

    public void clear() {
        pendingDamage = 0.0F;
        pendingHeal = 0.0F;
    }
}
