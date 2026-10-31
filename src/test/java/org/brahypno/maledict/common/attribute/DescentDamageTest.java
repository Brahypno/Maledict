package org.brahypno.maledict.common.attribute;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictAttributes;

import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class DescentDamageTest {
    @GameTest(template = "test_empty", batch = "descent")
    public static void bothSidesSumInsteadOfTakingMaximumOrMultiplyingTwice(GameTestHelper helper) {
        double[][] cases = {{0, 0, 10}, {10, 0, 28.963617}, {0, 10, 28.963617},
                {10, 10, 35.939941}, {20, 10, 38.506388}};
        for (double[] row : cases) {
            Cow attacker = cow(helper, row[0]);
            Cow victim = cow(helper, row[1]);
            hit(helper, victim, victim.damageSources().mobAttack(attacker), 10, row[2]);
        }
        helper.succeed();
    }

    public static final class CancelDamage {
        private final LivingEntity victim;

        CancelDamage(LivingEntity victim) { this.victim = victim; }

        @SubscribeEvent
        public void damage(LivingDamageEvent.Pre event) {
            if (event.getEntity() == victim) event.setNewDamage(0.0F);
        }
    }

    private static Cow cow(GameTestHelper helper, double descent) {
        Cow cow = EntityType.COW.create(helper.getLevel());
        cow.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        cow.setHealth(100);
        cow.getAttribute(MaledictAttributes.DESCENT).setBaseValue(descent);
        return cow;
    }

    private static FakePlayer player(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "DescentTest"));
        player.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        return player;
    }

    private static void hit(GameTestHelper helper, LivingEntity victim, DamageSource source,
                            float amount, double expectedLoss) {
        float initial = victim.getHealth();
        victim.invulnerableTime = 0;
        victim.hurt(source, amount);
        equal(helper, expectedLoss, initial - victim.getHealth(), "Actual post-mitigation health loss");
    }

    private static void equal(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.0001,
                message + ": expected " + expected + ", actual " + actual);
    }
}
