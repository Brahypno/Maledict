package org.brahypno.maledict.common.entity;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 无常的「适应」：记住<b>最近挨过的那几条伤害消息</b>，被记住的才减伤。
 *
 * <p>窗口容量就是「适应几」：适应 1 只记得最后一条，适应 2 记得最后两条，依此类推。
 * 窗口按"最近挨过"排序，最新在前；装不下时挤掉最久没挨的那条。
 *
 * <p>一记命中进来时<b>先记录、再判定</b>：消息先被记进窗口，然后才看它"挨这一下之前"
 * 在不在窗口里。
 *
 * <ul>
 *   <li><b>之前就在窗口里</b>——被记住着，倍率是 {@code e^-(它已经挨过的次数)}。</li>
 *   <li><b>之前不在</b>——忘了或者第一次见，照常全额。</li>
 * </ul>
 *
 * <p>两种情况下它都会移到最前，次数加一；被挤出去的那条次数一并作废，下次回来算第一次。
 *
 * <p>用「适应二」跑 A→B→C，六下全是全额——这正是"先记录"的意思：
 *
 * <pre>
 * A 命中：记录 [A]      判定 A 之前不在 → 全额
 * B 命中：记录 [B, A]   判定 B 之前不在 → 全额
 * C 命中：记录 [C, B]   判定 C 之前不在 → 全额（A 被挤掉，次数作废）
 * A 命中：记录 [A, C]   判定 A 之前不在 → 全额（B 被挤掉，次数作废）
 * B 命中：记录 [B, A]   判定 B 之前不在 → 全额（C 被挤掉，次数作废）
 * C 命中：记录 [C, B]   判定 C 之前不在 → 全额（A 被挤掉，次数作废）
 * </pre>
 *
 * <p>所以「适应二」要想起作用，得是<b>两条轮换</b>：两条都留得住，从第三下起各自递减
 * （e⁻¹、e⁻²……）。「适应一」连两条都留不住，永远免不了伤；「适应三」装得下三条，
 * 从第二轮起一直减伤。
 *
 * <p><b>顺序是有意义的</b>：同一 tick 里灌进来的一串伤害照样一条一条地过这里，谁先谁后
 * 由调用顺序决定，不存在"同一 tick 一起算"。
 *
 * <p>「伤害消息」就是死亡消息用的那个 id（{@code DamageSource#getMsgId()}）：玩家近战是
 * {@code player}、箭是 {@code arrow}、Malum 的镰刀横扫是 {@code scythe_sweep}——同一种消息
 * 在窗口里占一格。账目没有时限，唯一清账的时机是这场打完
 * （见 {@code FirstVicissitudeBossEntity#tickEmptyEncounter}）。
 *
 * <p>这个类只记账与换算，不碰世界、不依赖实体，所以 {@code DamageAdaptationTest} 能直接
 * 钉住上面这些规则；NBT 的存取留给实体（见 {@code FirstVicissitudeBossEntity}）。
 */
public final class DamageAdaptation {

    /** 最近挨过的消息，<b>下标 0 是最新的</b>；长度不超过「适应几」。 */
    private final List<String> window = new ArrayList<>();

    /** 还在窗口里的消息各挨过几次；被挤出去就作废。 */
    private final Map<String, Integer> hitsByMessage = new LinkedHashMap<>();

    /**
     * 为这一记伤害记账并给出倍率。
     *
     * <p><b>先记录，再算这一下的伤害</b>：消息先被记进窗口（满了挤掉最久没挨的那条），
     * 然后才判定——只有"挨这一下之前就已经在窗口里"的消息才吃递减，刚被记进来的这条是全额。
     *
     * <p>所以三条轮换在「适应二」下<b>一次都不会被减少</b>：每次记进来的时候，它上一条刚好
     * 被挤出去，它的账从来没留下过。两条轮换才留得住，从那以后每一下都递减。
     *
     * @param message 伤害消息（{@code DamageSource#getMsgId()}）；为空时不记账也不减伤
     * @param level   这个 Boss 是「适应几」，也就是窗口能记住几条消息；小于等于 0 时关闭
     */
    public float adapt(@Nullable String message, int level) {
        if (level <= 0 || message == null || message.isEmpty()) {
            return 1.0F;
        }
        int seen = window.indexOf(message);
        boolean remembered = seen >= 0;
        // 记录在前：挨过就移到最新，没挨过就记进来，满了先挤掉最久没挨的那条（次数作废）。
        if (remembered) {
            window.remove(seen);
        } else if (window.size() >= level) {
            hitsByMessage.remove(window.remove(window.size() - 1));
        }
        window.add(0, message);
        // 判定在后：用的是"这一下之前"的次数，第一次全额正是 e⁰。
        float multiplier = remembered
                           ? (float) Math.exp(-hitsByMessage.getOrDefault(message, 0))
                           : 1.0F;
        hitsByMessage.merge(message, 1, Integer::sum);
        return multiplier;
    }

    /** 重来一场：清空窗口，见 {@code FirstVicissitudeBossEntity#tickEmptyEncounter}。 */
    public void clear() {
        window.clear();
        hitsByMessage.clear();
    }

    public boolean isEmpty() {
        return window.isEmpty();
    }

    /** 窗口快照，最新在前，供存档写出；改动它不会影响内部状态。 */
    public List<String> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(window));
    }

    /** 还在窗口里的消息各挨过几次，供调试与测试观察。 */
    public Map<String, Integer> hitCounts() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(hitsByMessage));
    }

    /**
     * 从存档恢复窗口。
     *
     * <p>只认非空消息，顺序即"最新在前"。恢复出来的每条都算"刚挨过一次"：次数不跟着存档走，
     * 免得存档能被用来攒次数。
     */
    public void restore(List<String> saved) {
        clear();
        if (saved == null) {
            return;
        }
        for (String message : saved) {
            if (message != null && !message.isEmpty() && !hitsByMessage.containsKey(message)) {
                window.add(message);
                hitsByMessage.put(message, 1);
            }
        }
    }
}
