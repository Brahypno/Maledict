package org.brahypno.maledict.common.item.curio.hangman;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.recipe.VoidFavorRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.brahypno.maledict.common.item.curio.hangman.HangmanTrapEntity;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.item.curio.hangman.HangmanFoolItem;
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
    public static void stageProgressionUnlocksTrapAtZeroAndMagicAtOneThenUnequipRestores(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 0);
        double attack = f.player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double magic = f.player.getAttributeValue(LodestoneAttributes.MAGIC_PROFICIENCY);
        f.tickCurios();
        equal(helper, attack * 0.5D, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Attack total");
        equal(helper, magic, f.player.getAttributeValue(LodestoneAttributes.MAGIC_PROFICIENCY), "Stage zero has no magic bonus");
        f.assertTrapCapture(true, false);
        var item = (HangmanFoolItem) f.stack().getItem();
        var modifiers = item.getAttributeModifiers(new SlotContext("delusion", f.player, 0, false, true),
                net.minecraft.resources.ResourceLocation.parse("maledict:test/hangman"), f.stack());
        helper.assertTrue(modifiers.values().stream().allMatch(m -> m.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                "Both modifiers must use MULTIPLY_TOTAL");
        helper.assertTrue(!item.canEquip(new SlotContext("charm", f.player, 0, false, true), f.stack()), "Vision slot only");
        for (int stage = 1; stage <= 4; stage++) {
            f.equip(stage);
            f.tickCurios();
            equal(helper, attack * 0.5D, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Stage " + stage + " keeps attack penalty");
            equal(helper, magic * 1.5D,
                    f.player.getAttributeValue(LodestoneAttributes.MAGIC_PROFICIENCY), "Stage " + stage + " magic total");
            f.assertTrapCapture(true, stage >= 3);
        }
        f.equip(0);
        f.tickCurios();
        equal(helper, attack * 0.5D, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Upgrade must not double apply attack");
        equal(helper, magic, f.player.getAttributeValue(LodestoneAttributes.MAGIC_PROFICIENCY), "Downgrade removes magic bonus");
        f.inventory.setEquippedCurio("delusion", 0, ItemStack.EMPTY);
        f.tickCurios();
        equal(helper, attack, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Unequip attack");
        equal(helper, magic, f.player.getAttributeValue(LodestoneAttributes.MAGIC_PROFICIENCY), "Unequip magic");
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

    private static VoidFavorRecipe voidRecipe(net.minecraft.server.level.ServerLevel level, ItemStack input) {
        return level.getRecipeManager().getRecipeFor(
                com.sammy.malum.registry.common.recipe.MalumRecipeTypes.VOID_FAVOR.get(),
                new net.minecraft.world.item.crafting.SingleRecipeInput(input), level)
                .map(net.minecraft.world.item.crafting.RecipeHolder::value).orElse(null);
    }

    private static void assertRestrictions(GameTestHelper helper, Fixture f, boolean canceled) {
        var start = new LivingEntityUseItemEvent.Start(f.player, new ItemStack(Items.SHIELD), InteractionHand.MAIN_HAND, 72000);
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

        void assertTrapCapture(boolean expectedCapture, boolean expectedDiscard) {
            Arrow arrow = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), null);
            arrow.setDeltaMovement(0, 0, 3);
            Vec3 impact = player.position().add(0, 0, 4);
            boolean canceled = EventHooks.onProjectileImpact(arrow,
                    new BlockHitResult(impact, Direction.UP, BlockPos.containing(impact), false));
            helper.assertTrue(canceled == expectedCapture, "Only equipped wearers capture block impacts, starting at stage zero");
            var traps = helper.getLevel().getEntitiesOfClass(HangmanTrapEntity.class,
                    new AABB(impact, impact).inflate(0.1), trap -> trap.isAlive() && trap.isOwnedBy(player));
            helper.assertTrue(traps.size() == (expectedCapture ? 1 : 0), "A captured impact creates exactly one owned trap");
            helper.assertTrue(arrow.isRemoved() == expectedDiscard, "Only stage-three and higher shots stop after capture");
            traps.forEach(HangmanTrapEntity::discard);
            arrow.discard();
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
