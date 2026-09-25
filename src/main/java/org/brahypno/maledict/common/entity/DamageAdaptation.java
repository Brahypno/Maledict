package org.brahypno.maledict.common.entity;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 无常的「适应」：记住最近挨过的那几条伤害消息，被记住的才减伤。
 *
 * <p>窗口容量就是「适应几」，按「最近挨过」排序、最新在前，装不下时挤掉最久没挨的那条，
 * 被挤出去的那条次数一并作废。所谓「伤害消息」就是 {@code DamageSource#getMsgId()}。
 *
 * <p>一记命中进来时<b>先记录、再判定</b>：只有挨这一下之前就已经在窗口里的消息才吃
 * {@code e^-(它已经挨过的次数)}，刚被记进来的这条是全额。顺序有意义，同一 tick 里灌进来的
 * 一串伤害也照样一条一条地过这里；账目没有时限，唯一清账的时机是这场打完。
 */
public final class DamageAdaptation {

    /** 最近挨过的消息，下标 0 是最新的；长度不超过「适应几」。 */
    private final List<String> window = new ArrayList<>();

    /** 还在窗口里的消息各挨过几次；被挤出去就作废。 */
    private final Map<String, Integer> hitsByMessage = new LinkedHashMap<>();

    /**
     * 为这一记伤害记账并给出倍率。
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
        if (remembered) {
            window.remove(seen);
        } else if (window.size() >= level) {
            hitsByMessage.remove(window.remove(window.size() - 1));
        }
        window.add(0, message);
        float multiplier = remembered
                           ? (float) Math.exp(-hitsByMessage.getOrDefault(message, 0))
                           : 1.0F;
        hitsByMessage.merge(message, 1, Integer::sum);
        return multiplier;
    }

    /** 重来一场：清空窗口。 */
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

    /** 从存档恢复窗口；只认非空消息，顺序即「最新在前」，每条都算刚挨过一次。 */
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
