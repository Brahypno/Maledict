package org.brahypno.maledict.common.attribute;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.vitals.DelayedVitals;
import org.brahypno.maledict.registry.MaledictAttributes;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.registry.MaledictMobEffects;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.Map;
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

    @GameTest(template = "test_empty", batch = "descent")
    public static void environmentalAndSelfDamageCountVictimOnce(GameTestHelper helper) {
        Cow victim = cow(helper, 10);
        hit(helper, victim, victim.damageSources().fall(), 10, 28.963617);
        hit(helper, victim, victim.damageSources().mobAttack(victim), 10, 28.963617);
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "descent")
    public static void multiplierRunsAfterArmorAndAbsorption(GameTestHelper helper) {
        Cow attacker = cow(helper, 10);
        Cow victim = cow(helper, 0);
        victim.getAttribute(Attributes.ARMOR).setBaseValue(20);
        victim.setAbsorptionAmount(2);
        // Armor reduces 20 to 12; absorption takes 2; Descent scales the remaining 10.
        hit(helper, victim, victim.damageSources().mobAttack(attacker), 20, 28.963617);
        victim.setAbsorptionAmount(100);
        hit(helper, victim, victim.damageSources().mobAttack(attacker), 5, 0);
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "descent")
    public static void projectileReadsOwnerAtImpactAndStacksWithEnlightenment(GameTestHelper helper) {
        FakePlayer attacker = player(helper);
        attacker.getAttribute(MaledictAttributes.DESCENT.get()).setBaseValue(2);
        Arrow arrow = new Arrow(helper.getLevel(), attacker);
        attacker.getAttribute(MaledictAttributes.DESCENT.get()).setBaseValue(10);
        attacker.addEffect(new MobEffectInstance(MaledictMobEffects.AGE_OF_ENLIGHTENMENT.get(), 200));
        Cow victim = cow(helper, 0);
        hit(helper, victim, victim.damageSources().arrow(arrow, attacker), 5, 14.481809);
        hit(helper, victim, victim.damageSources().playerAttack(attacker), 5, 28.963617);
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "descent")
    public static void standardModifiersAffectDamageWithoutChangingBaseOrLifetime(GameTestHelper helper) {
        Cow victim = cow(helper, 2);
        var attribute = victim.getAttribute(MaledictAttributes.DESCENT.get());
        AttributeModifier permanent = new AttributeModifier(UUID.randomUUID(), "saved descent", 3,
                AttributeModifier.Operation.ADDITION);
        AttributeModifier temporary = new AttributeModifier(UUID.randomUUID(), "temporary descent", 5,
                AttributeModifier.Operation.ADDITION);
        attribute.addPermanentModifier(permanent);
        attribute.addTransientModifier(temporary);
        hit(helper, victim, victim.damageSources().generic(), 1, 2.896362);
        equal(helper, 2, attribute.getBaseValue(), "Damage must not bake modifiers into base value");

        CompoundTag saved = victim.saveWithoutId(new CompoundTag());
        Cow restored = cow(helper, 0);
        restored.load(saved);
        var restoredAttribute = restored.getAttribute(MaledictAttributes.DESCENT.get());
        equal(helper, 2, restoredAttribute.getBaseValue(), "Vanilla NBT retains base value");
        equal(helper, 5, restoredAttribute.getValue(), "Only permanent modifiers survive vanilla NBT");
        attribute.removeModifier(temporary);
        hit(helper, victim, victim.damageSources().generic(), 1, 2.180408);
        equal(helper, 100, victim.getMaxHealth(), "Descent must not change maximum health");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "descent")
    public static void priorityScalesDamageBeforeExistingDelayedPool(GameTestHelper helper) {
        FakePlayer victim = player(helper);
        var inventory = CuriosApi.getCuriosInventory(victim).orElseThrow(IllegalStateException::new);
        inventory.setCurios(Map.of("rune", new CurioStacksHandler(inventory, "rune", 1,
                true, true, true, ICurio.DropRule.DEFAULT)));
        inventory.setEquippedCurio("rune", 0, new ItemStack(MaledictItems.RUNE_OF_MELANCHOLIA.get()));
        victim.getAttribute(MaledictAttributes.DESCENT.get()).setBaseValue(10);
        LivingDamageEvent damage = new LivingDamageEvent(victim, victim.damageSources().generic(), 2);
        MinecraftForge.EVENT_BUS.post(damage);
        equal(helper, 0, damage.getAmount(), "Existing rune consumes this damage into its pool");
        equal(helper, 5.792723, DelayedVitals.get(victim).pool().pendingDamage(),
                "NORMAL-priority pool must receive already amplified damage");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "descent")
    public static void canceledDamageStaysCanceledAndExtremeDamageStaysFinite(GameTestHelper helper) {
        Cow victim = cow(helper, 10);
        CancelDamage cancel = new CancelDamage(victim);
        MinecraftForge.EVENT_BUS.register(cancel);
        try {
            hit(helper, victim, victim.damageSources().generic(), 10, 0);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancel);
        }
        LivingDamageEvent extreme = new LivingDamageEvent(victim, victim.damageSources().generic(), Float.MAX_VALUE);
        MinecraftForge.EVENT_BUS.post(extreme);
        helper.assertTrue(Float.isFinite(extreme.getAmount()), "Amplification must not overflow to infinity");
        helper.succeed();
    }

    public static final class CancelDamage {
        private final LivingEntity victim;

        CancelDamage(LivingEntity victim) { this.victim = victim; }

        @SubscribeEvent
        public void damage(LivingDamageEvent event) {
            if (event.getEntity() == victim) event.setCanceled(true);
        }
    }

    private static Cow cow(GameTestHelper helper, double descent) {
        Cow cow = EntityType.COW.create(helper.getLevel());
        cow.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        cow.setHealth(100);
        cow.getAttribute(MaledictAttributes.DESCENT.get()).setBaseValue(descent);
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
