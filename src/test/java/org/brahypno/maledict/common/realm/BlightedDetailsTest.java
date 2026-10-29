package org.brahypno.maledict.common.realm;

import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.raven.RavenEntity;
import org.brahypno.maledict.data.MaledictWorldgen;
import org.brahypno.maledict.registry.MaledictBlocks;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.registry.MaledictRealmFeatures;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class BlightedDetailsTest {
    @GameTest(template = "test_empty", batch = "blighted_details", timeoutTicks = 400)
    public static void portableGroundcoverAndMushroomPropagation(GameTestHelper helper) {
        var level = realm(helper);
        BlockPos center = new BlockPos(24000, 280, 24000);
        prepare(level, center);
        for (var block : new Block[]{MaledictBlocks.WITHERED_LEAF_LITTER.get(), MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get(),
                MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get(), MaledictBlocks.WITHERLIGHT_MUSHROOM.get()}) {
            var state = block.defaultBlockState();
            level.setBlockAndUpdate(center.below(), BlockRegistry.BLIGHTED_SOIL.get().defaultBlockState());
            helper.assertTrue(state.canSurvive(level, center), "All four decorations can be planted on blighted soil");
            var drops = Block.getDrops(state, level, center, null);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()) && drops.get(0).getCount() == 1,
                    "Collecting a decoration must return its placeable item");
        }
        var mushroom = MaledictBlocks.WITHERLIGHT_MUSHROOM.get().defaultBlockState();
        level.setBlockAndUpdate(center, mushroom);
        helper.assertTrue(mushroom.getLightEmission(level, center) == 7, "The planted mushroom emits light level seven");
        // Only one free planting position remains; propagation must find it and preserve every obstacle.
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 1, 2))) {
            if (!pos.equals(center)) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        BlockPos available = center.east();
        level.setBlockAndUpdate(available, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(center.north(), Blocks.CHEST.defaultBlockState());
        var block = (WitherlightMushroomBlock) mushroom.getBlock();
        helper.assertTrue(block.isValidBonemealTarget(level, center, mushroom, false), "The single empty patch accepts bone meal");
        block.performBonemeal(level, RandomSource.create(1), center, mushroom);
        helper.assertTrue(level.getBlockState(available).is(block) && level.getBlockState(center.north()).is(Blocks.CHEST),
                "Bone meal grows into the available patch without replacing a nearby container");
        level.setBlockAndUpdate(available.below(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(level.isEmptyBlock(available), "A mushroom loses support when its ground is removed");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "blighted_details", timeoutTicks = 400)
    public static void remnantsGenerateLootAndPreserveBuildings(GameTestHelper helper) {
        var level = realm(helper);
        for (boolean dig : new boolean[]{false, true}) {
            BlockPos center = new BlockPos(dig ? 24100 : 24050, 280, 24000);
            prepare(level, center);
            var key = dig ? MaledictWorldgen.ABANDONED_DIG : MaledictWorldgen.FAILED_RITE;
            var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(key);
            helper.assertTrue(feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(31), center),
                    "The registered remnant must place on flat natural ground");
            var chest = (ChestBlockEntity) level.getBlockEntity(center.offset(2, 0, 2));
            helper.assertTrue(chest != null, "A remnant contains its exploration reward");
            chest.unpackLootTable(null);
            boolean notes = false;
            boolean lead = false;
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                var stack = chest.getItem(slot);
                if (stack.is(Items.WRITTEN_BOOK)) {
                    notes = stack.hasTag() && stack.getTag().getList("pages", 8).size() == 2;
                }
                lead |= stack.is(org.brahypno.maledict.registry.MaledictItems.MALIGNANT_LEAD_FRAGMENT.get());
            }
            helper.assertTrue(notes && (!dig || lead), "Actual loot tables supply two-page notes and the dig's lead fragments");
            prepare(level, center);
            BlockPos obstruction = center.west(2);
            level.setBlockAndUpdate(obstruction, Blocks.CHEST.defaultBlockState());
            helper.assertTrue(!feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(31), center),
                    "A container in the footprint prevents the whole remnant from generating");
            helper.assertTrue(level.getBlockState(obstruction).is(Blocks.CHEST) && level.isEmptyBlock(center),
                    "Rejected placement preserves the building without leaving a partial ruin");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "blighted_details", timeoutTicks = 400)
    public static void forestFloorHabitatAndWorldgenIntegration(GameTestHelper helper) {
        var level = realm(helper);
        BlockPos center = new BlockPos(24150, 280, 24000);
        prepare(level, center);
        helper.assertTrue(!RavenEntity.checkRavenSpawnRules(MaledictEntities.RAVEN.get(), level, MobSpawnType.NATURAL,
                center, RandomSource.create(1)), "Bare wastes do not attract natural raven groups");
        level.setBlockAndUpdate(center.east(3), BlockRegistry.SOULWOOD_LOG.get().defaultBlockState());
        level.setBlockAndUpdate(center.above(4), BlockRegistry.SOULWOOD_LEAVES.get().defaultBlockState());
        helper.assertTrue(RavenEntity.checkRavenSpawnRules(MaledictEntities.RAVEN.get(), level, MobSpawnType.NATURAL,
                center, RandomSource.create(1)), "A nearby trunk provides raven habitat without a daylight requirement");
        helper.assertTrue(RealmFeatureGround.surface(level, center.getX(), center.getZ()).equals(center),
                "Ground details find the forest floor beneath the leaf canopy");
        var details = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(MaledictWorldgen.DETAILS);
        helper.assertTrue(details.place(level, level.getChunkSource().getGenerator(), RandomSource.create(5), center),
                "The configured ground-detail feature places patches beside a tree");
        var settings = level.getBiome(center).value().getGenerationSettings();
        for (var key : java.util.List.of(MaledictWorldgen.PLACED_DETAILS, MaledictWorldgen.PLACED_FALLEN_TREE,
                MaledictWorldgen.PLACED_FAILED_RITE, MaledictWorldgen.PLACED_ABANDONED_DIG)) {
            helper.assertTrue(settings.features().stream().flatMap(set -> set.stream()).anyMatch(feature -> feature.is(key)),
                    "The realm biome includes " + key.location());
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "blighted_details", timeoutTicks = 400)
    public static void fallenTreeDoesNotBridgeUnsupportedGround(GameTestHelper helper) {
        var level = realm(helper);
        BlockPos center = new BlockPos(24200, 280, 24000);
        prepare(level, center);
        // A single supported cell, surrounded by a one-block drop, rejects every trunk direction.
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -1, -5), center.offset(5, -1, 5))) {
            if (!pos.equals(center.below())) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        helper.assertTrue(!MaledictRealmFeatures.FALLEN_TREE.get().place(NoneFeatureConfiguration.INSTANCE, level,
                level.getChunkSource().getGenerator(), RandomSource.create(5), center), "A fallen trunk cannot float across a drop");
        helper.assertTrue(level.isEmptyBlock(center), "An unsupported tree leaves no partial trunk");
        helper.succeed();
    }

    private static ServerLevel realm(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(BlightedRealm.LEVEL);
        helper.assertTrue(level != null, "The realm loads on the game-test server");
        return level;
    }

    private static void prepare(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, -6, -6), center.offset(6, 8, 6))) {
            level.setBlockAndUpdate(pos, pos.getY() < center.getY()
                    ? BlockRegistry.BLIGHTED_EARTH.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
    }
}
