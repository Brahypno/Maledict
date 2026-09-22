package org.brahypno.maledict.common.entity;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 无常的适应效果：按伤害消息记账，重复的消息吃自然指数递减。
 *
 * <p>这里钉住的是规则本身（{@code DamageAdaptation} 是纯换算，不碰世界）：
 * 第一下全额、第二下 e⁻¹、第三下 e⁻²……；适应几格满了顶掉最早那条；
 * 适应几为 0 等于关掉；账目只按次数、不带时限。收益上到实体那一层——也就是
 * 「配置读多少、什么时候清空」——只能在游戏内验证，见 {@code docs/design/first-vicissitude}
 * 对应轮次。
 */
class DamageAdaptationTest {

    /** 比较浮点用的容差：e⁻⁹ 也远大于它。 */
    private static final double DELTA = 1.0E-6D;

    @Test
    void theFirstHitOfAMessageIsFullDamage() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("player", 2), DELTA);
        assertFalse(adaptation.isEmpty());
    }

    /** 第二下 ×e⁻¹、第三下 ×e⁻²，第 n 下 ×e⁻⁽ⁿ⁻¹⁾：指数递减，不需要配置倍率。 */
    @Test
    void aRecordedMessageDecaysOnTheNaturalExponential() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-2.0D), adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-3.0D), adaptation.adapt("player", 2), DELTA);
    }

    /** 「适应二」是两格各自记账：另一种消息第一次来仍然是全额。 */
    @Test
    void eachMessageKeepsItsOwnCount() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("player", 2), DELTA);
        assertEquals(1.0D, adaptation.adapt("arrow", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("arrow", 2), DELTA);
        assertEquals(Math.exp(-2.0D), adaptation.adapt("player", 2), DELTA);
    }

    /**
     * 两格记满之后，第三种消息顶掉最早记下的那一条。
     *
     * <p>于是被顶掉的消息等于「没记过」，再来就是全额——轮着换三种打，每一下都在挤掉上一条，
     * 也就一直是全额；一直用同一种打才会一路递减。
     */
    @Test
    void aThirdMessageReplacesTheOldestRecord() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("player", 2), DELTA);
        assertEquals(1.0D, adaptation.adapt("arrow", 2), DELTA);
        // 第三条消息：全额，并且挤掉最早记下的 player。
        assertEquals(1.0D, adaptation.adapt("scythe_sweep", 2), DELTA);
        // player 的账已经没了，回到全额；这一次挤掉的是 arrow。
        assertEquals(1.0D, adaptation.adapt("player", 2), DELTA);
        // arrow 也刚被挤掉，同样全额。
        assertEquals(1.0D, adaptation.adapt("arrow", 2), DELTA);
        // 三条轮换之后账上是 player 与 arrow：继续用其中一条，就从 e⁻¹ 开始。
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("arrow", 2), DELTA);
    }

    /** 适应几为 0（或负数）就是关掉：永远全额，也什么都不记。 */
    @Test
    void zeroAdaptationDisablesTheEffect() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("player", 0), DELTA);
        assertEquals(1.0D, adaptation.adapt("player", 0), DELTA);
        assertEquals(1.0D, adaptation.adapt("player", -3), DELTA);
        assertTrue(adaptation.isEmpty());
    }

    /** 读不出消息（null / 空串）时不减伤也不占格子：不能因为读不到就把伤害漏掉。 */
    @Test
    void aMissingMessageIsNeverAdapted() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt(null, 2), DELTA);
        assertEquals(1.0D, adaptation.adapt("", 2), DELTA);
        assertTrue(adaptation.isEmpty());
    }

    /** 回到未参战时清账：上一场适应过的东西，下一场重新算。 */
    @Test
    void clearingForgetsEveryRecord() {
        DamageAdaptation adaptation = new DamageAdaptation();
        adaptation.adapt("player", 2);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
        adaptation.clear();
        assertTrue(adaptation.isEmpty());
        assertEquals(1.0D, adaptation.adapt("player", 2), DELTA);
    }

    /** 存档往返：记下的次数与先后顺序都要还在。 */
    @Test
    void snapshotAndRestoreKeepTheRecords() {
        DamageAdaptation adaptation = new DamageAdaptation();
        adaptation.adapt("player", 2);
        adaptation.adapt("player", 2);
        adaptation.adapt("arrow", 2);

        DamageAdaptation loaded = new DamageAdaptation();
        loaded.restore(adaptation.snapshot());

        assertEquals(Math.exp(-2.0D), loaded.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-1.0D), loaded.adapt("arrow", 2), DELTA);
    }

    /** 快照是只读副本：改它不影响内部账目。 */
    @Test
    void theSnapshotIsAReadOnlyCopy() {
        DamageAdaptation adaptation = new DamageAdaptation();
        adaptation.adapt("player", 2);
        Map<String, Integer> snapshot = adaptation.snapshot();
        assertTrue(snapshot.containsKey("player"));
        try {
            snapshot.put("arrow", 5);
            throw new AssertionError("快照不该可写");
        } catch (UnsupportedOperationException expected) {
            // 就该这样。
        }
        // 内部账目没被动过：player 仍旧只挨过一下。
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
    }

    /** 存档里的坏行（次数 0 / 负数、空消息）一律丢掉，不能把记过的消息洗成全额。 */
    @Test
    void restoringDropsUnusableEntries() {
        Map<String, Integer> saved = new LinkedHashMap<>();
        saved.put("player", 3);
        saved.put("arrow", 0);
        saved.put("mob", -2);
        saved.put("", 4);

        DamageAdaptation adaptation = new DamageAdaptation();
        adaptation.restore(saved);

        assertEquals(Math.exp(-3.0D), adaptation.adapt("player", 2), DELTA);
        // 被丢掉的 arrow 当作没见过：全额，并且重新占一格。
        assertEquals(1.0D, adaptation.adapt("arrow", 2), DELTA);
    }
}
