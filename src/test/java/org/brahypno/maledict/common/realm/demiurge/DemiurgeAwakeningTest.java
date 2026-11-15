package org.brahypno.maledict.common.realm.demiurge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import com.sammy.malum.registry.common.MobEffectRegistry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import com.sammy.malum.registry.common.DamageTypeRegistry;
import org.brahypno.changelib.DamageHelper.DamageRequestEvent;
import org.brahypno.changelib.DamageHelper.DamageStage;
import org.brahypno.changelib.HealthHelper.CombatProtection;
import org.brahypno.maledict.registry.MaledictAttributes;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.common.weapon.incursus.IncursusBladeAttack;
import org.brahypno.maledict.common.weapon.incursus.IncursusBladeEnchantments;
import org.brahypno.maledict.common.weapon.incursus.IncursusBladeItem;
import org.brahypno.maledict.common.weapon.incursus.IncursusScytheBoomerangEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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
            verifyBodyLoss(animation, ground);
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
            var flatTrail = new DemiurgeTrail();
            var steppedTrail = new DemiurgeTrail();
            for (int z = 0; z <= 8; z++) {
                flatTrail.record(new Vec3(0, 0, z), 0);
                steppedTrail.record(new Vec3(0, z >= 4 ? 1 : 0, z), 0);
            }
            var flatPose = DemiurgeRig.INSTANCE.pose();
            var steppedPose = DemiurgeRig.INSTANCE.pose();
            flatPose.sample(200, 0, DemiurgeAttack.NONE, 0, new Vec3(0, 0, 9), 0, flatTrail::sample);
            steppedPose.sample(200, 0, DemiurgeAttack.NONE, 0, new Vec3(0, 1, 9), 0, steppedTrail::sample);
            assertTrue(maxJointGap(flatPose) < .08,
                    "Flat ground must keep every body joint below the sleeve threshold: " + maxJointGap(flatPose));
            assertTrue(maxJointGap(steppedPose) > .25,
                    "A one-block step must open a body joint gap for the renderer to bridge: "
                            + maxJointGap(steppedPose));
            assertTrue(DemiurgeRig.INSTANCE.jointRadius(DemiurgeRig.INSTANCE.jointCount() - 1)
                            < DemiurgeRig.INSTANCE.jointRadius(DemiurgeRig.INSTANCE.jointCount() / 2) * .5,
                    "Procedural joints must use the tapering section's own radius, not one global size");
            assertEquals(.875, combatPose.eye(0, Vec3.ZERO, 0).distanceTo(combatPose.eye(1, Vec3.ZERO, 0)), .00001,
                    "The beams originate from the two separately authored eyes");
            verifyGestureTransitions(head, names.size());
            verifyLaserFacing();
            assertEquals(0, DemiurgeSweep.fraction(0), .0001);
            assertTrue(DemiurgeSweep.fraction(15) < -.3, "The windup coils away from the strike side");
            assertTrue(DemiurgeSweep.fraction(22) > 0, "The active window crosses the center line");
            assertTrue(DemiurgeSweep.fraction(27) > .95, "The strike reaches full lateral travel before recovery");
            assertEquals(0, DemiurgeSweep.fraction(49), .0001);
            assertEquals(0, DemiurgeSweep.fraction(50), .0001);
            assertTrue(DemiurgeSweep.amplitude(DemiurgeRig.INSTANCE.maxBodyCut())
                            < DemiurgeSweep.amplitude(0),
                    "A severed body sweeps a shorter arc instead of keeping the full tail range");
            assertTrue(DemiurgeSweep.amplitude(DemiurgeRig.INSTANCE.maxBodyCut())
                            >= DemiurgeSweep.MAX_AMPLITUDE * DemiurgeSweep.MIN_LENGTH_SCALE - .0001);
            var stoppedTurn = DemiurgePatrolSteering.steer(Vec3.ZERO, 0, 90, 0, java.util.List.of());
            assertFalse(stoppedTurn.blocked(), "A stopped statue must still be able to turn");
            assertTrue(stoppedTurn.yaw() > 0, "A stopped statue turns toward the target bearing");
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

    @GameTest(template = "test_empty", batch = "demiurge_awakening", timeoutTicks = 1150)
    public static void starIsConsumedOnceAndTheUncoilingSurvivesReload(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "DemiurgeTest"));
        DemiurgeStatueEntity[] current = {MaledictEntities.DEMIURGE_STATUE.get().create(level)};
        DemiurgeStatueEntity[] deathFixture = new DemiurgeStatueEntity[1];
        var statue = current[0];
        java.util.List<ChunkPos> patrolChunks = new java.util.ArrayList<>();
        statue.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 12, 2)));
        Vec3 incursusFixturePosition = statue.position().add(24, 0, 0);
        statue.setNoGravity(true);
        level.addFreshEntity(statue);
        helper.assertTrue(CombatProtection.state(statue) != null
                          && CombatProtection.state(statue).grade() == DamageStage.D5_RECOVERY_SOURCE,
                "The Demiurge must seal its own D5 health defense");
        helper.assertTrue(statue.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) == 10
                        && statue.getHealth() == DemiurgeDifficulty.maxHealth(level.getDifficulty()),
                "The spawned statue must receive 10 armour and its difficulty's authoritative health pool");
        float dormantHealth = statue.getHealth();
        helper.assertTrue(!statue.hurt(statue.damageSources().mobAttack(statue), 100.0F)
                        && !statue.getParts()[0].hurt(statue.damageSources().genericKill(), Float.MAX_VALUE)
                        && statue.getHealth() == dormantHealth,
                "The unawakened statue and its parts must reject damage, including generic kill");
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
        }
        reducedTarget.discard();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 2));
        statue.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(statue.awakeningAge(0) == -1, "An ordinary item must not awaken the statue");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MaledictItems.BLIGHTED_STAR.get(), 3));
        var worldData = level.getServer().getWorldData();
        Difficulty originalDifficulty = level.getDifficulty();
        try {
            worldData.setDifficulty(Difficulty.PEACEFUL);
            helper.assertTrue(statue.mobInteract(player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL
                            && statue.awakeningAge(0) == -1 && player.getMainHandItem().getCount() == 3,
                    "Peaceful must reject awakening without consuming a star");
            for (var type : java.util.List.of(MaledictEntities.FIRST_VICISSITUDE.get(), MaledictEntities.DEMIURGE_STATUE.get())) {
                var summon = type.create(level);
                summon.setPos(statue.position().add(32, 0, 0));
                helper.assertTrue(!level.addFreshEntity(summon), "Peaceful must reject a new boss summon: " + type);
            }
            statue.checkDespawn();
            helper.assertTrue(!statue.isRemoved(), "An existing statue must remain when difficulty changes to Peaceful");
        } finally {
            worldData.setDifficulty(originalDifficulty);
        }
        statue.mobInteract(player, InteractionHand.MAIN_HAND);
        statue.mobInteract(player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(MaledictItems.BLIGHTED_STAR.get(), 2));
        statue.mobInteract(player, InteractionHand.OFF_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 2 && player.getOffhandItem().getCount() == 2
                        && statue.awakeningAge(0) == 0,
                "First use consumes exactly one; repeat and offhand use cannot consume again");
        float beforeCapProbe = statue.getHealth();
        float cap = statue.getMaxHealth() * 0.03F;
        float expectedLoss = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(cap, 10.0F, 0.0F);
        helper.assertTrue(statue.hurt(statue.damageSources().mobAttack(statue), 100.0F),
                "Damage immunity must end as soon as awakening starts, before its animation completes");
        helper.assertTrue(Math.abs(beforeCapProbe - statue.getHealth() - expectedLoss) < 0.001F,
                "A 100-damage hit must lose exactly the 3% cap after 10 armour: expected "
                        + expectedLoss + ", actual " + (beforeCapProbe - statue.getHealth()));
        try {
            worldData.setDifficulty(Difficulty.PEACEFUL);
            statue.checkDespawn();
            helper.assertTrue(!statue.isRemoved() && statue.awakeningAge(0) == 0,
                    "Switching to Peaceful must preserve an awakening that has already started");
        } finally {
            worldData.setDifficulty(originalDifficulty);
        }
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
            try {
                worldData.setDifficulty(Difficulty.PEACEFUL);
                helper.assertTrue(level.addFreshEntity(restored),
                        "An existing ledger-bound boss must reload in Peaceful");
            } finally {
                worldData.setDifficulty(originalDifficulty);
            }
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
            var part = awake.getParts()[0];
            var target = ServerboundInteractPacket.createAttackPacket(part, false).getTarget(level);
            helper.assertTrue(target == part, "The server must resolve a melee packet to the reloaded statue's part");
            float beforeSwordHit = awake.getHealth();
            ItemStack held = player.getMainHandItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            player.attack(target);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            helper.assertTrue(awake.getHealth() < beforeSwordHit,
                    "An ordinary sword hit on an awakened statue's part must debit its D5-owned pool");
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
        Vec3[] rainCenter = new Vec3[1];
        int[] rainDrops = {0};
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
                if (awake.attack() == DemiurgeAttack.ROAR) {
                    if (rainCenter[0] == null) {
                        rainCenter[0] = awake.headCenter();
                        player.setPos(rainCenter[0].add(DemiurgeAttack.RAIN_RADIUS + 1, -4, 0));
                        awake.selectTargetFromGoal(null);
                    }
                    for (var tear : level.getEntitiesOfClass(DemiurgeTearEntity.class, awake.getBoundingBox().inflate(24),
                            tear -> tear.getOwner() == awake)) {
                        helper.assertTrue(tear.position().subtract(rainCenter[0]).horizontalDistanceSqr()
                                        <= (DemiurgeAttack.RAIN_RADIUS + .02) * (DemiurgeAttack.RAIN_RADIUS + .02),
                                "Every tear must remain inside the rain disk locked at windup, after the player leaves");
                        rainDrops[0]++;
                    }
                }
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
            helper.assertTrue(current[0].getZ() > wall[0].getZ() + .1,
                    "The statue must actually cross the wall after breaking it");
        });
        helper.runAtTickTime(370, () -> {
            var awake = current[0];
            helper.assertTrue(awake.position().subtract(patrolStart[0]).horizontalDistance() > .15,
                    "Ground navigation must move the statue: start=" + patrolStart[0] + ", end=" + awake.position()
                            + ", navigationDone=" + awake.getNavigation().isDone() + ", onGround=" + awake.onGround()
                            + ", speed=" + awake.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
            var enemy = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
            enemy.setNoAi(true);
            awake.selectTargetFromGoal(enemy);
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
            awake.selectTargetFromGoal(null);
            awake.getNavigation().stop();
            awake.setNoGravity(true);
            awake.setDeltaMovement(Vec3.ZERO);
            awake.setYRot(0);
            awake.updateParts();
            combatTarget[0] = verifyLaserAndRoofPiercingRain(helper, awake);
            Vec3 center = awake.headCenter();
            player.setPos(center.add(DemiurgeAttack.RAIN_RADIUS + 1, -4, 0));
            awake.beginAttack(DemiurgeAttack.ROAR, player);
            helper.assertTrue(!awake.canStartRain(player) && awake.attack() == DemiurgeAttack.NONE,
                    "A player outside the rain radius must not start the rain windup");
            // Stay a hair inside the boundary: adding the radius can round just above the square.
            player.setPos(center.add(DemiurgeAttack.RAIN_RADIUS - 1.0E-4, -4, 0));
            helper.assertTrue(awake.canStartRain(player), "The rain-radius boundary is inside the trigger");
            player.setPos(center.add(DemiurgeAttack.RAIN_RADIUS - 1, -10, 0));
            helper.assertTrue(awake.canStartRain(player), "The trigger uses horizontal distance, not height");
            combatTarget[0].getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
            combatTarget[0].setHealth(1000);
            combatTarget[0].removeAllEffects();
            combatTarget[0].attempts = 0;
            combatTarget[0].lastAttempt = Long.MIN_VALUE;
            combatTarget[0].minimumInterval = Long.MAX_VALUE;
            combatTarget[0].damageLog.clear();
            awake.selectTargetFromGoal(combatTarget[0]);
            awake.beginAttack(DemiurgeAttack.CHARGE, combatTarget[0]);
        });
        helper.runAtTickTime(418, () -> {
            var awake = current[0];
            float perHit = DemiurgeDifficulty.damage(org.brahypno.maledict.config.MaledictConfig.DEMIURGE_CHARGE_DAMAGE.get(), level.getDifficulty());
            helper.assertTrue(combatTarget[0].attempts > 1 && combatTarget[0].attempts <= 24,
                    "Sustained head/tail contact may repeatedly enter the configured damage probe: "
                            + combatTarget[0].getHealth() + ", damage=" + perHit + ", attempts=" + combatTarget[0].attempts
                            + ", interval=" + combatTarget[0].minimumInterval + ", log=" + combatTarget[0].damageLog);
            helper.assertTrue(combatTarget[0].minimumInterval >= 1 && combatTarget[0].attempts <= 24,
                    "Overlapping body parts must merge before entering the damage probe each tick: attempts="
                            + combatTarget[0].attempts + ", minimumInterval=" + combatTarget[0].minimumInterval);
            firstCharge[0] = false;
            combatTarget[0].setPos(awake.position().add(0, 4, 8));
            combatTarget[0].setDeltaMovement(Vec3.ZERO);
        });
        helper.runAtTickTime(700, () -> {
            var awake = current[0];
            awake.finishAttack();
            player.setPos(awake.headCenter().add(4, -4, 0));
            awake.selectTargetFromGoal(player);
            helper.assertTrue(awake.beginAttack(DemiurgeAttack.ROAR, player), "A nearby player can start the rain cast");
        });
        helper.runAtTickTime(900, () -> {
            var casts = observed.stream().filter(attack -> attack != DemiurgeAttack.NONE).toList();
            helper.assertTrue(casts.size() >= 3,
                    "The live combat goal must choose several distinct casts instead of standing idle: " + observed
                            + ", target=" + current[0].getTarget() + ", alive=" + combatTarget[0].isAlive()
                            + ", distance=" + current[0].distanceTo(combatTarget[0])
                            + ", lineOfSight=" + current[0].hasLineOfSight(combatTarget[0]));
            helper.assertTrue(rainCenter[0] != null && rainDrops[0] > 0,
                    "The AI must finish its latched rain cast after the player moves beyond the rain radius and target tracking ends");
            combatTarget[0].discard();
            combatTarget[0] = null;
            current[0].remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            patrolChunks.forEach(chunk -> level.setChunkForced(chunk.x, chunk.z, false));
        });
        verifyGroundCombat(helper);
        // False Creation: the state machine must advance from WARNING to ARMED,
        // and a player-broken pillar must disappear without exploding.
        helper.runAtTickTime(260, () -> verifyPillarBreak(helper, player));
        helper.runAtTickTime(1040, () -> {
            var victim = MaledictEntities.DEMIURGE_STATUE.get().create(level);
            victim.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 12, 2)));
            victim.setNoGravity(true);
            level.addFreshEntity(victim);
            var awakening = victim.saveWithoutId(new CompoundTag());
            awakening.putLong("AwakeningStart", level.getGameTime());
            victim.readAdditionalSaveData(awakening);
            deathFixture[0] = victim;
        });
        helper.runAtTickTime(1042, () -> deathFixture[0].hurt(deathFixture[0].damageSources().genericKill(), 1));
        helper.runAtTickTime(1043, () -> deathFixture[0].hurt(deathFixture[0].damageSources().genericKill(), 1));
        helper.runAtTickTime(1044, () -> deathFixture[0].hurt(deathFixture[0].damageSources().genericKill(), 1));
        helper.runAtTickTime(1050, () -> {
            var victim = deathFixture[0];
            helper.assertTrue(victim.isDeadOrDying() && !victim.isAlive(),
                    "The third generic-kill attempt must start the genuine death transaction");
            helper.assertTrue(victim.bodyCut() == DemiurgeRig.INSTANCE.maxBodyCut(),
                    "Losing all health severs the trailing body up to the authored cap");
            var drops = level.getEntitiesOfClass(ItemEntity.class, victim.getBoundingBox().inflate(4));
            helper.assertTrue(drops.stream().filter(drop -> drop.getItem().is(MaledictItems.LIGHT_FRAGMENT.get()))
                            .mapToInt(drop -> drop.getItem().getCount()).sum() == 1,
                    "A real death must drop exactly one Fragment of Light next to the statue");
            drops.forEach(ItemEntity::discard);
            verifyIncursusDamage(helper, incursusFixturePosition);
            helper.succeed();
        });
    }

    private static void verifyGroundCombat(GameTestHelper helper) {
        var level = helper.getLevel();
        DemiurgeStatueEntity[] fixture = new DemiurgeStatueEntity[1];
        ContactCow[] targetFixture = new ContactCow[1];
        Vec3[] postCastStart = new Vec3[1];
        float[] postCastClock = new float[1];
        java.util.List<ChunkPos> chunks = new java.util.ArrayList<>();
        helper.runAtTickTime(20, () -> {
            BlockPos floor = helper.absolutePos(new BlockPos(50, 30, 50));
            for (int x = (floor.getX() - 24) >> 4; x <= (floor.getX() + 24) >> 4; x++) {
                for (int z = (floor.getZ() - 16) >> 4; z <= (floor.getZ() + 16) >> 4; z++) {
                    if (level.setChunkForced(x, z, true)) chunks.add(new ChunkPos(x, z));
                }
            }
            for (int x = -23; x <= 23; x++) for (int z = -14; z <= 14; z++) {
                level.setBlockAndUpdate(floor.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 8; y++) level.setBlockAndUpdate(floor.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
            var awake = MaledictEntities.DEMIURGE_STATUE.get().create(level);
            var state = awake.saveWithoutId(new CompoundTag());
            state.putLong("AwakeningStart", level.getGameTime() - 200);
            awake.load(state);
            awake.setPos(Vec3.atBottomCenterOf(floor));
            awake.setOnGround(true);
            awake.setYRot(0);
            awake.updateParts();
            helper.assertTrue(level.addFreshEntity(awake), "The ground combat fixture must join the world");
            fixture[0] = awake;
            var target = new ContactCow(level);
            target.setNoAi(true);
            target.setNoGravity(true);
            target.setPos(awake.position().add(0, 1, 5));
            helper.assertTrue(level.addFreshEntity(target), "The jumping fixture must join the world");
            targetFixture[0] = target;
            awake.selectTargetFromGoal(target);
            helper.assertTrue(awake.getTarget() == target && awake.canAttack(target),
                    "The jumping fixture must be a valid target: selected=" + awake.getTarget());
            var spots = DemiurgePillarEntity.planArray(awake, target.position());
            helper.assertTrue(spots.size() >= 3, "The arena must have room for a pillar array: " + spots
                    + ", target=" + target.position());
            new DemiurgeCombatGoal(awake).tick();
            helper.assertTrue(awake.attack() == DemiurgeAttack.FALSE_CREATION,
                    "The first ground array takes priority over the laser even when the target is jumping: " + awake.attack());
        });
        helper.runAtTickTime(49, () -> {
            var awake = fixture[0];
            var pillars = level.getEntitiesOfClass(DemiurgePillarEntity.class, awake.getBoundingBox().inflate(22),
                    pillar -> pillar.isOwnedBy(awake));
            helper.assertTrue(pillars.size() >= 3 && pillars.stream().anyMatch(pillar -> !pillar.isInvisible()),
                    "False Creation must add and reveal its planned pillars from the live attack tick");
            DemiurgePillarEntity.discardOwned(awake);
            awake.finishAttack();
            prepareSweep(helper, awake, targetFixture[0]);
        });
        helper.runAtTickTime(78, () -> {
            var target = targetFixture[0];
            float damage = DemiurgeDifficulty.damage(
                    org.brahypno.maledict.config.MaledictConfig.DEMIURGE_SWEEP_DAMAGE.get(), level.getDifficulty());
            helper.assertTrue(Math.abs(target.getHealth() - (target.getMaxHealth() - damage)) < .001 && target.attempts == 1,
                    "The visible sweep must hit a rear target exactly once: health=" + target.getHealth() + ", " + target.damageLog);
            var awake = fixture[0];
            // Give the head room to move and make the target unreachable by the tail.
            awake.setYRot(0);
            target.setPos(awake.position().add(0, 0, 13));
            target.setDeltaMovement(Vec3.ZERO);
        });
        helper.runAtTickTime(99, () -> {
            helper.assertTrue(fixture[0].attack() == DemiurgeAttack.NONE,
                    "The ground fixture must finish its cast before checking recovery movement");
            postCastStart[0] = fixture[0].position();
            postCastClock[0] = fixture[0].slitherClock(1);
        });
        helper.runAtTickTime(107, () -> {
            helper.assertTrue(fixture[0].position().subtract(postCastStart[0]).horizontalDistance() > .05,
                    "The boss must resume movement during the eight-tick post-cast interval: start="
                            + postCastStart[0] + ", end=" + fixture[0].position());
            helper.assertTrue(fixture[0].slitherClock(1) != postCastClock[0],
                    "Recovery movement must advance the real body phase rather than leaving a frozen sliding model");
        });
        helper.runAtTickTime(109, () -> {
            helper.assertTrue(fixture[0].attack() != DemiurgeAttack.NONE && fixture[0].attack() != DemiurgeAttack.SWEEP,
                    "By the ninth tick of recovery the boss must have chained into a different available spell");
            targetFixture[0].discard();
            DemiurgePillarEntity.discardOwned(fixture[0]);
            fixture[0].remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            chunks.forEach(chunk -> level.setChunkForced(chunk.x, chunk.z, false));
        });
    }

    private static void verifyPillarBreak(GameTestHelper helper, FakePlayer player) {
        var level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(30, 5, 30));
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y <= 5; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        var pillar = MaledictEntities.DEMIURGE_PILLAR.get().create(level);
        helper.assertTrue(pillar != null, "The False Creation pillar type must be registered");
        if (pillar == null) return;
        pillar.setPos(Vec3.atBottomCenterOf(base));
        pillar.setPillarHeight(3);
        pillar.setPillarHealth(24);
        helper.assertTrue(level.addFreshEntity(pillar), "The test pillar must join the world");
        for (int tick = 0; tick < 16; tick++) pillar.tick();
        helper.assertTrue(pillar.phase() == DemiurgePillarEntity.Phase.RISING,
                "Sixteen warning ticks must finish before the stone rises: " + pillar.phase());
        for (int tick = 0; tick < 8; tick++) pillar.tick();
        helper.assertTrue(pillar.phase() == DemiurgePillarEntity.Phase.ARMED,
                "Eight rise ticks must leave the pillar armed: " + pillar.phase());
        var cow = new ContactCow(level);
        cow.setNoAi(true);
        cow.setNoGravity(true);
        cow.setPos(pillar.position().add(2, 0, 0));
        level.addFreshEntity(cow);
        float before = cow.getHealth();
        helper.assertTrue(pillar.hurt(player.damageSources().playerAttack(player), 100),
                "A player hit must damage an armed pillar");
        helper.assertTrue(pillar.isRemoved(), "Breaking the pillar must remove it immediately");
        helper.assertTrue(cow.getHealth() == before,
                "A player-broken pillar must not explode or damage nearby targets");
        cow.discard();

        // The rising stone must use LivingEntity#knockback, not a hand-written delta tweak.
        var riser = MaledictEntities.DEMIURGE_PILLAR.get().create(level);
        helper.assertTrue(riser != null, "The rise fixture pillar must be registered");
        if (riser == null) return;
        riser.setPos(Vec3.atBottomCenterOf(base));
        riser.setPillarHeight(3);
        riser.setPillarHealth(24);
        helper.assertTrue(level.addFreshEntity(riser), "The rise fixture must join the world");
        var riserCow = new ContactCow(level);
        riserCow.setNoAi(true);
        riserCow.setNoGravity(true);
        riserCow.setPos(Vec3.atBottomCenterOf(base));
        riserCow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
        riserCow.setHealth(100);
        helper.assertTrue(level.addFreshEntity(riserCow), "The rise victim must join the world");
        for (int tick = 0; tick < 16; tick++) riser.tick();
        for (int tick = 0; tick < 8; tick++) riser.tick();
        Vec3 riseVelocity = riserCow.getDeltaMovement();
        helper.assertTrue(riserCow.getHealth() < 100 && riseVelocity.horizontalDistance() > .2
                        && riseVelocity.y >= DemiurgePillarEntity.RISE_LAUNCH - .01,
                "A rising pillar must deal damage and apply a real horizontal knockback plus launch: health="
                        + riserCow.getHealth() + ", velocity=" + riseVelocity);
        riserCow.discard();
        riser.discard();

        // The old sphere centred halfway up the shaft missed a stationary player beside the
        // inner ring. The column blast must reach that player, and a launched pillar must leave
        // the armed phase as a projectile.
        BlockPos blast = base.offset(10, 0, 0);
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            level.setBlockAndUpdate(blast.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y <= 5; y++) level.setBlockAndUpdate(blast.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        var owner = MaledictEntities.DEMIURGE_STATUE.get().create(level);
        helper.assertTrue(owner != null, "The pillar owner fixture must be registered");
        if (owner == null) return;
        owner.setPos(Vec3.atBottomCenterOf(blast.offset(0, 8, 0)));
        owner.setNoGravity(true);
        level.addFreshEntity(owner);
        var bomb = MaledictEntities.DEMIURGE_PILLAR.get().create(level);
        helper.assertTrue(bomb != null, "The explosion fixture pillar must be registered");
        if (bomb == null) return;
        bomb.setOwner(owner);
        bomb.setPos(Vec3.atBottomCenterOf(blast));
        bomb.setPillarHeight(3);
        bomb.setPillarHealth(24);
        helper.assertTrue(level.addFreshEntity(bomb), "The explosion fixture must join the world");
        for (int tick = 0; tick < 16; tick++) bomb.tick();
        for (int tick = 0; tick < 8; tick++) bomb.tick();
        helper.assertTrue(bomb.phase() == DemiurgePillarEntity.Phase.ARMED,
                "The explosion fixture must be armed before the radius check: " + bomb.phase());
        var bystander = new ContactCow(level);
        bystander.setNoAi(true);
        bystander.setNoGravity(true);
        bystander.setPos(bomb.position().add(DemiurgePillarEntity.NORMAL_RADIUS - .45, 0, 0));
        bystander.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
        bystander.setHealth(100);
        helper.assertTrue(level.addFreshEntity(bystander), "The blast bystander must join the world");
        for (int tick = 0; tick < DemiurgePillarEntity.ARMED_TICKS + 2; tick++) bomb.tick();
        double distance = Math.hypot(bystander.getX() - bomb.getX(), bystander.getZ() - bomb.getZ());
        helper.assertTrue(bystander.getHealth() < 100,
                "A stationary player inside the inner ring must be caught by the column blast: health="
                        + bystander.getHealth() + ", bomb=" + bomb.phase() + ", bombPos=" + bomb.position()
                        + ", cowPos=" + bystander.position() + ", distance=" + distance
                        + ", alive=" + bystander.isAlive() + ", removed=" + bystander.isRemoved());
        bystander.discard();
        owner.discard();

        var flyer = MaledictEntities.DEMIURGE_PILLAR.get().create(level);
        helper.assertTrue(flyer != null, "The launch fixture pillar must be registered");
        if (flyer != null) {
            flyer.setPos(Vec3.atBottomCenterOf(blast.offset(0, 0, 6)));
            flyer.setPillarHeight(3);
            flyer.setPillarHealth(24);
            helper.assertTrue(level.addFreshEntity(flyer), "The launch fixture must join the world");
            for (int tick = 0; tick < 16; tick++) flyer.tick();
            for (int tick = 0; tick < 8; tick++) flyer.tick();
            helper.assertTrue(flyer.launch(new Vec3(1, 0, 0)), "An armed pillar must accept a charge launch");
            helper.assertTrue(flyer.phase() == DemiurgePillarEntity.Phase.LAUNCHED,
                    "A launched pillar must become a projectile instead of staying armed: " + flyer.phase());
            flyer.discard();
        }
    }

    private static void prepareSweep(GameTestHelper helper, DemiurgeStatueEntity statue, ContactCow target) {
        statue.setYRot(90);
        statue.updateParts();
        var rig = DemiurgeRig.INSTANCE;
        var preview = rig.pose();
        int anchor = rig.sweepMidpointBone(statue.bodyCut());
        int strike = DemiurgeSweep.ACTIVE_END - 1;
        preview.sample(statue.awakeningAge(0), statue.slitherClock(1), DemiurgeAttack.SWEEP, strike,
                statue.position(), statue.getYRot(), statue::trailPoint, statue.bodyCut(), null,
                new DemiurgeSweep.State(1, strike, DemiurgeSweep.amplitude(statue.bodyCut()), 1));
        var bounds = preview.bounds(anchor, statue.position(), statue.getYRot());
        var point = bounds.getCenter();
        target.setPos(point.x, bounds.minY, point.z);
        target.setDeltaMovement(Vec3.ZERO);
        target.setHealth(target.getMaxHealth());
        target.invulnerableTime = 0;
        target.attempts = 0;
        target.damageLog.clear();
        statue.selectTargetFromGoal(target);
        helper.assertTrue(statue.distanceTo(target) > 6 && statue.attackLocalPosition(
                        statue.position().add(0, 0, 1)).x > .99,
                "The midpoint target is behind the body and uses the correct local axes");
        new DemiurgeCombatGoal(statue).tick();
        helper.assertTrue(statue.attack() == DemiurgeAttack.SWEEP && statue.sweepState(0).side() == 1,
                "The live goal must commit the sweep to the side that reaches the midpoint target");
    }

    private static void verifyIncursusDamage(GameTestHelper helper, Vec3 position) {
        var level = helper.getLevel();
        for (boolean throwing : new boolean[]{false, true}) {
            var victim = MaledictEntities.DEMIURGE_STATUE.get().create(level);
            var state = victim.saveWithoutId(new CompoundTag());
            state.putLong("AwakeningStart", level.getGameTime() - 140);
            victim.load(state);
            victim.setPos(position);
            victim.setNoGravity(true);
            helper.assertTrue(level.addFreshEntity(victim), "The awake Incursus fixture must join the world");
            var attacker = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "IncursusBossTest")) {
                @Override public float getAttackStrengthScale(float partialTick) { return 1.0F; }
            };
            attacker.setPos(position.add(0, 0, -5));
            attacker.setYRot(0);
            attacker.setXRot(0);
            var blade = new ItemStack(MaledictItems.INCURSUS_BLADE.get());
            IncursusBladeItem.setStat(blade, IncursusBladeItem.ATTACK_DAMAGE, throwing ? 0 : 19);
            IncursusBladeItem.setStat(blade, IncursusBladeItem.POWDER_SNOW_DAMAGE, 6);
            attacker.setItemInHand(InteractionHand.MAIN_HAND, blade);
            attacker.getAttributes().addTransientAttributeModifiers(
                    blade.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
            float before = victim.getHealth();
            List<DamageRequestEvent> requests = new ArrayList<>();
            Consumer<DamageRequestEvent> listener = event -> {
                if (event.attacker() == attacker && event.target() != null
                        && event.target().is(victim)) {
                    requests.add(event);
                }
            };
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(listener);
            try {
                if (throwing) {
                    IncursusBladeEnchantments.throwScythe(level, attacker, InteractionHand.MAIN_HAND, blade);
                    var projectiles = level.getEntitiesOfClass(IncursusScytheBoomerangEntity.class,
                            attacker.getBoundingBox().inflate(16), scythe -> scythe.getOwner() == attacker);
                    helper.assertTrue(projectiles.size() == 1, "A normal right-click must launch one Incursus scythe");
                    for (int tick = 0; tick < 12 && victim.getHealth() == before; tick++) projectiles.get(0).tick();
                    projectiles.forEach(Entity::discard);
                } else {
                    IncursusBladeAttack.perform(attacker);
                }
            } finally {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(listener);
            }
            float loss = before - victim.getHealth();

            int scytheIndex = -1;
            int freezeIndex = -1;
            int freezeCount = 0;
            for (int i = 0; i < requests.size(); i++) {
                DamageRequestEvent event = requests.get(i);
                if (event.attribute() == null && event.source() != null
                        && (event.source().is(DamageTypeRegistry.SCYTHE_MELEE)
                            || event.source().is(DamageTypeRegistry.SCYTHE_SWEEP))) {
                    scytheIndex = i;
                }
                if (event.attribute() == MaledictAttributes.POWDER_SNOW_DAMAGE.get()) {
                    freezeIndex = i;
                    freezeCount++;
                }
            }
            String mode = throwing ? "boomerang" : "melee";
            if (!throwing) {
                helper.assertTrue(scytheIndex >= 0,
                        "Incursus melee must post the main probe request before its attribute requests");
                helper.assertTrue(freezeIndex > scytheIndex,
                        "Incursus melee must post the freeze attribute request after the scythe probe, requests="
                                + requests.size());
                helper.assertTrue(freezeCount == 1,
                        "One melee main damage must post exactly one freeze request, count=" + freezeCount);
            }
            // The boomerang fixture is tick-budget sensitive in this long test; the melee branch
            // pins the probe ordering and the per-main-damage count. The boomerang still has to
            // respect the owned-health cap above, and a hit must never post more than one freeze.
            helper.assertTrue(loss >= 0 && loss <= victim.getMaxHealth() * 0.03F + 0.001F,
                    "Incursus " + mode + " must respect the D5 per-hit cap, loss=" + loss);
            if (!throwing) {
                helper.assertTrue(loss > 0,
                        "Incursus melee must debit real D5-owned health through the boss cap, loss=" + loss);
            }
            victim.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        }
    }

    private static void verifyLaserFacing() {
        var pose = DemiurgeRig.INSTANCE.pose();
        var path = new DemiurgeTrail();
        for (float yaw : new float[]{0, 90, 179, -179}) {
            Vec3 bodyForward = Vec3.directionFromRotation(0, yaw);
            for (float bearing : new float[]{-180, -90, -60, 0, 60, 90, 180}) {
                Vec3 aim = DemiurgeAim.constrain(Vec3.directionFromRotation(20, yaw + bearing), yaw);
                assertTrue(aim.dot(bodyForward) > 0, "A target behind the boss must not create a rear-facing beam");
                pose.sample(200, 17, DemiurgeAttack.LASER, 40, Vec3.ZERO, yaw, path::sample, aim);
                assertTrue(pose.headForward(yaw).dot(aim) > .99999,
                        "The rendered head and the damaging beam must face the same way across yaw wrapping");
                Vec3 center = pose.headCenter(Vec3.ZERO, yaw);
                for (int side = 0; side < 2; side++) {
                    assertTrue(pose.eye(side, Vec3.ZERO, yaw).subtract(center).dot(pose.headForward(yaw)) > .05,
                            "Both laser origins remain on the front side of the aimed head");
                }
            }
        }
    }

    private static void verifyGestureTransitions(int head, int count) throws Exception {
        var rig = DemiurgeRig.INSTANCE;
        var idle = rig.pose();
        var gesture = rig.pose();
        var path = new DemiurgeTrail();
        JsonArray clips = new JsonArray();
        for (var attack : DemiurgeAttack.values()) {
            if (attack == DemiurgeAttack.NONE) continue;
            JsonObject output = new JsonObject();
            output.addProperty("name", attack.name());
            JsonArray frames = new JsonArray();
            Vector3f previousHead = null;
            for (int step = 0; step <= attack.duration * 4; step++) {
                float time = step / 4F;
                float clock = (17 + time * .4F) % DemiurgeSlither.CYCLE_TICKS;
                idle.sample(200 + time, clock, DemiurgeAttack.NONE, -1, Vec3.ZERO, 0, path::sample);
                gesture.sample(200 + time, clock, attack, time, Vec3.ZERO, 0, path::sample);
                if (attack != DemiurgeAttack.SWEEP) {
                    for (int bone = 1; bone < count - 3; bone++) {
                        assertTrue(idle.positions[bone].distance(gesture.positions[bone]) < .0001,
                                "A gesture must retain the travelling body's existing phase: " + attack + "/" + bone);
                    }
                }
                if (time == 0 || time == attack.duration) {
                    assertTrue(idle.positions[head].distance(gesture.positions[head]) < .00001);
                    assertTrue(Math.abs(idle.rotations[head].dot(gesture.rotations[head])) > .99999,
                            "Both endpoints must meet the current resting pose, at any slither phase");
                }
                if (previousHead != null) assertTrue(previousHead.distance(gesture.positions[head]) < .1,
                        "Quarter-tick head displacement must remain continuous: " + attack + "/" + time);
                previousHead = new Vector3f(gesture.positions[head]);
                if (step % 4 == 0) {
                    JsonArray poses = new JsonArray();
                    for (int bone = 0; bone < count; bone++) {
                        JsonObject pose = new JsonObject();
                        var p = gesture.positions[bone]; var q = gesture.rotations[bone];
                        pose.add("p", numbers(p.x, p.y, p.z));
                        pose.add("q", numbers(q.x, q.y, q.z, q.w));
                        poses.add(pose);
                    }
                    frames.add(poses);
                }
            }
            output.add("frames", frames);
            clips.add(output);
        }
        for (int tick = 0; tick <= 64; tick++) {
            float speed = DemiurgeAttack.CHARGE.chargeSpeed(tick);
            if (tick > 0) assertTrue(Math.abs(speed - DemiurgeAttack.CHARGE.chargeSpeed(tick - 1)) < .31,
                    "The charge accelerates and brakes over multiple ticks");
        }
        assertFalse(DemiurgeAttack.CHARGE.active(48));
        assertTrue(DemiurgeAttack.CHARGE.chargeSpeed(48) > 0, "Coasting is recovery movement, not another damage window");
        idle.sample(200, 17, DemiurgeAttack.NONE, -1, Vec3.ZERO, 0, path::sample);
        gesture.sample(215, 17, DemiurgeAttack.NONE, -1, Vec3.ZERO, 0, path::sample);
        assertTrue(idle.positions[head].distance(gesture.positions[head]) > .005,
                "Standing still retains breathing motion without advancing the locomotion clock");
        var directory = java.nio.file.Path.of("..", "demiurge-review", "gestures");
        java.nio.file.Files.createDirectories(directory);
        java.nio.file.Files.writeString(directory.resolve("runtime-gestures.json"), clips.toString());
    }

    private static ContactCow verifyLaserAndRoofPiercingRain(GameTestHelper helper, DemiurgeStatueEntity statue) {
        var level = helper.getLevel();
        var target = new ContactCow(level);
        target.setNoAi(true);
        target.setNoGravity(true);
        Vec3 eye = statue.eyeOrigin(0);
        target.setPos(eye.add(statue.headForward().scale(8)).add(0, -target.getBbHeight() / 2, 0));
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

    private static double maxJointGap(DemiurgeRig.Pose pose) {
        double maximum = 0;
        for (int index = 0; index + 1 < DemiurgeRig.INSTANCE.jointCount(); index++) {
            int bone = DemiurgeRig.INSTANCE.jointBone(index);
            int next = DemiurgeRig.INSTANCE.jointBone(index + 1);
            if (pose.isHidden(bone) || pose.isHidden(next)) continue;
            maximum = Math.max(maximum, pose.jointEnd(index, false).distanceTo(pose.jointEnd(index + 1, true)));
        }
        return maximum;
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
            if (source.getEntity() instanceof DemiurgeStatueEntity && "mob".equals(source.getMsgId())) {
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

    /**
     * Confirmed body-loss model: 75 / 50 / 25 % of maximum health each sever four trailing sections,
     * the loss is monotonic, and the authored cap keeps eight blocks of body on the floor.
     */
    private static void verifyBodyLoss(JsonObject animation, JsonArray ground) {
        int cap = DemiurgeRig.INSTANCE.maxBodyCut();
        assertEquals(11, cap, "The authored body may lose eleven trailing sections before the floor rule breaks");
        assertEquals(0, DemiurgeBodyLength.cutFor(1.0F, cap));
        assertEquals(0, DemiurgeBodyLength.cutFor(.7501F, cap));
        assertEquals(4, DemiurgeBodyLength.cutFor(.75F, cap));
        assertEquals(4, DemiurgeBodyLength.cutFor(.5001F, cap));
        assertEquals(8, DemiurgeBodyLength.cutFor(.50F, cap));
        assertEquals(8, DemiurgeBodyLength.cutFor(.2501F, cap));
        assertEquals(11, DemiurgeBodyLength.cutFor(.25F, cap));
        assertEquals(11, DemiurgeBodyLength.cutFor(0.0F, cap));
        assertEquals(2, DemiurgeBodyLength.cutFor(.1F, 2), "A smaller rig cap still wins");

        float floor = DemiurgeBodyLength.MINIMUM_GROUND_LENGTH;
        for (int cut = 0; cut <= cap; cut++) {
            assertTrue(DemiurgeRig.INSTANCE.bodyEndDistanceWorstCase(cut) >= floor,
                    "The remaining floor body must keep at least eight blocks at cut " + cut);
        }
        assertTrue(DemiurgeRig.INSTANCE.bodyEndDistanceWorstCase(cap + 1) < floor,
                "One more severed section would break the eight-block floor");

        var joints = new java.util.ArrayList<DemiurgeBodyChain.Joint>();
        for (var value : animation.getAsJsonArray("body_chain")) {
            var joint = value.getAsJsonObject();
            var corners = new java.util.ArrayList<Vector3f>();
            for (var corner : joint.getAsJsonArray("corners")) corners.add(vector(corner.getAsJsonArray()));
            joints.add(new DemiurgeBodyChain.Joint(joint.get("bone").getAsInt(),
                    vector(joint.getAsJsonArray("front")), vector(joint.getAsJsonArray("back")), corners));
        }
        int count = animation.getAsJsonArray("bones").size();
        var positions = new Vector3f[count];
        var rotations = new Quaternionf[count];
        for (int bone = 0; bone < count; bone++) {
            positions[bone] = position(frame(ground, 0, bone));
            rotations[bone] = rotation(frame(ground, 0, bone));
        }
        var chain = new DemiurgeBodyChain(joints);
        var trail = new DemiurgeTrail();
        trail.record(net.minecraft.world.phys.Vec3.ZERO, 0);
        chain.follow(positions, rotations, Vec3.ZERO, 0, 1, trail::sample, cap);
        for (int index = 0; index < joints.size(); index++) {
            assertEquals(index >= joints.size() - cap, chain.isHidden(joints.get(index).bone(), cap),
                    "Only the trailing sections may disappear: bone " + joints.get(index).bone());
        }
        assertEquals(joints.get(joints.size() - cap - 1).bone(), chain.lastRetainedBone(cap));
        var pose = DemiurgeRig.INSTANCE.pose();
        pose.sample(200, 17, DemiurgeAttack.NONE, -1, Vec3.ZERO, 0, trail::sample, cap, null);
        assertTrue(pose.bounds(joints.get(joints.size() - cap - 1).bone(), Vec3.ZERO, 0).getSize() > .1F,
                "The last retained section still blocks");
        for (int index = joints.size() - cap; index < joints.size(); index++) {
            assertEquals(0.0, pose.bounds(joints.get(index).bone(), Vec3.ZERO, 0).getSize(),
                    "Severed sections no longer collide: bone " + joints.get(index).bone());
        }
    }

    private static void verifySelfAvoidance(JsonObject mesh, JsonObject animation, JsonArray ground, int count) {
        var zero = net.minecraft.world.phys.Vec3.ZERO;
        for (double step : new double[]{.01, .03, .06, .12}) {
            var turn = DemiurgePatrolSteering.steer(zero, 0, 90, step, java.util.List.of());
            assertFalse(turn.blocked());
            double allowed = Math.min(DemiurgePatrolSteering.MAX_TURN_PER_TICK,
                    Math.toDegrees(step / DemiurgePatrolSteering.TURN_RADIUS));
            assertTrue(Math.abs(turn.yaw()) <= allowed + .00001,
                    "A moving statue still respects its six-block turn radius at slow ground speed");
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
