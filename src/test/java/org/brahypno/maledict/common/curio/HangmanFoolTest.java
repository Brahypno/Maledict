package org.brahypno.maledict.common.curio;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.block.curiosities.spirit_altar.SpiritAltarBlockEntity;
import com.sammy.malum.common.block.curiosities.weeping_well.VoidConduitBlockEntity;
import com.sammy.malum.common.recipe.VoidFavorRecipe;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import com.sammy.malum.registry.common.block.MalumBlocks;
import com.sammy.malum.registry.common.item.MalumItems;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.item.HangmanFoolItem;
import org.brahypno.maledict.registry.MaledictItems;
import team.lodestar.lodestone.registry.common.LodestoneAttributes;
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
        var recipe = (SpiritInfusionRecipe) level.getRecipeManager().byKey(
                net.minecraft.resources.ResourceLocation.parse("maledict:spirit_infusion/hangman_fool_stage_2"))
                .map(net.minecraft.world.item.crafting.RecipeHolder::value).orElseThrow();
        helper.assertTrue(recipe != null, "The stage-two infusion recipe must load");
        ItemStack input = HangmanFoolItem.create(1);
        helper.assertTrue(recipe.input.test(input), "The infusion must accept the ordinary stage-one curio");
        for (int stage = 0; stage <= HangmanFoolItem.MAX_STAGE; stage++) {
            if (stage != 1) {
                helper.assertTrue(!recipe.input.test(HangmanFoolItem.create(stage)),
                        "Stage " + stage + " must not skip progression or be downgraded");
            }
        }
        ItemStack named = input.copy();
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("named stage one"));
        helper.assertTrue(!recipe.input.test(named), "The strict stage-one input must reject additional NBT");
        helper.assertTrue(!recipe.carryOverComponentData && HangmanFoolItem.stage(recipe.result) == 2,
                "Stage-two output NBT must not be replaced by the stage-one input tag");

        BlockPos relativePos = new BlockPos(1, 1, 1);
        helper.setBlock(relativePos, MalumBlocks.SPIRIT_ALTAR.get());
        BlockPos pos = helper.absolutePos(relativePos);
        var altar = (SpiritAltarBlockEntity) level.getBlockEntity(pos);
        altar.inventory.setStackInSlot(0, input.copy());
        altar.extrasInventory.setStackInSlot(0, new ItemStack(MalumItems.MNEMONIC_FRAGMENT.get(), 6));
        altar.extrasInventory.setStackInSlot(1, new ItemStack(MalumItems.AURIC_EMBERS.get(), 6));
        altar.extrasInventory.setStackInSlot(2, new ItemStack(MalumItems.VOID_SALTS.get(), 6));
        altar.extrasInventory.setStackInSlot(3, new ItemStack(MaledictItems.AGE_OF_ENLIGHTENMENT.get()));
        altar.spiritInventory.setStackInSlot(0, new ItemStack(MalumSpiritTypes.ELDRITCH_SPIRIT.getSpiritShard(), 16));
        altar.spiritInventory.setStackInSlot(1, new ItemStack(MalumSpiritTypes.ARCANE_SPIRIT.getSpiritShard(), 8));
        altar.recipe = recipe;
        altar.craft(level);

        var outputs = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2),
                entity -> entity.getItem().is(MaledictItems.HANGMAN_FOOL.get()));
        helper.assertTrue(outputs.size() == 1 && outputs.get(0).getItem().getCount() == 1
                && HangmanFoolItem.stage(outputs.get(0).getItem()) == 2,
                "The actual altar must emit one curio carrying both stage markers");
        helper.assertTrue(altar.inventory.getStackInSlot(0).isEmpty() && altar.extrasInventory.isEmpty()
                        && altar.spiritInventory.nonEmptyItemStacks.stream().allMatch(ItemStack::isEmpty),
                "The altar must consume the stage-one input, materials and spirits: input=" + altar.inventory.getStackInSlot(0)
                        + ", extras=" + altar.extrasInventory.nonEmptyItemStacks + ", spirits=" + altar.spiritInventory.nonEmptyItemStacks);
        outputs.get(0).getItem().set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("my stage-two curio"));
        helper.assertTrue(!recipe.result.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME), "The produced item must own its NBT");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void weepingWellUpgradesOnlyUntaggedStageZero(GameTestHelper helper) {
        var level = helper.getLevel();
        ItemStack input = new ItemStack(MaledictItems.HANGMAN_FOOL.get());
        var recipe = voidRecipe(level, input);
        helper.assertTrue(recipe != null && level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse(
                "maledict:void_favor/hangman_fool_stage_1")).orElseThrow().value() == recipe, "The loaded well recipe must accept an untagged stage zero");
        helper.assertTrue(HangmanFoolItem.stage(recipe.result) == 1 && recipe.result.getCount() == 1,
                "The loaded recipe must keep its stage-one output NBT");
        for (int stage = 1; stage <= HangmanFoolItem.MAX_STAGE; stage++) {
            helper.assertTrue(voidRecipe(level, HangmanFoolItem.create(stage)) == null,
                    "An existing stage " + stage + " must not be converted again or downgraded");
        }
        ItemStack named = input.copy();
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("named stage zero"));
        helper.assertTrue(voidRecipe(level, named) == null,
                "A named stage zero must not match the strict input");
        ItemStack enchanted = input.copy();
        enchanted.enchant(org.brahypno.maledict.TestSupport.enchantment(Enchantments.UNBREAKING), 2);
        helper.assertTrue(voidRecipe(level, enchanted) == null,
                "An enchanted stage zero must not match the strict input");

        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var well = new VoidConduitBlockEntity(pos, MalumBlocks.VOID_CONDUIT.get().defaultBlockState());
        well.setLevel(level);
        well.spitOutItem(input);
        var outputs = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1),
                entity -> entity.getItem().is(MaledictItems.HANGMAN_FOOL.get()));
        helper.assertTrue(outputs.size() == 1 && outputs.get(0).getItem().getCount() == 1
                && HangmanFoolItem.stage(outputs.get(0).getItem()) == 1,
                "The actual well conversion must emit one stage-one curio with its NBT");
        outputs.get(0).getItem().set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("my upgraded curio"));
        helper.assertTrue(!recipe.result.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME),
                "Changing an output's NBT must not mutate the recipe template");
        well.spitOutItem(input.copy());
        var nextOutputs = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1),
                entity -> entity.getItem().is(MaledictItems.HANGMAN_FOOL.get()) && !entity.getItem().has(net.minecraft.core.component.DataComponents.CUSTOM_NAME));
        helper.assertTrue(nextOutputs.size() == 1 && HangmanFoolItem.stage(nextOutputs.get(0).getItem()) == 1,
                "The next conversion must emit an independent, unnamed stage-one curio");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void stageSequencePreservesNbtAndRequiresEveryPreviousMarker(GameTestHelper helper) {
        ItemStack stack = new ItemStack(MaledictItems.HANGMAN_FOOL.get());
        helper.assertTrue(!stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA) && HangmanFoolItem.stage(stack) == 0,
                "A completely untagged item must already be stage zero");
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("preserved"));
        stack.enchant(org.brahypno.maledict.TestSupport.enchantment(Enchantments.UNBREAKING), 2);
        helper.assertTrue(HangmanFoolItem.stage(stack) == 0, "No stage NBT means stage zero");
        for (int stage = 1; stage <= 4; stage++) {
            helper.assertTrue(HangmanFoolItem.advance(stack), "Upgrade must add the next marker");
            stack = ItemStack.parse(org.brahypno.maledict.TestSupport.registries(), stack.save(org.brahypno.maledict.TestSupport.registries())).orElseThrow();
            helper.assertTrue(HangmanFoolItem.stage(stack) == stage, "Stage sequence must be 0 -> 1 -> 2 -> 3 -> 4");
            helper.assertTrue(stack.getHoverName().getString().equals("preserved")
                    && stack.getEnchantmentLevel(org.brahypno.maledict.TestSupport.enchantment(Enchantments.UNBREAKING)) == 2, "Upgrade must preserve other NBT");
        }
        helper.assertTrue(!HangmanFoolItem.advance(stack), "There is no stage five");
        org.brahypno.maledict.common.item.MaledictItemData.update(stack, data -> {
            var stages = data.getCompound(HangmanFoolItem.STAGES_TAG);
            stages.remove("stage_1");
            data.put(HangmanFoolItem.STAGES_TAG, stages);
        });
        helper.assertTrue(HangmanFoolItem.stage(stack) == 0, "Stage two cannot skip stage one");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void realCuriosEquipHalvesAttackAndMultipliesMagicThenUnequipRestores(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 0);
        double attack = f.player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double magic = f.player.getAttributeValue(LodestoneAttributes.MAGIC_PROFICIENCY);
        f.tickCurios();
        equal(helper, attack * 0.5D, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Attack total");
        equal(helper, magic * 1.5D, f.player.getAttributeValue(LodestoneAttributes.MAGIC_PROFICIENCY), "Magic total");
        var item = (HangmanFoolItem) f.stack().getItem();
        var modifiers = item.getAttributeModifiers(new SlotContext("delusion", f.player, 0, false, true),
                net.minecraft.resources.ResourceLocation.parse("maledict:test/hangman"), f.stack());
        helper.assertTrue(modifiers.values().stream().allMatch(m -> m.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                "Both modifiers must use MULTIPLY_TOTAL");
        helper.assertTrue(!item.canEquip(new SlotContext("charm", f.player, 0, false, true), f.stack()), "Vision slot only");
        f.equip(2);
        f.tickCurios();
        equal(helper, attack * 0.5D, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Upgrade must not double apply attack");
        equal(helper, magic * 1.5D, f.player.getAttributeValue(LodestoneAttributes.MAGIC_PROFICIENCY), "Upgrade preserves magic");
        f.inventory.setEquippedCurio("delusion", 0, ItemStack.EMPTY);
        f.tickCurios();
        equal(helper, attack, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Unequip attack");
        equal(helper, magic, f.player.getAttributeValue(LodestoneAttributes.MAGIC_PROFICIENCY), "Unequip magic");
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
        // The 1.21.1 test grid starts close to terrain; clear the complete flight corridor.
        for (BlockPos pos : BlockPos.betweenClosed(f.player.blockPosition().offset(-4, 0, -1),
                f.player.blockPosition().offset(4, 3, 1))) {
            helper.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        LivingEntity behind = f.target();
        behind.setPos(center.add(2, -0.5D, 0));
        Arrow arrow = new Arrow(helper.getLevel(), center.x - 2, center.y, center.z, new ItemStack(Items.ARROW), null);
        arrow.setDeltaMovement(4, 0, 0);
        org.brahypno.maledict.TestSupport.piercing(arrow, (byte) 3);
        helper.getLevel().addFreshEntity(arrow);
        arrow.tick();
        helper.assertTrue(HangmanFoolProjectiles.wasReflected(arrow), "Must reflect through the actual arrow tick hook; pos=" + arrow.position() + ", motion=" + arrow.getDeltaMovement()
                        + ", player=" + f.player.getBoundingBox() + ", stage=" + HangmanFoolItem.equippedStage(f.player)
                        + ", alive=" + f.player.isAlive() + ", pickable=" + f.player.isPickable());
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
        helper.assertTrue(item.getAttributeModifiers(context, org.brahypno.maledict.TestSupport.modifierId(UUID.randomUUID()), stack).isEmpty(),
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

    private static VoidFavorRecipe voidRecipe(net.minecraft.server.level.ServerLevel level, ItemStack input) {
        return level.getRecipeManager().getRecipeFor(
                com.sammy.malum.registry.common.recipe.MalumRecipeTypes.VOID_FAVOR.get(),
                new net.minecraft.world.item.crafting.SingleRecipeInput(input), level)
                .map(net.minecraft.world.item.crafting.RecipeHolder::value).orElse(null);
    }

    private static void assertRestrictions(GameTestHelper helper, Fixture f, boolean canceled) {
        var start = new LivingEntityUseItemEvent.Start(f.player, new ItemStack(Items.SHIELD), 72000);
        var shield = new LivingShieldBlockEvent(f.player, new net.neoforged.neoforge.common.damagesource.DamageContainer(f.player.damageSources().generic(), 4), true);
        var totem = new LivingUseTotemEvent(f.player, f.player.damageSources().generic(),
                new ItemStack(Items.TOTEM_OF_UNDYING), InteractionHand.OFF_HAND);
        NeoForge.EVENT_BUS.post(start);
        NeoForge.EVENT_BUS.post(shield);
        NeoForge.EVENT_BUS.post(totem);
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
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(player));
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
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
