package org.brahypno.maledict.common.curio.priestess;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.registry.MaledictEntities;
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
        assertOffset(helper, target, echo);
        Vec3 origin = echo.position();
        target.setPos(target.position().add(6, 0, 0));
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
            assertOffset(helper, target, echo);
            helper.assertTrue(echo.remainingTicks() == 60,
                    "A direct hit refreshes the full lifetime");
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
                echo.tick();
                equal(helper, target.getHealth(), echo.getHealth(), "The echo displays the body's health after damage");
                equal(helper, target.getMaxHealth(), echo.getMaxHealth(), "The echo displays the body's maximum health");
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(120);
                target.setHealth(110);
                echo.tick();
                equal(helper, 110, echo.getHealth(), "Healing updates the echo's displayed health");
                equal(helper, 120, echo.getMaxHealth(), "Maximum-health changes update the echo's displayed maximum");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(observer);
            }
            echo.discard();
        }
        assertMeltfallCreatesEcho(helper, EntityType.WARDEN);
        assertMeltfallCreatesEcho(helper, EntityType.byString("dummmmmmy:target_dummy").orElseThrow());
        helper.succeed();
    }

    private static void assertMeltfallCreatesEcho(GameTestHelper helper, EntityType<?> type) {
        try (Caster caster = new Caster(helper, 3)) {
            LivingEntity body = (LivingEntity) type.create(helper.getLevel());
            body.setPos(helper.absolutePos(new BlockPos(3, 2, 3)).getCenter());
            if (body instanceof Mob mob) mob.setNoAi(true);
            helper.getLevel().addFreshEntity(body);
            try {
                ItemStack axe = new ItemStack(MaledictItems.MELTFALL_AXE.get());
                caster.player.setItemSlot(EquipmentSlot.MAINHAND, axe);
                caster.player.getAttributes().addTransientAttributeModifiers(axe.getAttributeModifiers(EquipmentSlot.MAINHAND));
                caster.player.attack(body);
                var echoes = echoes(helper, body);
                helper.assertTrue(echoes.size() == 1,
                        "A stage-three wearer's Meltfall Axe magic damage creates one echo for " + type);
                SoulEchoEntity echo = echoes.get(0);
                assertOffset(helper, body, echo);
                helper.assertTrue(echo.noPhysics && !echo.isPushable() && !echo.canBeCollidedWith(),
                        "The echo has no physical collision or pushing");
                Vec3 anchor = echo.position();
                echo.push(1, 1, 1);
                helper.assertTrue(echo.position().equals(anchor) && echo.getDeltaMovement().lengthSqr() == 0,
                        "A direct push cannot move the echo");
                // Reconstruct the tracked spawn data: the client must be able to find the original model.
                SoulEchoEntity replica = new SoulEchoEntity(MaledictEntities.SOUL_ECHO.get(), helper.getLevel());
                replica.getEntityData().assignValues(echo.getEntityData().getNonDefaultValues());
                helper.assertTrue(replica.visualBody() == body,
                        "Synced echo data identifies the original body for rendering " + type);
                replica.discard();
                echo.discard();
            } finally {
                body.discard();
            }
        }
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

    private static void assertOffset(GameTestHelper helper, LivingEntity target, SoulEchoEntity echo) {
        Vec3 offset = echo.position().subtract(target.position());
        double distance = offset.horizontalDistance();
        helper.assertTrue(distance >= 0.5 - 0.000001 && distance <= 0.8 + 0.000001,
                "The occupied anchor shifts horizontally by 0.5 to 0.8 blocks");
        equal(helper, 0, offset.y, "The offset preserves the anchor height");
    }

    private static final class Caster implements AutoCloseable {
        final FakePlayer player;
        Caster(GameTestHelper helper, int stage) {
            player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "SoulEchoTest")) {
                @Override public float getAttackStrengthScale(float partialTick) { return 1; }
            };
            player.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
            var inventory = CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
            inventory.setCurios(Map.of("delusion", new CurioStacksHandler(inventory, "delusion", 1,
                    true, true, true, ICurio.DropRule.DEFAULT)));
            inventory.setEquippedCurio("delusion", 0, EidolonOfTheHighPriestessItem.create(stage));
        }
        @Override public void close() { player.discard(); }
    }
}
