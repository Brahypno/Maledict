package org.brahypno.maledict.common.realm.wither;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.realm.BlightedRealm;
import org.brahypno.maledict.common.realm.reliquary.InvertedReliquaryPiece;
import org.brahypno.maledict.common.realm.reliquary.ReliquaryEvents;
import org.brahypno.maledict.registry.MaledictBlocks;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class WeepingWitherEncounterTest {
    @GameTest(template = "test_empty", batch = "weeping_reliquary", timeoutTicks = 150)
    public static void generatedEncounterReformsWithoutStaleDeathAnimation(GameTestHelper helper) {
        verifyPlayerSummoning(helper);
        var realm = helper.getLevel().getServer().getLevel(BlightedRealm.LEVEL);
        BlockPos origin = new BlockPos(4096, 180, 4096);
        for (int x = 255; x <= 258; x++) for (int z = 255; z <= 258; z++) realm.setChunkForced(x, z, true);
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(34, 19, 34))) {
            realm.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
        }
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, 4, 13), origin.offset(2, 10, 21))) {
            realm.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        for (String variant : new String[]{"intact", "fractured", "sunken"}) {
            var template = realm.getStructureManager().get(InvertedReliquaryPiece.templateId(variant)).orElseThrow();
            helper.assertTrue(template.getSize().equals(new Vec3i(35, 20, 35)), "All three generated templates load");
        }
        var template = realm.getStructureManager().get(InvertedReliquaryPiece.templateId("fractured")).orElseThrow();
        helper.assertTrue(template.placeInWorld(realm, origin, origin,
                InvertedReliquaryPiece.settings(Rotation.CLOCKWISE_90), realm.random, 2), "The rotated template places inside solid terrain");
        BlockPos core = origin.offset(17, 4, 17);
        helper.assertTrue(realm.getBlockState(core).is(MaledictBlocks.RELIQUARY_ALTAR.get())
                        && realm.getBlockState(core.above(2)).isAir() && realm.getBlockState(core.above(6)).isAir(),
                "Placement clears the central spawn area");
        helper.assertTrue(realm.getBlockState(origin.offset(0, 6, 17)).isAir()
                        && realm.getBlockState(origin.offset(-1, 6, 17)).isAir(), "The cave connects through the broken wall");
        realm.setBlock(core.above(2), Blocks.STONE.defaultBlockState(), 2);
        AABB arena = new AABB(origin, origin.offset(35, 20, 35));
        var joins = new AtomicInteger();
        var encounter = new AtomicReference<WeepingWitherEntity>();
        var blastTarget = new AtomicReference<Cow>();
        var outsideTarget = new AtomicReference<Cow>();
        var explosions = new AtomicInteger();
        Consumer<ExplosionEvent.Start> observeExplosion = event -> {
            if (event.getLevel() == realm && event.getExplosion().getDirectSourceEntity() instanceof WeepingWitherEntity wither
                    && wither == encounter.get()) {
                explosions.incrementAndGet();
                helper.assertTrue(!wither.isReforming() && wither.getHealth() == 80,
                        "The revival explosion occurs after health and the active state are restored");
                helper.assertTrue(event.getExplosion().interactsWithBlocks()
                                == realm.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING),
                        "The revival explosion follows the mob-griefing rule");
            }
        };
        Consumer<EntityJoinLevelEvent> observe = event -> {
            if (event.getLevel() == realm && event.getEntity() instanceof WeepingWitherEntity wither
                    && arena.contains(wither.position())) {
                wither.setNoAi(true);
                wither.setNoGravity(true);
                encounter.set(wither);
                joins.incrementAndGet();
            }
        };
        var player = new FakePlayer(realm, new GameProfile(UUID.randomUUID(), "ReliquaryTest"));
        player.setPos(Vec3.atCenterOf(core.offset(5, 2, 0)));
        realm.players().add(player);
        MinecraftForge.EVENT_BUS.addListener(observe);
        MinecraftForge.EVENT_BUS.addListener(observeExplosion);
        var altar = MaledictBlocks.RELIQUARY_ALTAR.get();
        // A generated/old saved altar has no onPlace callback. Start from that dormant state.
        realm.getBlockTicks().clearArea(new BoundingBox(core));
        helper.assertTrue(!realm.getBlockTicks().hasScheduledTick(core, altar), "The generated altar starts without a queued tick");
        ReliquaryEvents.armAltarOnChunkLoad(new ChunkEvent.Load(realm.getChunkAt(core), true));
        helper.assertTrue(realm.getBlockTicks().hasScheduledTick(core, altar), "The loaded chunk arms its own altar without fetching other chunks");
        player.setGameMode(GameType.SPECTATOR);
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(joins.get() == 0 && realm.getBlockState(core).is(altar), "Spectators leave the altar untriggered");
            player.setGameMode(GameType.CREATIVE);
        });
        helper.runAtTickTime(35, () -> {
            helper.assertTrue(joins.get() == 1 && !realm.getBlockState(core).is(altar),
                    "A creative player triggers one custom entity even when its spawn position contains a solid block");
            realm.setBlock(core.above(2), Blocks.AIR.defaultBlockState(), 2);
            var wither = encounter.get();
            BlockPos combatPos = core.above(24);
            for (BlockPos pos : BlockPos.betweenClosed(combatPos.offset(-8, 0, -8), combatPos.offset(8, 13, 8))) {
                realm.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            wither.setPos(Vec3.atBottomCenterOf(combatPos));
            verifySkullDamageAndLifetime(helper, wither);
            verifyDamageLimit(helper);
            Vec3 blastCenter = new Vec3(wither.getX(), wither.getEyeY(), wither.getZ());
            blastTarget.set(shockTarget(realm, blastCenter.add(10, 0, 0)));
            outsideTarget.set(shockTarget(realm, blastCenter.add(15, 0, 0)));
            wither.setHealth(10.0F);
            wither.hurt(wither.damageSources().genericKill(), Float.MAX_VALUE);
            helper.assertTrue(wither.isReforming() && wither.getHealth() == 1 && wither.deathTime == 0
                    && wither.getPose() == Pose.STANDING && wither.isPowered(),
                    "Refused death remains alive and upright with the half-health shield active");
            wither.setHealth(0);
            wither.deathTime = 12;
            wither.setPose(Pose.DYING);
            wither.handleEntityEvent((byte) 3);
            helper.assertTrue(wither.getHealth() == 1 && wither.deathTime == 0 && wither.getPose() == Pose.STANDING,
                    "A death-animation event during reformation is cleared rather than entering vanilla death");
        });
        helper.runAtTickTime(40, () -> {
            var wither = encounter.get();
            CompoundTag saved = wither.saveWithoutId(new CompoundTag());
            saved.putShort("DeathTime", (short) 19);
            wither.setPose(Pose.DYING);
            wither.readAdditionalSaveData(saved);
            helper.assertTrue(wither.isReforming() && wither.deathTime == 0 && wither.getPose() == Pose.STANDING,
                    "A saved reformation clears stale DeathTime and pose while retaining its remaining duration");
            // Simulate a zero-health animation state arriving before the next normal entity tick.
            wither.setHealth(0);
            wither.deathTime = 19;
            wither.setPose(Pose.DYING);
        });
        helper.runAtTickTime(45, () -> {
            var wither = encounter.get();
            helper.assertTrue(!wither.isRemoved() && wither.getHealth() == 1 && wither.deathTime == 0
                    && wither.getPose() == Pose.STANDING && wither.isPowered() && explosions.get() == 0,
                    "Reformation retains its shield and does not explode before it finishes");
        });
        helper.runAtTickTime(65, () -> {
            helper.assertTrue(joins.get() == 1 && !realm.getBlockState(core).is(altar), "Persisted scheduled ticks cannot trigger the spent altar again");
        });
        helper.runAtTickTime(105, () -> {
            var wither = encounter.get();
            helper.assertTrue(!wither.isReforming() && wither.isAlive() && wither.getHealth() == 80
                    && wither.deathTime == 0 && wither.getPose() == Pose.STANDING,
                    "Reformation ends with restored health and no lingering death animation");
            helper.assertTrue(explosions.get() == 1 && blastTarget.get().getHealth() < blastTarget.get().getMaxHealth(),
                    "Reformation triggers one real strength-seven explosion reaching beyond the old six-block shock");
            helper.assertTrue(outsideTarget.get().getHealth() == outsideTarget.get().getMaxHealth(),
                    "The revival explosion does not reach beyond its fourteen-block damage radius");
            CompoundTag saved = wither.saveWithoutId(new CompoundTag());
            saved.putShort("DeathTime", (short) 12);
            wither.readAdditionalSaveData(saved);
            helper.assertTrue(wither.deathTime == 0, "Old positive-health saves also lose stale death animation time");
            wither.setHealth(10.0F);
            wither.hurt(wither.damageSources().genericKill(), Float.MAX_VALUE);
            helper.assertTrue(wither.isDeadOrDying() && !wither.isReforming() && wither.getPose() == Pose.DYING,
                    "The next lethal hit still enters real death");
            var drops = realm.getEntitiesOfClass(ItemEntity.class, wither.getBoundingBox().inflate(3));
            helper.assertTrue(drops.stream().filter(drop -> drop.getItem().is(MaledictItems.BLIGHTED_STAR.get()))
                            .mapToInt(drop -> drop.getItem().getCount()).sum() == 1
                            && drops.stream().noneMatch(drop -> drop.getItem().is(Items.NETHER_STAR)),
                    "A real death drops exactly one Blighted Star and no vanilla Nether Star");
            drops.forEach(ItemEntity::discard);
        });
        helper.runAtTickTime(120, () -> {
            try {
                helper.assertTrue(explosions.get() == 1, "Saving and the final death do not replay the revival explosion");
                encounter.get().discard();
                blastTarget.get().discard();
                outsideTarget.get().discard();
                helper.succeed();
            } finally {
                realm.players().remove(player);
                MinecraftForge.EVENT_BUS.unregister(observe);
                MinecraftForge.EVENT_BUS.unregister(observeExplosion);
                for (int x = 255; x <= 258; x++) for (int z = 255; z <= 258; z++) realm.setChunkForced(x, z, false);
            }
        });
    }

    private static void verifyPlayerSummoning(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(4, 7, 4));
        var skull = MaledictBlocks.BLIGHTED_WITHER_SKULL.get().defaultBlockState();
        helper.assertTrue(!skull.canOcclude() && skull.getOcclusionShape(level, base).isEmpty()
                        && skull.getLightBlock(level, base) == 0,
                "The recessed skull faces neither occlude nor block their own lighting, like vanilla skulls");
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "WitherSummoner"));
        verifySkullRotations(helper, player, base);
        for (Direction across : new Direction[]{Direction.EAST, Direction.SOUTH}) {
            for (int[] order : new int[][]{{-1, 0, 1}, {-1, 1, 0}, {0, -1, 1}, {0, 1, -1}, {1, -1, 0}, {1, 0, -1}}) {
                clearSummoningArea(level, base);
                placeSummoningStones(level, base, across);
                for (int index = 0; index < 3; index++) {
                    BlockPos support = base.above().relative(across, order[index]);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MaledictItems.BLIGHTED_WITHER_SKULL.get()));
                    player.setYRot((index * 3 + 1) * 22.5F);
                    var hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5D, 0), Direction.UP, support, false);
                    MaledictItems.BLIGHTED_WITHER_SKULL.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
                    var spawned = level.getEntitiesOfClass(WeepingWitherEntity.class, new AABB(base).inflate(3));
                    helper.assertTrue(spawned.size() == (index == 2 ? 1 : 0),
                            "Only the final placed skull summons one custom Wither, in either axis and any skull order");
                    if (index == 2) {
                        helper.assertTrue(spawned.get(0).getInvulnerableTicks() == 220,
                                "Player summoning starts the vanilla Wither charge-up");
                        helper.assertTrue(level.getBlockState(base).isAir(), "Summoning consumes the stem");
                        for (int offset = -1; offset <= 1; offset++) {
                            helper.assertTrue(level.getBlockState(base.above().relative(across, offset)).isAir()
                                            && level.getBlockState(base.above(2).relative(across, offset)).isAir(),
                                    "Summoning consumes all three crossbar stones and all three skulls");
                        }
                        spawned.get(0).discard();
                    }
                }
            }
            for (int invalid = 0; invalid < 4; invalid++) {
                clearSummoningArea(level, base);
                placeSummoningStones(level, base, across);
                for (int offset = -1; offset <= 1; offset++) {
                    level.setBlock(base.above(2).relative(across, offset),
                            MaledictBlocks.BLIGHTED_WITHER_SKULL.get().defaultBlockState(), 2);
                }
                if (invalid == 0) { // Swapping center and stem must not produce a valid recipe.
                    level.setBlock(base.above(), MaledictBlocks.CRYING_TWISTED_ROCK.get().defaultBlockState(), 2);
                    level.setBlock(base, MaledictBlocks.CRYING_TAINTED_ROCK.get().defaultBlockState(), 2);
                } else if (invalid == 1) {
                    level.setBlock(base.above(2), Blocks.WITHER_SKELETON_SKULL.defaultBlockState(), 2);
                } else if (invalid == 2) {
                    level.setBlock(base.above().relative(across), Blocks.SOUL_SAND.defaultBlockState(), 2);
                } else {
                    level.setBlock(base.relative(across), Blocks.STONE.defaultBlockState(), 2);
                }
                WeepingWitherSummoning.trySpawn(level, base.above(2));
                helper.assertTrue(level.getEntitiesOfClass(WeepingWitherEntity.class, new AABB(base).inflate(3)).isEmpty()
                                && !level.getBlockState(base).isAir()
                                && level.getBlockState(base.above(2).relative(across)).is(MaledictBlocks.BLIGHTED_WITHER_SKULL.get()),
                        "Wrong stones, vanilla skulls and blocked bottom corners neither summon nor consume materials");
            }
        }
        clearSummoningArea(level, base);
    }

    private static void placeSummoningStones(ServerLevel level, BlockPos base, Direction across) {
        level.setBlock(base, MaledictBlocks.CRYING_TWISTED_ROCK.get().defaultBlockState(), 2);
        level.setBlock(base.above(), MaledictBlocks.CRYING_TAINTED_ROCK.get().defaultBlockState(), 2);
        level.setBlock(base.above().relative(across), MaledictBlocks.CRYING_TWISTED_ROCK.get().defaultBlockState(), 2);
        level.setBlock(base.above().relative(across.getOpposite()), MaledictBlocks.CRYING_TWISTED_ROCK.get().defaultBlockState(), 2);
    }

    private static void verifySkullRotations(GameTestHelper helper, FakePlayer player, BlockPos support) {
        var level = helper.getLevel();
        var rotation = BlightedWitherSkullBlock.ROTATION;
        clearSummoningArea(level, support);
        level.setBlock(support, Blocks.STONE.defaultBlockState(), 2);
        for (int segment = 0; segment < 16; segment++) {
            for (float adjustment : new float[]{-11.0F, 0.0F, 11.0F}) {
                level.setBlock(support.above(), Blocks.AIR.defaultBlockState(), 2);
                player.setYRot(segment * 22.5F + adjustment);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MaledictItems.BLIGHTED_WITHER_SKULL.get()));
                var hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5D, 0), Direction.UP, support, false);
                var use = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
                var expected = Blocks.WITHER_SKELETON_SKULL.getStateForPlacement(new BlockPlaceContext(use));
                MaledictItems.BLIGHTED_WITHER_SKULL.get().useOn(use);
                var actual = level.getBlockState(support.above());
                helper.assertTrue(actual.is(MaledictBlocks.BLIGHTED_WITHER_SKULL.get())
                                && actual.getValue(rotation).equals(expected.getValue(SkullBlock.ROTATION)),
                        "All 16 placed angles, including near their rounding boundaries, match vanilla skulls");
                for (Rotation turn : Rotation.values()) {
                    helper.assertTrue(actual.rotate(turn).getValue(rotation).equals(expected.rotate(turn).getValue(SkullBlock.ROTATION)),
                            "Structure rotation matches the vanilla skull's 16-segment rotation");
                }
                for (Mirror mirror : Mirror.values()) {
                    helper.assertTrue(actual.mirror(mirror).getValue(rotation).equals(expected.mirror(mirror).getValue(SkullBlock.ROTATION)),
                            "Structure mirroring matches vanilla skulls at diagonal angles");
                }
            }
        }
        clearSummoningArea(level, support);
    }

    private static void clearSummoningArea(ServerLevel level, BlockPos base) {
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-2, 0, -2), base.offset(2, 3, 2))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static Cow shockTarget(ServerLevel level, Vec3 pos) {
        Cow target = EntityType.COW.create(level);
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setPos(pos);
        level.addFreshEntity(target);
        return target;
    }

    private static WeepingWitherEntity damageTarget(GameTestHelper helper) {
        var target = MaledictEntities.WEEPING_WITHER.get().create(helper.getLevel());
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 10, 1))));
        helper.getLevel().addFreshEntity(target);
        return target;
    }

    private static void verifyDamageLimit(GameTestHelper helper) {
        for (float[] hits : new float[][]{{80, 60}, {60, 80}, {5, 12, 8}, {12, 5, 8}}) {
            var target = damageTarget(helper);
            try {
                float expected = 0;
                for (float hit : hits) {
                    target.hurt(target.damageSources().generic(), hit);
                    expected = Math.max(expected, Math.min(hit, 20));
                    helper.assertTrue(target.getHealth() == 400 - expected,
                            "Same-tick hits debit only the largest capped loss in either order");
                }
            } finally {
                target.discard();
            }
        }
        var scaled = damageTarget(helper);
        try {
            scaled.getAttribute(Attributes.MAX_HEALTH).setBaseValue(800);
            scaled.setHealth(100);
            scaled.hurt(scaled.damageSources().generic(), 80);
            helper.assertTrue(scaled.getHealth() == 60, "The five-percent cap uses maximum health, not current health");
        } finally {
            scaled.discard();
        }
        var nested = damageTarget(helper);
        Consumer<LivingHurtEvent> extraChannel = event -> {
            if (event.getEntity() == nested && event.getSource().getMsgId().equals("generic")) {
                nested.hurt(nested.damageSources().magic(), 12);
            }
        };
        Consumer<LivingDamageEvent> amplify = event -> {
            if (event.getEntity() == nested && event.getSource().getMsgId().equals("generic")) event.setAmount(100);
        };
        MinecraftForge.EVENT_BUS.addListener(extraChannel);
        MinecraftForge.EVENT_BUS.addListener(amplify);
        try {
            nested.hurt(nested.damageSources().generic(), 5);
            helper.assertTrue(nested.getHealth() == 380,
                    "Nested channels and post-mitigation event multipliers share the same final cap");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(extraChannel);
            MinecraftForge.EVENT_BUS.unregister(amplify);
            nested.discard();
        }
        var cooldown = damageTarget(helper);
        cooldown.hurt(cooldown.damageSources().generic(), 80);
        cooldown.hurt(cooldown.damageSources().generic(), 5);
        cooldown.hurt(cooldown.damageSources().wither(), 80);
        helper.runAfterDelay(1, () -> {
            cooldown.hurt(cooldown.damageSources().generic(), 80);
            helper.assertTrue(cooldown.getHealth() == 380, "A later tick retains vanilla hurt immunity");
        });
        helper.runAfterDelay(21, () -> {
            try {
                cooldown.hurt(cooldown.damageSources().generic(), 80);
                helper.assertTrue(cooldown.getHealth() == 360, "The next accepted tick gets a fresh damage budget");
            } finally {
                cooldown.discard();
            }
        });
    }

    private static void verifySkullDamageAndLifetime(GameTestHelper helper, WeepingWitherEntity owner) {
        ServerLevel level = (ServerLevel) owner.level();
        for (boolean alreadyWithered : new boolean[]{false, true}) {
            Cow target = shockTarget(level, owner.position().add(5, 1, 0));
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0D);
            target.setHealth(40.0F);
            if (alreadyWithered) target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200));
            WeepingWitherSkull skull = MaledictEntities.WEEPING_WITHER_SKULL.get().create(level);
            skull.setOwner(owner);
            skull.onHitEntity(new EntityHitResult(target));
            helper.assertTrue(target.getHealth() == target.getMaxHealth() - (alreadyWithered ? 14 : 12),
                    "The custom direct-hit path adds exactly one heart after the existing wither bonus");
            target.discard();
        }
        for (boolean charged : new boolean[]{false, true}) {
            WeepingWitherSkull skull = MaledictEntities.WEEPING_WITHER_SKULL.get().create(level);
            skull.setOwner(owner);
            skull.setDangerous(charged);
            skull.setPos(owner.position().add(charged ? -4 : 4, 10, 0));
            for (int tick = 0; tick < 100; tick++) skull.tick();
            CompoundTag saved = skull.saveWithoutId(new CompoundTag());
            skull.discard();
            WeepingWitherSkull loaded = MaledictEntities.WEEPING_WITHER_SKULL.get().create(level);
            loaded.load(saved);
            loaded.setDangerous(charged);
            for (int tick = 0; tick < 499; tick++) loaded.tick();
            helper.assertTrue(!loaded.isRemoved(), "Normal and charged skulls survive 599 ticks including before reload");
            BlockPos obstacle = loaded.blockPosition();
            level.setBlock(obstacle, Blocks.GLASS.defaultBlockState(), 2);
            loaded.tick();
            helper.assertTrue(loaded.isRemoved() && level.getBlockState(obstacle).is(Blocks.GLASS),
                    "At tick 600 the skull expires before collision, without a terrain explosion");
            level.setBlock(obstacle, Blocks.AIR.defaultBlockState(), 2);
        }
    }
}
