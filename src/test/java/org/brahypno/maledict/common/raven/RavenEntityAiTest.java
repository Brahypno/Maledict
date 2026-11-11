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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.registry.MaledictEntities;

import java.util.ArrayList;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = "maledict")
public final class RavenEntityAiTest {
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
