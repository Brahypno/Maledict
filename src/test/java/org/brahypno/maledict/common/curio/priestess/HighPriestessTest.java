package org.brahypno.maledict.common.curio.priestess;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.vitals.DelayedVitals;
import org.brahypno.maledict.common.vitals.DelayedVitalsPool;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.Map;
import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class HighPriestessTest {

    @GameTest(template = "test_empty", batch = "high_priestess")
    public static void incomingDamageUsesTheExistingPoolAfterArmorAndAbsorptionWithoutAdaptation(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper, 2); Fixture attacker = new Fixture(helper, 0)) {
            f.player.getAttribute(Attributes.ARMOR).setBaseValue(20);
            for (int hit = 1; hit <= 2; hit++) {
                f.player.invulnerableTime = 0;
                f.player.setAbsorptionAmount(2);
                f.player.receive(f.player.damageSources().mobAttack(attacker.player), 20);
                equal(helper, 20, f.player.getHealth(), "Damage is queued instead of deducted immediately");
                equal(helper, 10 * hit, f.pool().pendingDamage(),
                        "Armor reduces 20 to 12 and absorption takes 2; Priestess grants no rune adaptation");
            }
            equal(helper, 20 * DelayedVitalsPool.DAMAGE_RELEASE_PER_TICK + DelayedVitalsPool.RELEASE_FLOOR,
                    f.pool().releaseDamage(), "The original gradual release formula is reused");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "high_priestess")
    public static void confirmedCounterDamageCancelsDebtFirstAndOnlyExcessBecomesGradualHealing(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper, 2)) {
            // The confirmed sequence: 10 damage, 2 already paid, 8 pending, then deal 3.
            f.player.setHealth(18);
            DelayedVitals.get(f.player).queueDamage(8, f.player.damageSources().generic());
            LivingEntity target = target(helper);
            target.hurt(target.damageSources().mobAttack(f.player), 3);
            equal(helper, 5, f.pool().pendingDamage(), "Dealing 3 reduces the remaining debt from 8 to 5");
            equal(helper, 0, f.pool().pendingHeal(), "No healing is banked while debt remains");
            equal(helper, 18, f.player.getHealth(), "Countering does not heal already lost health instantly");

            target.invulnerableTime = 0;
            target.hurt(target.damageSources().mobAttack(f.player), 7);
            equal(helper, 0, f.pool().pendingDamage(), "The next 7 clears the remaining 5 debt");
            equal(helper, 2, f.pool().pendingHeal(), "Only the excess 2 enters the existing healing pool");
            equal(helper, 18, f.player.getHealth(), "Excess healing is also gradual");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "high_priestess")
    public static void recoveryUsesPostMitigationDamageAndDoesNotCountOverkill(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper, 2)) {
            LivingEntity target = target(helper);
            target.getAttribute(Attributes.ARMOR).setBaseValue(20);
            target.setAbsorptionAmount(2);
            target.hurt(target.damageSources().mobAttack(f.player), 20);
            equal(helper, 10, f.pool().pendingHeal(), "Only the 10 damage past armor and absorption counts");

            target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            target.setHealth(2);
            target.invulnerableTime = 0;
            target.hurt(target.damageSources().mobAttack(f.player), 20);
            equal(helper, 12, f.pool().pendingHeal(), "A target with 2 health cannot provide 20 healing");
        }
        helper.succeed();
    }

    private static LivingEntity target(GameTestHelper helper) {
        var cow = helper.spawn(EntityType.COW, new BlockPos(3, 2, 3));
        cow.setNoAi(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        cow.setHealth(100);
        return cow;
    }

    private static void equal(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.001D, message + ": expected " + expected + ", got " + actual);
    }

    private static final class Fixture implements AutoCloseable {
        final TestPlayer player;
        final ICuriosItemHandler inventory;

        Fixture(GameTestHelper helper, int stage) {
            player = new TestPlayer(helper.getLevel());
            player.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
            inventory = CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
            inventory.setCurios(Map.of(
                    "delusion", new CurioStacksHandler(inventory, "delusion", 1, true, true, true, ICurio.DropRule.DEFAULT),
                    "rune", new CurioStacksHandler(inventory, "rune", 1, true, true, true, ICurio.DropRule.DEFAULT),
                    "charm", new CurioStacksHandler(inventory, "charm", 1, true, true, true, ICurio.DropRule.DEFAULT)));
            inventory.setEquippedCurio("delusion", 0, EidolonOfTheHighPriestessItem.create(stage));
        }

        void tickCurios() {
            MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent(player));
        }

        DelayedVitalsPool pool() { return DelayedVitals.get(player).pool(); }

        @Override public void close() { player.discard(); }
    }

    /** FakePlayer is permanently invulnerable; expose the real Player damage pipeline for these tests. */
    private static final class TestPlayer extends FakePlayer {
        TestPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "PriestessTest"));
        }

        @Override public boolean isInvulnerableTo(DamageSource source) { return false; }

        void receive(DamageSource source, float amount) { actuallyHurt(source, amount); }
    }
}
