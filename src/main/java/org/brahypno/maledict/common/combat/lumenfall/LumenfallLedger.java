package org.brahypno.maledict.common.combat.lumenfall;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 服务端的沉辉账本集合：一份 {@code 目标 UUID + 攻击者 UUID} 只有一本账。
 *
 * <p>两张索引：
 * <ul>
 *   <li>{@code marks}：目标 → 攻击者 → 账本。死亡事件只关心「这个目标身上有几本账」，不该为此
 *       扫全表 —— 刷怪塔里一秒几十个死亡，全表扫描会变成平方级。</li>
 *   <li>{@code expiry}：到期时刻 → 账本集合。每 tick 只用 {@link #pollDue(long)} 摘走
 *       {@code <= now} 的那几桶，没人到期时是一次 {@code firstKey()} 比较，不再遍历全部账本、
 *       也不用按维度分几趟扫。</li>
 * </ul>
 *
 * <p>索引不变量：账本<b>当且仅当</b>处于 {@code ACCUMULATING} 且没被排进死亡队列时，才待在
 * {@code expiry} 里，键是 {@link LumenfallMark#dueTick()}。所以刷新命中、排进死亡队列、复活、
 * 移除这四处都得同步搬一次索引，见 {@link #record}、{@link #suspend}、{@link #resume}、
 * {@link #remove}。
 *
 * <p>纯内存、不落盘：这功能只在服务端结算，也不为了一份积蓄去加载区块 —— 目标卸载或移除之后，
 * 对应的账本连同那点存量一起丢掉。
 */
public final class LumenfallLedger {
    private final Map<UUID, Map<UUID, LumenfallMark>> marks = new HashMap<>();
    private final NavigableMap<Long, Set<LumenfallMark>> expiry = new TreeMap<>();

    /**
     * 这一击之后的账本：没有就开窗，有就记一笔并把它挪到新的到期桶。
     *
     * @return 这笔积蓄所属的账本
     */
    public LumenfallMark record(
            UUID victim, UUID owner, ResourceLocation dimension, long tick,
            float damage, double weaponAttackDamage) {
        Map<UUID, LumenfallMark> byOwner = marks.computeIfAbsent(victim, ignored -> new LinkedHashMap<>());
        LumenfallMark mark = byOwner.get(owner);
        if (mark == null || mark.phase() == LumenfallMark.Phase.CONSUMED) {
            mark = LumenfallMark.open(victim, owner, dimension, tick, weaponAttackDamage);
            byOwner.put(owner, mark);
        } else {
            mark.moveTo(dimension);
            // 先按旧的到期时刻摘掉，再让 record 刷新 lastHitTick，最后按新的时刻挂回去。
            unindex(mark);
        }
        mark.record(damage, tick);
        index(mark);
        return mark;
    }

    /**
     * 摘走到期时刻 {@code <= now} 的全部账本。
     *
     * <p>整桶取走：返回的是脱离索引的拷贝，调用方在结算过程中触发 {@code hurt} 再往账本里加新条目
     * （死亡散辉的连锁就是这么来的）也不会改到正在遍历的桶。
     */
    public List<LumenfallMark> pollDue(long now) {
        if (expiry.isEmpty() || expiry.firstKey() > now) {
            return List.of();
        }
        List<LumenfallMark> due = new ArrayList<>();
        while (!expiry.isEmpty() && expiry.firstKey() <= now) {
            for (LumenfallMark mark : expiry.pollFirstEntry().getValue()) {
                // 桶里只该有真正能释放的账本；被摘桶前一刻改了状态的，这里再挡一次。
                if (mark.phase() == LumenfallMark.Phase.ACCUMULATING && !mark.isDeathQueued()) {
                    due.add(mark);
                }
            }
        }
        return due;
    }

    /**
     * 某个目标身上的全部账本（可能来自多个攻击者，各自独立）。
     */
    public List<LumenfallMark> forVictim(UUID victim) {
        Map<UUID, LumenfallMark> byOwner = marks.get(victim);
        return byOwner == null || byOwner.isEmpty() ? List.of() : new ArrayList<>(byOwner.values());
    }

    /**
     * 死亡事件已经把它排进散辉队列：从到期索引里摘掉。
     * {@code isDue} 此时已经返回 false，留在桶里只会在到期时被子白取一次。
     */
    public void suspend(LumenfallMark mark) {
        mark.markDeathQueued();
        unindex(mark);
    }

    /**
     * 死亡被取消或实体复活：按原来的倒计时挂回索引，账本继续积累。
     */
    public void resume(LumenfallMark mark) {
        mark.clearDeathQueued();
        index(mark);
    }

    /**
     * 消费并移除一份账本；已经移除过的再调一次也无害。
     */
    public void remove(LumenfallMark mark) {
        unindex(mark);
        mark.consume();
        Map<UUID, LumenfallMark> byOwner = marks.get(mark.victim());
        if (byOwner == null) {
            return;
        }
        byOwner.remove(mark.owner());
        if (byOwner.isEmpty()) {
            marks.remove(mark.victim());
        }
    }

    public int size() {
        int total = 0;
        for (Map<UUID, LumenfallMark> byOwner : marks.values()) {
            total += byOwner.size();
        }
        return total;
    }

    public void clear() {
        marks.clear();
        expiry.clear();
    }

    private void index(LumenfallMark mark) {
        if (mark.phase() != LumenfallMark.Phase.ACCUMULATING || mark.isDeathQueued()) {
            return;
        }
        expiry.computeIfAbsent(mark.dueTick(), ignored -> new LinkedHashSet<>()).add(mark);
    }

    /**
     * 按账本当前的 {@link LumenfallMark#dueTick()} 摘一项；空桶顺手删掉，{@code TreeMap} 只会更干净。
     */
    private void unindex(LumenfallMark mark) {
        long due = mark.dueTick();
        Set<LumenfallMark> bucket = expiry.get(due);
        if (bucket == null) {
            return;
        }
        bucket.remove(mark);
        if (bucket.isEmpty()) {
            expiry.remove(due);
        }
    }
}
