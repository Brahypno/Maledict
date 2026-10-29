package org.brahypno.maledict.common.curio.priestess;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class SoulDisplacementTest {

    @GameTest(template = "test_empty", batch = "priestess_soul", timeoutTicks = 130)
    public static void confirmedSequenceForwardsWithoutRefreshingThenDirectHitRefreshes(GameTestHelper helper) {
        Caster caster = new Caster(helper, 3);
        LivingEntity target = target(helper);
        DamageSource spell = spell(target, caster.player);
        target.hurt(spell, 4);
        SoulEchoEntity echo = echoes(helper, target).get(0);
        Vec3 origin = target.position();
        target.setPos(origin.add(6, 0, 0));
        helper.runAtTickTime(20, () -> {
            int remaining = echo.remainingTicks();
            float before = target.getHealth();
            target.invulnerableTime = 0;
            helper.assertTrue(echo.hurt(spell, 3), "A spell at the old position must reach the body");
            equal(helper, 3, before - target.getHealth(), "Only the body loses health");
            helper.assertTrue(echo.remainingTicks() == remaining && echo.position().equals(origin),
                    "Relayed damage neither refreshes the timer nor moves the echo");
            helper.assertTrue(echoes(helper, target).size() == 1, "Relayed damage cannot create another echo");
        });
        helper.runAtTickTime(40, () -> {
            target.invulnerableTime = 0;
            target.hurt(spell, 2);
            helper.assertTrue(echoes(helper, target).size() == 1 && echoes(helper, target).get(0) == echo,
                    "A direct hit reuses the same echo");
            helper.assertTrue(echo.position().equals(target.position()) && echo.remainingTicks() == 60,
                    "A direct hit refreshes the anchor position and full lifetime");
        });
        helper.runAtTickTime(99, () -> helper.assertTrue(!echo.isRemoved(), "The refreshed echo lives until its new deadline"));
        helper.runAtTickTime(102, () -> {
            helper.assertTrue(echo.isRemoved(), "The echo expires at the refreshed deadline");
            caster.close();
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", batch = "priestess_soul")
    public static void forwardingPreservesSourceAndUsesBodyDefensesOnce(GameTestHelper helper) {
        try (Caster caster = new Caster(helper, 3)) {
            LivingEntity target = target(helper);
            DamageSource spell = spell(target, caster.player);
            target.hurt(spell, 4);
            SoulEchoEntity echo = echoes(helper, target).get(0);
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200));
            target.invulnerableTime = 0;
            HitObserver observer = new HitObserver(target, spell);
            MinecraftForge.EVENT_BUS.register(observer);
            try {
                float before = target.getHealth();
                helper.assertTrue(!echo.hurt(target.damageSources().mobAttack(caster.player), 4),
                        "Physical damage cannot use the echo");
                helper.assertTrue(echo.hurt(spell, 4), "Magic damage must reach the body");
                equal(helper, 3.2, before - target.getHealth(), "The body's resistance reduces forwarded damage once");
                helper.assertTrue(observer.hits == 1 && observer.sameSource, "One body damage event retains the original spell source");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(observer);
            }
            echo.discard();
        }
        helper.succeed();
    }

    public static final class HitObserver {
        private final LivingEntity target;
        private final DamageSource source;
        int hits;
        boolean sameSource;
        HitObserver(LivingEntity target, DamageSource source) { this.target = target; this.source = source; }
        @SubscribeEvent public void damage(LivingDamageEvent event) {
            if (event.getEntity() == target) { hits++; sameSource = event.getSource() == source; }
        }
    }

    private static LivingEntity target(GameTestHelper helper) {
        var cow = helper.spawn(EntityType.COW, new BlockPos(3, 2, 3));
        cow.setNoAi(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        cow.setHealth(100);
        return cow;
    }

    private static DamageSource spell(LivingEntity target, FakePlayer caster) {
        return target.damageSources().indirectMagic(caster, caster);
    }

    private static List<SoulEchoEntity> echoes(GameTestHelper helper, LivingEntity target) {
        return helper.getLevel().getEntitiesOfClass(SoulEchoEntity.class, target.getBoundingBox().inflate(16),
                echo -> target.getUUID().equals(echo.targetUuid()));
    }

    private static void equal(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.001, message + ": expected " + expected + ", got " + actual);
    }

    private static final class Caster implements AutoCloseable {
        final FakePlayer player;
        Caster(GameTestHelper helper, int stage) {
            player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "SoulEchoTest"));
            player.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
            var inventory = CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
            inventory.setCurios(Map.of("delusion", new CurioStacksHandler(inventory, "delusion", 1,
                    true, true, true, ICurio.DropRule.DEFAULT)));
            inventory.setEquippedCurio("delusion", 0, EidolonOfTheHighPriestessItem.create(stage));
        }
        @Override public void close() { player.discard(); }
    }
}
