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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.item.curio.hangman.HangmanFoolItem;
import org.brahypno.maledict.data.worldgen.MaledictWorldgen;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.Map;
import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class BlightedRealmTest {

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
