package org.brahypno.maledict.common.realm;

import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.brahypno.maledict.registry.MaledictBlocks;

/** Small patches follow trees and shorelines, leaving the open wastes mostly bare. */
public final class BlightedDetailsFeature extends Feature<NoneFeatureConfiguration> {
    public BlightedDetailsFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        var random = context.random();
        boolean placed = false;
        for (int attempt = 0; attempt < 28; attempt++) {
            var pos = RealmFeatureGround.surface(level, context.origin().getX() + random.nextInt(9) - 4,
                    context.origin().getZ() + random.nextInt(9) - 4);
            if (!RealmFeatureGround.dryGround(level, pos)) continue;
            boolean wooded = nearWood(level, pos);
            boolean shore = RealmFeatureGround.waterNearby(level, pos);
            if (shore && random.nextInt(3) == 0) {
                level.setBlock(pos.below(), (random.nextBoolean() ? Blocks.MUD : Blocks.GRAVEL).defaultBlockState(), 2);
                placed = true;
            }
            Block detail;
            if (wooded) {
                detail = switch (random.nextInt(8)) {
                    case 0, 1 -> MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get();
                    case 2 -> MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get();
                    case 3 -> MaledictBlocks.WITHERLIGHT_MUSHROOM.get();
                    default -> MaledictBlocks.WITHERED_LEAF_LITTER.get();
                };
            } else if (shore) {
                detail = random.nextBoolean() ? MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get()
                        : MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get();
            } else {
                if (random.nextInt(5) != 0) continue;
                detail = MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get();
            }
            var state = detail.defaultBlockState();
            if (state.canSurvive(level, pos)) {
                level.setBlock(pos, state, 2);
                placed = true;
            }
        }
        return placed;
    }

    private static boolean nearWood(WorldGenLevel level, BlockPos pos) {
        for (BlockPos sample : BlockPos.betweenClosed(pos.offset(-3, 0, -3), pos.offset(3, 3, 3))) {
            var state = level.getBlockState(sample);
            if (state.is(BlockTags.LOGS) || state.is(BlockRegistry.SOULWOOD_LEAVES.get())) return true;
        }
        return false;
    }
}
