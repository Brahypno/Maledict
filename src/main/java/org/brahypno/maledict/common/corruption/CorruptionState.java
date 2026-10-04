package org.brahypno.maledict.common.corruption;

import net.minecraft.nbt.CompoundTag;

/**
 * 一个实体身上的腐败账目：D（休眠，尚未危险）与 A（活性，可被收走）。
 *
 * <p>两个数都是**当前血量的一部分**，不是第二个血池：它们不参与任何扣血，只是元数据，
 * 唯一的硬约束是 {@code D + A <= 当前血量}（见 {@link #clamp}）。「受伤让剩下的血更腐败」因此
 * 是自动成立的，不需要在掉血时按比例搬账。
 *
 * <p>本类**不 tick**：净化是惰性的 —— 每次账目被碰到（回血、挨打、被收）时按经过的 tick 补算，
 * 所以没有常驻开销，也不需要维护「有腐败的实体」名册。
 */
public final class CorruptionState {

    /**
     * 每次新 gain 把「旧 D」的多少转入 A（原文 §5 的 Dormant → Active）。
     *
     * <p>用旧 D 而不是加完本次 gain 之后的 D，是用户裁定的：本次新来的那一份不参与本次转化，
     * 否则第一次治疗就当场把自己的一半变成活性腐败，等于第一次就受罚（原文 §2.2 与 Invariant B）。
     */
    public static final float DORMANT_TO_ACTIVE_RATIO = 0.5F;

    /** 距上一次产生新腐败多久之后才开始净化（tick）。反复求增益会不断把这个计时推后。 */
    public static final int PURIFICATION_IDLE_TICKS = 100;

    /** 净化多久结算一次（tick）。 */
    public static final int PURIFICATION_INTERVAL_TICKS = 40;

    /** 每次净化扣掉多少点：先扣 D，再扣 A。 */
    public static final float PURIFICATION_PER_INTERVAL = 1.0F;

    private static final String DORMANT_TAG = "Dormant";
    private static final String ACTIVE_TAG = "Active";
    private static final String LAST_GAIN_TAG = "LastGain";
    private static final String PURIFIED_TAG = "Purified";

    private float dormant;
    private float active;

    /** 上一次产生新腐败的 tick；净化从它之后 {@link #PURIFICATION_IDLE_TICKS} 才开始。 */
    private long lastGainTick;

    /** 已经净化到哪个 tick，免得同一段时间被结算两遍。 */
    private long purifiedThrough;

    public boolean isEmpty() {
        return dormant <= 0.0F && active <= 0.0F;
    }

    /**
     * 记一次有效增益：先把**旧 D** 的一半转进 A，再把本次折算出的腐败量并进 D。
     *
     * <p>调用前先 {@link #purify}，免得把「上一段空闲期该净化的量」和这次 gain 混在一起。
     */
    public void gain(float amount, long now) {
        if (amount <= 0.0F || !Float.isFinite(amount)) {
            return;
        }
        float transferred = dormant * DORMANT_TO_ACTIVE_RATIO;
        active += transferred;
        dormant += amount - transferred;
        lastGainTick = now;
    }

    /**
     * 把 {@code D + A} 夹到当前血量以内：**先夹 D，再夹 A**（用户裁定）。
     *
     * <p>先夹 D 的意思是「攒起来的债」落在攻击方的收益上：目标挨打之后，先蒸发的是尚未成形的
     * 那一部分，活性腐败尽可能留到被收的那一刻。
     */
    public void clamp(float health) {
        // 血量可能被别的模组写成 NaN/Inf（仓库里无常 BOSS 的 setHealth 也防了这一手）：
        // 一旦让它进来，D/A 会被污染成 NaN 再也夹不回来，所以直接不动账。
        if (!Float.isFinite(health)) {
            return;
        }
        float excess = dormant + active - Math.max(0.0F, health);
        if (excess <= 0.0F) {
            return;
        }
        float fromDormant = Math.min(dormant, excess);
        dormant -= fromDormant;
        active = Math.max(0.0F, active - (excess - fromDormant));
    }

    /**
     * 收一笔活性腐败，返回本次实际收走的血量。
     *
     * <p>三重约束：账上有的、单次上限、以及「收不致死」（给目标留下 {@code minimumSurvivable} 点血，
     * 真正致命的是紧接着落地的那一记普通攻击）。
     */
    public float harvest(float cap, float health, float minimumSurvivable) {
        if (!Float.isFinite(health) || !Float.isFinite(cap) || !Float.isFinite(minimumSurvivable)) {
            return 0.0F;
        }
        float budget = Math.max(0.0F, health - minimumSurvivable);
        float collapse = Math.min(active, Math.min(Math.max(0.0F, cap), budget));
        if (collapse <= 0.0F) {
            return 0.0F;
        }
        active -= collapse;
        return collapse;
    }

    /**
     * 惰性净化：距上一次新腐败超过 {@link #PURIFICATION_IDLE_TICKS} 之后，每
     * {@link #PURIFICATION_INTERVAL_TICKS} 扣 {@link #PURIFICATION_PER_INTERVAL} 点，先 D 后 A。
     *
     * <p>补算而不是逐 tick 结算：数值只在这几个事件里被读到，补算出来的结果与「一直在跑」完全一致，
     * 却不用为每个生物挂一个 tick。
     */
    public void purify(long now) {
        long start = Math.max(purifiedThrough, lastGainTick + PURIFICATION_IDLE_TICKS);
        if (now <= start) {
            return;
        }
        long steps = (now - start) / PURIFICATION_INTERVAL_TICKS;
        if (steps <= 0) {
            return;
        }
        purifiedThrough = start + steps * PURIFICATION_INTERVAL_TICKS;
        if (isEmpty()) {
            return;
        }
        float amount = steps * PURIFICATION_PER_INTERVAL;
        float fromDormant = Math.min(dormant, amount);
        dormant -= fromDormant;
        active = Math.max(0.0F, active - (amount - fromDormant));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(DORMANT_TAG, dormant);
        tag.putFloat(ACTIVE_TAG, active);
        tag.putLong(LAST_GAIN_TAG, lastGainTick);
        tag.putLong(PURIFIED_TAG, purifiedThrough);
        return tag;
    }

    public static CorruptionState load(CompoundTag tag) {
        CorruptionState state = new CorruptionState();
        state.dormant = Math.max(0.0F, tag.getFloat(DORMANT_TAG));
        state.active = Math.max(0.0F, tag.getFloat(ACTIVE_TAG));
        state.lastGainTick = tag.getLong(LAST_GAIN_TAG);
        state.purifiedThrough = tag.getLong(PURIFIED_TAG);
        return state;
    }
}
