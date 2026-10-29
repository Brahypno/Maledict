package org.brahypno.maledict.common.vicissitude;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtIo;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.CombatRules;
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
