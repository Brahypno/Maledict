package org.brahypno.maledict.common.realm;

import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

final class RealmFeatureGround {
    static BlockPos surface(WorldGenLevel level, int x, int z) {
        // The WG map stops updating in a finished LevelChunk; /place and game tests need its live counterpart.
        var heightmap = level instanceof net.minecraft.server.level.ServerLevel
                ? Heightmap.Types.OCEAN_FLOOR : Heightmap.Types.OCEAN_FLOOR_WG;
        BlockPos top = level.getHeightmapPos(heightmap, new BlockPos(x, 0, z));
        // Trees also raise this heightmap. Find their floor instead of decorating the canopy.
        for (int down = 1; down <= 48 && top.getY() - down > level.getMinBuildHeight(); down++) {
            BlockPos below = top.below(down);
            var state = level.getBlockState(below);
            if (natural(state)) return below.above();
            if (!state.isAir() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)
                    && !(state.getBlock() instanceof RealmGroundcoverBlock)) break;
        }
        return top;
    }

    static boolean natural(BlockState state) {
        return state.is(BlockRegistry.BLIGHTED_EARTH.get()) || state.is(BlockRegistry.BLIGHTED_SOIL.get())
                || state.is(BlockRegistry.TAINTED_ROCK.get()) || state.is(BlockRegistry.TWISTED_ROCK.get())
                || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.MUD);
    }

    static boolean dryGround(WorldGenLevel level, BlockPos pos) {
        return level.isEmptyBlock(pos) && natural(level.getBlockState(pos.below()));
    }

    static boolean waterNearby(WorldGenLevel level, BlockPos pos) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (level.getFluidState(pos.offset(dx, 0, dz)).is(FluidTags.WATER)
                        || level.getFluidState(pos.offset(dx, -1, dz)).is(FluidTags.WATER)) return true;
            }
        }
        return false;
    }

    private RealmFeatureGround() {}
}
