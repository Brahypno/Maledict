package org.brahypno.maledict.common.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 无常的适应：记住<b>最近挨过的那几条伤害消息</b>，窗口容量就是「适应几」。
 *
 * <p>钉住的是规则本身（{@code DamageAdaptation} 是纯换算，不碰世界）：<b>先记录、再判定</b>
 * ——只有"挨这一下之前就在窗口里"的消息才递减；挨过就移到最新；被挤出去就作废。
 * 收益上到实体那一层——也就是「配置读多少、什么时候清空」——只能在游戏内验证。
 */
class DamageAdaptationTest {

    /** 比较浮点用的容差。 */
    private static final double DELTA = 1.0E-6D;

    /** 第一次见到的消息永远是全额。 */
    @Test
    void aNewMessageIsFullDamage() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("player", 2), DELTA);
        assertTrue(!adaptation.isEmpty());
    }

    /**
     * 留得住的消息从第二下起递减：第二下 e⁻¹、第三下 e⁻²。
     *
     * <p>「适应二」装得下两条，所以单独用一条消息时它一直留在窗口里。
     */
    @Test
    void aRememberedMessageDecaysOnTheNaturalExponential() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-2.0D), adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-3.0D), adaptation.adapt("player", 2), DELTA);
    }

    /** 「适应一」只留最后一条，三条轮换永远碰不上它：一直全额，免不了伤。 */
    @Test
    void adaptationOneCannotReduceAThreeMessageRotation() {
        DamageAdaptation adaptation = new DamageAdaptation();
        for (int round = 0; round < 4; round++) {
            for (String message : new String[] {"A", "B", "C"}) {
                assertEquals(1.0D, adaptation.adapt(message, 1), DELTA);
            }
        }
    }

    /**
     * 「适应二」跑 A→B→C 六下：<b>一下都不会被减少</b>。
     *
     * <pre>
     * A 命中：记录 [A]      判定 A 之前不在 → 全额
     * B 命中：记录 [B, A]   判定 B 之前不在 → 全额
     * C 命中：记录 [C, B]   判定 C 之前不在 → 全额（A 被挤掉，次数作废）
     * A 命中：记录 [A, C]   判定 A 之前不在 → 全额（B 被挤掉，次数作废）
     * B 命中：记录 [B, A]   判定 B 之前不在 → 全额（C 被挤掉，次数作废）
     * C 命中：记录 [C, B]   判定 C 之前不在 → 全额（A 被挤掉，次数作废）
     * </pre>
     */
    @Test
    void adaptationTwoCannotReduceAThreeMessageRotation() {
        DamageAdaptation adaptation = new DamageAdaptation();
        for (int round = 0; round < 3; round++) {
            for (String message : new String[] {"A", "B", "C"}) {
                assertEquals(1.0D, adaptation.adapt(message, 2), DELTA);
            }
        }
    }

    /** 两条轮换正好装满「适应二」：两下都进来之后，各自一路递减。 */
    @Test
    void adaptationTwoHoldsATwoMessageRotation() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("A", 2), DELTA);
        assertEquals(1.0D, adaptation.adapt("B", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("A", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("B", 2), DELTA);
        assertEquals(Math.exp(-2.0D), adaptation.adapt("A", 2), DELTA);
        assertEquals(Math.exp(-2.0D), adaptation.adapt("B", 2), DELTA);
    }

    /**
     * 「适应三」装得下三条，谁也挤不出去：从第二轮起一直减伤。
     *
     * <p>只要挤不出去，次数就只增不减——第二轮的伤害已经贴地，越往后越接近 0。
     */
    @Test
    void adaptationThreeIsImmuneFromTheSecondRoundOn() {
        DamageAdaptation adaptation = new DamageAdaptation();
        for (String message : new String[] {"A", "B", "C"}) {
            assertEquals(1.0D, adaptation.adapt(message, 3), DELTA);
        }
        // 第二轮：三条都还在窗口里，各自往下数。
        assertEquals(Math.exp(-1.0D), adaptation.adapt("A", 3), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("B", 3), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("C", 3), DELTA);
        // 第三轮继续：三条各第三次挨到。
        assertEquals(Math.exp(-2.0D), adaptation.adapt("A", 3), DELTA);
        assertEquals(Math.exp(-2.0D), adaptation.adapt("B", 3), DELTA);
        assertEquals(Math.exp(-2.0D), adaptation.adapt("C", 3), DELTA);
    }

    /** 挨过就移到最新：窗口里装的始终是"最近挨过的那几条"。 */
    @Test
    void hittingAMessageAgainMovesItToTheFront() {
        DamageAdaptation adaptation = new DamageAdaptation();
        adaptation.adapt("A", 2);
        adaptation.adapt("B", 2);
        assertEquals(List.of("B", "A"), adaptation.snapshot());
        adaptation.adapt("B", 2);
        assertEquals(List.of("B", "A"), adaptation.snapshot());
        adaptation.adapt("A", 2);
        assertEquals(List.of("A", "B"), adaptation.snapshot());
    }

    /** 被挤出窗口 = 作废：次数不保留，回来时重新从全额起算。 */
    @Test
    void slidingOutErasesThatMessagesRecord() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("A", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("A", 2), DELTA);
        // B、C 依次进来，把 A 挤出去。
        assertEquals(1.0D, adaptation.adapt("B", 2), DELTA);
        assertEquals(1.0D, adaptation.adapt("C", 2), DELTA);
        // A 回来是全额（不是 e⁻²），因为它的账已经作废。
        assertEquals(1.0D, adaptation.adapt("A", 2), DELTA);
        // 再挨一下才是第二次。
        assertEquals(Math.exp(-1.0D), adaptation.adapt("A", 2), DELTA);
    }

    /** 同一 tick 里灌进来的一串伤害也有先后：每一条都会改窗口，后面那条看到的是改过之后的样子。 */
    @Test
    void orderMattersEvenWithinASingleTick() {
        DamageAdaptation adaptation = new DamageAdaptation();
        // 同一条消息连来两次：第二次就吃递减，与是不是同一个 tick 无关。
        assertEquals(1.0D, adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
        // 换成另一条消息进来：它把 player 留在窗口里，自己从全额起。
        assertEquals(1.0D, adaptation.adapt("arrow", 2), DELTA);
        // player 还在窗口里，接着往下。
        assertEquals(Math.exp(-2.0D), adaptation.adapt("player", 2), DELTA);
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

    /** 读不出消息（null / 空串）时不减伤也不占窗口：不能因为读不到就把伤害漏掉。 */
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

    /** 存档往返：窗口里的消息与顺序（最新在前）都要还在。 */
    @Test
    void snapshotAndRestoreKeepTheWindowOrder() {
        DamageAdaptation adaptation = new DamageAdaptation();
        adaptation.adapt("player", 2);
        adaptation.adapt("player", 2);
        adaptation.adapt("arrow", 2);
        assertEquals(List.of("arrow", "player"), adaptation.snapshot());

        DamageAdaptation loaded = new DamageAdaptation();
        loaded.restore(adaptation.snapshot());

        assertEquals(List.of("arrow", "player"), loaded.snapshot());
        assertEquals(Math.exp(-1.0D), loaded.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-1.0D), loaded.adapt("arrow", 2), DELTA);
    }

    /** 快照是只读副本：改它不影响内部窗口。 */
    @Test
    void theSnapshotIsAReadOnlyCopy() {
        DamageAdaptation adaptation = new DamageAdaptation();
        adaptation.adapt("player", 2);
        List<String> snapshot = adaptation.snapshot();
        assertTrue(snapshot.contains("player"));
        try {
            snapshot.add("arrow");
            throw new AssertionError("快照不该可写");
        } catch (UnsupportedOperationException expected) {
            // 就该这样。
        }
        // 内部窗口没被动过：player 仍旧只挨过一下。
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
    }

    /** 存档里的坏行（空消息、重复）一律丢掉。 */
    @Test
    void restoringDropsUnusableEntries() {
        List<String> saved = new ArrayList<>();
        saved.add("player");
        saved.add("");
        saved.add(null);
        saved.add("player");

        DamageAdaptation adaptation = new DamageAdaptation();
        adaptation.restore(saved);

        assertEquals(List.of("player"), adaptation.snapshot());
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
    }
}
