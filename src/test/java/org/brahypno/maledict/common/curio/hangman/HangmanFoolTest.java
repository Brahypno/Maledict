package org.brahypno.maledict.common.curio.hangman;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
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
    public static void stageProgressionUnlocksTrapAtZeroAndMagicAtOneThenUnequipRestores(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 0);
        double attack = f.player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double magic = f.player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get());
        f.tickCurios();
        equal(helper, attack * 0.5D, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Attack total");
        equal(helper, magic, f.player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get()), "Stage zero has no magic bonus");
        f.assertTrapCapture(true, false);
        var item = (HangmanFoolItem) f.stack().getItem();
        var modifiers = item.getAttributeModifiers(new SlotContext("delusion", f.player, 0, false, true),
                                                   UUID.randomUUID(), f.stack());
        helper.assertTrue(modifiers.values().stream().allMatch(m -> m.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL),
                          "Attribute modifiers must use MULTIPLY_TOTAL");
        helper.assertTrue(!item.canEquip(new SlotContext("charm", f.player, 0, false, true), f.stack()), "Vision slot only");
        for (int stage = 1; stage <= 4; stage++) {
            f.equip(stage);
            f.tickCurios();
            equal(helper, attack * 0.5D, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Stage " + stage + " keeps attack penalty");
            equal(helper, magic * 1.5D,
                  f.player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get()), "Stage " + stage + " magic total");
            f.assertTrapCapture(true, stage >= 3);
        }
        f.equip(0);
        f.tickCurios();
        equal(helper, magic, f.player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get()), "Downgrade removes magic bonus");
        f.inventory.setEquippedCurio("delusion", 0, ItemStack.EMPTY);
        f.tickCurios();
        equal(helper, attack, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Unequip attack");
        equal(helper, magic, f.player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get()), "Unequip magic");
        f.assertTrapCapture(false, false);
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

        void assertTrapCapture(boolean expectedCapture, boolean expectedDiscard) {
            Arrow arrow = new Arrow(helper.getLevel(), player);
            arrow.setDeltaMovement(0, 0, 3);
            Vec3 impact = player.position().add(0, 0, 4);
            boolean canceled = ForgeEventFactory.onProjectileImpact(arrow,
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
