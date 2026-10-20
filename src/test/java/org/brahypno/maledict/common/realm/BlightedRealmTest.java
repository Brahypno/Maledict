package org.brahypno.maledict.common.realm;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.core.handlers.WeepingWellRejectionHandler;
import com.sammy.malum.registry.common.MalumAttachmentTypes;
import com.sammy.malum.registry.common.MalumMobEffects;
import com.sammy.malum.registry.common.block.MalumBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.item.curio.HangmanFoolItem;
import org.brahypno.maledict.data.worldgen.MaledictWorldgen;
import org.brahypno.maledict.registry.MaledictItems;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.Map;
import java.util.List;
import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class BlightedRealmTest {
    @GameTest(template = "test_empty", batch = "blighted_realm")
    public static void coordinateScaleAndIndependentSavedPairs(GameTestHelper helper) {
        check(helper, WeepingWellTravel.scaled(new BlockPos(1600, 12, -800), false).equals(new BlockPos(100, 12, -50)),
                "Entry scales X/Z by 1/16, keeping Y out of the horizontal conversion");
        check(helper, WeepingWellTravel.scaled(new BlockPos(-1, 20, -17), false).equals(new BlockPos(-1, 20, -2)),
                "Negative coordinates floor consistently with block positions");
        check(helper, WeepingWellTravel.scaled(new BlockPos(100, 12, -50), true).equals(new BlockPos(1600, 12, -800)),
                "Return-side unpaired wells scale X/Z by 16");
        var data = new WeepingWellLinks();
        var a = endpoint(Level.OVERWORLD, 1600, 64, -800);
        var b = endpoint(Level.OVERWORLD, 3200, 90, 1600);
        var x = endpoint(BlightedRealm.LEVEL, 105, 80, -50); // Safe-site displacement is preserved.
        var y = endpoint(BlightedRealm.LEVEL, 205, 80, 100);
        data.pair(a, x);
        data.pair(b, y);
        var loaded = WeepingWellLinks.load(data.save(new CompoundTag(), org.brahypno.maledict.TestSupport.registries()), org.brahypno.maledict.TestSupport.registries());
        check(helper, loaded.find(a.core()).other(a.core()).equals(x), "A always leads to its actual X well");
        check(helper, loaded.find(y.core()).other(y.core()).equals(b),
                "A player arriving from A but using Y returns to B, including after NBT reload");
        check(helper, loaded.find(x.core()).other(x.core()).equals(a), "The second pair cannot overwrite the first");
        var rebuiltA = endpoint(Level.OVERWORLD, 1632, 70, -800);
        loaded.replaceEndpoint(loaded.find(x.core()), rebuiltA);
        var rebuiltX = endpoint(BlightedRealm.LEVEL, 137, 80, -50);
        loaded.replaceEndpoint(loaded.find(rebuiltA.core()), rebuiltX);
        var rebuilt = WeepingWellLinks.load(loaded.save(new CompoundTag(), org.brahypno.maledict.TestSupport.registries()), org.brahypno.maledict.TestSupport.registries());
        check(helper, rebuilt.find(a.core()) == null && rebuilt.find(x.core()) == null,
                "Rebuilding either endpoint removes both stale identities");
        check(helper, rebuilt.find(rebuiltX.core()).other(rebuiltX.core()).equals(rebuiltA)
                        && rebuilt.find(rebuiltA.core()).other(rebuiltA.core()).equals(rebuiltX),
                "Rebuilt A' and X' remain mutually paired after NBT reload");
        check(helper, rebuilt.find(y.core()).other(y.core()).equals(b), "Rebuilding A/X cannot change B/Y");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "blighted_realm", timeoutTicks = 300)
    public static void dimensionAndNativeWorldgenLoad(GameTestHelper helper) {
        var realm = helper.getLevel().getServer().getLevel(BlightedRealm.LEVEL);
        check(helper, realm != null, "The generated dimension must load on a dedicated server");
        check(helper, realm.dimensionType().coordinateScale() == 16 && realm.dimensionType().hasFixedTime(),
                "The dimension has 16:1 scale and fixed celestial time");
        check(helper, !realm.dimensionType().respawnAnchorWorks(), "Respawn anchors must not bind a spawn");
        check(helper, realm.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator,
                "Terrain uses the overworld-style noise generator");
        BlockPos sample = new BlockPos(512, 64, 512);
        realm.getChunk(sample);
        int surface = realm.getHeight(Heightmap.Types.OCEAN_FLOOR, sample.getX(), sample.getZ());
        var biome = realm.getBiome(sample);
        check(helper, biome.is(BiomeTags.IS_OVERWORLD) && biome.is(BlightedRealm.WASTES),
                "The initial biome exposes the overworld generation tag");
        check(helper, biome.is(BiomeTags.HAS_STRONGHOLD),
                "Strongholds inherit overworld membership through the vanilla nested tag");
        check(helper, !biome.value().hasPrecipitation() && biome.value().getWaterColor() == 0x30243E,
                "The biome has still weather and purple water");
        var ground = realm.getBlockState(new BlockPos(sample.getX(), surface - 1, sample.getZ()));
        check(helper, ground.is(MalumBlocks.BLIGHTED_EARTH.get())
                        || ground.is(MalumBlocks.TAINTED_ROCK.get()) || ground.is(MalumBlocks.BLIGHTED_SOULWOOD.get())
                        || ground.is(MalumBlocks.SOULWOOD_LOG.get()) || ground.is(BlockTags.LEAVES),
                "Surface generation must use blighted ground or soulwood; got " + ground + " at y=" + surface);
        check(helper, realm.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).containsKey(MaledictWorldgen.TREES.location()),
                "The native soulwood configured feature must load");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "blighted_realm")
    public static void actualEquipmentPermissionsIgnoreStageAndRender(GameTestHelper helper) {
        FakePlayer player = player(helper.getLevel());
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        var vision = new CurioStacksHandler(inventory, "delusion", 1, true, true, true, ICurio.DropRule.DEFAULT);
        inventory.setCurios(Map.of("delusion", vision));
        check(helper, !WellAdmission.hasVisionEquipment(player), "An empty vision slot grants no passage");
        for (int stage = 0; stage <= 4; stage++) {
            vision.getStacks().setStackInSlot(0, HangmanFoolItem.create(stage));
            vision.getRenders().set(0, false);
            check(helper, WellAdmission.hasVisionEquipment(player), "Vision-exclusive equipment works at stage " + stage + " with rendering off");
        }
        vision.getStacks().setStackInSlot(0, new ItemStack(MaledictItems.AGE_OF_ENLIGHTENMENT.get()));
        check(helper, !WellAdmission.hasVisionEquipment(player), "Forcing a different-slot curio into vision does not grant passage");
        vision.getStacks().setStackInSlot(0, new ItemStack(MaledictItems.BLEEDING_EYE.get()));
        check(helper, !WellAdmission.hasVisionEquipment(player), "A material is not vision equipment");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "blighted_realm")
    public static void unqualifiedRejectionRemainsNative(GameTestHelper helper) {
        FakePlayer player = player(helper.getLevel());
        BlockPos core = makeSource(helper, player);
        var darkness = player.getData(MalumAttachmentTypes.WEEPING_WELL_INFO);
        darkness.voidRejection = 59;
        darkness.primordialGoopTimer = 2;
        WeepingWellRejectionHandler.entityTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(player));
        check(helper, player.serverLevel() == helper.getLevel() && darkness.isInRejectedState && darkness.voidRejection == 60,
                "An unqualified 60th contact tick must execute Malum's rejection");
        check(helper, player.hasEffect(MalumMobEffects.REJECTED)
                        && player.getData(MalumAttachmentTypes.PROGRESSION_DATA).hasBeenRejected,
                "Native rejected effect and revelation state remain intact");
        check(helper, WeepingWellLinks.get(helper.getLevel()).find(GlobalPos.of(Level.OVERWORLD, core)) == null,
                "Unqualified rejection must not create a paired destination");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "blighted_realm", timeoutTicks = 400)
    public static void rejectionHookBuildsAndReturnsThroughPairedWell(GameTestHelper helper) {
        FakePlayer player = player(helper.getLevel());
        BlockPos core = makeSource(helper, player);
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow(IllegalStateException::new);
        inventory.setCurios(Map.of("delusion", new CurioStacksHandler(inventory, "delusion", 1,
                true, true, true, ICurio.DropRule.DEFAULT)));
        inventory.setEquippedCurio("delusion", 0, HangmanFoolItem.create(0));
        var darkness = player.getData(MalumAttachmentTypes.WEEPING_WELL_INFO);
        darkness.voidRejection = 59;
        darkness.primordialGoopTimer = 2;
        WeepingWellRejectionHandler.entityTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(player));
        check(helper, BlightedRealm.is(player.serverLevel()), "The real 60th-tick rejection call must traverse through the mixin; dimension="
                + player.level().dimension().location() + ", rejection=" + darkness.voidRejection + ", contact=" + darkness.primordialGoopTimer
                + ", eligible=" + WellAdmission.hasVisionEquipment(player));
        var links = WeepingWellLinks.get(player.serverLevel());
        var link = links.find(GlobalPos.of(Level.OVERWORLD, core));
        check(helper, link != null && player.serverLevel().getBlockState(link.realm().core().pos()).is(MalumBlocks.VOID_CONDUIT.get()),
                "Traversal must place the native well template and remember both ends");
        darkness = player.getData(MalumAttachmentTypes.WEEPING_WELL_INFO);
        check(helper, darkness.isInRejectedState && darkness.voidRejection == 60 && player.getData(MalumAttachmentTypes.TOUCH_OF_DARKNESS).touchOfDarkness == 100
                        && !player.hasEffect(MalumMobEffects.REJECTED)
                        && !player.getData(MalumAttachmentTypes.PROGRESSION_DATA).hasBeenRejected,
                "Emergence reuses native motion and darkness without rejected potion or revelation: rejection=" + darkness.voidRejection
                        + ", rejected=" + darkness.isInRejectedState + ", touch=" + player.getData(MalumAttachmentTypes.TOUCH_OF_DARKNESS).touchOfDarkness
                        + ", potion=" + player.hasEffect(MalumMobEffects.REJECTED)
                        + ", progression=" + player.getData(MalumAttachmentTypes.PROGRESSION_DATA).hasBeenRejected);
        check(helper, player.getPersistentData().contains(WeepingWellTravel.EMERGENCE_UNTIL),
                "The emergence fall has explicit, temporary protection");
        var fall = new LivingFallEvent(player, 16, 1);
        NeoForge.EVENT_BUS.post(fall);
        check(helper, fall.isCanceled(), "The emergence fall must not cause damage");
        var secondFall = new LivingFallEvent(player, 16, 1);
        NeoForge.EVENT_BUS.post(secondFall);
        check(helper, !secondFall.isCanceled(), "The same transit cannot grant permanent fall immunity");
        inventory.setEquippedCurio("delusion", 0, ItemStack.EMPTY);
        player.setPos(Vec3.atBottomCenterOf(link.realm().core().pos()));
        check(helper, WeepingWellTravel.tryTraverse(player) && BlightedRealm.is(player.serverLevel()),
                "A transit cooldown must prevent immediately returning");
        player.getPersistentData().remove("maledict:well_cooldown");
        WeepingWellTravel.clearContact(player);
        WeepingWellRejectionHandler.launchPlayer(player);
        check(helper, player.serverLevel() == helper.getLevel(), "The paired return must work after removing the equipment");
        check(helper, player.position().distanceTo(Vec3.atCenterOf(core)) < 12,
                "Return must emerge at the paired source, rather than the dimension spawn");
        player.discard();
        helper.getLevel().setBlockAndUpdate(core, Blocks.AIR.defaultBlockState());
        var realm = helper.getLevel().getServer().getLevel(BlightedRealm.LEVEL);
        FakePlayer stranded = player(realm);
        stranded.setPos(Vec3.atBottomCenterOf(link.realm().core().pos()));
        WeepingWellRejectionHandler.launchPlayer(stranded);
        var repaired = links.find(link.realm().core());
        check(helper, repaired != null && !repaired.overworld().core().equals(link.overworld().core())
                        && links.find(link.overworld().core()) == null,
                "Returning through X must replace destroyed A with a newly paired A' rather than using spawn");
        check(helper, stranded.serverLevel() == helper.getLevel()
                        && helper.getLevel().getBlockState(repaired.overworld().core().pos()).is(MalumBlocks.VOID_CONDUIT.get())
                        && stranded.position().distanceTo(Vec3.atCenterOf(repaired.overworld().core().pos())) < 12,
                "An unequipped returner must emerge from the actual rebuilt overworld well");
        BlockPos repairedCore = repaired.overworld().core().pos();
        double dx = (double) repairedCore.getX() - core.getX();
        double dz = (double) repairedCore.getZ() - core.getZ();
        check(helper, dx * dx + dz * dz < 100 * 100,
                "The rebuilt source must stay near its former X/Z; Y is selected from safe terrain: " + core + " -> " + repairedCore);
        stranded.discard();
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "blighted_realm")
    public static void blightedGroundSupportsAnimalsWithoutChangingOverworld(GameTestHelper helper) {
        var realm = helper.getLevel().getServer().getLevel(BlightedRealm.LEVEL);
        BlockPos pos = new BlockPos(1024, 160, 1024);
        realm.setBlockAndUpdate(pos.below(), MalumBlocks.BLIGHTED_EARTH.get().defaultBlockState());
        var allowed = new MobSpawnEvent.SpawnPlacementCheck(EntityType.COW, realm, MobSpawnType.NATURAL,
                pos, realm.random, false);
        NeoForge.EVENT_BUS.post(allowed);
        check(helper, allowed.getResult() == MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED, "Animals can use the realm's blighted surface");
        var untouched = new MobSpawnEvent.SpawnPlacementCheck(EntityType.COW, helper.getLevel(), MobSpawnType.NATURAL,
                pos, realm.random, false);
        NeoForge.EVENT_BUS.post(untouched);
        check(helper, untouched.getResult() == MobSpawnEvent.SpawnPlacementCheck.Result.DEFAULT, "The same adaptation must not change the overworld");
        realm.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "blighted_realm")
    public static void fractionalGroundAndSpawnBinding(GameTestHelper helper) {
        FakePlayer player = player(helper.getLevel());
        BlockPos ground = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(ground, MalumBlocks.BLIGHTED_EARTH.get().defaultBlockState());
        Vec3 landing = new Vec3(ground.getX() + 0.5D, ground.getY() + 14.0D / 16, ground.getZ() + 0.5D);
        check(helper, WellPlacement.safe(helper.getLevel(), player, landing), "Safe feet height must use the real 14/16 collision shape");
        check(helper, !WellPlacement.safe(helper.getLevel(), player, landing.add(0, 0.125, 0)),
                "A full-block-height assumption must not count as grounded");
        player.setRespawnPosition(BlightedRealm.LEVEL, new BlockPos(0, 80, 0), 0, true, false);
        check(helper, !player.getRespawnDimension().equals(BlightedRealm.LEVEL), "Spawn binding to the realm must be rejected");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "blighted_realm", timeoutTicks = 400)
    public static void undergroundHostLayersAndOreReplacement(GameTestHelper helper) {
        var realm = helper.getLevel().getServer().getLevel(BlightedRealm.LEVEL);
        var settings = realm.registryAccess().registryOrThrow(Registries.NOISE_SETTINGS).get(BlightedRealm.NOISE);
        check(helper, settings != null && settings.defaultBlock().is(MalumBlocks.TAINTED_ROCK.get()),
                "Tainted rock must be the full noise terrain's default block, not only its surface rule");
        var tainted = MalumBlocks.TAINTED_ROCK.get().defaultBlockState();
        var twisted = MalumBlocks.TWISTED_ROCK.get().defaultBlockState();
        check(helper, tainted.is(BlockTags.STONE_ORE_REPLACEABLES) && twisted.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                        && tainted.is(BlockTags.BASE_STONE_OVERWORLD) && twisted.is(BlockTags.BASE_STONE_OVERWORLD),
                "Both layers must join their matching ore-replacement and base-stone tags");
        realm.getChunk(4096 >> 4, 4096 >> 4);
        int upper = 0;
        int lower = 0;
        int wrongLayer = 0;
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(4096, -48, 4096), new BlockPos(4111, 32, 4111))) {
            var state = realm.getBlockState(pos);
            if (pos.getY() >= 16 && state.is(MalumBlocks.TAINTED_ROCK.get())) upper++;
            if (pos.getY() <= -16 && state.is(MalumBlocks.TWISTED_ROCK.get())) lower++;
            if ((pos.getY() >= 16 && state.is(MalumBlocks.TWISTED_ROCK.get()))
                    || (pos.getY() <= -16 && state.is(MalumBlocks.TAINTED_ROCK.get()))) wrongLayer++;
        }
        check(helper, upper > 128 && lower > 128 && wrongLayer == 0,
                "Generated underground mass must have the confirmed upper/lower hosts: upper=" + upper + ", lower=" + lower + ", wrong=" + wrongLayer);

        var ore = new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(List.of(
                OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), Blocks.IRON_ORE.defaultBlockState()),
                OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), Blocks.DEEPSLATE_IRON_ORE.defaultBlockState())), 12));
        for (boolean deep : new boolean[]{false, true}) {
            BlockPos center = new BlockPos(4200, deep ? -24 : 24, 4200);
            var host = deep ? twisted : tainted;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -5, -5), center.offset(5, 5, 5))) {
                realm.setBlockAndUpdate(pos, host);
            }
            ore.place(realm, realm.getChunkSource().getGenerator(), RandomSource.create(73), center);
            int found = 0;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -5, -5), center.offset(5, 5, 5))) {
                if (realm.getBlockState(pos).is(deep ? Blocks.DEEPSLATE_IRON_ORE : Blocks.IRON_ORE)) found++;
            }
            check(helper, found > 0, "The real vanilla ore feature must replace the " + (deep ? "twisted" : "tainted") + " host");
        }
        helper.succeed();
    }

    private static FakePlayer player(ServerLevel level) {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "BlightTest"));
        // Real dimension transfer queries negotiated payload channels. FakePlayer has no channel.
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                player.connection.getConnection(), player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public boolean hasChannel(net.minecraft.resources.ResourceLocation id) { return false; }
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,
                                       net.minecraft.network.PacketSendListener listener) {}
        };
        return player;
    }

    private static BlockPos makeSource(GameTestHelper helper, FakePlayer player) {
        BlockPos core = helper.absolutePos(new BlockPos(2, 2, 2));
        for (BlockPos ground : BlockPos.betweenClosed(core.offset(-4, -1, -4), core.offset(4, -1, 4))) {
            helper.getLevel().setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());
        }
        helper.getLevel().setBlockAndUpdate(core, MalumBlocks.VOID_CONDUIT.get().defaultBlockState());
        player.setPos(Vec3.atBottomCenterOf(core));
        return core;
    }

    private static WeepingWellLinks.Endpoint endpoint(net.minecraft.resources.ResourceKey<Level> dimension, int x, int y, int z) {
        return new WeepingWellLinks.Endpoint(dimension, new BlockPos(x, y, z), new Vec3(x + 3.5D, y + 2, z + 0.5D));
    }

    private static void check(GameTestHelper helper, boolean result, String message) {
        helper.assertTrue(result, message);
    }
}
