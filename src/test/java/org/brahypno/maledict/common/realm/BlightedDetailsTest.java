package org.brahypno.maledict.common.realm;

import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.raven.RavenEntity;
import org.brahypno.maledict.data.MaledictWorldgen;
import org.brahypno.maledict.registry.MaledictEntities;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class BlightedDetailsTest {

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
