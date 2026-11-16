package org.brahypno.maledict.common.combat.lumenfall;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 一份「目标 + 攻击者」的沉辉账本。规范见 {@code 沉辉铋_Tetra双特效_实现规范_v3.md} 第 4 章。
 *
 * <p>这个类刻意不碰实体、不查注册表，连维度都只记 {@link ResourceLocation}，好用普通 JUnit 把
 * 时序直接钉死：
 * <ul>
 *   <li>每一击记 {@link #CONVERSION}（20%），上限在<b>窗口第一次命中</b>时按那件武器的攻击伤害
 *       基准快照一次（{@link #CAP_SHARE} = 120%），之后再换武器也不追认。</li>
 *   <li>每次有效命中刷新 {@link #lastHitTick}；{@link #DELAY_TICKS}（80 tick）没人再打就到期。</li>
 *   <li>{@code ACCUMULATING → RELEASING → CONSUMED}，一份账本只释放一次。</li>
 * </ul>
 */
public final class LumenfallMark {
    /**
     * 积蓄比例：真正进入扣血阶段的伤害的 20%。
     */
    public static final double CONVERSION = 0.20D;

    /**
     * 积蓄上限：触发武器攻击伤害基准的 120%。
     */
    public static final double CAP_SHARE = 1.20D;

    /**
     * 最后一次有效命中之后 80 tick（4 秒）结算。
     */
    public static final int DELAY_TICKS = 80;

    /**
     * 死亡散辉半径，3.5 格，按目标实体中心判定，不衰减。
     */
    public static final double SPREAD_RADIUS = 3.5D;

    /**
     * 死亡散辉每个服务端 tick 最多处理几份账本，剩下的排到下一个 tick，避免同 tick 风暴。
     */
    public static final int DEATH_BATCH = 32;

    public enum Phase {
        ACCUMULATING,
        RELEASING,
        CONSUMED
    }

    private final UUID victim;
    private final UUID owner;
    private final float maximum;

    private ResourceLocation dimension;
    private float stored;
    private long lastHitTick;
    private Phase phase = Phase.ACCUMULATING;
    private float releaseSnapshot;
    private boolean deathQueued;

    private LumenfallMark(UUID victim, UUID owner, ResourceLocation dimension, long tick, float maximum) {
        this.victim = victim;
        this.owner = owner;
        this.dimension = dimension;
        this.lastHitTick = tick;
        this.maximum = maximum;
    }

    /**
     * 开一扇新窗口：上限按这一击所用武器的攻击伤害基准快照。
     */
    public static LumenfallMark open(
            UUID victim, UUID owner, ResourceLocation dimension, long tick,
            double weaponAttackDamage) {
        return new LumenfallMark(victim, owner, dimension, tick,
                                 (float) Math.max(0.0D, weaponAttackDamage * CAP_SHARE));
    }

    /**
     * 记一笔积蓄，返回真正记进去的量。
     *
     * @param damage {@code LivingDamageEvent} 里那一份（护甲之后、写血量之前）的伤害
     */
    public float record(float damage, long tick) {
        if (phase != Phase.ACCUMULATING || deathQueued || damage <= 0.0F){
            return 0.0F;
        }
        float added = Math.min((float) (damage * CONVERSION), Math.max(0.0F, maximum - stored));
        stored += added;
        lastHitTick = tick;
        return added;
    }

    /**
     * 目标在别的维度接着挨打时只搬账本，不清账。
     */
    public void moveTo(ResourceLocation dimension) {
        this.dimension = dimension;
    }

    public boolean isDue(long tick) {
        return phase == Phase.ACCUMULATING && !deathQueued && tick - lastHitTick >= DELAY_TICKS;
    }

    /**
     * 到期时刻，也就是桶索引里的键：{@code lastHitTick + 80}。
     * 每次刷新命中都会变，所以账本每改一次 {@code lastHitTick} 就得在索引里搬一次。
     */
    public long dueTick() {
        return lastHitTick + DELAY_TICKS;
    }

    /**
     * 切进释放态并快照存量，防止嵌套与并发重复触发。
     */
    public float beginRelease() {
        if (phase != Phase.ACCUMULATING){
            return 0.0F;
        }
        phase = Phase.RELEASING;
        releaseSnapshot = stored;
        return releaseSnapshot;
    }

    /**
     * 死亡散辉要用的量：定时释放打死的用那次释放的快照，被别的伤害打死的用当前积蓄。
     * （致命那一击的 20% 在自己死亡之前就已经记进来了。）
     */
    public float deathAmount() {
        return phase == Phase.RELEASING ? releaseSnapshot : stored;
    }

    /**
     * 只允许 {@link LumenfallLedger} 改这一位：排进死亡队列的同时必须把到期索引那一项摘掉。
     */
    void markDeathQueued() {
        deathQueued = true;
    }

    /**
     * 死亡被取消或实体复活：这次死亡候选作废，账本留在原处继续（同样只能由账本调用）。
     */
    void clearDeathQueued() {
        deathQueued = false;
    }

    public boolean isDeathQueued() {
        return deathQueued;
    }

    /**
     * 消费掉这份账本；同样只由账本调用，保证索引那一项一起摘。
     */
    void consume() {
        phase = Phase.CONSUMED;
    }

    public UUID victim() {
        return victim;
    }

    public UUID owner() {
        return owner;
    }

    public ResourceLocation dimension() {
        return dimension;
    }

    public float stored() {
        return stored;
    }

    public float maximum() {
        return maximum;
    }

    public long lastHitTick() {
        return lastHitTick;
    }

    public Phase phase() {
        return phase;
    }

    public float releaseSnapshot() {
        return releaseSnapshot;
    }
}
