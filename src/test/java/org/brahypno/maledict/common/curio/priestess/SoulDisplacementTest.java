package org.brahypno.maledict.common.curio.priestess;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.capability.MalumLivingEntityDataCapability;
import com.sammy.malum.core.handlers.SoulDataHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import org.brahypno.changelib.DamageHelper.DamagePolicy;
import org.brahypno.changelib.DamageHelper.DamageStage;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.registry.MaledictEntities;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class SoulDisplacementTest {

    @GameTest(template = "test_empty", batch = "priestess_soul", timeoutTicks = 130)
    public static void confirmedSequenceForwardsWithoutRefreshingThenDirectHitRefreshes(GameTestHelper helper) {
        Caster caster = new Caster(helper, 3);
        LivingEntity target = target(helper);
        target.hurt(target.damageSources().mobAttack(caster.player), 1);
        helper.assertTrue(echoes(helper, target).isEmpty(), "Physical hits must not create an echo");
        target.invulnerableTime = 0;
        SoulDataHandler.exposeSoul(target);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200));
        DamageSource spell = spell(target, caster.player);
        target.hurt(spell, 4);
        SoulEchoEntity echo = echoes(helper, target).get(0);
        helper.assertTrue(echo.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == target.getEffect(MobEffects.MOVEMENT_SLOWDOWN)
                        && MalumLivingEntityDataCapability.getCapability(echo)
                        == MalumLivingEntityDataCapability.getCapability(target),
                "A new echo immediately shares effects and soul exposure that predate its creation");
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
                equal(helper, target.getHealth(), echo.getHealth(), "Health is shared immediately without an echo tick");
                equal(helper, target.getMaxHealth(), echo.getMaxHealth(), "The echo displays the body's maximum health");
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(120);
                target.setHealth(110);
                equal(helper, 110, echo.getHealth(), "Healing updates the echo's displayed health");
                equal(helper, 120, echo.getMaxHealth(), "Maximum-health changes update the echo's displayed maximum");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(observer);
            }
            assertSharedState(helper, target, echo, caster.player);
            echo.discard();
            helper.assertTrue(target.getCapability(MalumLivingEntityDataCapability.CAPABILITY).isPresent(),
                    "Discarding the echo must not invalidate the body's capabilities");
        }
        assertMeltfallCreatesEcho(helper, EntityType.WARDEN);
        assertMeltfallCreatesEcho(helper, EntityType.byString("dummmmmmy:target_dummy").orElseThrow());
        assertWitherEchoDeath(helper, false);
        assertWitherEchoDeath(helper, true);
        helper.succeed();
    }

    private static void assertWitherEchoDeath(GameTestHelper helper, boolean probeFallback) {
        try (Caster caster = new Caster(helper, 3); Caster finisher = new Caster(helper, 0)) {
            var wither = MaledictEntities.WEEPING_WITHER.get().create(helper.getLevel());
            wither.setNoAi(true);
            wither.setNoGravity(true);
            wither.getAttribute(Attributes.ARMOR).setBaseValue(0);
            wither.setPos(helper.absolutePos(new BlockPos(3, 10, 3)).getCenter());
            // Test the final life; the encounter test covers the first lethal hit's reformation.
            CompoundTag saved = wither.saveWithoutId(new CompoundTag());
            saved.putBoolean("ReformationUsed", true);
            saved.putFloat("Health", probeFallback ? 81 : 11);
            wither.readAdditionalSaveData(saved);
            helper.getLevel().addFreshEntity(wither);
            wither.hurt(spell(wither, caster.player), 1);
            SoulEchoEntity echo = echoes(helper, wither).get(0);
            DamageSource source = spell(wither, finisher.player);
            wither.setPos(wither.position().add(6, 0, 0));
            wither.invulnerableTime = 0;
            var bodyDeaths = new AtomicInteger();
            var echoDeaths = new AtomicInteger();
            Consumer<LivingDeathEvent> observe = event -> {
                if (event.getEntity() == wither) {
                    bodyDeaths.incrementAndGet();
                    helper.assertTrue(event.getSource() == source, "The body dies with the original spell source");
                } else if (event.getEntity() == echo) echoDeaths.incrementAndGet();
            };
            MinecraftForge.EVENT_BUS.addListener(observe);
            try {
                if (probeFallback) {
                    DamageProbe.mediumDamageMethod(echo, source, 1000);
                    helper.assertTrue(wither.getHealth() >= 61 && bodyDeaths.get() == 0,
                            "Probing the echo must respect the body's D3 cap");
                    DamageProbe.damage(wither, source, 1000, DamagePolicy.through(DamageStage.D4_CONSISTENCY_WRITE));
                } else {
                    helper.assertTrue(echo.hurt(source, 20), "A regular lethal spell reaches the body");
                }
                echo.die(source); // A probe may try to finalize a hit whose body already handled death.
                helper.assertTrue(bodyDeaths.get() == 1 && echoDeaths.get() == 0,
                        "Both regular spells and probe fallback must run the body's death exactly once: fallback="
                                + probeFallback + ", body=" + bodyDeaths + ", echo=" + echoDeaths);
                helper.assertTrue(wither.getKillCredit() == finisher.player,
                        "The body credits the final attacker rather than the player who created the echo");
                var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, wither.getBoundingBox().inflate(3));
                helper.assertTrue(drops.stream().filter(drop -> drop.getItem().is(MaledictItems.BLIGHTED_STAR.get()))
                                .mapToInt(drop -> drop.getItem().getCount()).sum() == 1
                                && drops.stream().noneMatch(drop -> drop.getItem().is(Items.NETHER_STAR)),
                        "Killing through an echo drops exactly one Blighted Star at the body: fallback=" + probeFallback);
                drops.forEach(ItemEntity::discard);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(observe);
                echo.discard();
                wither.discard();
            }
        }
    }

    private static void assertSharedState(GameTestHelper helper, LivingEntity body, SoulEchoEntity echo,
                                          FakePlayer caster) {
        var data = MalumLivingEntityDataCapability.getCapability(body);
        SoulDataHandler.exposeSoul(body);
        helper.assertTrue(MalumLivingEntityDataCapability.getCapability(echo) == data,
                "Malum soul exposure is the same capability, not a periodically copied value");
        data.soulData.exposedSoulDuration = 0;
        SoulDataHandler.exposeSoul(echo);
        equal(helper, 200, data.soulData.exposedSoulDuration, "Exposing the echo exposes the body's soul");

        body.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200));
        helper.assertTrue(echo.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == body.getEffect(MobEffects.MOVEMENT_SLOWDOWN)
                        && echo.getActiveEffectsMap() == body.getActiveEffectsMap(),
                "Existing body effects are shared immediately, including the same duration and amplifier");
        helper.assertTrue(echo.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 240, 1), caster),
                "Applying an effect through the echo upgrades the body effect");
        helper.assertTrue(body.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 1,
                "The upgraded amplifier belongs to the body");
        var movement = body.getAttribute(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(echo.getAttribute(Attributes.MOVEMENT_SPEED) == movement
                        && echo.getAttributes().getInstance(Attributes.MOVEMENT_SPEED) == movement,
                "Direct and AttributeMap access both return the body's actual attribute instance");
        equal(helper, body.getAttributeValue(Attributes.MOVEMENT_SPEED),
                echo.getAttributeValue(Attributes.MOVEMENT_SPEED), "Effect modifiers are applied once");
        echo.getAttributes().getDirtyAttributes().clear();
        helper.assertTrue(body.getAttributes().getDirtyAttributes().contains(movement),
                "Echo tracking must not consume attribute changes that still need sending for the body");

        echo.getAttribute(Attributes.MAX_HEALTH).setBaseValue(140);
        equal(helper, 140, body.getMaxHealth(), "Changing echo attributes changes the body");
        echo.setHealth(100);
        equal(helper, 100, body.getHealth(), "Writing echo health writes body health");
        echo.setAbsorptionAmount(7);
        equal(helper, 7, body.getAbsorptionAmount(), "Absorption is shared");
        helper.assertTrue(echo.getPersistentData() == body.getPersistentData(), "Persistent mod data is shared");
        echo.setInvisible(true);
        helper.assertTrue(body.isInvisible() && echo.isInvisible(), "Invisibility belongs to the body");
        body.setInvisible(false);
        helper.assertTrue(!echo.isInvisible(), "Removing body invisibility is visible through the echo immediately");
        helper.assertTrue(echo.fireImmune() == body.fireImmune() && echo.canFreeze() == body.canFreeze(),
                "The echo uses the body's fire and freeze immunities");
        echo.setTicksFrozen(40);
        helper.assertTrue(body.getTicksFrozen() == 40, "Freeze state belongs to the body");
        echo.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        helper.assertTrue(echo.getItemBySlot(EquipmentSlot.HEAD) == body.getItemBySlot(EquipmentSlot.HEAD),
                "Equipment access shares the body's stack");

        int duration = body.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration();
        echo.tick();
        equal(helper, duration, body.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration(),
                "The echo must not tick the body's effects a second time");
        equal(helper, 200, data.soulData.exposedSoulDuration,
                "The echo must not fire living ticks that decrement shared Malum capability state");
        body.tick();
        equal(helper, duration - 1, echo.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration(),
                "Only a body tick advances the shared effect duration");
        equal(helper, 199, data.soulData.exposedSoulDuration, "Only a body tick advances soul exposure");

        helper.assertTrue(echo.removeEffect(MobEffects.MOVEMENT_SLOWDOWN)
                        && !body.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "Removing through the echo removes the body effect and its attribute modifiers");
        echo.forceAddEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200), caster);
        helper.assertTrue(body.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Forced effects also reach the body");
        helper.assertTrue(echo.curePotionEffects(new ItemStack(Items.MILK_BUCKET)) && !body.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "Forge effect cures operate on the body");
        echo.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200), caster);
        helper.assertTrue(echo.removeAllEffects() && body.getActiveEffects().isEmpty(),
                "Clearing echo effects clears the body's effects");
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
                helper.assertTrue(replica.getAttribute(Attributes.MAX_HEALTH) == body.getAttribute(Attributes.MAX_HEALTH)
                                && MalumLivingEntityDataCapability.getCapability(replica)
                                == MalumLivingEntityDataCapability.getCapability(body),
                        "A replica bound only by synced entity ID also resolves the body's gameplay state");
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
