package org.brahypno.maledict.common.realm;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.core.handlers.WeepingWellRejectionHandler;
import com.sammy.malum.registry.common.MalumAttachmentTypes;
import com.sammy.malum.registry.common.MalumMobEffects;
import com.sammy.malum.registry.common.block.MalumBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.item.curio.hangman.HangmanFoolItem;
import org.brahypno.maledict.common.realm.reliquary.InvertedReliquaryPiece;
import org.brahypno.maledict.common.realm.reliquary.InvertedReliquaryStructure;
import org.brahypno.maledict.data.worldgen.MaledictWorldgen;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.Map;
import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class BlightedRealmTest {

    /**
     * 地表断言写的是「表面规则跑过」，不是「每一列都恰好是腐枯材料」。
     *
     * <p>装饰步骤把矿脉放在表面规则之后（本维度里矿脉寄生在污秽石/扭曲石上，落下来的是原版矿石方块），
     * 所以露头矿脉能在地表掏出一块 {@code minecraft:iron_ore}，单列断言就红在这块矿石上——原版主世界
     * 同样如此，这不是表面规则的失败。改成两半：任何原版地表材料出现在采样列即失败（表面规则整体没跑时
     * 必红），腐枯材料必须占九列里的多数（表面规则大面积失效时必红），单列被矿脉或地衣穿透时不红。
     *
     * <p>地宫另有一条：模板必须真的被 {@code StructureTemplateManager} 读出来。1.21 把数据包里的模板目录
     * 从 {@code structures/} 改成了 {@code structure/}，读不到时管理器不抛异常，只给一份空模板——piece
     * 照样成立、{@code /locate} 照样能定位，可放置阶段（{@code placeInWorld} 见 {@code palettes} 为空即返回）
     * 一块方块都不放，正是「能定位却不生成」。GameTest 服务端本身不生成结构（{@code generateStructures=false}），
     * 所以这里查模板与 piece 的包围盒，而不是等自然生成。
     */
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
        var biome = realm.getBiome(sample);
        check(helper, biome.is(BiomeTags.IS_OVERWORLD) && biome.is(BlightedRealm.WASTES),
                "The initial biome exposes the overworld generation tag");
        check(helper, biome.is(BiomeTags.HAS_STRONGHOLD),
                "Strongholds inherit overworld membership through the vanilla nested tag");
        check(helper, !biome.value().hasPrecipitation() && biome.value().getWaterColor() == 0x30243E,
                "The biome has still weather and purple water");
        // 九列都留在 sample 自己那个区块里（区块占 512..527）；跨区块取样本会连带生成邻区块。
        int blighted = 0;
        String offender = "";
        String other = "";
        for (int dx : new int[]{2, 8, 13}) {
            for (int dz : new int[]{2, 8, 13}) {
                int x = sample.getX() + dx;
                int z = sample.getZ() + dz;
                int height = realm.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                var ground = realm.getBlockState(new BlockPos(x, height - 1, z));
                if (blightedGround(ground)) {
                    blighted++;
                } else if (vanillaSurface(ground)) {
                    offender = ground + " at " + x + "," + (height - 1) + "," + z;
                } else {
                    other = ground + " at " + x + "," + (height - 1) + "," + z;
                }
            }
        }
        check(helper, offender.isEmpty(), "Surface generation must not leave vanilla surface materials; got " + offender);
        check(helper, blighted * 2 > 9,
                "Blighted ground must still dominate the sampled surface; only " + blighted + "/9 columns, last outlier " + other);
        check(helper, realm.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).containsKey(MaledictWorldgen.TREES.location()),
                "The native soulwood configured feature must load");

        var templates = helper.getLevel().getServer().getStructureManager();
        Vec3i size = new Vec3i(InvertedReliquaryStructure.SIZE, InvertedReliquaryStructure.HEIGHT, InvertedReliquaryStructure.SIZE);
        for (String variant : new String[]{"intact", "fractured", "sunken"}) {
            var template = templates.get(InvertedReliquaryPiece.templateId(variant));
            check(helper, template.isPresent() && template.get().getSize().equals(size),
                    "The " + variant + " reliquary template must load as " + size + " from "
                            + "data/maledict/structure/inverted_reliquary/" + variant + ".nbt (1.21's template folder is singular); got "
                            + template.map(loaded -> loaded.getSize().toString()).orElse("no such template"));
        }
        var turned = new InvertedReliquaryPiece(templates, BlockPos.ZERO, Rotation.CLOCKWISE_90, "intact").getBoundingBox();
        check(helper, turned.maxX() - turned.minX() + 1 == InvertedReliquaryStructure.SIZE
                        && turned.maxZ() - turned.minZ() + 1 == InvertedReliquaryStructure.SIZE
                        && turned.maxY() - turned.minY() + 1 == InvertedReliquaryStructure.HEIGHT,
                "A quarter-turned reliquary must stay inside its own footprint around the pivot; got " + turned);
        helper.succeed();
    }

    private static boolean blightedGround(BlockState state) {
        return state.is(MalumBlocks.BLIGHTED_EARTH.get()) || state.is(MalumBlocks.TAINTED_ROCK.get())
                || state.is(MalumBlocks.BLIGHTED_SOULWOOD.get()) || state.is(MalumBlocks.SOULWOOD_LOG.get())
                || state.is(BlockTags.LEAVES);
    }

    /** 出现即说明表面规则没跑：矿石、地衣、水这些后置装饰步骤放上去的方块都不在此列。 */
    private static boolean vanillaSurface(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE) || state.is(Blocks.TUFF)
                || state.is(Blocks.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.PACKED_ICE);
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
