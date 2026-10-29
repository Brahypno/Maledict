package org.brahypno.maledict.common.curio.hangman;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class HangmanFoolRelayTest {
    @GameTest(template = "test_empty", batch = "hangman_relay")
    public static void sameTickShotsCycleABCThenResortAtTheNextRound(GameTestHelper helper) {
        Fixture f = new Fixture(helper);
        var c = f.circle(6);
        var a = f.circle(2);
        var b = f.circle(4);
        f.assertOrigin(f.shot(), a);
        f.assertOrigin(f.shot(), b);
        f.assertOrigin(f.shot(), c);
        f.assertOrigin(f.shot(), a);
        // Moving mid-round must not reorder its remaining B, C turns.
        f.player.setPos(f.player.position().add(7, 0, 0));
        f.assertOrigin(f.shot(), b);
        f.assertOrigin(f.shot(), c);
        f.assertOrigin(f.shot(), c);
        f.assertOrigin(f.shot(), b);
        f.assertOrigin(f.shot(), a);
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_relay")
    public static void relayPreservesArrowPayloadAndFireballSpeedAndAcceleration(GameTestHelper helper) {
        Fixture f = new Fixture(helper);
        var circle = f.circle(2);
        Arrow arrow = new Arrow(helper.getLevel(), f.player);
        arrow.setDeltaMovement(0, 0, 3);
        arrow.setBaseDamage(7);
        arrow.setPierceLevel((byte) 2);
        arrow.setCritArrow(true);
        arrow.setSecondsOnFire(20);
        helper.getLevel().addFreshEntity(arrow);
        f.entities.add(arrow);
        f.assertOrigin(arrow, circle);
        equal(helper, 3, arrow.getDeltaMovement().length(), "Original speed");
        helper.assertTrue(arrow.getOwner() == f.player && arrow.getBaseDamage() == 7
                && arrow.getPierceLevel() == 2 && arrow.isCritArrow() && arrow.isOnFire(), "Original payload and owner");
        Vec3 aim = HangmanFoolRelay.aimTarget(f.player, arrow).getLocation();
        vector(helper, aim.subtract(arrow.position()).normalize(), arrow.getDeltaMovement().normalize(), "Arrow aim");
        var fireball = new SmallFireball(helper.getLevel(), f.player, 0, 0, 1);
        fireball.setDeltaMovement(0, 0, 2);
        helper.getLevel().addFreshEntity(fireball);
        f.entities.add(fireball);
        f.assertOrigin(fireball, circle);
        equal(helper, 2, fireball.getDeltaMovement().length(), "Fireball speed");
        Vec3 power = new Vec3(fireball.xPower, fireball.yPower, fireball.zPower);
        equal(helper, 0.1, power.length(), "Fireball acceleration magnitude");
        vector(helper, fireball.getDeltaMovement().normalize(), power.normalize(), "Fireball acceleration follows aim");
        helper.assertTrue(circle.isAlive(), "Shooting must not consume the trap");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_relay")
    public static void blockedBCyclesACAAndRejoinsAfterTheObstructionIsRemoved(GameTestHelper helper) {
        Fixture f = new Fixture(helper);
        var a = f.circle(2);
        var b = f.circle(4);
        var c = f.circle(6);
        BlockPos target = BlockPos.containing(f.player.getEyePosition().add(0, 0, 8));
        BlockPos obstruction = BlockPos.containing(b.position().add(0, 0, 1));
        helper.getLevel().setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        f.assertOrigin(f.shot(), a);
        // Obstructions are checked again for every shot, including in the middle of a round.
        helper.getLevel().setBlockAndUpdate(obstruction, Blocks.STONE.defaultBlockState());
        f.assertOrigin(f.shot(), c);
        f.assertOrigin(f.shot(), a);
        helper.getLevel().setBlockAndUpdate(obstruction, Blocks.AIR.defaultBlockState());
        f.assertOrigin(f.shot(), c);
        f.assertOrigin(f.shot(), a);
        f.assertOrigin(f.shot(), b);
        helper.getLevel().setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        f.finish();
    }

    private static void equal(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.0001D, message + ": " + expected + " != " + actual);
    }

    private static void vector(GameTestHelper helper, Vec3 expected, Vec3 actual, String message) {
        helper.assertTrue(expected.distanceTo(actual) < 0.0001D, message + ": " + expected + " != " + actual);
    }

    private static final class Fixture {
        final GameTestHelper helper;
        final FakePlayer player;
        final List<Entity> entities = new ArrayList<>();

        Fixture(GameTestHelper helper) {
            this.helper = helper;
            player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "RelayTest"));
            // The saved world contains terrain outside the cleared structures. Place relay scenes in empty sky.
            Vec3 scene = helper.absolutePos(new BlockPos(1, 3, 1)).getCenter();
            player.setPos(scene.x, helper.getLevel().getMaxBuildHeight() - 8, scene.z);
            player.setYRot(0);
            player.setXRot(0);
            var inventory = CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
            inventory.setCurios(Map.of("delusion", new CurioStacksHandler(inventory, "delusion", 1,
                    true, true, true, ICurio.DropRule.DEFAULT)));
            equip(3);
        }

        void equip(int stage) {
            CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new)
                    .setEquippedCurio("delusion", 0, HangmanFoolItem.create(stage));
        }

        HangmanTrapEntity circle(double distance) {
            return circle(distance, Direction.UP);
        }

        HangmanTrapEntity circle(double distance, Direction face) {
            Vec3 impact = player.position().add(distance, 0, 0);
            var payload = new Arrow(helper.getLevel(), player);
            payload.setDeltaMovement(0, 0, 3);
            helper.assertTrue(HangmanTrapEntity.capture(payload,
                    new BlockHitResult(impact, face, BlockPos.containing(impact), false)), "Capture succeeds");
            var circle = helper.getLevel().getEntitiesOfClass(HangmanTrapEntity.class, new AABB(impact, impact).inflate(0.1))
                    .stream().filter(entity -> !entities.contains(entity)).findFirst().orElseThrow();
            entities.add(circle);
            return circle;
        }

        Arrow shot() {
            var arrow = new Arrow(helper.getLevel(), player);
            arrow.setPos(player.getEyePosition());
            arrow.setDeltaMovement(0, 0, 3);
            helper.getLevel().addFreshEntity(arrow);
            entities.add(arrow);
            return arrow;
        }

        void assertOrigin(net.minecraft.world.entity.projectile.Projectile projectile, HangmanTrapEntity circle) {
            Vec3 origin = circle.position().add(0, 0.05D, 0);
            vector(helper, origin, projectile.position(), "Circle origin");
        }

        void finish() {
            entities.forEach(Entity::discard);
            player.discard();
            helper.succeed();
        }
    }
}
