package org.brahypno.maledict.common.curio;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.block.curiosities.spirit_altar.SpiritAltarBlockEntity;
import com.sammy.malum.common.block.curiosities.weeping_well.VoidConduitBlockEntity;
import com.sammy.malum.common.recipe.FavorOfTheVoidRecipe;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.registry.common.block.BlockRegistry;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
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
    public static void infusionConsumesStageOneAndEmitsStageTwoNbt(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipe = SpiritInfusionRecipe.getRecipe(level,
                candidate -> candidate.getId().toString().equals("maledict:spirit_infusion/hangman_fool_stage_2"));
        helper.assertTrue(recipe != null, "The stage-two infusion recipe must load");
        ItemStack input = HangmanFoolItem.create(1);
        helper.assertTrue(recipe.doesInputMatch(input), "The infusion must accept the ordinary stage-one curio");
        for (int stage = 0; stage <= HangmanFoolItem.MAX_STAGE; stage++) {
            if (stage != 1) {
                helper.assertTrue(!recipe.doesInputMatch(HangmanFoolItem.create(stage)),
                        "Stage " + stage + " must not skip progression or be downgraded");
            }
        }
        ItemStack named = input.copy();
        named.setHoverName(Component.literal("named stage one"));
        helper.assertTrue(!recipe.doesInputMatch(named), "The strict stage-one input must reject additional NBT");
        helper.assertTrue(!recipe.useNbtFromInput && HangmanFoolItem.stage(recipe.output) == 2,
                "Stage-two output NBT must not be replaced by the stage-one input tag");

        BlockPos relativePos = new BlockPos(1, 1, 1);
        helper.setBlock(relativePos, BlockRegistry.SPIRIT_ALTAR.get());
        BlockPos pos = helper.absolutePos(relativePos);
        var altar = (SpiritAltarBlockEntity) level.getBlockEntity(pos);
        altar.inventory.setStackInSlot(0, input.copy());
        altar.extrasInventory.setStackInSlot(0, new ItemStack(ItemRegistry.MNEMONIC_FRAGMENT.get(), 6));
        altar.extrasInventory.setStackInSlot(1, new ItemStack(ItemRegistry.AURIC_EMBERS.get(), 6));
        altar.extrasInventory.setStackInSlot(2, new ItemStack(ItemRegistry.VOID_SALTS.get(), 6));
        altar.extrasInventory.setStackInSlot(3, new ItemStack(MaledictItems.AGE_OF_ENLIGHTENMENT.get()));
        altar.spiritInventory.setStackInSlot(0, new ItemStack(SpiritTypeRegistry.ELDRITCH_SPIRIT.spiritShard.get(), 16));
        altar.spiritInventory.setStackInSlot(1, new ItemStack(SpiritTypeRegistry.ARCANE_SPIRIT.spiritShard.get(), 8));
        altar.recipe = recipe;
        altar.craft();

        var outputs = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2),
                entity -> entity.getItem().is(MaledictItems.HANGMAN_FOOL.get()));
        helper.assertTrue(outputs.size() == 1 && outputs.get(0).getItem().getCount() == 1
                && HangmanFoolItem.stage(outputs.get(0).getItem()) == 2,
                "The actual altar must emit one curio carrying both stage markers");
        helper.assertTrue(altar.inventory.isEmpty() && altar.extrasInventory.isEmpty() && altar.spiritInventory.isEmpty(),
                "The altar must consume the stage-one input, materials and spirits");
        outputs.get(0).getItem().setHoverName(Component.literal("my stage-two curio"));
        helper.assertTrue(!recipe.output.hasCustomHoverName(), "The produced item must own its NBT");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void weepingWellUpgradesOnlyUntaggedStageZero(GameTestHelper helper) {
        var level = helper.getLevel();
        ItemStack input = new ItemStack(MaledictItems.HANGMAN_FOOL.get());
        var recipe = FavorOfTheVoidRecipe.getRecipe(level, input);
        helper.assertTrue(recipe != null && recipe.getId().toString().equals(
                "maledict:void_favor/hangman_fool_stage_1"), "The loaded well recipe must accept an untagged stage zero");
        helper.assertTrue(HangmanFoolItem.stage(recipe.output) == 1 && recipe.output.getCount() == 1,
                "The loaded recipe must keep its stage-one output NBT");
        for (int stage = 1; stage <= HangmanFoolItem.MAX_STAGE; stage++) {
            helper.assertTrue(FavorOfTheVoidRecipe.getRecipe(level, HangmanFoolItem.create(stage)) == null,
                    "An existing stage " + stage + " must not be converted again or downgraded");
        }
        ItemStack named = input.copy();
        named.setHoverName(Component.literal("named stage zero"));
        helper.assertTrue(FavorOfTheVoidRecipe.getRecipe(level, named) == null,
                "A named stage zero must not match the strict input");
        ItemStack enchanted = input.copy();
        enchanted.enchant(Enchantments.UNBREAKING, 2);
        helper.assertTrue(FavorOfTheVoidRecipe.getRecipe(level, enchanted) == null,
                "An enchanted stage zero must not match the strict input");

        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var well = new VoidConduitBlockEntity(pos, BlockRegistry.VOID_CONDUIT.get().defaultBlockState());
        well.setLevel(level);
        well.spitOutItem(input);
        var outputs = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1),
                entity -> entity.getItem().is(MaledictItems.HANGMAN_FOOL.get()));
        helper.assertTrue(outputs.size() == 1 && outputs.get(0).getItem().getCount() == 1
                && HangmanFoolItem.stage(outputs.get(0).getItem()) == 1,
                "The actual well conversion must emit one stage-one curio with its NBT");
        outputs.get(0).getItem().setHoverName(Component.literal("my upgraded curio"));
        helper.assertTrue(!recipe.output.hasCustomHoverName(),
                "Changing an output's NBT must not mutate the recipe template");
        well.spitOutItem(input.copy());
        var nextOutputs = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1),
                entity -> entity.getItem().is(MaledictItems.HANGMAN_FOOL.get()) && !entity.getItem().hasCustomHoverName());
        helper.assertTrue(nextOutputs.size() == 1 && HangmanFoolItem.stage(nextOutputs.get(0).getItem()) == 1,
                "The next conversion must emit an independent, unnamed stage-one curio");
        helper.succeed();
    }

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
