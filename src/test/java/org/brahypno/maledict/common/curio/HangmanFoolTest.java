package org.brahypno.maledict.common.curio;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.item.HangmanFoolItem;
import org.brahypno.maledict.registry.MaledictItems;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.Map;
import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class HangmanFoolTest {
    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void stageSequencePreservesNbtAndRequiresEveryPreviousMarker(GameTestHelper helper) {
        ItemStack stack = new ItemStack(MaledictItems.HANGMAN_FOOL.get());
        helper.assertTrue(stack.getTag() == null && HangmanFoolItem.stage(stack) == 0,
                "A completely untagged item must already be stage zero");
        stack.setHoverName(Component.literal("preserved"));
        stack.enchant(Enchantments.UNBREAKING, 2);
        helper.assertTrue(HangmanFoolItem.stage(stack) == 0, "No stage NBT means stage zero");
        for (int stage = 1; stage <= 4; stage++) {
            helper.assertTrue(HangmanFoolItem.advance(stack), "Upgrade must add the next marker");
            stack = ItemStack.of(stack.save(new CompoundTag()));
            helper.assertTrue(HangmanFoolItem.stage(stack) == stage, "Stage sequence must be 0 -> 1 -> 2 -> 3 -> 4");
            helper.assertTrue(stack.getHoverName().getString().equals("preserved")
                    && stack.getEnchantmentLevel(Enchantments.UNBREAKING) == 2, "Upgrade must preserve other NBT");
        }
        helper.assertTrue(!HangmanFoolItem.advance(stack), "There is no stage five");
        stack.getOrCreateTag().getCompound(HangmanFoolItem.STAGES_TAG).remove("stage_1");
        helper.assertTrue(HangmanFoolItem.stage(stack) == 0, "Stage two cannot skip stage one");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void realCuriosEquipHalvesAttackAndMultipliesMagicThenUnequipRestores(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 0);
        double attack = f.player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double magic = f.player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get());
        f.tickCurios();
        equal(helper, attack * 0.5D, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Attack total");
        equal(helper, magic * 1.5D, f.player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get()), "Magic total");
        var item = (HangmanFoolItem) f.stack().getItem();
        var modifiers = item.getAttributeModifiers(new SlotContext("delusion", f.player, 0, false, true),
                UUID.randomUUID(), f.stack());
        helper.assertTrue(modifiers.values().stream().allMatch(m -> m.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL),
                "Both modifiers must use MULTIPLY_TOTAL");
        helper.assertTrue(!item.canEquip(new SlotContext("charm", f.player, 0, false, true), f.stack()), "Vision slot only");
        f.equip(2);
        f.tickCurios();
        equal(helper, attack * 0.5D, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Upgrade must not double apply attack");
        equal(helper, magic * 1.5D, f.player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get()), "Upgrade preserves magic");
        f.inventory.setEquippedCurio("delusion", 0, ItemStack.EMPTY);
        f.tickCurios();
        equal(helper, attack, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Unequip attack");
        equal(helper, magic, f.player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get()), "Unequip magic");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void restrictionsStartAtTwoAndStopAnAlreadyRaisedShield(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 1);
        assertRestrictions(helper, f, false);
        f.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        f.player.startUsingItem(InteractionHand.OFF_HAND);
        helper.assertTrue(f.player.isUsingItem(), "Stage one may raise a shield");
        f.equip(2);
        f.tickCurios();
        helper.assertTrue(!f.player.isUsingItem(), "Equipping stage two must stop an existing block");
        assertRestrictions(helper, f, true);
        f.player.startUsingItem(InteractionHand.OFF_HAND);
        helper.assertTrue(!f.player.isUsingItem(), "Actual shield start must be rejected");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void piercingArrowActuallyReversesBeforeOldRayHitsAnythingElse(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 2);
        f.player.setHealth(4);
        helper.getLevel().addNewPlayer(f.player);
        Vec3 center = f.player.getBoundingBox().getCenter();
        LivingEntity behind = f.target();
        behind.setPos(center.add(2, -0.5D, 0));
        Arrow arrow = new Arrow(helper.getLevel(), center.x - 2, center.y, center.z);
        arrow.setDeltaMovement(4, 0, 0);
        arrow.setPierceLevel((byte) 3);
        helper.getLevel().addFreshEntity(arrow);
        arrow.tick();
        helper.assertTrue(HangmanFoolProjectiles.wasReflected(arrow), "Must reflect through the actual arrow tick hook");
        equal(helper, -4 * 0.99F, arrow.getDeltaMovement().x, "Reversed arrow retains normal vanilla drag");
        helper.assertTrue(arrow.getPierceLevel() == 3 && arrow.getX() < f.player.getX() - 2,
                "Cancellation ends the old ray and lets the reversed arrow finish its normal movement");
        helper.assertTrue(behind.getHealth() == behind.getMaxHealth(), "Old forward ray must not damage the entity behind wearer");
        helper.assertTrue(arrow.getYRot() == arrow.yRotO && Math.abs(arrow.getYRot() + 90) < 0.01,
                "Rendered arrow yaw must immediately follow reversed flight");
        arrow.discard();
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void healthChanceHasConfirmedBoundaries(GameTestHelper helper) {
        equal(helper, 0.25D, HangmanFoolProjectiles.reflectionChance(20, 20, 0.25), "Full health");
        equal(helper, 0.75D, HangmanFoolProjectiles.reflectionChance(10, 20, 0.25), "Half health");
        equal(helper, 1, HangmanFoolProjectiles.reflectionChance(5, 20, 0.25), "Quarter health");
        equal(helper, 1, HangmanFoolProjectiles.reflectionChance(1, 20, 0.25), "Clamped chance");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void forcedNonVisionEquipmentHasNoAttributesPassivesOrRestrictions(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 0);
        var charm = new CurioStacksHandler(f.inventory, "charm", 1, true, true, true, ICurio.DropRule.DEFAULT);
        var vision = new CurioStacksHandler(f.inventory, "delusion", 1, true, true, true, ICurio.DropRule.DEFAULT);
        f.inventory.setCurios(Map.of("charm", charm, "delusion", vision));
        ItemStack stack = HangmanFoolItem.create(2);
        // Bypass slot validation to exercise effects independently of canEquip().
        charm.getStacks().setStackInSlot(0, stack);
        var context = new SlotContext("charm", f.player, 0, false, true);
        var item = (HangmanFoolItem) stack.getItem();
        helper.assertTrue(!item.canEquip(context, stack), "Only vision slots permit equip");
        helper.assertTrue(item.getAttributeModifiers(context, UUID.randomUUID(), stack).isEmpty(),
                "Forced equipment in another slot must grant neither attack nor magic modifiers");
        helper.assertTrue(HangmanFoolItem.equippedStage(f.player) == -1,
                "Combat passives must ignore this item outside vision slots");
        assertRestrictions(helper, f, false);
        f.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        f.player.startUsingItem(InteractionHand.OFF_HAND);
        item.curioTick(context, stack);
        helper.assertTrue(f.player.isUsingItem(), "Non-vision curio ticks cannot stop shield use");
        f.finish();
    }

    private static void assertRestrictions(GameTestHelper helper, Fixture f, boolean canceled) {
        var start = new LivingEntityUseItemEvent.Start(f.player, new ItemStack(Items.SHIELD), 72000);
        var shield = new ShieldBlockEvent(f.player, f.player.damageSources().generic(), 4);
        var totem = new LivingUseTotemEvent(f.player, f.player.damageSources().generic(),
                new ItemStack(Items.TOTEM_OF_UNDYING), InteractionHand.OFF_HAND);
        MinecraftForge.EVENT_BUS.post(start);
        MinecraftForge.EVENT_BUS.post(shield);
        MinecraftForge.EVENT_BUS.post(totem);
        helper.assertTrue(start.isCanceled() == canceled && shield.isCanceled() == canceled && totem.isCanceled() == canceled,
                "Shield start, block, and totem restrictions must begin at stage two");
    }

    private static void equal(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.001D, message + ": expected " + expected + ", got " + actual);
    }

    private static final class Fixture {
        final GameTestHelper helper;
        final FakePlayer player;
        final ICuriosItemHandler inventory;

        Fixture(GameTestHelper helper, int stage) {
            this.helper = helper;
            player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "HangmanTest"));
            player.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
            inventory = CuriosApi.getCuriosInventory(player).orElseThrow(() -> new IllegalStateException("Missing Curios capability"));
            inventory.setCurios(Map.of("delusion", new CurioStacksHandler(inventory, "delusion", 1,
                    true, true, true, ICurio.DropRule.DEFAULT)));
            equip(stage);
        }

        void equip(int stage) {
            inventory.setEquippedCurio("delusion", 0, HangmanFoolItem.create(stage));
        }

        ItemStack stack() {
            return inventory.getCurios().get("delusion").getStacks().getStackInSlot(0);
        }

        void tickCurios() {
            MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent(player));
        }

        LivingEntity target() {
            var pig = helper.spawn(EntityType.PIG, new BlockPos(3, 2, 3));
            pig.setNoAi(true);
            pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            pig.setHealth(200);
            return pig;
        }

        void finish() {
            player.discard();
            helper.succeed();
        }
    }
}
