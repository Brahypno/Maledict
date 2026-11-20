package org.brahypno.maledict.common.combat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests the adaptation state machine; boss event delivery and entity persistence require game verification. */
class DamageAdaptationTest {

    private static final double DELTA = 1.0E-6D;

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
    void adaptationThreeReducesFromTheSecondRoundOn() {
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

    /** A repeated A/B/C rotation at capacity two stays full; the order is part of this example. */
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

}
