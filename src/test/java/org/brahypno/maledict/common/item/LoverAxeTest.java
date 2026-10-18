package org.brahypno.maledict.common.item;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffects;
import org.brahypno.maledict.registry.MaledictAttributes;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class LoverAxeTest {
    @GameTest(template = "test_empty", batch = "lover_axe")
    public static void acceptsTheSameEnchantmentsAsAnOrdinaryAxe(GameTestHelper helper) {
        ItemStack axe = new ItemStack(MaledictItems.LOVER_AXE.get());
        ItemStack ordinary = new ItemStack(Items.NETHERITE_AXE);
        helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .holders().forEach(holder -> {
                    var enchantment = holder.value();
                    if (enchantment.canEnchant(ordinary)) helper.assertTrue(enchantment.canEnchant(axe),
                            "Missing axe/anvil enchantment: " + holder.key().location());
                    if (ordinary.supportsEnchantment(holder)) helper.assertTrue(axe.supportsEnchantment(holder),
                            "Missing axe/table enchantment: " + holder.key().location());
                });
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "lover_axe")
    public static void leftClickAppliesSharpnessAndDurabilityAndResetsCooldown(GameTestHelper helper) {
        ChargedPlayer player = player(helper);
        ItemStack weapon = player.getMainHandItem();
        weapon.enchant(org.brahypno.maledict.TestSupport.enchantment(Enchantments.SHARPNESS), 5);
        Cow victim = cow(helper);
        player.cooldownReset = false;
        player.attack(victim);
        equal(helper, 87, victim.getHealth(), "Sharpness V adds three damage to the axe's ten");
        equal(helper, 98, victim.getMaxHealth(), "Enchanted hit wounds once");
        equal(helper, 2, descent(player), "Enchanted hit rewards once");
        equal(helper, 2, weapon.getDamageValue(), "Axe melee consumes normal durability");
        helper.assertTrue(player.cooldownReset, "Left-click replacement resets attack cooldown");
        ChargedPlayer falling = player(helper);
        falling.getMainHandItem().enchant(org.brahypno.maledict.TestSupport.enchantment(Enchantments.SHARPNESS), 5);
        falling.fallDistance = 1;
        falling.setOnGround(false);
        Cow criticalTarget = cow(helper);
        falling.attack(criticalTarget);
        equal(helper, 82, criticalTarget.getHealth(), "Falling crit multiplies base damage before Sharpness");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "lover_axe")
    public static void baneOfArthropodsRetainsItsDamageAndPostHitEffect(GameTestHelper helper) {
        ChargedPlayer player = player(helper);
        player.getMainHandItem().enchant(org.brahypno.maledict.TestSupport.enchantment(Enchantments.BANE_OF_ARTHROPODS), 2);
        Spider target = new Spider(EntityType.SPIDER, helper.getLevel()) {
            @Override
            public boolean fireImmune() { return true; }
        };
        target.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.setHealth(100);
        player.attack(target);
        equal(helper, 85, target.getHealth(), "Bane II adds five against arthropods");
        helper.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "Axe attack invokes the enchantment's post-hit callback");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "lover_axe")
    public static void canceledPlayerAttackNeverEntersTheLeftClickAttack(GameTestHelper helper) {
        ChargedPlayer player = player(helper);
        Cow target = cow(helper);
        CancelPlayerAttack cancel = new CancelPlayerAttack(target);
        NeoForge.EVENT_BUS.register(cancel);
        try {
            player.attack(target);
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        equal(helper, 100, target.getHealth(), "Forge player attack cancellation prevents the weapon attack");
        equal(helper, 100, target.getMaxHealth(), "Canceled left-click does not wound");
        equal(helper, 0, descent(player), "Canceled left-click grants no Descent");
        equal(helper, 0, player.getMainHandItem().getDamageValue(), "Canceled left-click consumes no durability");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "lover_axe")
    public static void confirmedSequenceAndPreHitDescent(GameTestHelper helper) {
        ChargedPlayer player = player(helper);
        Cow victim = cow(helper);
        player.attack(victim);
        equal(helper, 98, victim.getMaxHealth(), "First wound");
        equal(helper, 90, victim.getHealth(), "First hit uses Descent zero");
        equal(helper, 2, descent(player), "First full hit rewards once");
        victim.invulnerableTime = 0;
        player.attack(victim);
        equal(helper, 96.04, victim.getMaxHealth(), "Second wound multiplies remaining maximum");
        equal(helper, 90 - 10 * (1 + 3 * (1 - Math.exp(-0.2))), victim.getHealth(),
                "Second hit uses previous Descent two");
        equal(helper, 4, descent(player), "Second full hit rewards once");
        victim.heal(100);
        equal(helper, 96.04, victim.getHealth(), "Healing cannot restore lost maximum");
        Cow restored = cow(helper);
        restored.load(victim.saveWithoutId(new CompoundTag()));
        equal(helper, 96.04, restored.getMaxHealth(), "Wound survives save/load");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "lover_axe")
    public static void mediumFallbackAndWeakHitsDoNotDuplicateRewards(GameTestHelper helper) {
        ChargedPlayer player = player(helper);
        Cow victim = cow(helper);
        CancelAttack cancel = new CancelAttack(victim);
        NeoForge.EVENT_BUS.register(cancel);
        try {
            player.attack(victim);
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        helper.assertTrue(victim.getHealth() < 100, "Medium probe bypasses canceled normal damage");
        equal(helper, 98, victim.getMaxHealth(), "Probe fallback wounds only once");
        equal(helper, 2, descent(player), "Probe fallback rewards only once");
        player.charge = 0.5F;
        victim.invulnerableTime = 0;
        player.attack(victim);
        equal(helper, 96.04, victim.getMaxHealth(), "Weak effective hit still wounds");
        equal(helper, 2, descent(player), "Weak hit grants no Descent reward");
        player.charge = 1.0F;
        victim.setHealth(0);
        ItemStack beforeDeadHit = player.getMainHandItem().copy();
        player.attack(victim);
        equal(helper, 2, descent(player), "Dead target grants no reward");
        helper.assertTrue(ItemStack.matches(beforeDeadHit, player.getMainHandItem()),
                "Dead target cannot switch color or consume durability");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "lover_axe")
    public static void eitherHandGrowsOnceThenBaseDecaysWithoutBakingModifiers(GameTestHelper helper) {
        Cow holder = cow(helper);
        var attribute = holder.getAttribute(MaledictAttributes.DESCENT);
        attribute.addTransientModifier(new AttributeModifier(org.brahypno.maledict.TestSupport.modifierId(UUID.randomUUID()), 5, AttributeModifier.Operation.ADD_VALUE));
        holder.tickCount = 20;
        holder.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(MaledictItems.LOVER_AXE.get()));
        LoverAxeEvents.tick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(holder));
        equal(helper, 0.1, attribute.getBaseValue(), "Offhand grows base");
        holder.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MaledictItems.LOVER_AXE.get()));
        holder.tickCount = 40;
        LoverAxeEvents.tick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(holder));
        equal(helper, 0.2, attribute.getBaseValue(), "Dual wield does not double growth");
        holder.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        holder.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        for (int i = 3; i <= 6; i++) {
            holder.tickCount = i * 20;
            LoverAxeEvents.tick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(holder));
        }
        equal(helper, 0, attribute.getBaseValue(), "Decay stops at zero");
        equal(helper, 5, attribute.getValue(), "External modifier remains independent");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "lover_axe")
    public static void deathCloneClearsWoundButEndReturnKeepsIt(GameTestHelper helper) {
        ChargedPlayer attacker = player(helper);
        ChargedPlayer victim = player(helper);
        attacker.attack(victim);
        equal(helper, 19.6, victim.getMaxHealth(), "Player wound");
        ChargedPlayer endReturn = player(helper);
        LoverAxeEvents.clone(new PlayerEvent.Clone(endReturn, victim, false));
        equal(helper, 19.6, endReturn.getMaxHealth(), "End return preserves wound");
        LoverAxeEvents.clone(new PlayerEvent.Clone(endReturn, victim, true));
        equal(helper, 20, endReturn.getMaxHealth(), "Death respawn clears wound");
        helper.succeed();
    }

    public static final class CancelAttack {
        private final LivingEntity victim;
        CancelAttack(LivingEntity victim) { this.victim = victim; }
        @SubscribeEvent
        public void cancel(LivingIncomingDamageEvent event) {
            if (event.getEntity() == victim) event.setCanceled(true);
        }
    }

    public static final class CancelPlayerAttack {
        private final LivingEntity target;
        CancelPlayerAttack(LivingEntity target) { this.target = target; }
        @SubscribeEvent
        public void cancel(AttackEntityEvent event) {
            if (event.getTarget() == target) event.setCanceled(true);
        }
    }

    private static Cow cow(GameTestHelper helper) {
        // ChangeLib's guard scan invokes lavaHurt; isolate melee amplification from that incidental hit.
        Cow cow = new Cow(EntityType.COW, helper.getLevel()) {
            @Override
            public boolean fireImmune() { return true; }
        };
        cow.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        cow.setHealth(100);
        return cow;
    }

    private static ChargedPlayer player(GameTestHelper helper) {
        ChargedPlayer player = new ChargedPlayer(helper);
        ItemStack axe = new ItemStack(MaledictItems.LOVER_AXE.get());
        player.setItemSlot(EquipmentSlot.MAINHAND, axe);
        axe.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) ->
                player.getAttribute(attribute).addTransientModifier(modifier));
        return player;
    }

    private static double descent(LivingEntity entity) {
        return entity.getAttribute(MaledictAttributes.DESCENT).getBaseValue();
    }

    private static void equal(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.0001,
                message + ": expected " + expected + ", actual " + actual);
    }

    private static final class ChargedPlayer extends FakePlayer {
        float charge = 1.0F;
        boolean cooldownReset;
        ChargedPlayer(GameTestHelper helper) {
            super(helper.getLevel(), new GameProfile(UUID.randomUUID(), "LoverAxeTest"));
            setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        }
        @Override
        public float getAttackStrengthScale(float partialTick) { return charge; }
        @Override
        public void resetAttackStrengthTicker() {
            super.resetAttackStrengthTicker();
            cooldownReset = true;
        }
    }
}
