package org.brahypno.maledict.common.entity.vicissitude;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtIo;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.changelib.DamageHelper.DamageStage;
import org.brahypno.changelib.HealthHelper.CombatProtection;
import org.brahypno.changelib.protection.ai.AiProtection;
import org.brahypno.maledict.common.entity.vicissitude.VicissitudeBossEntity;
import org.brahypno.maledict.common.entity.vicissitude.VicissitudeVitality;
import org.brahypno.maledict.common.entity.vicissitude.VicissitudeVitalityLedger;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusBladeItem;
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
                                                         .afterDamage(14.0F).killAttempt(11, 100).killAttempt(12, 100);
        VicissitudeVitality corpse = VicissitudeVitality.initial(1000.0F)
                                                        .killAttempt(1, 100).killAttempt(2, 100).killAttempt(3, 100);
        VicissitudeVitalityLedger ledger = new VicissitudeVitalityLedger();
        ledger.commit(woundedId, wounded);
        ledger.commit(deadId, corpse);
        Path file = directory.resolve("ledger.dat");
        ledger.save(file.toFile());
        VicissitudeVitalityLedger restored =
                VicissitudeVitalityLedger.load(NbtIo.readCompressed(file.toFile()).getCompound("data"));
        assertTrue(wounded.equals(restored.find(woundedId)), "Saving must retain wounds and timers");
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
        listener.hurt = event -> {
            if (event.getAmount() > 8)
                event.setCanceled(true);
        };
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
            if (event.getAmount() == 2)
                boss.hurt(boss.damageSources().magic(), 10);
        };
        withListener(listener, () -> boss.hurt(boss.damageSources().generic(), 2));
        equal(helper, initial - 10, boss.getHealth(), "Outer 2 and nested 10 must total 10");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 30)
    @SuppressWarnings("unchecked")
    public static void canceledDeathRevivesAndNeverFinalizes(GameTestHelper helper) throws ReflectiveOperationException {
        TestBoss boss = spawn(helper);
        boss.restoreAuthority(VicissitudeVitality.initial(boss.getMaxHealth()).withCurrent(5));
        Events listener = new Events(boss);
        listener.death = event -> event.setCanceled(true);
        withListener(listener, () -> boss.hurt(boss.damageSources().generic(), 10));
        equal(helper, 1, boss.getHealth(), "Canceled death must restore one health");
        var field = VicissitudeBossEntity.class.getDeclaredField("DISPLAY_VITALITY");
        field.setAccessible(true);
        var display = (EntityDataAccessor<Float>) field.get(null);
        boss.getEntityData().set(display, 0.0F);
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(!boss.isRemoved() && boss.isAlive(), "Canceled death must survive removal deadline");
            helper.assertTrue(boss.finalDeaths == 0 && boss.lootDrops == 0, "No death callbacks or rewards");
            equal(helper, 1, boss.getEntityData().get(display), "A stale zero display must recover without a new health commit");
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
        for (var tier : IncursusBladeItem.DamageTier.values()) {
            TestBoss probed = spawn(helper);
            probed.getAttribute(Attributes.ARMOR).setBaseValue(20);
            probed.setAbsorptionAmount(4);
            float before = probed.getHealth();
            var result = tier.deal(probed, probed.damageSources().mobAttack(probed), 20);
            equal(helper, first, before - probed.getHealth(),
                  tier + " must settle the same mitigated loss as a direct hit");
            helper.assertTrue(result.instantEffective(), tier + " must recognize a capped authoritative hit");
            probed.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
        }
        TestBoss owned = spawn(helper);
        LivingEntity external = EntityType.COW.create(helper.getLevel());
        external.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        external.setHealth(100);
        external.setPos(owned.position());
        helper.getLevel().addFreshEntity(external);
        CombatProtection.protect(external, DamageStage.D4_CONSISTENCY_WRITE, 8);
        try {
            float before = owned.getHealth();
            var light = IncursusBladeItem.DamageTier.LIGHT;
            var result = light.deal(owned, owned.damageSources().mobAttack(owned), 160, false,
                                    DamageStage.D5_RECOVERY_SOURCE);
            equal(helper, before - 160, owned.getHealth(), "Light with internal D5 can penetrate a Maledict D4 pool");
            helper.assertTrue(result.effectiveStage() == DamageStage.D5_RECOVERY_SOURCE,
                              "D5 must actually settle the part that D4 blocked");
            light.deal(external, external.damageSources().generic(), 20, false, DamageStage.D5_RECOVERY_SOURCE);
            equal(helper, 92, external.getHealth(), "Light keeps its ordinary probe on an external D4 pool");
        }
        finally {
            owned.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
            external.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void anAuthoredCutShortensTheWindowWithoutReconfiguringIt(GameTestHelper helper) {
        TestBoss boss = spawn(helper);
        float initial = boss.getHealth();
        boss.hurt(boss.damageSources().mobAttack(boss), 6);
        equal(helper, initial - 6, boss.getHealth(), "The first hit must land");
        helper.runAtTickTime(1, () -> {
            helper.assertTrue(CombatProtection.rhythmRemaining(boss) > 0, "The window must be running");
            // 附魔/技能走的就是这条路：外部直接削目标的当前无敌帧，不需要经过 Boss 自己。
            int left = CombatProtection.shortenRhythm(boss, 20);
            helper.assertTrue(left == 0, "An outside shave must open the window: left=" + left);
            boss.hurt(boss.damageSources().mobAttack(boss), 6);
            equal(helper, initial - 12, boss.getHealth(), "The hit after the outside shave must land");
        });
        helper.runAtTickTime(2, () -> {
            helper.assertTrue(CombatProtection.rhythmRemaining(boss) > 15,
                              "The shave must not change the configured interval: remaining="
                              + CombatProtection.rhythmRemaining(boss));
            // Boss 自己的入口走同一条逻辑。
            boss.cutWindow(20);
            helper.assertTrue(CombatProtection.rhythmRemaining(boss) == 0, "The own-hook cut must open the window too");
            boss.hurt(boss.damageSources().mobAttack(boss), 6);
            equal(helper, initial - 18, boss.getHealth(), "The hit after the own-hook cut must land");
            helper.succeed();
        });
    }

    private static TestBoss spawn(GameTestHelper helper) {
        TestBoss boss = new TestBoss(helper.getLevel());
        boss.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        boss.setNoGravity(true);
        helper.assertTrue(helper.getLevel().addFreshEntity(boss), "Test boss must join the real level");
        helper.assertTrue(CombatProtection.state(boss) != null && AiProtection.state(boss) != null,
                          "Joining a Vicissitude boss must bind health and AI protection");
        return boss;
    }

    private static void equal(GameTestHelper helper, float expected, float actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.001F, message + ": expected " + expected + ", actual " + actual);
    }

    private static void withListener(Events listener, Runnable action) {
        MinecraftForge.EVENT_BUS.register(listener);
        try {action.run();} finally {MinecraftForge.EVENT_BUS.unregister(listener);}
    }

    public static final class Events {
        private final TestBoss boss;
        Consumer<LivingHurtEvent> hurt = event -> {};
        Consumer<LivingDamageEvent> damage = event -> {};
        Consumer<LivingDeathEvent> death = event -> {};

        Events(TestBoss boss) {this.boss = boss;}

        @SubscribeEvent
        public void hurt(LivingHurtEvent event) {
            if (event.getEntity() == boss)
                hurt.accept(event);
        }

        @SubscribeEvent
        public void damage(LivingDamageEvent event) {
            if (event.getEntity() == boss)
                damage.accept(event);
        }

        @SubscribeEvent
        public void death(LivingDeathEvent event) {
            if (event.getEntity() == boss)
                death.accept(event);
        }
    }

    private static final class TestBoss extends VicissitudeBossEntity {
        int finalDeaths;
        int lootDrops;

        TestBoss(ServerLevel level) {
            super(MaledictEntities.FIRST_VICISSITUDE.get(), level);
            getAttribute(Attributes.ARMOR).setBaseValue(0);
            getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
        }

        @Override
        protected void registerGoals() {}

        @Override
        protected float getVitalityDamageLimit() {return 100;}

        @Override
        protected void onFinalDeath(DamageSource source) {finalDeaths++;}

        @Override
        protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {lootDrops++;}

        void cutWindow(int ticks) {shortenHitWindow(ticks);}
    }
}
