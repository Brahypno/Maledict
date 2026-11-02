package org.brahypno.maledict.common.realm.demiurge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.Vec3;
import com.sammy.malum.registry.common.MobEffectRegistry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.registry.MaledictItems;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class DemiurgeAwakeningTest {
    @Test
    void authoredClipRaisesTheSerpentKeepsTheBaseGroundedAndRoarsOnTheServerBeat() throws Exception {
        try (var input = getClass().getResourceAsStream("/assets/maledict/models/entity/demiurge_statue.mesh.json")) {
            assertNotNull(input);
            var root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            var animation = root.getAsJsonObject("animation");
            assertEquals(DemiurgeAwakening.DURATION, animation.get("duration").getAsInt());
            assertEquals(DemiurgeAwakening.ROAR_TICK, animation.get("roar_tick").getAsInt());
            var frames = animation.getAsJsonArray("frames");
            var names = animation.getAsJsonArray("bones");
            int head = bone(names, "head"), jaw = bone(names, "jaw");
            assertEquals(101, frames.size());
            assertTrue(position(frame(frames, 100, head)).y - position(frame(frames, 0, head)).y > 3,
                    "The upper coil must actually uncoil and raise the head by more than three blocks");
            Quaternionf roar = rotation(frame(frames, 66, head)).conjugate().mul(rotation(frame(frames, 66, jaw)));
            assertTrue(roar.angle() > .7, "The lion must open its jaw widely during the roar");
            Quaternionf settled = rotation(frame(frames, 100, head)).conjugate().mul(rotation(frame(frames, 100, jaw)));
            assertTrue(settled.angle() < .005, "The jaw closes again after the roar");
            for (int tick = 0; tick <= 100; tick++) {
                assertEquals(new Vector3f(), position(frame(frames, tick, 0)), "The supporting coil stays grounded");
                for (var quadValue : root.getAsJsonArray("quads")) {
                    var quad = quadValue.getAsJsonObject();
                    var transform = frame(frames, tick, quad.get("bone").getAsInt());
                    Quaternionf q = rotation(transform);
                    assertEquals(1, q.lengthSquared(), .00001);
                    for (var v : quad.getAsJsonArray("v")) {
                        var xyz = v.getAsJsonArray();
                        Vector3f world = q.transform(new Vector3f(xyz.get(0).getAsFloat(), xyz.get(1).getAsFloat(), xyz.get(2).getAsFloat()))
                                .add(position(transform));
                        assertTrue(world.y >= -.001 && world.y <= DemiurgeAwakening.height(tick) + .02,
                                "Animated vertices must remain above ground and inside the collision height at " + tick);
                    }
                }
            }
            var ground = animation.getAsJsonArray("slither_frames");
            assertEquals(61, ground.size());
            assertEquals(40, animation.get("ground_transition").getAsInt());
            assertFalse(DemiurgeSlither.canPatrol(139, true, false));
            assertTrue(DemiurgeSlither.canPatrol(140, true, false));
            assertFalse(DemiurgeSlither.canPatrol(140, true, true));
            assertFalse(DemiurgeSlither.canPatrol(140, false, false));
            for (int tick = 0; tick <= 60; tick++) {
                assertTrue(position(frame(ground, tick, head)).y > 4.3F, "The cobra-like forebody holds the head near six blocks high");
                for (var value : root.getAsJsonArray("quads")) {
                    var quad = value.getAsJsonObject();
                    var transform = frame(ground, tick, quad.get("bone").getAsInt());
                    var q = rotation(transform);
                    for (var v : quad.getAsJsonArray("v")) {
                        var xyz = v.getAsJsonArray();
                        var point = q.transform(new Vector3f(xyz.get(0).getAsFloat(), xyz.get(1).getAsFloat(), xyz.get(2).getAsFloat()), new Vector3f())
                                .add(position(transform));
                        assertTrue(point.y >= -.001 && point.y <= DemiurgeSlither.HEIGHT,
                                "Ground cycle must stay above the floor and below the raised-front envelope");
                    }
                }
            }
            for (int bone = 0; bone < names.size(); bone++) {
                assertTrue(position(frame(ground, 0, bone)).distance(position(frame(ground, 60, bone))) < .0001);
                assertTrue(Math.abs(rotation(frame(ground, 0, bone)).dot(rotation(frame(ground, 60, bone)))) > .99999);
            }
            DemiurgeTrail trail = new DemiurgeTrail();
            trail.record(new net.minecraft.world.phys.Vec3(0, 0, 0), 0);
            trail.record(new net.minecraft.world.phys.Vec3(0, 0, 2), 0);
            trail.record(new net.minecraft.world.phys.Vec3(-2, 0, 2), 90);
            assertEquals(new net.minecraft.world.phys.Vec3(0, 0, 1), trail.sample(3, new net.minecraft.world.phys.Vec3(-2, 0, 2), 90).position(),
                    "The body follows the actual corner rather than rotating around the head");
            assertTrue(trail.sample(1, new net.minecraft.world.phys.Vec3(-1.9, 0, 2), 90).position()
                            .distanceTo(new net.minecraft.world.phys.Vec3(-.9, 0, 2)) < .000001,
                    "Interpolated rendering must not sample the future tick position and reverse the tail heading");
            verifyBodyConnections(root, animation, ground, names.size());
            verifyContinuousMotion(animation, ground, names.size());
            verifySelfAvoidance(root, animation, ground, names.size());
            for (var attack : DemiurgeAttack.values()) {
                if (attack == DemiurgeAttack.NONE) continue;
                var clip = animation.getAsJsonObject("attacks").getAsJsonObject(attack.name().toLowerCase(java.util.Locale.ROOT));
                var keys = clip.getAsJsonArray("frames");
                assertEquals(attack.duration + 1, keys.size());
                assertEquals(attack.release, clip.get("release").getAsInt());
                assertEquals(attack.recovery, clip.get("recovery").getAsInt());
                for (int tick = 0; tick <= attack.duration; tick++) {
                    var transform = frame(keys, tick, head);
                    assertTrue(position(transform).y > 4.3, "All casts retain the raised forebody");
                    if (attack == DemiurgeAttack.CHARGE) {
                        var forward = rotation(transform).transform(new Vector3f(0, 0, 1));
                        assertTrue(forward.y >= 0, "Charge never pitches the face down");
                    }
                }
                if (attack == DemiurgeAttack.ROAR) {
                    var opening = rotation(frame(keys, 40, head)).conjugate().mul(rotation(frame(keys, 40, jaw)));
                    assertTrue(opening.angle() > .7, "Rain roar must visibly open the jaw");
                }
            }
            var combatPose = DemiurgeRig.INSTANCE.pose();
            combatPose.sample(200, 0, DemiurgeAttack.LASER, 40, Vec3.ZERO, 0, new DemiurgeTrail()::sample);
            assertEquals(.875, combatPose.eye(0, Vec3.ZERO, 0).distanceTo(combatPose.eye(1, Vec3.ZERO, 0)), .00001,
                    "The beams originate from the two separately authored eyes");
            assertEquals(400, DemiurgeDifficulty.maxHealth(net.minecraft.world.Difficulty.EASY));
            assertEquals(600, DemiurgeDifficulty.maxHealth(net.minecraft.world.Difficulty.NORMAL));
            assertEquals(800, DemiurgeDifficulty.maxHealth(net.minecraft.world.Difficulty.HARD));
            try (var spirits = getClass().getResourceAsStream("/data/maledict/spirit_data/entity/demiurge_statue.json")) {
                assertNotNull(spirits);
                var spiritData = JsonParser.parseReader(new InputStreamReader(spirits, StandardCharsets.UTF_8)).getAsJsonObject();
                var counts = new java.util.HashMap<String, Integer>();
                for (var value : spiritData.getAsJsonArray("spirits")) {
                    var entry = value.getAsJsonObject();
                    counts.put(entry.get("spirit").getAsString(), entry.get("count").getAsInt());
                }
                assertEquals(java.util.Map.of("sacred", 6, "wicked", 7, "eldritch", 8, "umbral", 3), counts);
            }
        }
    }

    @GameTest(template = "test_empty", batch = "demiurge_awakening", timeoutTicks = 1000)
    public static void starIsConsumedOnceAndTheUncoilingSurvivesReload(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "DemiurgeTest"));
        DemiurgeStatueEntity[] current = {MaledictEntities.DEMIURGE_STATUE.get().create(level)};
        var statue = current[0];
        java.util.List<ChunkPos> patrolChunks = new java.util.ArrayList<>();
        statue.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 12, 2)));
        statue.setNoGravity(true);
        level.addFreshEntity(statue);
        helper.assertTrue(statue.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) == 20
                        && statue.getHealth() == DemiurgeDifficulty.maxHealth(level.getDifficulty()),
                "The spawned statue must receive 20 armour and its difficulty's authoritative health pool");
        var spiritDrops = com.sammy.malum.core.listeners.SpiritDataReloadListener.SPIRIT_DATA.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("maledict", "demiurge_statue"));
        helper.assertTrue(spiritDrops != null && spiritDrops.totalSpirits == 24 && spiritDrops.droppedSpirits.size() == 4,
                "Malum's live reload listener must load all four spirit types, totalling 24 spirits");
        for (var difficulty : net.minecraft.world.Difficulty.values()) {
            double base = 7;
            float unit = DemiurgeDifficulty.damage(1, difficulty);
            helper.assertTrue(Math.abs(DemiurgeDifficulty.damage(base, difficulty) - unit * base) < .00001,
                    "Changing base damage must scale proportionally in every difficulty");
            var healthFixture = MaledictEntities.DEMIURGE_STATUE.get().create(level);
            var tag = healthFixture.saveWithoutId(new CompoundTag());
            tag.putInt("CombatDifficulty", difficulty.getId());
            healthFixture.load(tag);
            healthFixture.setPos(statue.position().add(32, 0, 0));
            level.addFreshEntity(healthFixture);
            helper.assertTrue(healthFixture.getHealth() == DemiurgeDifficulty.maxHealth(difficulty)
                            && healthFixture.getMaxHealth() == DemiurgeDifficulty.maxHealth(difficulty),
                    "The ledger and vanilla attribute must agree on the captured difficulty health: " + difficulty);
            healthFixture.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        }
        var originalDamage = statue.damageSources().mobAttack(statue);
        var scaledDamage = DemiurgeDifficulty.source(originalDamage);
        helper.assertTrue(!scaledDamage.scalesWithDifficulty() && scaledDamage.typeHolder().equals(originalDamage.typeHolder())
                        && scaledDamage.getEntity() == statue && scaledDamage.getDirectEntity() == statue,
                "Difficulty must be applied exactly once while retaining source tags and boss attribution");
        var reducedTarget = new ContactCow(level);
        reducedTarget.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
        reducedTarget.setPos(statue.position().add(32, 0, 0));
        reducedTarget.damageMultiplier = .25F;
        level.addFreshEntity(reducedTarget);
        for (var difficulty : net.minecraft.world.Difficulty.values()) {
            reducedTarget.setHealth(100);
            reducedTarget.invulnerableTime = 0;
            reducedTarget.attempts = 0;
            float expected = DemiurgeDifficulty.damage(8, difficulty) * .25F;
            DemiurgeDifficulty.hurt(reducedTarget, originalDamage, 8, difficulty);
            helper.assertTrue(Math.abs(reducedTarget.getHealth() - (100 - expected)) < .0001
                            && reducedTarget.attempts == (expected > 0 ? 1 : 0),
                    "Every difficulty must stop after the first reduced hit without compensating: " + difficulty
                            + ", health=" + reducedTarget.getHealth() + ", attempts=" + reducedTarget.attempts);
            DemiurgeDifficulty.hurt(reducedTarget, originalDamage, 8, difficulty);
            helper.assertTrue(Math.abs(reducedTarget.getHealth() - (100 - expected)) < .0001
                            && reducedTarget.attempts == (expected > 0 ? 1 : 0),
                    "The active vanilla i-frame window must block another probe run: " + difficulty);
        }
        reducedTarget.discard();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 2));
        statue.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(statue.awakeningAge(0) == -1, "An ordinary item must not awaken the statue");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MaledictItems.BLIGHTED_STAR.get(), 3));
        statue.mobInteract(player, InteractionHand.MAIN_HAND);
        statue.mobInteract(player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(MaledictItems.BLIGHTED_STAR.get(), 2));
        statue.mobInteract(player, InteractionHand.OFF_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 2 && player.getOffhandItem().getCount() == 2
                        && statue.awakeningAge(0) == 0,
                "First use consumes exactly one; repeat and offhand use cannot consume again");
        helper.runAtTickTime(10, () -> helper.assertTrue(
                !level.getEntitiesOfClass(DemiurgeTearEntity.class, current[0].getBoundingBox().inflate(15),
                        tear -> tear.getOwner() == current[0]).isEmpty(),
                "Rain must start during awakening, before the roar and before combat AI becomes available"));
        helper.runAtTickTime(35, () -> {
            var old = current[0];
            float age = old.awakeningAge(0);
            var saved = old.saveWithoutId(new CompoundTag());
            old.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            var restored = MaledictEntities.DEMIURGE_STATUE.get().create(level);
            restored.load(saved);
            level.addFreshEntity(restored);
            current[0] = restored;
            helper.assertTrue(restored.awakeningAge(0) == age && age > 20,
                    "Reload must preserve the actual in-progress awakening instead of restarting it");
        });
        helper.runAtTickTime(72, () -> helper.assertTrue(
                current[0].saveWithoutId(new CompoundTag()).getBoolean("AwakeningRoarPlayed"),
                "The server must issue the roar once on the authored beat"));
        helper.runAtTickTime(112, () -> {
            var awake = current[0];
            awake.mobInteract(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(awake.isAwake() && Math.abs(awake.getBbHeight() - DemiurgeSlither.height(awake.awakeningAge(0))) < .01
                            && player.getMainHandItem().getCount() == 2,
                    "The raised statue stays awake, grows its collision height and rejects another payment");
            var tag = awake.saveWithoutId(new CompoundTag());
            awake.readAdditionalSaveData(tag);
            helper.assertTrue(awake.isAwake() && tag.getBoolean("AwakeningRoarPlayed"),
                    "Completed awakening and the consumed roar marker survive save/load");
            BlockPos floor = helper.absolutePos(new BlockPos(50, 5, 50));
            for (int x = (floor.getX() - 15) >> 4; x <= (floor.getX() + 15) >> 4; x++) {
                for (int z = (floor.getZ() - 15) >> 4; z <= (floor.getZ() + 15) >> 4; z++) {
                    if (!level.getForcedChunks().contains(ChunkPos.asLong(x, z))) {
                        level.setChunkForced(x, z, true);
                        patrolChunks.add(new ChunkPos(x, z));
                    }
                }
            }
            for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                level.setBlockAndUpdate(floor.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 9; y++) level.setBlockAndUpdate(floor.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
            // Let the statue fall before finishing its transition. Recording
            // that descent used to leave the tail hanging on an airborne path.
            awake.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(floor).add(0, 6, 0));
            awake.setNoGravity(false);
            awake.setOnGround(false);
        });
        helper.runAtTickTime(139, () -> {
            var awake = current[0];
            helper.assertTrue(awake.onGround(), "The transition fixture must have landed before patrol");
            for (double distance : new double[]{5, 10, 20}) {
                helper.assertTrue(Math.abs(awake.trailPoint(distance, awake.position(), awake.getYRot()).position().y - awake.getY()) < .001,
                        "The resting tail must be grounded immediately after landing, before any forward movement");
            }
        });
        net.minecraft.world.phys.Vec3[] patrolStart = new net.minecraft.world.phys.Vec3[1];
        BlockPos[] wall = new BlockPos[1];
        float[] previousYaw = new float[1];
        ContactCow[] combatTarget = new ContactCow[1];
        var observed = java.util.EnumSet.noneOf(DemiurgeAttack.class);
        boolean[] firstCharge = {true};
        helper.onEachTick(() -> {
            if (patrolStart[0] != null && !current[0].isRemoved()) {
                float yaw = current[0].getYRot();
                helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - previousYaw[0])) <= 2.01F,
                        "Patrol must turn in a broad arc, at most two degrees per tick");
                previousYaw[0] = yaw;
            }
            var awake = current[0];
            if (combatTarget[0] != null) {
                observed.add(awake.attack());
                if (firstCharge[0] && awake.attack() == DemiurgeAttack.CHARGE && awake.attack().active(awake.attackAge(0))) {
                    // Move the same target between head and tail while preserving
                    // vanilla i-frames. Overlapping parts must share one damage attempt.
                    int bone = ((int) awake.attackAge(0) & 1) == 0 ? 14 : 25;
                    var bounds = awake.getParts()[bone].getBoundingBox();
                    var center = bounds.getCenter();
                    combatTarget[0].setPos(center.x, bounds.minY, center.z);
                    combatTarget[0].setDeltaMovement(Vec3.ZERO);
                }
            }
        });
        helper.runAtTickTime(145, () -> {
            var awake = current[0];
            helper.assertTrue(awake.canPatrol() && Math.abs(awake.getBbHeight() - 6.4) < .01,
                    "Patrol begins only after the grounded raised-front transition: age=" + awake.awakeningAge(0)
                            + ", height=" + awake.getBbHeight() + ", alive=" + awake.isAlive());
            patrolStart[0] = awake.position();
            previousYaw[0] = awake.getYRot();
            var patrol = new DemiurgePatrolGoal(awake);
            awake.getRandom().setSeed(1230);
            patrol.trigger();
            helper.assertTrue(patrol.canUse(), "The real ground patrol goal must find a walkable destination");
            patrol.start();
            awake.getNavigation().stop();
            awake.setYRot(0);
            awake.setDeltaMovement(0, 0, 0);
            previousYaw[0] = 0;
            wall[0] = verifyContactDestruction(helper, awake);
            for (int x = -12; x <= 12; x++) for (int y = 0; y < 7; y++) {
                level.setBlockAndUpdate(wall[0].offset(x, y, 0),
                        (x + y) % 2 == 0 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.STONE.defaultBlockState());
            }
            var target = awake.position().add(0, 0, 9);
            helper.assertTrue(awake.getNavigation().moveTo(target.x, target.y, target.z, .8),
                    "Ground navigation must plan through a wither-breakable wall instead of rejecting it");
        });
        helper.runAtTickTime(190, () -> {
            helper.assertTrue(level.getBlockState(wall[0]).isAir(),
                    "The moving statue must open its passage through the obsidian wall");
            helper.assertTrue(level.getBlockState(BlockPos.containing(current[0].position()).below()).is(Blocks.STONE),
                    "Contact destruction must leave the supporting floor intact");
        });
        helper.runAtTickTime(370, () -> {
            var awake = current[0];
            helper.assertTrue(awake.position().subtract(patrolStart[0]).horizontalDistance() > .15,
                    "Ground navigation must move the statue: start=" + patrolStart[0] + ", end=" + awake.position()
                            + ", navigationDone=" + awake.getNavigation().isDone() + ", onGround=" + awake.onGround()
                            + ", speed=" + awake.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
            helper.assertTrue(awake.getZ() > wall[0].getZ() + .1,
                    "The statue must actually cross the wall after breaking it: position=" + awake.position() + ", wall=" + wall[0]);
            var enemy = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
            enemy.setNoAi(true);
            awake.setTarget(enemy);
            helper.assertTrue(!awake.canPatrol() && !new DemiurgePatrolGoal(awake).canUse(),
                    "A live enemy excludes idle patrol");
            enemy.discard();
            patrolStart[0] = null;
            // Restore into open air above the patrol fixtures. The old ground
            // walls must not obstruct an unrelated autonomous casting check.
            var combatSave = awake.saveWithoutId(new CompoundTag());
            Vec3 combatOrigin = new Vec3(awake.getX(), level.getMaxBuildHeight() - 40, awake.getZ());
            awake.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            awake = MaledictEntities.DEMIURGE_STATUE.get().create(level);
            awake.load(combatSave);
            awake.setPos(combatOrigin);
            level.addFreshEntity(awake);
            current[0] = awake;
            awake.setTarget(null);
            awake.getNavigation().stop();
            awake.setNoGravity(true);
            awake.setDeltaMovement(Vec3.ZERO);
            awake.setYRot(0);
            awake.updateParts();
            combatTarget[0] = verifyLaserAndRoofPiercingRain(helper, awake);
            combatTarget[0].getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
            combatTarget[0].setHealth(1000);
            combatTarget[0].removeAllEffects();
            combatTarget[0].attempts = 0;
            combatTarget[0].lastAttempt = Long.MIN_VALUE;
            combatTarget[0].minimumInterval = Long.MAX_VALUE;
            combatTarget[0].damageLog.clear();
            awake.setTarget(combatTarget[0]);
            awake.beginAttack(DemiurgeAttack.CHARGE, combatTarget[0]);
        });
        helper.runAtTickTime(418, () -> {
            var awake = current[0];
            float perHit = DemiurgeDifficulty.damage(org.brahypno.maledict.config.MaledictConfig.DEMIURGE_CHARGE_DAMAGE.get(), level.getDifficulty());
            float hits = (1000 - combatTarget[0].getHealth()) / perHit;
            helper.assertTrue(hits >= 2 && hits <= 3,
                    "Sustained head/tail contact may damage again after vanilla i-frames, not once per cast: "
                            + combatTarget[0].getHealth() + ", damage=" + perHit + ", attempts=" + combatTarget[0].attempts
                            + ", interval=" + combatTarget[0].minimumInterval + ", log=" + combatTarget[0].damageLog);
            helper.assertTrue(combatTarget[0].minimumInterval >= 10 && combatTarget[0].attempts <= 3,
                    "Overlapping body parts must not spam hurt during the i-frame window: attempts="
                            + combatTarget[0].attempts + ", minimumInterval=" + combatTarget[0].minimumInterval);
            firstCharge[0] = false;
            combatTarget[0].setPos(awake.position().add(0, 4, 8));
            combatTarget[0].setDeltaMovement(Vec3.ZERO);
        });
        helper.runAtTickTime(900, () -> {
            helper.assertTrue(observed.containsAll(java.util.List.of(DemiurgeAttack.CHARGE, DemiurgeAttack.LASER, DemiurgeAttack.ROAR)),
                    "The live combat goal must autonomously enter all three attacks: " + observed
                            + ", target=" + current[0].getTarget() + ", alive=" + combatTarget[0].isAlive()
                            + ", distance=" + current[0].distanceTo(combatTarget[0])
                            + ", lineOfSight=" + current[0].hasLineOfSight(combatTarget[0]));
            combatTarget[0].discard();
            current[0].remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            patrolChunks.forEach(chunk -> level.setChunkForced(chunk.x, chunk.z, false));
            helper.succeed();
        });
    }

    private static ContactCow verifyLaserAndRoofPiercingRain(GameTestHelper helper, DemiurgeStatueEntity statue) {
        var level = helper.getLevel();
        var target = new ContactCow(level);
        target.setNoAi(true);
        target.setNoGravity(true);
        Vec3 eye = statue.eyeOrigin(0);
        target.setPos(eye.add(0, -target.getBbHeight() / 2, 8));
        level.addFreshEntity(target);
        statue.beginAttack(DemiurgeAttack.LASER, target);
        var wall = new java.util.ArrayList<BlockPos>();
        // The patrol can finish beyond its original cleared floor. Reserve an
        // unobstructed beam corridor before installing the deliberate test wall.
        BlockPos entrance = BlockPos.containing(eye);
        for (int x = -3; x <= 3; x++) for (int y = -2; y <= 2; y++) for (int z = 0; z <= 11; z++) {
            level.setBlockAndUpdate(entrance.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        BlockPos center = BlockPos.containing(eye.add(0, 0, 4));
        for (int x = -3; x <= 3; x++) for (int y = -2; y <= 2; y++) {
            var pos = center.offset(x, y, 0);
            level.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
            wall.add(pos);
        }
        float health = target.getHealth();
        float laserDamage = DemiurgeDifficulty.damage(org.brahypno.maledict.config.MaledictConfig.DEMIURGE_LASER_DAMAGE.get(), level.getDifficulty());
        float rainDamage = DemiurgeDifficulty.damage(org.brahypno.maledict.config.MaledictConfig.DEMIURGE_RAIN_DAMAGE.get(), level.getDifficulty());
        statue.fireLasers();
        helper.assertTrue(target.getHealth() == health && !target.hasEffect(MobEffectRegistry.SILENCED.get()),
                "A solid wall must block BOTH laser damage and silence");
        wall.forEach(pos -> level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
        statue.fireLasers();
        helper.assertTrue(Math.abs(target.getHealth() - (health - laserDamage)) < .00001 && target.hasEffect(MobEffectRegistry.SILENCED.get()),
                "An unobstructed twin-eye hit deals one scaled pulse and applies Malum silence: health=" + target.getHealth()
                        + ", previous=" + health + ", silence=" + target.hasEffect(MobEffectRegistry.SILENCED.get())
                        + ", eye=" + eye + ", end=" + statue.laserEnd(eye) + ", target=" + target.getBoundingBox()
                        + ", direction=" + statue.beamDirection());
        statue.finishAttack();
        target.invulnerableTime = 0;
        target.setPos(target.position().add(8, 0, 0));
        BlockPos roof = BlockPos.containing(target.position().add(0, 3, 0));
        level.setBlockAndUpdate(roof, Blocks.BEDROCK.defaultBlockState());
        var tear = MaledictEntities.DEMIURGE_TEAR.get().create(level);
        tear.setOwner(statue);
        tear.setPos(target.position().add(0, 5, 0));
        tear.setDeltaMovement(0, -DemiurgeTearEntity.FALL_SPEED, 0);
        level.addFreshEntity(tear);
        for (int tick = 0; tick < 8 && !tear.isRemoved(); tick++) tear.tick();
        helper.assertTrue(tear.isRemoved() && Math.abs(target.getHealth() - (health - laserDamage - rainDamage)) < .00001
                        && level.getBlockState(roof).is(Blocks.BEDROCK),
                "A falling tear must pass THROUGH an intact roof and deal one scaled hit to the target below");
        level.setBlockAndUpdate(roof, Blocks.AIR.defaultBlockState());
        target.setPos(statue.position().add(0, 4, 8));
        target.setDeltaMovement(Vec3.ZERO);
        return target;
    }

    private static final class ContactCow extends Cow {
        private int attempts;
        private long lastAttempt = Long.MIN_VALUE;
        private long minimumInterval = Long.MAX_VALUE;
        private final java.util.List<String> damageLog = new java.util.ArrayList<>();
        private float damageMultiplier = 1;

        private ContactCow(net.minecraft.world.level.Level level) { super(EntityType.COW, level); }

        @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
            damageLog.add(level().getGameTime() + ":" + source.getMsgId() + ":" + amount + ":iframes=" + invulnerableTime);
            if (source.getEntity() instanceof DemiurgeStatueEntity) {
                long now = level().getGameTime();
                if (lastAttempt != Long.MIN_VALUE) minimumInterval = Math.min(minimumInterval, now - lastAttempt);
                lastAttempt = now;
                attempts++;
            }
            return super.hurt(source, amount * damageMultiplier);
        }
    }

    private static BlockPos verifyContactDestruction(GameTestHelper helper, DemiurgeStatueEntity statue) {
        var level = helper.getLevel();
        var base = BlockPos.containing(statue.position());
        var stone = base.offset(0, 0, 3);
        var obsidian = stone.above();
        var bedrock = stone.offset(1, 2, 0);
        var protectedBlock = stone.offset(-1, 2, 0);
        var cobweb = stone.above(2);
        level.setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(obsidian, Blocks.OBSIDIAN.defaultBlockState());
        level.setBlockAndUpdate(bedrock, Blocks.BEDROCK.defaultBlockState());
        level.setBlockAndUpdate(protectedBlock, Blocks.DIAMOND_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(cobweb, Blocks.COBWEB.defaultBlockState());
        java.util.function.Consumer<net.minecraftforge.event.entity.living.LivingDestroyBlockEvent> deny = event -> {
            if (event.getEntity() == statue && event.getPos().equals(protectedBlock)) event.setCanceled(true);
        };
        var griefing = level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        boolean original = griefing.get();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(deny);
        try {
            griefing.set(false, level.getServer());
            DemiurgeBlockBreaking.clearAhead(statue, 0);
            helper.assertTrue(level.getBlockState(stone).is(Blocks.STONE) && level.getBlockState(obsidian).is(Blocks.OBSIDIAN),
                    "mobGriefing=false must prevent contact destruction");
            griefing.set(true, level.getServer());
            helper.assertTrue(!DemiurgeBlockBreaking.clearAhead(statue, 0),
                    "Immune or event-protected blocks must remain blocking");
            helper.assertTrue(level.getBlockState(stone).isAir() && level.getBlockState(obsidian).isAir()
                            && level.getBlockState(cobweb).isAir(),
                    "Wither-strength destruction must break stone, obsidian and non-solid obstacles such as cobwebs");
            helper.assertTrue(level.getBlockState(bedrock).is(Blocks.BEDROCK)
                            && level.getBlockState(protectedBlock).is(Blocks.DIAMOND_BLOCK),
                    "Wither immunity and Forge destruction cancellation must be respected");
            helper.assertTrue(level.getBlockState(base.below()).is(Blocks.STONE), "Do not dig away the floor");
        } finally {
            griefing.set(original, level.getServer());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(deny);
            level.setBlockAndUpdate(bedrock, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(protectedBlock, Blocks.AIR.defaultBlockState());
        }
        return stone;
    }

    private static void verifyBodyConnections(JsonObject mesh, JsonObject animation, JsonArray ground, int count) {
        var joints = new java.util.ArrayList<DemiurgeBodyChain.Joint>();
        for (var value : animation.getAsJsonArray("body_chain")) {
            var joint = value.getAsJsonObject();
            var corners = new java.util.ArrayList<Vector3f>();
            for (var corner : joint.getAsJsonArray("corners")) corners.add(vector(corner.getAsJsonArray()));
            joints.add(new DemiurgeBodyChain.Joint(joint.get("bone").getAsInt(),
                    vector(joint.getAsJsonArray("front")), vector(joint.getAsJsonArray("back")), corners));
        }
        assertEquals(23, joints.size(), "Every moving body and tail section participates in the connected chain");
        var chain = new DemiurgeBodyChain(joints);
        var origin = net.minecraft.world.phys.Vec3.ZERO;
        var solids = new java.util.ArrayList<Solid>();
        for (int bone = 0; bone < count; bone++) {
            var vertices = new java.util.ArrayList<Vector3f>();
            var axes = new java.util.ArrayList<Vector3f>();
            for (var value : mesh.getAsJsonArray("quads")) {
                var quad = value.getAsJsonObject();
                if (quad.get("bone").getAsInt() != bone) continue;
                for (var v : quad.getAsJsonArray("v")) vertices.add(vector(v.getAsJsonArray()));
                var normal = vector(quad.getAsJsonArray("n"));
                if (axes.stream().noneMatch(axis -> Math.abs(axis.dot(normal)) > .999F)) axes.add(normal);
            }
            solids.add(new Solid(vertices, axes));
        }
        double minimumY = Double.POSITIVE_INFINITY;
        for (int route = 0; route < 3; route++) {
            var path = new DemiurgeTrail();
            if (route > 0) {
                path.record(route == 1 ? new net.minecraft.world.phys.Vec3(10, 0, -8)
                        : new net.minecraft.world.phys.Vec3(0, 0, 10), 0);
                path.record(new net.minecraft.world.phys.Vec3(0, 0, -8), route == 1 ? 90 : -90);
                path.record(origin, 0);
            }
            for (int tick = 0; tick <= 60; tick++) for (float moving : new float[]{0, .35F, 1}) {
                var positions = new Vector3f[count];
                var rotations = new Quaternionf[count];
                for (int bone = 0; bone < count; bone++) {
                    positions[bone] = position(frame(ground, tick, bone)).lerp(position(frame(ground, 0, bone)), 1 - moving);
                    rotations[bone] = rotation(frame(ground, tick, bone)).slerp(rotation(frame(ground, 0, bone)), 1 - moving);
                }
                chain.follow(positions, rotations, origin, 0, 1, path::sample);
                Solid previous = null;
                for (var joint : joints) {
                    var front = rotations[joint.bone()].transform(joint.front(), new Vector3f()).add(positions[joint.bone()]);
                    var back = rotations[joint.bone()].transform(joint.back(), new Vector3f()).add(positions[joint.bone()]);
                    Solid solid = solids.get(joint.bone()).transform(positions[joint.bone()], rotations[joint.bone()]);
                    if (previous != null) assertTrue(overlaps(previous, solid),
                            "Adjacent stone solids must intersect through turns, movement and stopping: route="
                                    + route + ", tick=" + tick + ", moving=" + moving + ", bone=" + joint.bone());
                    assertEquals(joint.front().distance(joint.back()), front.distance(back), .0001,
                            "Stone sections must retain their original length");
                    assertEquals(1, rotations[joint.bone()].lengthSquared(), .00001);
                    previous = solid;
                }
                for (var value : mesh.getAsJsonArray("quads")) {
                    var quad = value.getAsJsonObject();
                    int bone = quad.get("bone").getAsInt();
                    for (var v : quad.getAsJsonArray("v")) {
                        var point = rotations[bone].transform(vector(v.getAsJsonArray()), new Vector3f()).add(positions[bone]);
                        minimumY = Math.min(minimumY, point.y);
                    }
                }
            }
        }
        assertTrue(minimumY >= -.001, "Connected ground body must not sink through the floor: " + minimumY);
    }

    private record Solid(java.util.List<Vector3f> vertices, java.util.List<Vector3f> axes) {
        Solid transform(Vector3f position, Quaternionf rotation) {
            return new Solid(vertices.stream().map(v -> rotation.transform(v, new Vector3f()).add(position)).toList(),
                    axes.stream().map(v -> rotation.transform(v, new Vector3f())).toList());
        }
    }

    private static void verifyContinuousMotion(JsonObject animation, JsonArray ground, int count) throws Exception {
        var joints = new java.util.ArrayList<DemiurgeBodyChain.Joint>();
        for (var value : animation.getAsJsonArray("body_chain")) {
            var joint = value.getAsJsonObject();
            var corners = new java.util.ArrayList<Vector3f>();
            for (var corner : joint.getAsJsonArray("corners")) corners.add(vector(corner.getAsJsonArray()));
            joints.add(new DemiurgeBodyChain.Joint(joint.get("bone").getAsInt(),
                    vector(joint.getAsJsonArray("front")), vector(joint.getAsJsonArray("back")), corners));
        }
        var chain = new DemiurgeBodyChain(joints);
        var trail = new DemiurgeTrail();
        var origin = net.minecraft.world.phys.Vec3.ZERO;
        trail.record(origin, 0);
        float yaw = 0, clock = 0;
        Quaternionf[] previousRotations = null;
        Vector3f[] previousPositions = null;
        float maxRotation = 0, maxStep = 0, maxBend = 0, minimumSpacing = Float.POSITIVE_INFINITY;
        var snapshots = new JsonArray();
        for (int tick = 0; tick < 400; tick++) {
            float oldYaw = yaw, oldClock = clock;
            var before = origin;
            boolean stopped = tick >= 180 && tick < 220 || tick >= 360;
            if (!stopped) {
                if (tick >= 100 && tick < 180) yaw += 1.5F;
                if (tick >= 220 && tick < 340) yaw -= 1.5F;
                double distance = tick % 5 == 0 ? .04 : .08;
                origin = origin.add(net.minecraft.world.phys.Vec3.directionFromRotation(0, yaw).scale(distance));
                clock += (float) distance * DemiurgeSlither.CYCLE_TICKS / DemiurgeSlither.BLOCKS_PER_CYCLE;
                trail.record(origin, yaw);
            }
            for (int sample = 0; sample < 4; sample++) {
                float partial = sample / 4F;
                var renderOrigin = before.lerp(origin, partial);
                float renderYaw = oldYaw + (yaw - oldYaw) * partial;
                float phase = (oldClock + (clock - oldClock) * partial) % 60;
                int key = (int) phase;
                var positions = new Vector3f[count];
                var rotations = new Quaternionf[count];
                for (int bone = 0; bone < count; bone++) {
                    positions[bone] = position(frame(ground, key, bone)).lerp(position(frame(ground, key + 1, bone)), phase - key);
                    rotations[bone] = rotation(frame(ground, key, bone)).slerp(rotation(frame(ground, key + 1, bone)), phase - key);
                }
                chain.follow(positions, rotations, renderOrigin, renderYaw, 1, trail::sample);
                var worldRotation = new Quaternionf().rotationY((float) Math.toRadians(-renderYaw));
                for (int bone = 0; bone < count; bone++) {
                    worldRotation.transform(positions[bone]).add((float) renderOrigin.x, (float) renderOrigin.y, (float) renderOrigin.z);
                    rotations[bone].premul(worldRotation);
                }
                Vector3f previousDirection = null;
                DemiurgeBodyChain.Joint previousJoint = null;
                for (var joint : joints) {
                    var direction = rotations[joint.bone()].transform(new Vector3f(joint.back()).sub(joint.front())).normalize();
                    if (previousDirection != null) {
                        float bend = (float) Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, direction.dot(previousDirection)))));
                        maxBend = Math.max(maxBend, bend);
                        float nominal = (joint.front().distance(joint.back())
                                + previousJoint.front().distance(previousJoint.back())) * .5F;
                        minimumSpacing = Math.min(minimumSpacing, positions[joint.bone()].distance(positions[previousJoint.bone()]) / nominal);
                    }
                    if (previousRotations != null) {
                        float dot = Math.min(1, Math.abs(rotations[joint.bone()].dot(previousRotations[joint.bone()])));
                        maxRotation = Math.max(maxRotation, (float) Math.toDegrees(2 * Math.acos(dot)));
                        maxStep = Math.max(maxStep, positions[joint.bone()].distance(previousPositions[joint.bone()]));
                    }
                    previousDirection = direction;
                    previousJoint = joint;
                }
                if (tick % 2 == 0 && sample == 0) {
                    var poses = new JsonArray();
                    for (int bone = 0; bone < count; bone++) {
                        var pose = new JsonObject();
                        var p = positions[bone]; var q = rotations[bone];
                        pose.add("p", numbers(p.x, p.y, p.z));
                        pose.add("q", numbers(q.x, q.y, q.z, q.w));
                        poses.add(pose);
                    }
                    snapshots.add(poses);
                }
                previousPositions = positions;
                previousRotations = rotations;
            }
        }
        var report = new JsonObject();
        report.add("bones", animation.get("bones"));
        report.add("frames", snapshots);
        report.addProperty("max_rotation_per_quarter_tick", maxRotation);
        report.addProperty("max_center_step", maxStep);
        report.addProperty("max_adjacent_bend", maxBend);
        report.addProperty("minimum_spacing_ratio", minimumSpacing);
        var output = java.nio.file.Path.of("..", "demiurge-review", "motion", "runtime-motion.json");
        java.nio.file.Files.createDirectories(output.getParent());
        java.nio.file.Files.writeString(output, report.toString());
        assertTrue(maxRotation < 4, "Consecutive render samples must not snap: " + maxRotation + " degrees");
        assertTrue(maxStep < .15, "Consecutive render samples must not jump: " + maxStep + " blocks");
        assertTrue(maxBend < 50, "Broad patrol turns must not fold the chain back: " + maxBend + " degrees");
        assertTrue(minimumSpacing > .8, "Adjacent centres must not collapse into one another: " + minimumSpacing);
    }

    private static void verifySelfAvoidance(JsonObject mesh, JsonObject animation, JsonArray ground, int count) {
        var zero = net.minecraft.world.phys.Vec3.ZERO;
        for (double step : new double[]{.01, .03, .06, .12}) {
            var turn = DemiurgePatrolSteering.steer(zero, 0, 90, step, java.util.List.of());
            assertFalse(turn.blocked());
            assertTrue(Math.abs(turn.yaw()) <= Math.toDegrees(step / DemiurgePatrolSteering.TURN_RADIUS) + .00001,
                    "Turn radius must remain broad even at the statue's slow actual ground speed");
        }
        var sidestep = DemiurgePatrolSteering.steer(zero, 0, -90, .06,
                java.util.List.of(new net.minecraft.world.phys.Vec3(1.75, 0, 4.5)));
        assertFalse(sidestep.blocked());
        assertTrue(sidestep.yaw() > 0, "A body section on the right must redirect the front toward the open left side");
        var barrier = new java.util.ArrayList<net.minecraft.world.phys.Vec3>();
        for (double x = -6; x <= 6; x += .5) barrier.add(new net.minecraft.world.phys.Vec3(x, 0, 4));
        assertTrue(DemiurgePatrolSteering.steer(zero, 0, 0, .06, barrier).blocked(),
                "When neither steering arc clears the body, stop rather than push through it");
        assertFalse(DemiurgePatrolSteering.clearDestination(new net.minecraft.world.phys.Vec3(0, 0, -10), zero, 0, java.util.List.of()),
                "Patrol must not pick a destination directly back through its trailing body");

        var joints = new java.util.ArrayList<DemiurgeBodyChain.Joint>();
        for (var value : animation.getAsJsonArray("body_chain")) {
            var joint = value.getAsJsonObject();
            var corners = new java.util.ArrayList<Vector3f>();
            for (var corner : joint.getAsJsonArray("corners")) corners.add(vector(corner.getAsJsonArray()));
            joints.add(new DemiurgeBodyChain.Joint(joint.get("bone").getAsInt(),
                    vector(joint.getAsJsonArray("front")), vector(joint.getAsJsonArray("back")), corners));
        }
        var solids = new java.util.ArrayList<Solid>();
        for (var joint : joints) {
            var vertices = new java.util.ArrayList<Vector3f>();
            var axes = new java.util.ArrayList<Vector3f>();
            for (var value : mesh.getAsJsonArray("quads")) {
                var quad = value.getAsJsonObject();
                if (quad.get("bone").getAsInt() != joint.bone()) continue;
                for (var v : quad.getAsJsonArray("v")) vertices.add(vector(v.getAsJsonArray()));
                var normal = vector(quad.getAsJsonArray("n"));
                if (axes.stream().noneMatch(axis -> Math.abs(axis.dot(normal)) > .999F)) axes.add(normal);
            }
            solids.add(new Solid(vertices, axes));
        }
        var chain = new DemiurgeBodyChain(joints);
        var trail = new DemiurgeTrail();
        var origin = zero;
        float yaw = 0, clock = 0;
        double travelled = 0;
        var goals = new net.minecraft.world.phys.Vec3[]{
                new net.minecraft.world.phys.Vec3(0, 0, 12), new net.minecraft.world.phys.Vec3(-10, 0, 8),
                new net.minecraft.world.phys.Vec3(4, 0, -4), new net.minecraft.world.phys.Vec3(10, 0, 12), zero};
        int goalIndex = 0;
        var goal = goals[0];
        trail.record(origin, yaw);
        for (int tick = 0; tick < 2000; tick++) {
            if (tick % 200 == 0 || origin.distanceToSqr(goal) < 4) goal = goals[goalIndex++ % goals.length];
            var body = DemiurgePatrolSteering.body(origin, yaw, trail::sample);
            float target = (float) Math.toDegrees(Math.atan2(origin.x - goal.x, goal.z - origin.z));
            var turn = DemiurgePatrolSteering.steer(origin, yaw, target, .06, body);
            if (turn.blocked()) {
                goal = origin.add(net.minecraft.world.phys.Vec3.directionFromRotation(0, yaw + 30).scale(12));
            } else {
                yaw = turn.yaw();
                origin = origin.add(net.minecraft.world.phys.Vec3.directionFromRotation(0, yaw).scale(.06));
                travelled += .06;
                clock = (clock + .06F * DemiurgeSlither.CYCLE_TICKS / DemiurgeSlither.BLOCKS_PER_CYCLE) % 60;
                trail.record(origin, yaw);
            }
            if (tick % 5 != 0) continue;
            int key = (int) clock;
            var positions = new Vector3f[count];
            var rotations = new Quaternionf[count];
            for (int bone = 0; bone < count; bone++) {
                positions[bone] = position(frame(ground, key, bone)).lerp(position(frame(ground, key + 1, bone)), clock - key);
                rotations[bone] = rotation(frame(ground, key, bone)).slerp(rotation(frame(ground, key + 1, bone)), clock - key);
            }
            chain.follow(positions, rotations, origin, yaw, 1, trail::sample);
            var posed = new java.util.ArrayList<Solid>();
            for (int i = 0; i < joints.size(); i++) {
                int bone = joints.get(i).bone();
                posed.add(solids.get(i).transform(positions[bone], rotations[bone]));
            }
            for (int i = 0; i < joints.size(); i++) for (int j = i + 2; j < joints.size(); j++) {
                assertFalse(overlaps(posed.get(i), posed.get(j)),
                        "Patrol must avoid non-neighbouring body solids: tick=" + tick + ", sections=" + i + "/" + j);
            }
        }
        assertTrue(travelled > 80, "Self avoidance must keep patrolling instead of permanently stopping: " + travelled);
    }

    private static JsonArray numbers(float... values) {
        var result = new JsonArray();
        for (float value : values) result.add(value);
        return result;
    }

    /** Separating-axis check on the actual cuboids, including the axes from their edge pairs. */
    private static boolean overlaps(Solid a, Solid b) {
        var axes = new java.util.ArrayList<>(a.axes);
        axes.addAll(b.axes);
        for (var x : a.axes) for (var y : b.axes) {
            Vector3f cross = x.cross(y, new Vector3f());
            if (cross.lengthSquared() > .000001F) axes.add(cross.normalize());
        }
        for (var axis : axes) {
            float minA = Float.POSITIVE_INFINITY, maxA = Float.NEGATIVE_INFINITY;
            float minB = Float.POSITIVE_INFINITY, maxB = Float.NEGATIVE_INFINITY;
            for (var v : a.vertices) { minA = Math.min(minA, v.dot(axis)); maxA = Math.max(maxA, v.dot(axis)); }
            for (var v : b.vertices) { minB = Math.min(minB, v.dot(axis)); maxB = Math.max(maxB, v.dot(axis)); }
            if (maxA < minB - .0001F || maxB < minA - .0001F) return false;
        }
        return true;
    }

    private static Vector3f vector(JsonArray p) {
        return new Vector3f(p.get(0).getAsFloat(), p.get(1).getAsFloat(), p.get(2).getAsFloat());
    }

    private static int bone(JsonArray names, String name) {
        for (int i = 0; i < names.size(); i++) if (names.get(i).getAsString().equals(name)) return i;
        throw new AssertionError(name);
    }

    private static JsonObject frame(JsonArray frames, int tick, int bone) {
        return frames.get(tick).getAsJsonArray().get(bone).getAsJsonObject();
    }

    private static Vector3f position(JsonObject pose) {
        var p = pose.getAsJsonArray("p");
        return new Vector3f(p.get(0).getAsFloat(), p.get(1).getAsFloat(), p.get(2).getAsFloat());
    }

    private static Quaternionf rotation(JsonObject pose) {
        var q = pose.getAsJsonArray("q");
        return new Quaternionf(q.get(0).getAsFloat(), q.get(1).getAsFloat(), q.get(2).getAsFloat(), q.get(3).getAsFloat());
    }
}
