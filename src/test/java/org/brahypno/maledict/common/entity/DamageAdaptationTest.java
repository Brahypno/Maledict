package org.brahypno.maledict.common.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
}
