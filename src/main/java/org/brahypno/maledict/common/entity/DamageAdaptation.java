package org.brahypno.maledict.common.entity;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 无常的「适应」：把挨到的伤害按伤害消息记账，重复的消息吃自然指数递减。
 *
 * <p>「伤害消息」就是死亡消息用的那个 id（{@code DamageSource#getMsgId()}）：玩家近战是
 * {@code player}、箭是 {@code arrow}、Malum 的镰刀横扫是 {@code scythe_sweep}——同一种消息
 * 在表里只占一格。记过账的消息第二次命中 ×e⁻¹、第三次 ×e⁻²，第 n 次 ×e⁻⁽ⁿ⁻¹⁾；
 * 第一次见到它时照常全额，因为那一下正是「记下来」本身。
 *
 * <p>能同时记几种由配置 {@code firstVicissitude.adaptationLevel}（「适应几」）决定，
 * 默认 2，0 时整个效果关闭。记满之后再遇到没见过的消息，就顶掉<b>最早记下的那一条</b>：
 * 于是轮着用三种以上伤害类型打，每一下都是新消息，也就一直是全额——两格适应只压得住两种。
 *
 * <p><b>账目与时间无关</b>：它按次数累计，整场遭遇战里一直有效，唯一的清账时机是这场打完
 * （见 {@code FirstVicissitudeBossEntity#tickEmptyEncounter}）。无常本身另有一层「无敌帧」——
 * {@code VicissitudeVitality#nextHit} 那套每次有效命中重算 20 tick 的线性缩放——那是独立的
 * 一层，适应既不读它、也不改它。
 *
 * <p>递减用自然指数而不是某个可调倍率：没有「该配多少」的问题，数起来也简单。
 * 换算成 {@code float} 之后指数一大就自然落到 0，那一击也就没有伤害可言了。
 *
 * <p>这个类只记账与换算，不碰世界、不依赖实体，所以 {@code DamageAdaptationTest} 能直接
 * 钉住上面这些规则；NBT 的存取留给实体（见 {@code FirstVicissitudeBossEntity}）。
 */
public final class DamageAdaptation {

    /** 伤害消息 → 已经挨过的次数；迭代顺序就是记下的先后，淘汰时取第一个。 */
    private final Map<String, Integer> hitsByMessage = new LinkedHashMap<>();

    /**
     * 为这一记伤害记账并给出倍率。
     *
     * @param message 伤害消息（{@code DamageSource#getMsgId()}）；为空时不记账也不减伤
     * @param level   这个 Boss 是「适应几」，也就是能同时记住几种消息；小于等于 0 时关闭
     * @return 这一记伤害要乘的倍率：没见过的消息是 1，已记账的是 {@code e^-(已挨次数)}
     */
    public float adapt(@Nullable String message, int level) {
        if (level <= 0 || message == null || message.isEmpty()) {
            return 1.0F;
        }
        Integer seen = hitsByMessage.get(message);
        if (seen == null) {
            // 只在真的要记一条新消息时才腾地方，且顶掉的是最早那条（FIFO）。
            while (hitsByMessage.size() >= level) {
                Iterator<String> oldest = hitsByMessage.keySet().iterator();
                if (!oldest.hasNext()) {
                    break;
                }
                oldest.next();
                oldest.remove();
            }
            hitsByMessage.put(message, 1);
            return 1.0F;
        }
        // 重复命中不改动迭代顺序：淘汰永远按「最早记下」算，不按最近一次。
        hitsByMessage.put(message, seen + 1);
        return (float) Math.exp(-seen);
    }

    /** 重来一场：清空全部账目，见 {@code FirstVicissitudeBossEntity#tickEmptyEncounter}。 */
    public void clear() {
        hitsByMessage.clear();
    }

    public boolean isEmpty() {
        return hitsByMessage.isEmpty();
    }

    /** 记账顺序的快照，供存档写出；改动它不会影响内部状态。 */
    public Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(hitsByMessage));
    }

    /**
     * 从存档恢复账目。
     *
     * <p>次数必须是正数：0 或负数在内部表示的是「没见过这条消息」，让它进来等于把一条
     * 记过的消息洗成全额，属于存档能改规则，所以这里直接丢掉。
     */
    public void restore(Map<String, Integer> saved) {
        hitsByMessage.clear();
        for (Map.Entry<String, Integer> entry : saved.entrySet()) {
            String message = entry.getKey();
            Integer hits = entry.getValue();
            if (message != null && !message.isEmpty() && hits != null && hits > 0) {
                hitsByMessage.put(message, hits);
            }
        }
    }
}
