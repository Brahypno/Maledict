package org.brahypno.maledict.common.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 无常的适应：窗口容量就是「适应几」，同一 tick 内也按调用顺序逐条记账。 */
class DamageAdaptationTest {

    private static final double DELTA = 1.0E-6D;

    @Test
    void aRememberedMessageDecaysOnTheNaturalExponential() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-2.0D), adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-3.0D), adaptation.adapt("player", 2), DELTA);
    }

    @Test
    void adaptationTwoCannotReduceAThreeMessageRotation() {
        DamageAdaptation adaptation = new DamageAdaptation();
        for (int round = 0; round < 3; round++) {
            for (String message : new String[] {"A", "B", "C"}) {
                assertEquals(1.0D, adaptation.adapt(message, 2), DELTA);
            }
        }
    }

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

    @Test
    void adaptationThreeIsImmuneFromTheSecondRoundOn() {
        DamageAdaptation adaptation = new DamageAdaptation();
        for (String message : new String[] {"A", "B", "C"}) {
            assertEquals(1.0D, adaptation.adapt(message, 3), DELTA);
        }
        assertEquals(Math.exp(-1.0D), adaptation.adapt("A", 3), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("B", 3), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("C", 3), DELTA);
        assertEquals(Math.exp(-2.0D), adaptation.adapt("A", 3), DELTA);
        assertEquals(Math.exp(-2.0D), adaptation.adapt("B", 3), DELTA);
        assertEquals(Math.exp(-2.0D), adaptation.adapt("C", 3), DELTA);
    }

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

    @Test
    void slidingOutErasesThatMessagesRecord() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("A", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("A", 2), DELTA);
        assertEquals(1.0D, adaptation.adapt("B", 2), DELTA);
        assertEquals(1.0D, adaptation.adapt("C", 2), DELTA);
        assertEquals(1.0D, adaptation.adapt("A", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("A", 2), DELTA);
    }

    @Test
    void orderMattersEvenWithinASingleTick() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
        assertEquals(1.0D, adaptation.adapt("arrow", 2), DELTA);
        assertEquals(Math.exp(-2.0D), adaptation.adapt("player", 2), DELTA);
    }

    @Test
    void zeroAdaptationDisablesTheEffect() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("player", 0), DELTA);
        assertEquals(1.0D, adaptation.adapt("player", 0), DELTA);
        assertEquals(1.0D, adaptation.adapt("player", -3), DELTA);
        assertTrue(adaptation.isEmpty());
    }

    @Test
    void aMissingMessageIsNeverAdapted() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt(null, 2), DELTA);
        assertEquals(1.0D, adaptation.adapt("", 2), DELTA);
        assertTrue(adaptation.isEmpty());
    }

    @Test
    void clearingForgetsEveryRecord() {
        DamageAdaptation adaptation = new DamageAdaptation();
        adaptation.adapt("player", 2);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
        adaptation.clear();
        assertTrue(adaptation.isEmpty());
        assertEquals(1.0D, adaptation.adapt("player", 2), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
    }

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
        assertEquals(Math.exp(-1.0D), adaptation.adapt("player", 2), DELTA);
    }

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

    /** 新存档连次数一起存：读回来之后该多钝还是多钝，不会因为重登退回第一下。 */
    @Test
    void restoreKeepsTheHitCountsWhenTheyAreSaved() {
        DamageAdaptation adaptation = new DamageAdaptation();
        adaptation.adapt("mob|minecraft:zombie", 1);
        adaptation.adapt("mob|minecraft:zombie", 1);
        adaptation.adapt("mob|minecraft:zombie", 1);
        assertEquals(Map.of("mob|minecraft:zombie", 3), adaptation.hitCounts());

        DamageAdaptation loaded = new DamageAdaptation();
        loaded.restore(adaptation.snapshot(), adaptation.hitCounts());

        assertEquals(List.of("mob|minecraft:zombie"), loaded.snapshot());
        assertEquals(Math.exp(-3.0D), loaded.adapt("mob|minecraft:zombie", 1), DELTA);
        assertEquals(Map.of("mob|minecraft:zombie", 4), loaded.hitCounts());
    }

    /** 一次挥砍会用同一个伤害源打好几记（补足、追加通道）：同一刻的同一条消息只算一记。 */
    @Test
    void oneTickCountsOneHitPerMessage() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adaptInBatch("scythe_sweep", 2, 100), DELTA);
        assertEquals(1.0D, adaptation.adaptInBatch("scythe_sweep", 2, 100), DELTA);
        assertEquals(1.0D, adaptation.adaptInBatch("scythe_sweep", 2, 100), DELTA);
        assertEquals(Map.of("scythe_sweep", 1), adaptation.hitCounts());

        assertEquals(Math.exp(-1.0D), adaptation.adaptInBatch("scythe_sweep", 2, 101), DELTA);
        assertEquals(Math.exp(-2.0D), adaptation.adaptInBatch("scythe_sweep", 2, 102), DELTA);
    }

    /** 批内重复的那几记沿用第一记的倍率：补足伤害不该比主伤害多吃一层减伤。 */
    @Test
    void theRestOfTheBatchReusesTheFirstVerdict() {
        DamageAdaptation adaptation = new DamageAdaptation();
        adaptation.adaptInBatch("scythe_sweep", 2, 100);
        assertEquals(Math.exp(-1.0D), adaptation.adaptInBatch("scythe_sweep", 2, 101), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adaptInBatch("scythe_sweep", 2, 101), DELTA);
        assertEquals(Map.of("scythe_sweep", 2), adaptation.hitCounts());
    }

    /** 一批里的三条通道各记一记，轮换照旧成立：三种以上伤害类型永远是全额。 */
    @Test
    void aThreeChannelSwingStaysAtFullDamageAcrossTicks() {
        DamageAdaptation adaptation = new DamageAdaptation();
        for (long tick = 100; tick < 104; tick++) {
            for (String message : new String[] {"scythe_sweep", "voodoo", "freeze"}) {
                assertEquals(1.0D, adaptation.adaptInBatch(message, 2, tick), DELTA);
                assertEquals(1.0D, adaptation.adaptInBatch(message, 2, tick), DELTA);
            }
        }
        assertEquals(1.0D, adaptation.adapt("scythe_sweep", 2), DELTA);
    }

    /** 不带批的记账还是老规矩：逐记都算，符文那边不受影响。 */
    @Test
    void adaptWithoutABatchStillRecordsEveryHit() {
        DamageAdaptation adaptation = new DamageAdaptation();
        assertEquals(1.0D, adaptation.adapt("mob|minecraft:zombie", 1), DELTA);
        assertEquals(Math.exp(-1.0D), adaptation.adapt("mob|minecraft:zombie", 1), DELTA);
        assertEquals(Map.of("mob|minecraft:zombie", 2), adaptation.hitCounts());
    }

    /** 清账连当前这一批一起作废，不然重开一场会把上一场的批内缓存接着用。 */
    @Test
    void clearAlsoDropsTheBatch() {
        DamageAdaptation adaptation = new DamageAdaptation();
        adaptation.adaptInBatch("scythe_sweep", 2, 100);
        assertEquals(Math.exp(-1.0D), adaptation.adaptInBatch("scythe_sweep", 2, 101), DELTA);
        adaptation.clear();
        assertEquals(1.0D, adaptation.adaptInBatch("scythe_sweep", 2, 101), DELTA);
        assertEquals(Map.of("scythe_sweep", 1), adaptation.hitCounts());
    }
}
