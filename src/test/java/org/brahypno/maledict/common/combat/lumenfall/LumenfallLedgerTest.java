package org.brahypno.maledict.common.combat.lumenfall;

import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.common.combat.lumenfall.LumenfallLedger;
import org.brahypno.maledict.common.combat.lumenfall.LumenfallMark;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 沉辉账本的时序与算术，对应 v3 规范第 1.2 与 5.2 节的用例表。
 * 实体、伤害来源与粒子都在游戏里验证，这里只钉住状态机。
 */
class LumenfallLedgerTest {

    private static final double DELTA = 1.0E-6D;

    /**
     * 只用 {@link ResourceLocation} 当下标记：{@code ResourceKey.create(Registries.DIMENSION, ...)}
     * 会连带初始化 {@code BuiltInRegistries}，在纯 JUnit 环境里起不来。
     */
    private static final ResourceLocation DIMENSION =
            ResourceLocation.fromNamespaceAndPath("maledict", "test_dimension");

    private static final UUID VICTIM = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    /**
     * 10 点真实伤害打三次 → 积蓄 6 点（没到上限）。
     */
    @Test
    void threeHitsOfTenRealDamageStoreSix() {
        LumenfallMark mark = LumenfallMark.open(VICTIM, OWNER, DIMENSION, 100L, 8.0D);
        assertEquals(2.0F, mark.record(10.0F, 100L), DELTA);
        assertEquals(2.0F, mark.record(10.0F, 110L), DELTA);
        assertEquals(2.0F, mark.record(10.0F, 120L), DELTA);
        assertEquals(6.0F, mark.stored(), DELTA);
    }

    /**
     * 上限在窗口第一次命中时按触发武器快照：同窗口换武器不追认，新窗口才重新取值。
     */
    @Test
    void theCapIsSnapshottedWhenTheWindowOpens() {
        LumenfallLedger ledger = new LumenfallLedger();
        LumenfallMark first = ledger.record(VICTIM, OWNER, DIMENSION, 100L, 4.0F, 4.0D);
        // 4.0 × 1.2 = 4.8 的上限，之后再怎么打也只能到 4.8
        assertEquals(4.8F, first.maximum(), DELTA);
        first.record(1_000.0F, 100L);
        assertEquals(4.8F, first.stored(), DELTA);

        LumenfallMark widened = ledger.record(VICTIM, OWNER, DIMENSION, 101L, 1.0F, 20.0D);
        assertEquals(4.8F, widened.maximum(), DELTA);

        ledger.remove(widened);
        LumenfallMark fresh = ledger.record(VICTIM, OWNER, DIMENSION, 200L, 1.0F, 20.0D);
        assertEquals(24.0F, fresh.maximum(), DELTA);
    }

    /**
     * 连续攻击不到 80 tick 就一直刷新倒计时，最后一次命中之后才开始数。
     */
    @Test
    void everyHitRefreshesTheEightyTickTimer() {
        LumenfallMark mark = LumenfallMark.open(VICTIM, OWNER, DIMENSION, 100L, 8.0D);
        mark.record(10.0F, 150L);
        assertFalse(mark.isDue(179L));
        assertTrue(mark.isDue(230L));
    }

    /**
     * 一份账本只释放一次：定时释放拿走的快照是当时的存量，之后不能再拿第二份。
     */
    @Test
    void aWindowPaysOutOnlyOnce() {
        LumenfallMark mark = LumenfallMark.open(VICTIM, OWNER, DIMENSION, 100L, 8.0D);
        mark.record(10.0F, 100L);
        assertEquals(2.0F, mark.beginRelease(), DELTA);
        assertEquals(0.0F, mark.beginRelease(), DELTA);
        mark.consume();
        assertEquals(0.0F, mark.record(10.0F, 200L), DELTA);
    }

