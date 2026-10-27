package org.brahypno.maledict.common.curio.priestess;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.curio.AbstractVisionCurioItem;
import org.brahypno.maledict.common.curio.hangman.HangmanFoolItem;
import org.brahypno.maledict.common.vitals.DelayedVitals;
import org.brahypno.maledict.common.vitals.DelayedVitalsPool;
import org.brahypno.maledict.registry.MaledictAttributes;
import org.brahypno.maledict.registry.MaledictItems;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.Map;
import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class HighPriestessTest {
    @GameTest(template = "test_empty", batch = "high_priestess")
    public static void sharedVisionStagesRetainEarlierMarkersAndOtherNbt(GameTestHelper helper) {
        ItemStack stack = EidolonOfTheHighPriestessItem.create(0);
        helper.assertTrue(stack.getItem() instanceof AbstractVisionCurioItem
                        && MaledictItems.HANGMAN_FOOL.get() instanceof AbstractVisionCurioItem,
                "Both vision curios must use the shared base");
        helper.assertTrue(stack.getTag() == null, "Stage zero needs no NBT");
        stack.setHoverName(Component.literal("preserved"));
        stack.enchant(Enchantments.UNBREAKING, 2);
        for (int stage = 1; stage <= EidolonOfTheHighPriestessItem.MAX_STAGE; stage++) {
            helper.assertTrue(EidolonOfTheHighPriestessItem.advance(stack), "Advance one stage");
            stack = ItemStack.of(stack.save(new CompoundTag()));
            helper.assertTrue(EidolonOfTheHighPriestessItem.stage(stack) == stage,
                    "Every previous stage must remain active after saving");
            helper.assertTrue(stack.getHoverName().getString().equals("preserved")
                            && stack.getEnchantmentLevel(Enchantments.UNBREAKING) == 2,
                    "Upgrading preserves names and enchantments");
        }
        helper.assertTrue(!EidolonOfTheHighPriestessItem.advance(stack), "There is no stage five");
        helper.assertTrue(!HangmanFoolItem.advance(stack), "Each curio owns its stage markers");
        stack.getOrCreateTag().getCompound(EidolonOfTheHighPriestessItem.STAGES_TAG).remove("stage_1");
        helper.assertTrue(EidolonOfTheHighPriestessItem.stage(stack) == 0, "Later stages cannot skip earlier markers");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "high_priestess")
    public static void allStagesKeepHalfAnOmenAndOnlyVisionSlotsActivateAbilities(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper, 0)) {
            var item = (EidolonOfTheHighPriestessItem) MaledictItems.EIDOLON_OF_THE_HIGH_PRIESTESS.get();
            for (int stage = 0; stage <= EidolonOfTheHighPriestessItem.MAX_STAGE; stage++) {
                f.inventory.setEquippedCurio("delusion", 0, EidolonOfTheHighPriestessItem.create(stage));
                f.tickCurios();
                equal(helper, 0.5, f.player.getAttributeValue(MaledictAttributes.TRANSCEND_OMEN.get()),
                        "Each stage keeps the same base Omen without stacking upgrades");
                helper.assertTrue(EidolonOfTheHighPriestessItem.hasWoundYetToCome(f.player) == (stage >= 2),
                        "The Wound Yet to Come starts at stage two and remains active later");
            }
            f.inventory.setEquippedCurio("delusion", 0, ItemStack.EMPTY);
            f.tickCurios();
            equal(helper, 0, f.player.getAttributeValue(MaledictAttributes.TRANSCEND_OMEN.get()), "Unequip removes Omen");
            ItemStack stack = EidolonOfTheHighPriestessItem.create(2);
            var context = new SlotContext("charm", f.player, 0, false, true);
            f.inventory.getCurios().get("charm").getStacks().setStackInSlot(0, stack);
            helper.assertTrue(!item.canEquip(context, stack)
                            && item.getAttributeModifiers(context, UUID.randomUUID(), stack).isEmpty()
                            && !EidolonOfTheHighPriestessItem.hasWoundYetToCome(f.player),
                    "Forced equipment outside vision must grant neither attributes nor passives");
        }
        helper.succeed();
    }

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

    @GameTest(template = "test_empty", batch = "high_priestess")
    public static void projectileOwnersRecoverEvenWhenTheVictimAlsoDelaysDamage(GameTestHelper helper) {
        try (Fixture attacker = new Fixture(helper, 2); Fixture victim = new Fixture(helper, 2)) {
            Arrow arrow = new Arrow(helper.getLevel(), attacker.player);
            victim.player.receive(victim.player.damageSources().arrow(arrow, attacker.player), 5);
            equal(helper, 5, attacker.pool().pendingHeal(), "Projectile damage heals its owner");
            equal(helper, 5, victim.pool().pendingDamage(), "The victim queues the same damage only once");
            equal(helper, 20, victim.player.getHealth(), "Victim damage is still gradual");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "high_priestess")
    public static void canceledAbsorbedAndSelfDamageDoNotGrantRecovery(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper, 2)) {
            LivingEntity target = target(helper);
            CancelDamage cancel = new CancelDamage(target);
            MinecraftForge.EVENT_BUS.register(cancel);
            try {
                target.hurt(target.damageSources().mobAttack(f.player), 5);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(cancel);
            }
            helper.assertTrue(f.pool().isEmpty(), "Canceled damage grants no healing");
            target.invulnerableTime = 0;
            target.setAbsorptionAmount(100);
            target.hurt(target.damageSources().mobAttack(f.player), 5);
            helper.assertTrue(f.pool().isEmpty(), "Fully absorbed damage grants no healing");

            f.player.receive(f.player.damageSources().mobAttack(f.player), 5);
            equal(helper, 5, f.pool().pendingDamage(), "Self damage remains damage and cannot cancel itself");
            equal(helper, 0, f.pool().pendingHeal(), "Self damage grants no recovery");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "high_priestess")
    public static void earlierStagesDoNotDelayOrRecoverAndBothItemsShareOnePool(GameTestHelper helper) {
        for (int stage = 0; stage < 2; stage++) {
            try (Fixture f = new Fixture(helper, stage)) {
                var event = new LivingDamageEvent(f.player, f.player.damageSources().generic(), 4);
                MinecraftForge.EVENT_BUS.post(event);
                equal(helper, 4, event.getAmount(), "Earlier stages do not delay incoming damage");
                LivingEntity target = target(helper);
                target.hurt(target.damageSources().mobAttack(f.player), 3);
                helper.assertTrue(f.pool().isEmpty(), "Earlier stages do not recover health on damage");
            }
        }
        try (Fixture f = new Fixture(helper, 2)) {
            f.inventory.setEquippedCurio("rune", 0, new ItemStack(MaledictItems.RUNE_OF_MELANCHOLIA.get()));
            var event = new LivingDamageEvent(f.player, f.player.damageSources().generic(), 10);
            MinecraftForge.EVENT_BUS.post(event);
            equal(helper, 0, event.getAmount(), "Both items consume the damage together");
            equal(helper, 10, f.pool().pendingDamage(), "Equipping both does not double queue damage");
            f.player.heal(3);
            equal(helper, 7, f.pool().pendingDamage(), "Ordinary healing uses the same counteracting pool");
            f.inventory.setEquippedCurio("delusion", 0, ItemStack.EMPTY);
            f.inventory.setEquippedCurio("rune", 0, ItemStack.EMPTY);
            equal(helper, 7, f.pool().pendingDamage(), "Unequipping does not erase existing debt");
        }
        helper.succeed();
    }

    public static final class CancelDamage {
        private final LivingEntity target;

        CancelDamage(LivingEntity target) { this.target = target; }

        @SubscribeEvent(priority = EventPriority.HIGH)
        public void damage(LivingDamageEvent event) {
            if (event.getEntity() == target) event.setCanceled(true);
        }
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
