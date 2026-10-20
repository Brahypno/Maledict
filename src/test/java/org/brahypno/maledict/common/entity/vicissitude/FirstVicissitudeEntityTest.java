package org.brahypno.maledict.common.entity.vicissitude;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.curio.vicissitude.VicissitudeCurioLedger;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.rig.VicissitudeRig;

import java.lang.reflect.Method;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exercises the registered encounter itself, including virtual weapons, NBT and world ledger. */
@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class FirstVicissitudeEntityTest {
    @GameTest(template = "test_empty", batch = "vicissitude_regressions")
    public static void bystandersStaySafeAndCreativePlayersLoseAggroInBothPhases(GameTestHelper helper) throws Exception {
        Map<UUID, ServerPlayer> online = onlinePlayers(helper);
        for (boolean phaseTwo : new boolean[]{false, true}) {
            FirstVicissitudeBossEntity boss = spawn(helper, phaseTwo ? phaseTwoTag() : new CompoundTag());
            UUID identity = UUID.randomUUID();
            FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(identity, "NeutralTest"));
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(boss.position().add(0, 0, 0.5));
            online.put(identity, player);
            try {
                // Malum 1.21 resolves the orb's collector by UUID in the server level.
                helper.getLevel().addNewPlayer(player);
                float health = player.getHealth();
                helper.assertTrue(!boss.isValidCombatParticipant(player)
                                  && !boss.hurtParticipant(player, boss.damageSources().mobAttack(boss), 10)
                                  && player.getHealth() == health,
                        "An unprovoked player must be protected even at the shared skill damage entry point");
                invoke(boss, "syncCombatState", new Class<?>[0], new Object[0]);
                helper.assertTrue(!boss.hasCombatOpponent(),
                        "An unprovoked encounter must not activate music for nearby listeners");

                boss.hurt(boss.damageSources().playerAttack(player), 1);
                helper.assertTrue(boss.isValidCombatParticipant(player),
                        "An actual attack must register the player, including an immune phase-one hit");
                invoke(boss, "syncCombatState", new Class<?>[0], new Object[0]);
                helper.assertTrue(boss.hasCombatOpponent(),
                        "A nearby enemy must activate music for all nearby listeners, including bystanders");
                player.setPos(boss.position().add(300, 0, 0));
                invoke(boss, "syncCombatState", new Class<?>[0], new Object[0]);
                helper.assertTrue(!boss.hasCombatOpponent(),
                        "Remembered enemies outside the engagement range must not keep music playing");
                player.setPos(boss.position().add(0, 0, 0.5));

                VicissitudeLightOrbEntity orb = new VicissitudeLightOrbEntity(
                        MaledictEntities.VICISSITUDE_LIGHT_ORB.get(), helper.getLevel());
                CompoundTag orbTag = new CompoundTag();
                orbTag.putUUID("OrbCaster", boss.getUUID());
                orb.readAdditionalSaveData(orbTag);
                orb.setDestination(new com.sammy.malum.common.entity.FloatingItemDestinationData(player));
                orb.collect(helper.getLevel());
                helper.assertTrue(player.getHealth() == 1,
                        "A homing orb must still press the health of a registered survival player");
                player.setHealth(health);

                player.setGameMode(GameType.CREATIVE);
                orb.collect(helper.getLevel());
                helper.assertTrue(player.getHealth() == health, "An in-flight orb must not hurt a creative player");
                invoke(boss, "forgetDeadParticipants", new Class<?>[0], new Object[0]);
                invoke(boss, "syncCombatState", new Class<?>[0], new Object[0]);
                CompoundTag saved = boss.saveWithoutId(new CompoundTag());
                helper.assertTrue(saved.getList("PhaseOneTargets", Tag.TAG_INT_ARRAY).isEmpty()
                                  && saved.getList("PhaseTwoPlayers", Tag.TAG_INT_ARRAY).isEmpty()
                                  && saved.getList("PhaseOneTargetDeaths", Tag.TAG_COMPOUND).isEmpty()
                                  && saved.getList("PhaseTwoPlayerDeaths", Tag.TAG_COMPOUND).isEmpty()
                                  && boss.getTarget() == null
                                  && !boss.hasCombatOpponent(),
                        "Creative mode must remove saved aggro, the current target and music eligibility");
                player.setGameMode(GameType.SURVIVAL);
                LivingEntity selected = (LivingEntity) invoke(boss,
                        phaseTwo ? "selectBalancedPhaseTwoTarget" : "currentPhaseOneTarget",
                        new Class<?>[0], new Object[0]);
                helper.assertTrue(selected == null && !boss.isValidCombatParticipant(player),
                        "Returning to survival must stay neutral until the player attacks again");
                orb.collect(helper.getLevel());
                orb.tick();
                helper.assertTrue(player.getHealth() == health && orb.isRemoved(),
                        "A stale homing orb must disappear without hurting a player who left the enemy roster");
            } finally {
                online.remove(identity);
                player.remove(Entity.RemovalReason.DISCARDED);
                boss.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "vicissitude_regressions")
    public static void closeTargetDoesNotPullPhaseOneDownButPhaseTwoStillDives(GameTestHelper helper) throws Exception {
        CompoundTag setup = new CompoundTag();
        setup.putByte("VicissitudeStage", (byte) VicissitudeBossStage.PHASE_ONE.ordinal());
        FirstVicissitudeBossEntity boss = spawn(helper, setup);
        boss.setNoAi(true);
        LivingEntity target = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 3));
        target.setPos(boss.position().add(0, -1, 2));
        boss.setDeltaMovement(Vec3.ZERO);
        invoke(boss, "updatePhaseOneMovement", new Class<?>[]{LivingEntity.class}, new Object[]{target});
        helper.assertTrue(boss.getDeltaMovement().y > 0,
                "A nearby target must make phase one ascend to its ranged hover height, never dive for melee");
        boss.setDeltaMovement(Vec3.ZERO);
        invoke(boss, "updatePhaseTwoMovement", new Class<?>[]{LivingEntity.class}, new Object[]{target});
        helper.assertTrue(boss.getDeltaMovement().y < 0, "Phase two must retain its blade-height melee approach");
        boss.discard();
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "vicissitude_regressions")
    public static void offlineAggroSurvivesResetWindowReloadAndLoginInBothPhases(GameTestHelper helper) throws Exception {
        Map<UUID, ServerPlayer> online = onlinePlayers(helper);
        boolean forgive = helper.getLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_FORGIVE_DEAD_PLAYERS);
        helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_FORGIVE_DEAD_PLAYERS)
                .set(true, helper.getLevel().getServer());
        try {
            for (boolean phaseTwo : new boolean[]{false, true}) {
                UUID identity = UUID.randomUUID();
                String rosterKey = phaseTwo ? "PhaseTwoPlayers" : "PhaseOneTargets";
                String deathsKey = phaseTwo ? "PhaseTwoPlayerDeaths" : "PhaseOneTargetDeaths";
                CompoundTag setup = phaseTwo ? phaseTwoTag() : new CompoundTag();
                if (!phaseTwo) setup.putByte("VicissitudeStage", (byte) VicissitudeBossStage.PHASE_ONE.ordinal());
                setup.putInt("PhaseOneDuration", 1800);
                setup.putInt("PhaseOneTicks", 45);
                ListTag roster = new ListTag(); roster.add(NbtUtils.createUUID(identity)); setup.put(rosterKey, roster);
                CompoundTag death = new CompoundTag(); death.putUUID("Player", identity); death.putInt("Deaths", 0);
                ListTag deaths = new ListTag(); deaths.add(death); setup.put(deathsKey, deaths);
                FirstVicissitudeBossEntity boss = spawn(helper, setup);
                boss.setNoAi(true);
                FakePlayer oldPlayer = new FakePlayer(helper.getLevel(), new GameProfile(identity, "AggroTest"));
                oldPlayer.setPos(boss.position().add(0, 0, 3));
                online.put(identity, oldPlayer);
                boss.setTarget(oldPlayer);
                // Logout removes the old player object; its replacement on login has the same UUID.
                online.remove(identity);
                oldPlayer.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
                invoke(boss, "forgetDeadParticipants", new Class<?>[0], new Object[0]);
                helper.assertTrue(boss.getTarget() == null, "Offline players must stop being active attack targets");
                for (int tick = 0; tick <= FirstVicissitudeBossEntity.EMPTY_ENCOUNTER_RESET_TICKS; tick++)
                    invoke(boss, "tickStage", new Class<?>[0], new Object[0]);
                CompoundTag saved = boss.saveWithoutId(new CompoundTag());
                helper.assertTrue(saved.getList(rosterKey, Tag.TAG_INT_ARRAY).size() == 1,
                        "Logout must preserve aggro past the empty-encounter reset window in both phases");
                if (!phaseTwo) helper.assertTrue(saved.getInt("PhaseOneTicks") == 45
                                && boss.getStage() == VicissitudeBossStage.PHASE_ONE,
                        "An encounter whose opponents are all offline must pause phase one");
                boss.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                FirstVicissitudeBossEntity loaded = MaledictEntities.FIRST_VICISSITUDE.get().create(helper.getLevel());
                loaded.load(saved);
                loaded.setNoAi(true);
                helper.assertTrue(helper.getLevel().addFreshEntity(loaded), "Saved encounter must reload");
                invoke(loaded, "forgetDeadParticipants", new Class<?>[0], new Object[0]);
                helper.assertTrue(loaded.saveWithoutId(new CompoundTag()).getList(rosterKey, Tag.TAG_INT_ARRAY).size() == 1,
                        "Loading the boss before player login must retain the saved UUID");
                FakePlayer returned = new FakePlayer(helper.getLevel(), new GameProfile(identity, "AggroTest"));
                returned.setPos(loaded.position().add(0, 0, 3));
                online.put(identity, returned);
                try {
                    invoke(loaded, "forgetDeadParticipants", new Class<?>[0], new Object[0]);
                    LivingEntity selected = (LivingEntity) invoke(loaded,
                            phaseTwo ? "selectBalancedPhaseTwoTarget" : "currentPhaseOneTarget", new Class<?>[0], new Object[0]);
                    helper.assertTrue(selected == returned, "Login must reacquire the new player object without another attack");
                    if (!phaseTwo) {
                        invoke(loaded, "tickStage", new Class<?>[0], new Object[0]);
                        helper.assertTrue(loaded.saveWithoutId(new CompoundTag()).getInt("PhaseOneTicks") == 46,
                                "Phase one must resume counting when the player returns");
                    }
                    returned.getStats().setValue(returned, Stats.CUSTOM.get(Stats.DEATHS), 1);
                    invoke(loaded, "forgetDeadParticipants", new Class<?>[0], new Object[0]);
                    helper.assertTrue(loaded.saveWithoutId(new CompoundTag()).getList(rosterKey, Tag.TAG_INT_ARRAY).isEmpty(),
                            "Death forgiveness must still clear the remembered player in both phases");
                } finally {
                    online.remove(identity);
                    loaded.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                }
            }
        } finally {
            helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_FORGIVE_DEAD_PLAYERS)
                    .set(forgive, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "vicissitude_regressions")
    public static void transitionCarriesOfflineAggroIntoPhaseTwo(GameTestHelper helper) throws Exception {
        UUID identity = UUID.randomUUID();
        CompoundTag setup = new CompoundTag();
        ListTag roster = new ListTag(); roster.add(NbtUtils.createUUID(identity)); setup.put("PhaseOneTargets", roster);
        CompoundTag death = new CompoundTag(); death.putUUID("Player", identity); death.putInt("Deaths", 2);
        ListTag deaths = new ListTag(); deaths.add(death); setup.put("PhaseOneTargetDeaths", deaths);
        FirstVicissitudeBossEntity boss = spawn(helper, setup);
        invoke(boss, "completeTransition", new Class<?>[0], new Object[0]);
        CompoundTag saved = boss.saveWithoutId(new CompoundTag());
        helper.assertTrue(saved.getList("PhaseTwoPlayers", Tag.TAG_INT_ARRAY).size() == 1
                        && saved.getList("PhaseTwoPlayerDeaths", Tag.TAG_COMPOUND).getCompound(0).getInt("Deaths") == 2,
                "A player offline during the transition must keep aggro and their recorded death count");
        boss.discard();
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ServerPlayer> onlinePlayers(GameTestHelper helper) throws Exception {
        var field = PlayerList.class.getDeclaredField("playersByUUID");
        field.setAccessible(true);
        return (Map<UUID, ServerPlayer>) field.get(helper.getLevel().getServer().getPlayerList());
    }

    @GameTest(template = "test_empty", batch = "vitality_pool", timeoutTicks = 20)
    public static void newExtremeHas1500HealthButExistingLedgerKeepsItsCapturedPool(GameTestHelper helper) throws Exception {
        CompoundTag setup = phaseTwoTag();
        setup.putString("VicissitudeDifficulty", "extreme");
        FirstVicissitudeBossEntity boss = spawn(helper, setup);
        helper.assertTrue(boss.getHealth() == 1500 && boss.getVitalityMaximum() == 1500,
                "New registered extreme boss must actually initialize 1500 authoritative health");
        boss.getAttribute(Attributes.ARMOR).setBaseValue(0);
        var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        attacker.setNoAi(true);
        // The encounter intentionally rejects sources without a causing entity.
        helper.assertTrue(boss.hurt(boss.damageSources().mobAttack(attacker), 10000) && boss.getHealth() == 1485,
                "Actual body damage must use the authored pool's one-percent limit of fifteen: health="
                        + boss.getHealth() + ", armor=" + boss.getAttributeValue(Attributes.ARMOR)
                        + ", max=" + boss.getVitalityMaximum() + ", cap=" + boss.getVitalityDamageLimit());
        var eventField = FirstVicissitudeBossEntity.class.getDeclaredField("bossEvent");
        eventField.setAccessible(true);
        ServerBossEvent event = (ServerBossEvent) eventField.get(boss);
        helper.runAtTickTime(2, () -> {
            helper.assertTrue(Math.abs(event.getProgress() - 0.99F) < 0.0001F,
                    "Server boss bar must show 1485/1500, not clamp against vanilla's attribute maximum");
            CompoundTag saved = boss.saveWithoutId(new CompoundTag());
            boss.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            FirstVicissitudeBossEntity loaded = MaledictEntities.FIRST_VICISSITUDE.get().create(helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(helper.getLevel().addFreshEntity(loaded) && loaded.getHealth() == 1485
                    && loaded.getVitalityMaximum() == 1500, "Real entity reload must retain both wounds and its captured pool");
            loaded.heal(100);
            helper.assertTrue(loaded.getHealth() == 1500, "Authoritative healing must reach 1500 despite the vanilla attribute cap");
            loaded.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);

            FirstVicissitudeBossEntity existing = MaledictEntities.FIRST_VICISSITUDE.get().create(helper.getLevel());
            existing.readAdditionalSaveData(setup);
            existing.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
            VicissitudeVitalityLedger.get(helper.getLevel()).commit(existing.getUUID(),
                    VicissitudeVitality.initial(1024).withCurrent(900));
            helper.assertTrue(helper.getLevel().addFreshEntity(existing) && existing.getHealth() == 900
                    && existing.getVitalityMaximum() == 1024,
                    "An existing world record must keep 900/1024; the fix must not migrate or heal it");
            existing.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            attacker.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 10)
    public static void missingFlyingScytheImmediatelyRestoresVirtualWeapon(GameTestHelper helper) throws Exception {
        FirstVicissitudeBossEntity boss = spawn(helper, phaseTwoTag());
        double strength = boss.getAttributeValue(Attributes.ATTACK_DAMAGE);
        invoke(boss, "throwScythe", new Class<?>[]{net.minecraft.world.entity.LivingEntity.class}, new Object[]{null});
        CompoundTag saved = boss.saveWithoutId(new CompoundTag());
        UUID token = saved.getUUID("ScytheToken");
        Entity scythe = helper.getLevel().getEntity(token);
        helper.assertTrue(scythe instanceof VicissitudeScytheProjectileEntity, "Throw must spawn an owned scythe");
        helper.assertTrue(!boss.shouldRenderHeldWeapon() && boss.getMainHandItem().isEmpty(), "Flying weapon is virtual");
        scythe.discard();
        helper.runAtTickTime(2, () -> {
            helper.assertTrue(boss.shouldRenderHeldWeapon() && boss.getMainHandItem().isEmpty(), "Missing scythe restores rendering immediately");
            helper.assertTrue(!boss.ownsFlyingScythe(token), "Lost token must be cleared");
            helper.assertTrue(boss.getAttributeValue(Attributes.ATTACK_DAMAGE) == strength, "Recovery must preserve attack strength");
            VicissitudeScytheProjectileEntity stale = new VicissitudeScytheProjectileEntity(helper.getLevel(), boss,
                    boss.position(), new Vec3(0, 0, 1), boss.getBossDifficulty(), 8);
            stale.setUUID(token);
            helper.getLevel().addFreshEntity(stale);
            stale.tick();
            helper.assertTrue(stale.isRemoved(), "A stale loaded scythe must not resume attacking");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void virtualWeaponAttributesSurviveReloadWithoutDuplication(GameTestHelper helper) {
        for (var difficulty : FirstVicissitudeBossEntity.BossDifficulty.values()) {
            CompoundTag setup = phaseTwoTag();
            setup.putString("VicissitudeDifficulty", difficulty.serializedName());
            FirstVicissitudeBossEntity boss = spawn(helper, setup);
            float[] authoredPools = {500, 750, 1000, 1500};
            helper.assertTrue(boss.getHealth() == authoredPools[difficulty.ordinal()],
                    difficulty + ": actual initial health must match the confirmed difficulty table");
            double strength = boss.getAttributeValue(Attributes.ATTACK_DAMAGE);
            var armed = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
            armed.setNoAi(true);
            armed.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
            armed.setItemSlot(EquipmentSlot.MAINHAND, FirstVicissitudeBossEntity.createWeaponForDifficulty(difficulty));
            armed.tick(); // Vanilla equipment processing supplies the physical weapon baseline.
            double expected = armed.getAttributeValue(Attributes.ATTACK_DAMAGE);
            helper.assertTrue(strength == expected, difficulty + ": held weapon attack " + expected + ", virtual " + strength);
            armed.discard();
            CompoundTag saved = boss.saveWithoutId(new CompoundTag());
            helper.assertTrue(boss.getMainHandItem().isEmpty() && !saved.contains("StashedWeapon"), "No authoritative item stack");
            boss.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            FirstVicissitudeBossEntity loaded = MaledictEntities.FIRST_VICISSITUDE.get().create(helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(helper.getLevel().addFreshEntity(loaded), "Saved boss must reload");
            helper.assertTrue(loaded.getMainHandItem().isEmpty() && loaded.shouldRenderHeldWeapon(), "Weapon state survives loading");
            helper.assertTrue(loaded.getAttributeValue(Attributes.ATTACK_DAMAGE) == strength, "Loading must not duplicate modifiers");
            loaded.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void actualEntitySaveRetainsAdaptationCountersForNextHit(GameTestHelper helper) {
        DamageAdaptation adaptation = new DamageAdaptation();
        for (int hit = 0; hit < 3; hit++) adaptation.adapt("mob", 2);
        CompoundTag setup = phaseTwoTag();
        setup.put("DamageAdaptation", DamageAdaptationStorage.save(adaptation));
        FirstVicissitudeBossEntity boss = spawn(helper, setup);
        CompoundTag saved = boss.saveWithoutId(new CompoundTag());
        helper.assertTrue(saved.getList("DamageAdaptation", Tag.TAG_COMPOUND).getCompound(0).getInt("Hits") == 3,
                "Actual boss writer must preserve the loaded counter");
        var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        float initial = boss.getHealth();
        boss.hurt(boss.damageSources().mobAttack(attacker), 5);
        helper.assertTrue(boss.getHealth() < initial, "The real hit must debit vitality");
        CompoundTag afterHit = boss.saveWithoutId(new CompoundTag());
        helper.assertTrue(afterHit.getList("DamageAdaptation", Tag.TAG_COMPOUND).getCompound(0).getInt("Hits") == 4,
                "Real hurt must continue the remembered counter");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 90)
    public static void realDeathHandsConfiscatedCuriosToWorldLedgerOnce(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        CompoundTag setup = phaseTwoTag();
        CompoundTag curio = new CompoundTag();
        curio.putString("Slot", "charm");
        curio.putInt("Index", 0);
        curio.put("Stack", new ItemStack(Items.DIAMOND).save(org.brahypno.maledict.TestSupport.registries()));
        ListTag items = new ListTag(); items.add(curio);
        CompoundTag player = new CompoundTag(); player.putUUID("Player", owner); player.put("Items", items);
        ListTag players = new ListTag(); players.add(player); setup.put("ConfiscatedCurios", players);
        FirstVicissitudeBossEntity boss = spawn(helper, setup);
        VicissitudeVitalityLedger.get(helper.getLevel()).commit(boss.getUUID(),
                VicissitudeVitality.initial(boss.getMaxHealth()).withCurrent(0));
        boss.die(boss.damageSources().generic());
        boss.die(boss.damageSources().generic());
        var ledger = VicissitudeCurioLedger.get(helper.getLevel());
        helper.assertTrue(ledger.entries(owner).size() == 1, "Death must hand over exactly one confiscated item");
        helper.assertTrue(boss.getUUID().equals(ledger.entries(owner).get(0).encounter())
                && ledger.entries(owner).get(0).returnSite().position().equals(boss.blockPosition()),
                "Genuine death must persist the encounter and final return position for its offline owner");
        helper.runAtTickTime(85, () -> {
            helper.assertTrue(boss.isRemoved(), "Authored death sequence must finish");
            helper.assertTrue(ledger.entries(owner).size() == 1, "Removal must not duplicate or lose returned items");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    @SuppressWarnings("unchecked")
    public static void projectilePartUsesCollisionPointRatherThanProjectileOrigin(GameTestHelper helper) throws Exception {
        FirstVicissitudeBossEntity boss = spawn(helper, phaseTwoTag());
        invoke(boss, "refreshPose", new Class<?>[0], new Object[0]);
        List<VicissitudeRig.SegmentVolume> volumes = (List<VicissitudeRig.SegmentVolume>)
                invoke(boss, "segmentVolumes", new Class<?>[0], new Object[0]);
        var head = volumes.stream().filter(v -> v.segment() == VicissitudeRig.Segment.HEAD).findFirst().orElseThrow().box();
        Vec3 collision = new Vec3(head.centerX(), head.centerY(), head.centerZ());
        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        arrow.setPos(boss.position().add(0, -5, -5));
        NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(arrow, new EntityHitResult(boss, collision)));
        helper.assertTrue(boss.incomingHitWeight(boss.damageSources().arrow(arrow, null)) == 1.25F,
                "Recorded head impact must take the head cap even when the arrow origin is elsewhere");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    @SuppressWarnings("unchecked")
    public static void playerPartFollowsLookRayRatherThanAttackerFeet(GameTestHelper helper) throws Exception {
        FirstVicissitudeBossEntity boss = spawn(helper, phaseTwoTag());
        boss.setPos(128.5, 128, 128.5);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
            helper.getLevel().getChunk((128 >> 4) + dx, 8 + dz);
        invoke(boss, "refreshPose", new Class<?>[0], new Object[0]);
        List<VicissitudeRig.SegmentVolume> volumes = (List<VicissitudeRig.SegmentVolume>)
                invoke(boss, "segmentVolumes", new Class<?>[0], new Object[0]);
        var head = volumes.stream().filter(v -> v.segment() == VicissitudeRig.Segment.HEAD).findFirst().orElseThrow().box();
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PartHitTest"));
        // Start inside the upper head: this is an unambiguous head contact, with feet below it.
        player.setPos(head.centerX(), head.maxY() - 0.1 - player.getEyeHeight(), head.centerZ());
        player.setYRot(0);
        player.setXRot(0);
        helper.assertTrue(boss.incomingHitWeight(boss.damageSources().playerAttack(player)) == 1.25F,
                "Eye inside the upper head must resolve a head contact");
        // The broad moving wing root may occlude this head ray. Both are valid first contacts;
        // begin in front of every volume so looking away cannot begin inside an animated wing.
        double front = volumes.stream().mapToDouble(volume -> volume.box().minZ()).min().orElseThrow() - 0.25;
        player.setPos(head.centerX(), head.maxY() - 0.1 - player.getEyeHeight(), front);
        player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE)
                .setBaseValue(Math.max(3, head.centerZ() - front + 1));
        float contactWeight = boss.incomingHitWeight(boss.damageSources().playerAttack(player));
        helper.assertTrue(contactWeight == 1.25F || contactWeight == 0.75F,
                "Head/occluding-wing ray: eye=" + player.getEyePosition() + ", look=" + player.getLookAngle()
                        + ", reach=" + player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE)
                        + ", volumes=" + volumes + ", weight=" + boss.incomingHitWeight(boss.damageSources().playerAttack(player)));
        player.setYRot(180);
        helper.assertTrue(boss.incomingHitWeight(boss.damageSources().playerAttack(player)) == 1.0F,
                "The same attacker position looking away must fall back to body");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    @SuppressWarnings("unchecked")
    public static void actualSwingHitsNormalTargetAndMissesOtherFloors(GameTestHelper helper) throws Exception {
        FirstVicissitudeBossEntity boss = spawn(helper, phaseTwoTag());
        LivingEntity normal = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 4));
        LivingEntity above = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 4));
        LivingEntity below = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 4));
        normal.setPos(boss.position().add(0, 0, 3));
        above.setPos(boss.position().add(0, 8, 3));
        below.setPos(boss.position().add(0, -6, 3));
        for (LivingEntity victim : new LivingEntity[]{normal, above, below})
            boss.onIncomingAttack(boss.damageSources().mobAttack(victim), 1);
        float normalHealth = normal.getHealth(), aboveHealth = above.getHealth(), belowHealth = below.getHealth();
        boss.setYRot(0);
        invoke(boss, "startAction", new Class<?>[]{VicissitudeRig.Action.class, boolean.class},
                new Object[]{VicissitudeRig.Action.SLASH_HORIZONTAL, false});
        var clock = FirstVicissitudeBossEntity.class.getDeclaredField("DATA_ACTION_START");
        clock.setAccessible(true);
        boss.getEntityData().set((EntityDataAccessor<Long>) clock.get(null),
                helper.getLevel().getGameTime() - VicissitudeRig.Action.SLASH_HORIZONTAL.releaseTick());
        invoke(boss, "tickBladeHits", new Class<?>[0], new Object[0]);
        helper.assertTrue(normal.getHealth() < normalHealth, "Real swing must hit a standing target in front");
        helper.assertTrue(above.getHealth() == aboveHealth && below.getHealth() == belowHealth,
                "Same horizontal coordinates on different floors must stay outside the real attack");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "collision")
    public static void worldUnstickSearchEscapesThePreviouslyExhaustedNegativeSide(GameTestHelper helper) throws Exception {
        FirstVicissitudeBossEntity boss = spawn(helper, phaseTwoTag());
        boss.setPos(256.5, 128, 128.5);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
            helper.getLevel().getChunk((256 >> 4) + dx, 8 + dz);
        Map<BlockPos, BlockState> restore = new LinkedHashMap<>();
        int checked = 0;
        for (int dx = -4; dx <= 4 && checked < 64; dx += 2)
            for (int dz = -4; dz <= 4 && checked < 64; dz += 2)
                for (int dy = -8; dy <= 8 && checked < 64; dy += 2) {
                    checked++;
                    BlockPos marker = BlockPos.containing(boss.getX() + dx, boss.getY() + dy, boss.getZ() + dz);
                    restore.put(marker, helper.getLevel().getBlockState(marker));
                    helper.getLevel().setBlockAndUpdate(marker, Blocks.STONE.defaultBlockState());
                }
        double oldX = boss.getX();
        try {
            boolean escaped = (boolean) invoke(boss, "tryUnstickTeleport", new Class<?>[]{net.minecraft.server.level.ServerLevel.class},
                    new Object[]{helper.getLevel()});
            helper.assertTrue(escaped && boss.getX() > oldX, "Actual collision search must reach the free positive side");
            helper.assertTrue(helper.getLevel().noCollision(boss), "Escape position must be collision free");
        } finally {
            restore.forEach((position, state) -> helper.getLevel().setBlockAndUpdate(position, state));
        }
        helper.succeed();
    }

    private static CompoundTag phaseTwoTag() {
        CompoundTag tag = new CompoundTag();
        tag.putByte("VicissitudeStage", (byte) VicissitudeBossStage.PHASE_TWO.ordinal());
        tag.putBoolean("PhaseTwoReached", true);
        tag.putString("VicissitudeDifficulty", "simple");
        return tag;
    }

    private static FirstVicissitudeBossEntity spawn(GameTestHelper helper, CompoundTag setup) {
        FirstVicissitudeBossEntity boss = MaledictEntities.FIRST_VICISSITUDE.get().create(helper.getLevel());
        boss.readAdditionalSaveData(setup);
        boss.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        helper.assertTrue(helper.getLevel().addFreshEntity(boss), "Registered boss must join the real level");
        return boss;
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object[] arguments) throws Exception {
        Method method = FirstVicissitudeBossEntity.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, arguments);
    }
}
