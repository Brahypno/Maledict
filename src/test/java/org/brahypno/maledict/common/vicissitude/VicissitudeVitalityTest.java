package org.brahypno.maledict.common.vicissitude;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictEntities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class VicissitudeVitalityTest {
    @BeforeAll
    static void initializeMinecraftVersion() {
        SharedConstants.tryDetectVersion();
    }

    @Test
    void thirdDistinctKillBypasses() {
        VicissitudeVitality state = VicissitudeVitality.initial(1000.0F);
        state = state.killAttempt(10, 100);
        for (int probe = 0; probe < 20; probe++) {
            state = state.killAttempt(10, 100);
        }
        assertTrue(state.current() == 1000.0F && state.killAttempts() == 1,
                "One tick of probes must not immediately bypass");
        state = state.killAttempt(11, 100);
        assertTrue(state.current() == 1000.0F && state.killAttempts() == 2, "Second attempt must survive");
        state = state.killAttempt(12, 100);
        assertTrue(state.current() == 0.0F && state.killAttempts() == 3, "Third attempt must kill");
        assertTrue(state.killAttempt(13, 100).equals(state), "Repeated death calls must remain terminal");
    }

    @Test
    void expiredSequenceRestarts() {
        VicissitudeVitality state = VicissitudeVitality.initial(1000.0F)
                .killAttempt(0, 100).killAttempt(100, 100);
        assertTrue(state.killAttempts() == 2, "Window boundary must be inclusive");
        state = state.killAttempt(201, 100);
        assertTrue(state.killAttempts() == 1 && state.current() == 1000.0F, "Expired sequence must restart");
        state = state.killAttempt(20, 100);
        assertTrue(state.killAttempts() == 1, "Moving world time backwards must restart the sequence");
    }

    @Test
    void ordinaryWoundsDoNotResetTheSequence() {
        VicissitudeVitality state = VicissitudeVitality.initial(1000.0F)
                .killAttempt(10, 100).afterDamage(10.0F);
        assertTrue(state.current() == 990.0F, "Damage must come off the pool");
        state = state.killAttempt(20, 100).withCurrent(995.0F).killAttempt(30, 100);
        assertTrue(state.current() == 0.0F, "Damage/healing within the window must not erase bypass attempts");
    }

    @Test
    void persistenceKeepsWoundsAndDeadIdentities(@TempDir Path directory) throws IOException {
        UUID woundedId = UUID.randomUUID();
        UUID deadId = UUID.randomUUID();
        VicissitudeVitality wounded = VicissitudeVitality.initial(1400.0F)
                .afterDamage(14.0F).afterAcceptedHit(37, 20).killAttempt(11, 100).killAttempt(12, 100);
        VicissitudeVitality corpse = VicissitudeVitality.initial(1000.0F)
                .killAttempt(1, 100).killAttempt(2, 100).killAttempt(3, 100);
        VicissitudeVitalityLedger ledger = new VicissitudeVitalityLedger();
        ledger.commit(woundedId, wounded);
        ledger.commit(deadId, corpse);
        Path file = directory.resolve("ledger.dat");
        ledger.save(file.toFile());
        VicissitudeVitalityLedger restored =
                VicissitudeVitalityLedger.load(NbtIo.readCompressed(file.toFile()).getCompound("data"));
        assertTrue(wounded.equals(restored.find(woundedId)), "Saving must retain the open gate, wounds and timers");
        assertTrue(corpse.equals(restored.find(deadId)), "A dead UUID must stay dead on reload");
        assertTrue(restored.find(woundedId).killAttempt(13, 100).current() == 0.0F,
                "Reload must not reset two accumulated attempts");
        assertTrue(restored.find(UUID.randomUUID()) == null, "Different UUIDs must not share vitality");
    }

    @Test
    void boundCapabilityMirrorRejectsNbtUntilReboundToAuthority() {
        UUID identity = UUID.randomUUID();
        VicissitudeVitality authority = VicissitudeVitality.initial(1000.0F)
                .afterDamage(10.0F);
        VicissitudeVitalityLedger ledger = new VicissitudeVitalityLedger();
        ledger.commit(identity, authority);
        VicissitudeVitalityCapability.Mirror capability = new VicissitudeVitalityCapability.Mirror();
        capability.synchronize(authority);
        CompoundTag forged = capability.serializeNBT();
        forged.putFloat("Current", 0.0F);
        forged.putInt("KillAttempts", 3);
        capability.deserializeNBT(forged);
        assertTrue(capability.snapshot().current() == 990.0F && capability.snapshot().killAttempts() == 0,
                "Bound mirror deserialization must not change its value or spend bypass attempts");

        VicissitudeVitalityCapability.Mirror loaded = new VicissitudeVitalityCapability.Mirror();
        loaded.deserializeNBT(forged);
        loaded.synchronize(ledger.find(identity));
        assertTrue(loaded.snapshot().current() == 990.0F, "Explicit synchronization must replace the unbound mirror value");
    }

    @Test
    void canceledDeathRestoresOneHealthAndRestartsForcedKillCounting() {
        VicissitudeVitality revived = VicissitudeVitality.initial(1000)
                .killAttempt(1, 100).killAttempt(2, 100).killAttempt(3, 100).afterCanceledDeath();
        assertTrue(revived.current() == 1 && revived.killAttempts() == 0);
        assertTrue(revived.killAttempt(4, 100).killAttempts() == 1);
    }

    @GameTest(template = "test_empty", timeoutTicks = 30)
    public static void canceledLargerChannelNeverReturnsNextTick(GameTestHelper helper) {
        TestBoss boss = spawn(helper);
        float initial = boss.getHealth();
        Events listener = new Events(boss);
        listener.hurt = event -> { if (event.getAmount() > 8) event.setCanceled(true); };
        withListener(listener, () -> {
            boss.hurt(boss.damageSources().generic(), 6);
            boss.hurt(boss.damageSources().generic(), 14);
        });
        equal(helper, initial - 6, boss.getHealth(), "Canceled channel must not debit");
        helper.runAtTickTime(2, () -> {
            equal(helper, initial - 6, boss.getHealth(), "No deferred canceled damage");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void finalEventLossIsTheBatchMaximum(GameTestHelper helper) {
        TestBoss boss = spawn(helper);
        float initial = boss.getHealth();
        Events listener = new Events(boss);
        listener.damage = event -> event.setAmount(event.getAmount() / 2);
        withListener(listener, () -> {
            boss.hurt(boss.damageSources().generic(), 12);
            boss.hurt(boss.damageSources().generic(), 28);
        });
        equal(helper, initial - 14, boss.getHealth(), "Final losses 6 and 14 must total 14");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void nestedLargerChannelRestoresOuterTransaction(GameTestHelper helper) {
        TestBoss boss = spawn(helper);
        float initial = boss.getHealth();
        Events listener = new Events(boss);
        listener.hurt = event -> {
            if (event.getAmount() == 2) boss.hurt(boss.damageSources().magic(), 10);
        };
        withListener(listener, () -> boss.hurt(boss.damageSources().generic(), 2));
        equal(helper, initial - 10, boss.getHealth(), "Outer 2 and nested 10 must total 10");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void nestedSmallerChannelCannotSpendOuterBudget(GameTestHelper helper) {
        TestBoss boss = spawn(helper);
        float initial = boss.getHealth();
        Events listener = new Events(boss);
        listener.damage = event -> {
            if (event.getAmount() == 10) boss.hurt(boss.damageSources().magic(), 2);
        };
        withListener(listener, () -> boss.hurt(boss.damageSources().generic(), 10));
        equal(helper, initial - 10, boss.getHealth(), "Outer 10 and nested 2 must total 10");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void canceledFinalDamageCannotReplaceNestedLoss(GameTestHelper helper) {
        TestBoss boss = spawn(helper);
        float initial = boss.getHealth();
        Events listener = new Events(boss);
        listener.damage = event -> {
            if (event.getAmount() == 20) {
                boss.hurt(boss.damageSources().magic(), 6);
                event.setCanceled(true);
            }
        };
        withListener(listener, () -> boss.hurt(boss.damageSources().generic(), 20));
        equal(helper, initial - 6, boss.getHealth(), "Only the uncanceled nested channel debits");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void callbacksCannotWriteHealthOrExpandTheCap(GameTestHelper helper) {
        TestBoss boss = spawn(helper);
        float initial = boss.getHealth();
        Events listener = new Events(boss);
        listener.hurt = event -> boss.setHealth(0);
        listener.damage = event -> event.setAmount(5000);
        withListener(listener, () -> boss.hurt(boss.damageSources().generic(), 2));
        equal(helper, initial - 100, boss.getHealth(), "Final write must enforce the 100-health cap");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 30)
    public static void canceledDeathRevivesAndNeverFinalizes(GameTestHelper helper) {
        TestBoss boss = spawn(helper);
        VicissitudeVitalityLedger.get(helper.getLevel()).commit(boss.getUUID(),
                VicissitudeVitality.initial(boss.getMaxHealth()).withCurrent(5));
        Events listener = new Events(boss);
        listener.death = event -> event.setCanceled(true);
        withListener(listener, () -> boss.hurt(boss.damageSources().generic(), 10));
        equal(helper, 1, boss.getHealth(), "Canceled death must restore one health");
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(!boss.isRemoved() && boss.isAlive(), "Canceled death must survive removal deadline");
            helper.assertTrue(boss.finalDeaths == 0 && boss.lootDrops == 0, "No death callbacks or rewards");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 30)
    public static void lethalLargerChannelFinalizesExactlyOnce(GameTestHelper helper) {
        TestBoss boss = spawn(helper);
        VicissitudeVitalityLedger.get(helper.getLevel()).commit(boss.getUUID(),
                VicissitudeVitality.initial(boss.getMaxHealth()).withCurrent(12));
        boss.hurt(boss.damageSources().generic(), 6);
        boss.hurt(boss.damageSources().generic(), 14);
        boss.die(boss.damageSources().generic());
        helper.assertTrue(boss.finalDeaths == 1 && boss.lootDrops == 1, "Exactly one real death and reward transaction");
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(boss.isRemoved(), "Completed death sequence must remove entity");
            helper.assertTrue(boss.finalDeaths == 1 && boss.lootDrops == 1, "No repeated finalization");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void forgedEntityNbtAndReloadCannotHealOrKill(GameTestHelper helper) {
        TestBoss boss = spawn(helper);
        boss.hurt(boss.damageSources().generic(), 6);
        float wounded = boss.getHealth();
        UUID identity = boss.getUUID();
        CompoundTag tag = boss.saveWithoutId(new CompoundTag());
        tag.putFloat("Health", 0);
        tag.putUUID("UUID", UUID.randomUUID());
        boss.load(tag);
        equal(helper, wounded, boss.getHealth(), "Live entity NBT cannot debit vitality");
        helper.assertTrue(identity.equals(boss.getUUID()), "Live entity cannot change its ledger identity");
        boss.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        tag.putUUID("UUID", identity);
        tag.putFloat("Health", 1000);
        TestBoss loaded = new TestBoss(helper.getLevel());
        loaded.load(tag);
        helper.assertTrue(helper.getLevel().addFreshEntity(loaded), "Reloaded entity must join");
        equal(helper, wounded, loaded.getHealth(), "Reload must bind to wounded world record");
        equal(helper, wounded, loaded.getCapability(VicissitudeVitalityCapability.CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("Missing attached capability")).current(),
                "Real attached capability must synchronize to authority");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 30)
    public static void thirdDistinctKillStartsTheRealDeathTransaction(GameTestHelper helper) {
        TestBoss boss = spawn(helper);
        float initial = boss.getHealth();
        for (int call = 0; call < 20; call++) boss.kill();
        equal(helper, initial, boss.getHealth(), "One tick of kill calls counts once");
        helper.runAtTickTime(2, () -> {
            boss.kill();
            equal(helper, initial, boss.getHealth(), "Second tick must survive");
        });
        helper.runAtTickTime(3, () -> {
            boss.kill();
            helper.assertTrue(boss.finalDeaths == 1 && boss.lootDrops == 1, "Third tick must start normal death");
        });
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(boss.isRemoved(), "Third kill must finish removal");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void batchMaximumUsesArmorAndAbsorptionResults(GameTestHelper helper) {
        TestBoss boss = spawn(helper);
        boss.getAttribute(Attributes.ARMOR).setBaseValue(20);
        boss.setAbsorptionAmount(4);
        float initial = boss.getHealth();
        float first = CombatRules.getDamageAfterAbsorb(20, 20, 0) - 4;
        float second = CombatRules.getDamageAfterAbsorb(28, 20, 0);
        boss.hurt(boss.damageSources().mobAttack(boss), 20);
        equal(helper, initial - first, boss.getHealth(), "First channel must consume absorption after armor");
        boss.hurt(boss.damageSources().mobAttack(boss), 28);
        equal(helper, initial - Math.max(first, second), boss.getHealth(), "Batch must maximize reduced health loss");
        equal(helper, 0, boss.getAbsorptionAmount(), "Absorption must be consumed once available");
        helper.succeed();
    }

    private static TestBoss spawn(GameTestHelper helper) {
        TestBoss boss = new TestBoss(helper.getLevel());
        boss.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        boss.setNoGravity(true);
        helper.assertTrue(helper.getLevel().addFreshEntity(boss), "Test boss must join the real level");
        return boss;
    }

    private static void equal(GameTestHelper helper, float expected, float actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.001F, message + ": expected " + expected + ", actual " + actual);
    }

    private static void withListener(Events listener, Runnable action) {
        MinecraftForge.EVENT_BUS.register(listener);
        try { action.run(); } finally { MinecraftForge.EVENT_BUS.unregister(listener); }
    }

    public static final class Events {
        private final TestBoss boss;
        Consumer<LivingHurtEvent> hurt = event -> {};
        Consumer<LivingDamageEvent> damage = event -> {};
        Consumer<LivingDeathEvent> death = event -> {};
        Events(TestBoss boss) { this.boss = boss; }
        @SubscribeEvent public void hurt(LivingHurtEvent event) { if (event.getEntity() == boss) hurt.accept(event); }
        @SubscribeEvent public void damage(LivingDamageEvent event) { if (event.getEntity() == boss) damage.accept(event); }
        @SubscribeEvent public void death(LivingDeathEvent event) { if (event.getEntity() == boss) death.accept(event); }
    }

    private static final class TestBoss extends VicissitudeBossEntity {
        int finalDeaths;
        int lootDrops;
        TestBoss(ServerLevel level) {
            super(MaledictEntities.FIRST_VICISSITUDE.get(), level);
            getAttribute(Attributes.ARMOR).setBaseValue(0);
            getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
        }
        @Override protected void registerGoals() {}
        @Override protected float getVitalityDamageLimit() { return 100; }
        @Override protected void onFinalDeath(DamageSource source) { finalDeaths++; }
        @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) { lootDrops++; }
    }
}
