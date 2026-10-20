package org.brahypno.maledict.common.raven;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.registry.MaledictEntities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = "maledict")
public final class RavenEntityAiTest {
    private static final Map<UUID, Parents> PARENTS = new HashMap<>();

    @BeforeBatch(batch = "raven_ai")
    public static void clearPreviousFixtures(ServerLevel level) {
        clearFixtures(level);
    }

    @AfterBatch(batch = "raven_ai")
    public static void clearCompletedFixtures(ServerLevel level) {
        clearFixtures(level);
    }

    private static void clearFixtures(ServerLevel level) {
        var fixtures = new ArrayList<Entity>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof RavenEntity && entity.getPersistentData().getBoolean("RavenAiTestFixture")) {
                fixtures.add(entity);
            }
        }
        fixtures.forEach(Entity::discard);
        PARENTS.clear();
    }

    @SubscribeEvent
    public static void recordOffspring(BabyEntitySpawnEvent event) {
        Parents parents = PARENTS.get(event.getParentA().getUUID());
        if (parents != null && parents.contains(event.getParentB()) && event.getChild() instanceof RavenEntity chick) {
            parents.chick = chick;
            parents.births++;
            chick.getPersistentData().putBoolean("RavenAiTestFixture", true);
        }
    }

    @SubscribeEvent
    public static void recordFixtureDamage(LivingAttackEvent event) {
        if (event.getEntity() instanceof RavenEntity raven
                && raven.getPersistentData().getBoolean("RavenAiTestFixture")) {
            raven.getPersistentData().putString("RavenAiTestLastAttack", event.getSource().getMsgId()
                    + " amount=" + event.getAmount() + " pos=" + raven.position()
                    + " head=" + raven.level().getBlockState(BlockPos.containing(raven.getEyePosition())));
        }
    }

    @GameTest(template = "raven_ai_empty", batch = "raven_ai", timeoutTicks = 300)
    public static void lovingAdultsFiveBlocksApartAcrossAOneBlockStepBreed(GameTestHelper helper) {
        prepareArena(helper);
        // Synthetic one-block step; this does not load or reproduce a complete user save.
        for (int x = 8; x < 17; x++) {
            for (int z = 0; z < 17; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.NETHERRACK);
            }
        }
        RavenEntity first = spawn(helper, 5, 1, 8);
        RavenEntity second = spawn(helper, 10, 2, 8);
        Parents parents = parents(helper, first, second);
        first.setInLove(null);
        second.setInLove(null);
        helper.succeedWhen(() -> parents.assertBred(helper));
    }

    @GameTest(template = "raven_ai_empty", batch = "raven_ai", timeoutTicks = 300)
    public static void lovingAdultsOnDifferentHeightsFlyToMeetAndBreed(GameTestHelper helper) {
        prepareArena(helper);
        for (int x = 6; x <= 8; x++) {
            for (int z = 7; z <= 9; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        RavenEntity first = spawn(helper, 7, 5, 8);
        RavenEntity second = spawn(helper, 10, 1, 8);
        Parents parents = parents(helper, first, second);
        first.setInLove(null);
        second.setInLove(null);
        helper.succeedWhen(() -> parents.assertBred(helper));
    }

    @GameTest(template = "raven_ai_empty", batch = "raven_ai", timeoutTicks = 300)
    public static void lovingAdultsSixBlocksApartApproachAndBreed(GameTestHelper helper) {
        prepareArena(helper);
        RavenEntity first = spawn(helper, 5, 1, 8);
        RavenEntity second = spawn(helper, 11, 1, 8);
        Parents parents = parents(helper, first, second);
        helper.runAtTickTime(5, () -> {
            first.setInLove(null);
            second.setInLove(null);
        });
        helper.succeedWhen(() -> parents.assertBred(helper));
    }

    @GameTest(template = "raven_ai_empty", batch = "raven_ai", timeoutTicks = 100)
    public static void courtshipContinuesWhileAirborneAndOutOfBreedingRange(GameTestHelper helper) {
        prepareArena(helper);
        RavenEntity first = spawn(helper, 5, 4, 8);
        RavenEntity second = spawn(helper, 11, 4, 8);
        Parents parents = parents(helper, first, second);
        first.setInLove(null);
        second.setInLove(null);
        helper.onEachTick(() -> {
            // Keep both airborne and six blocks apart for the first 40 ticks, then
            // bring them within breeding range without giving them time to land.
            if (first.getAge() == 0 && second.getAge() == 0) {
                holdAirborne(helper, first, 5);
                holdAirborne(helper, second, helper.getTick() < 40 ? 11 : 7);
            }
        });
        helper.runAtTickTime(55, () -> helper.assertTrue(
                parents.chick == null,
                "Proximity must not skip the courtship wait"));
        helper.runAtTickTime(85, () -> {
            parents.assertBred(helper);
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
        prepareArena(helper);
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
        prepareArena(helper);
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

    private static void prepareArena(GameTestHelper helper) {
        for (int x = 0; x < 17; x++) {
            for (int z = 0; z < 17; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y < 10; y++) {
                    boolean boundary = x == 0 || x == 16 || z == 0 || z == 16 || y == 9;
                    helper.setBlock(new BlockPos(x, y, z), boundary ? Blocks.GLASS : Blocks.AIR);
                }
            }
        }
    }

    private static Parents parents(GameTestHelper helper, RavenEntity first, RavenEntity second) {
        Parents parents = new Parents(first, second);
        PARENTS.put(first.getUUID(), parents);
        PARENTS.put(second.getUUID(), parents);
        helper.onEachTick(() -> {
            helper.assertTrue(first.getHealth() == first.getMaxHealth() && second.getHealth() == second.getMaxHealth(),
                    "Breeding fixture received unexpected damage: " + describe(first) + "; " + describe(second));
        });
        return parents;
    }

    private static final class Parents {
        private final RavenEntity first;
        private final RavenEntity second;
        private RavenEntity chick;
        private int births;

        private Parents(RavenEntity first, RavenEntity second) {
            this.first = first;
            this.second = second;
        }

        private boolean contains(Entity entity) {
            return entity == first || entity == second;
        }

        private void assertBred(GameTestHelper helper) {
            helper.assertTrue(births == 1 && chick != null && chick.isAlive() && chick.isBaby()
                            && helper.getLevel().getEntity(chick.getUUID()) == chick,
                    "This pair must produce its own live chick: " + describe(first) + "; " + describe(second));
            helper.assertTrue(first.getAge() > 0 && second.getAge() > 0,
                    "Both parents must enter breeding cooldown");
        }
    }

    private static String describe(RavenEntity raven) {
        return "pos=" + raven.position() + ", flying=" + raven.isFlying() + ", ground=" + raven.onGround()
                + ", motion=" + raven.getDeltaMovement() + ", pathDone=" + raven.getNavigation().isDone()
                + ", love=" + raven.isInLove() + ", age=" + raven.getAge()
                + ", health=" + raven.getHealth()
                + ", lastAttack=" + raven.getPersistentData().getString("RavenAiTestLastAttack");
    }

    private static RavenEntity spawn(GameTestHelper helper, int x, int y, int z) {
        RavenEntity raven = MaledictEntities.RAVEN.get().create(helper.getLevel());
        helper.assertTrue(raven != null, "Registered Raven must be constructible");
        BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
        raven.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        raven.setPersistenceRequired();
        raven.getPersistentData().putBoolean("RavenAiTestFixture", true);
        helper.assertTrue(helper.getLevel().noCollision(raven), "Fixture spawn intersects a block: " + describe(raven));
        helper.assertTrue(helper.getLevel().addFreshEntity(raven), "Raven must join the real level");
        return raven;
    }
}
