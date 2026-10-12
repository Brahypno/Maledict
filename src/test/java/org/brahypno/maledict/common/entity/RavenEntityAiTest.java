package org.brahypno.maledict.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictEntities;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class RavenEntityAiTest {
    @GameTest(template = "raven_ai_empty", batch = "raven_ai", timeoutTicks = 300)
    public static void lovingAdultsFiveBlocksApartAcrossANetherStepBreed(GameTestHelper helper) {
        prepareFloor(helper);
        // The user's Nether save has a one-block rise between x=21 and x=22.
        for (int x = 8; x < 17; x++) {
            for (int z = 0; z < 17; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.NETHERRACK);
            }
        }
        RavenEntity first = spawn(helper, 5, 1, 8);
        RavenEntity second = spawn(helper, 10, 2, 8);
        first.setInLove(null);
        second.setInLove(null);
        helper.succeedWhen(() -> {
            AABB bounds = new AABB(helper.absolutePos(BlockPos.ZERO),
                    helper.absolutePos(new BlockPos(17, 10, 17)));
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(RavenEntity.class, bounds,
                    RavenEntity::isBaby).size() == 1, "Loving parents must meet across the saved Nether terrain's step: "
                    + describe(first) + "; " + describe(second));
        });
    }

    @GameTest(template = "raven_ai_empty", batch = "raven_ai", timeoutTicks = 300)
    public static void lovingAdultsOnDifferentHeightsFlyToMeetAndBreed(GameTestHelper helper) {
        prepareFloor(helper);
        for (int x = 6; x <= 8; x++) {
            for (int z = 7; z <= 9; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        RavenEntity first = spawn(helper, 7, 5, 8);
        RavenEntity second = spawn(helper, 10, 1, 8);
        first.setInLove(null);
        second.setInLove(null);
        helper.succeedWhen(() -> {
            AABB bounds = new AABB(helper.absolutePos(BlockPos.ZERO),
                    helper.absolutePos(new BlockPos(17, 10, 17)));
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(RavenEntity.class, bounds,
                    RavenEntity::isBaby).size() == 1, "Nearby parents separated by a ledge must fly to meet and breed: "
                    + describe(first) + "; " + describe(second));
        });
    }

    @GameTest(template = "raven_ai_empty", batch = "raven_ai", timeoutTicks = 300)
    public static void lovingAdultsSixBlocksApartApproachAndBreed(GameTestHelper helper) {
        prepareFloor(helper);
        RavenEntity first = spawn(helper, 5, 1, 8);
        RavenEntity second = spawn(helper, 11, 1, 8);
        helper.runAtTickTime(5, () -> {
            first.setInLove(null);
            second.setInLove(null);
        });
        helper.succeedWhen(() -> {
            AABB bounds = new AABB(helper.absolutePos(BlockPos.ZERO),
                    helper.absolutePos(new BlockPos(17, 10, 17)));
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(RavenEntity.class, bounds,
                    RavenEntity::isBaby).size() == 1, "Loving adults must approach and produce one chick: "
                    + describe(first) + "; " + describe(second));
            helper.assertTrue(first.getAge() > 0 && second.getAge() > 0,
                    "Successful breeding must put both parents on cooldown");
        });
    }

    @GameTest(template = "raven_ai_empty", batch = "raven_ai", timeoutTicks = 100)
    public static void courtshipContinuesWhileAirborneAndOutOfBreedingRange(GameTestHelper helper) {
        prepareFloor(helper);
        RavenEntity first = spawn(helper, 5, 4, 8);
        RavenEntity second = spawn(helper, 11, 4, 8);
        first.setInLove(null);
        second.setInLove(null);
        AABB bounds = new AABB(helper.absolutePos(BlockPos.ZERO),
                helper.absolutePos(new BlockPos(17, 10, 17)));
        helper.onEachTick(() -> {
            // Keep both airborne and six blocks apart for the first 40 ticks, then
            // bring them within breeding range without giving them time to land.
            if (first.getAge() == 0 && second.getAge() == 0) {
                holdAirborne(helper, first, 5);
                holdAirborne(helper, second, helper.getTick() < 40 ? 11 : 7);
            }
        });
        helper.runAtTickTime(55, () -> helper.assertTrue(
                helper.getLevel().getEntitiesOfClass(RavenEntity.class, bounds, RavenEntity::isBaby).isEmpty(),
                "Proximity must not skip the courtship wait"));
        helper.runAtTickTime(85, () -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(RavenEntity.class, bounds,
                    RavenEntity::isBaby).size() == 1,
                    "Airborne courtship must count time spent outside breeding range: "
                            + describe(first) + "; " + describe(second));
            helper.assertTrue(first.getAge() > 0 && second.getAge() > 0,
                    "Successful airborne breeding must put both parents on cooldown");
            helper.succeed();
        });
    }

    private static void holdAirborne(GameTestHelper helper, RavenEntity raven, int x) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, 4, 8));
        raven.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        raven.setNoGravity(true);
        raven.setOnGround(false);
        raven.setDeltaMovement(Vec3.ZERO);
    }

    @GameTest(template = "raven_ai_empty", batch = "raven_ai", timeoutTicks = 250)
    public static void environmentalDamageEscapesAroundACornerThenLands(GameTestHelper helper) {
        prepareFloor(helper);
        // Neither leg near the start is three blocks long. A safe destination needs a turn,
        // which cannot pass an expanded-AABB test of the entire straight route.
        for (int x = 2; x <= 9; x++) {
            for (int z = 2; z <= 10; z++) {
                boolean corridor = (x >= 3 && x <= 6 && z >= 3 && z <= 4)
                        || (x >= 5 && x <= 6 && z >= 5 && z <= 9);
                for (int y = 1; y <= 3; y++) {
                    if (!corridor || y == 3) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                    }
                }
            }
        }
        RavenEntity raven = spawn(helper, 4, 1, 3);
        Vec3 origin = raven.position();
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(raven.hurt(helper.getLevel().damageSources().cactus(), 1.0F),
                    "Environmental damage must actually land");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(raven.position().distanceToSqr(origin) >= 9.0D,
                    "Raven must route at least three blocks away from the damage origin");
            helper.assertTrue(raven.onGround() && !raven.isFlying(),
                    "Escape must finish with a real landing, not an indefinite airborne stop");
            helper.assertTrue(helper.getLevel().noCollision(raven), "Escape must respect world collision");
        });
    }

    @GameTest(template = "raven_ai_empty", batch = "raven_ai", timeoutTicks = 250)
    public static void standingInFireEscapesAndLandsAwayFromFire(GameTestHelper helper) {
        prepareFloor(helper);
        helper.setBlock(new BlockPos(8, 1, 8), Blocks.FIRE);
        RavenEntity raven = spawn(helper, 8, 1, 8);
        Vec3 origin = raven.position();
        helper.succeedWhen(() -> {
            helper.assertTrue(raven.isAlive(), "Raven must survive escaping a single fire block");
            helper.assertTrue(raven.position().distanceToSqr(origin) >= 9.0D,
                    "Raven must move at least three blocks away from fire: " + describe(raven));
            helper.assertTrue(raven.onGround() && !raven.isFlying(), "Raven must land after escaping fire");
            helper.assertTrue(!raven.blockPosition().equals(helper.absolutePos(new BlockPos(8, 1, 8))),
                    "Landing must not return to the fire");
        });
    }

    private static void prepareFloor(GameTestHelper helper) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 17; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    private static String describe(RavenEntity raven) {
        return "pos=" + raven.position() + ", flying=" + raven.isFlying() + ", ground=" + raven.onGround()
                + ", motion=" + raven.getDeltaMovement() + ", pathDone=" + raven.getNavigation().isDone()
                + ", love=" + raven.isInLove() + ", age=" + raven.getAge();
    }

    private static RavenEntity spawn(GameTestHelper helper, int x, int y, int z) {
        RavenEntity raven = MaledictEntities.RAVEN.get().create(helper.getLevel());
        helper.assertTrue(raven != null, "Registered Raven must be constructible");
        BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
        raven.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        raven.setPersistenceRequired();
        helper.assertTrue(helper.getLevel().addFreshEntity(raven), "Raven must join the real level");
        return raven;
    }
}