    /**
     * 定时释放打死目标时，散辉要按那次的快照算，而不是被清零后的存量。
     */
    @Test
    void theDeathAmountUsesTheReleaseSnapshotWhileReleasing() {
        LumenfallMark mark = LumenfallMark.open(VICTIM, OWNER, DIMENSION, 100L, 8.0D);
        mark.record(10.0F, 100L);
        assertEquals(2.0F, mark.beginRelease(), DELTA);
        assertEquals(2.0F, mark.deathAmount(), DELTA);

        LumenfallMark accumulating = LumenfallMark.open(VICTIM, OWNER, DIMENSION, 100L, 8.0D);
        accumulating.record(10.0F, 100L);
        accumulating.record(10.0F, 101L);
        assertEquals(4.0F, accumulating.deathAmount(), DELTA);
    }

    /**
     * 没人到期时一次都不该取；到期那一刻整桶摘走，取过一次就不再出现。
     */
    @Test
    void pollDueOnlyHandsOutTheTicksThatCame() {
        LumenfallLedger ledger = new LumenfallLedger();
        ledger.record(VICTIM, OWNER, DIMENSION, 100L, 10.0F, 8.0D);

        assertTrue(ledger.pollDue(179L).isEmpty());
        assertEquals(1, ledger.pollDue(180L).size());
        assertTrue(ledger.pollDue(181L).isEmpty());
    }

    /**
     * 刷新命中的那一刻要挪桶：旧到期时刻不再数到它，新时刻才数得到。
     */
    @Test
    void aRefreshedHitMovesTheMarkToALaterBucket() {
        LumenfallLedger ledger = new LumenfallLedger();
        ledger.record(VICTIM, OWNER, DIMENSION, 100L, 10.0F, 8.0D);
        ledger.record(VICTIM, OWNER, DIMENSION, 150L, 10.0F, 8.0D);

        assertTrue(ledger.pollDue(180L).isEmpty(), "旧的 180 那一桶就该空了");
        assertEquals(1, ledger.pollDue(230L).size());
    }

    /**
     * 排进死亡队列的账本不再留在到期桶里；死亡被取消后按原倒计时挂回去。
     */
    @Test
    void aDeathQueuedMarkLeavesTheExpiryIndexUntilItIsResumed() {
        LumenfallLedger ledger = new LumenfallLedger();
        LumenfallMark mark = ledger.record(VICTIM, OWNER, DIMENSION, 100L, 10.0F, 8.0D);

        ledger.suspend(mark);
        assertTrue(ledger.pollDue(1000L).isEmpty());

        ledger.resume(mark);
        assertEquals(1, ledger.pollDue(1000L).size());
    }

    /**
     * 移除之后桶里也不能留残影，否则每次 tick 都会白取一个已经消费掉的账本。
     */
    @Test
    void aRemovedMarkNoLongerShowsUpInTheExpiryIndex() {
        LumenfallLedger ledger = new LumenfallLedger();
        LumenfallMark mark = ledger.record(VICTIM, OWNER, DIMENSION, 100L, 10.0F, 8.0D);
        ledger.remove(mark);

        assertTrue(ledger.pollDue(1000L).isEmpty());
        assertEquals(0, ledger.size());
    }

    /**
     * 维度只是「去哪个 level 找目标」的提示，不是到期桶的键：目标换维度之后（没再挨这一刀）
     * 账本仍应原样到期，回收路径先按旧维度找、找不到再比照其它维度。
     */
    @Test
    void aDimensionChangeDoesNotDisturbTheExpiryBucket() {
        LumenfallLedger ledger = new LumenfallLedger();
        LumenfallMark mark = ledger.record(VICTIM, OWNER, DIMENSION, 100L, 10.0F, 8.0D);

        mark.moveTo(ResourceLocation.fromNamespaceAndPath("maledict", "elsewhere"));

        assertEquals(1, ledger.pollDue(180L).size());
        assertEquals(2.0F, mark.stored(), DELTA);
    }
}
