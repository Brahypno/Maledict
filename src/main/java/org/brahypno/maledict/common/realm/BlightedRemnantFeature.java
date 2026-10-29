package org.brahypno.maledict.common.realm;

import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictBlocks;

/** Surface remnants use a checked seven-block footprint and never contain a functioning ritual. */
public final class BlightedRemnantFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceLocation RITE_LOOT = ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "chests/failed_rite");
    public static final ResourceLocation DIG_LOOT = ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "chests/abandoned_dig");
    private final boolean dig;

    public BlightedRemnantFeature(boolean dig) {
        super(NoneFeatureConfiguration.CODEC);
        this.dig = dig;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        var random = context.random();
        BlockPos center = RealmFeatureGround.surface(level, context.origin().getX(), context.origin().getZ());
        int min = center.getY();
        int max = min;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos surface = RealmFeatureGround.surface(level, center.getX() + dx, center.getZ() + dz);
                if (!RealmFeatureGround.dryGround(level, surface)) return false;
                min = Math.min(min, surface.getY());
                max = Math.max(max, surface.getY());
            }
        }
        if (max - min > 2) return false;
        center = new BlockPos(center.getX(), max, center.getZ());
        // Preflight every space that can be written before any mutation.
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -4, -3), center.offset(3, 3, 3))) {
            var state = level.getBlockState(pos);
            if (!state.isAir() && !RealmFeatureGround.natural(state)) return false;
        }
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        if (dig) {
            placeDig(level, center, facing);
        } else {
            placeRite(level, center, random);
        }
        BlockPos chest = center.offset(2, 0, 2);
        level.setBlock(chest.below(), BlockRegistry.TAINTED_ROCK_BRICKS.get().defaultBlockState(), 2);
        support(level, chest.below());
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
        RandomizableContainerBlockEntity.setLootTable(level, random, chest, dig ? DIG_LOOT : RITE_LOOT);
        return true;
    }

    private static void placeRite(WorldGenLevel level, BlockPos center, net.minecraft.util.RandomSource random) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz > 10) continue;
                BlockPos floor = center.offset(dx, -1, dz);
                for (int depth = 1; depth <= 3; depth++) {
                    BlockPos support = floor.below(depth);
                    if (!level.isEmptyBlock(support)) break;
                    level.setBlock(support, BlockRegistry.TAINTED_ROCK.get().defaultBlockState(), 2);
                }
                var stone = random.nextInt(4) == 0 ? BlockRegistry.TAINTED_ROCK.get()
                        : BlockRegistry.TAINTED_ROCK_BRICKS.get();
                level.setBlock(floor, stone.defaultBlockState(), 2);
            }
        }
        level.setBlock(center.below(), BlockRegistry.RUNIC_TAINTED_ROCK_BRICKS.get().defaultBlockState(), 2);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pillar = center.relative(direction, 2);
            int height = 1 + random.nextInt(3);
            for (int y = 0; y < height; y++) {
                level.setBlock(pillar.above(y), BlockRegistry.SOULWOOD_LOG.get().defaultBlockState(), 2);
            }
        }
        level.setBlock(center, MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get().defaultBlockState(), 2);
        level.setBlock(center.offset(1, 0, 1), MaledictBlocks.WITHERLIGHT_MUSHROOM.get().defaultBlockState(), 2);
    }

    private static void placeDig(WorldGenLevel level, BlockPos center, Direction facing) {
        // Three steps descend into a shallow, open quarry; no new ore placements are introduced.
        for (int depth = 0; depth < 3; depth++) {
            for (int side = -1; side <= 1; side++) {
                BlockPos step = center.relative(facing, depth - 1).relative(facing.getClockWise(), side).below(depth);
                level.setBlock(step.below(), BlockRegistry.TAINTED_ROCK.get().defaultBlockState(), 2);
                for (int clear = 0; clear <= depth + 1; clear++) level.setBlock(step.above(clear), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        for (int side : new int[]{-2, 2}) {
            BlockPos post = center.relative(facing.getClockWise(), side);
            for (int y = -1; y < 2; y++) level.setBlock(post.above(y), BlockRegistry.SOULWOOD_LOG.get().defaultBlockState(), 2);
            support(level, post.below());
        }
        var beam = BlockRegistry.SOULWOOD_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, facing.getClockWise().getAxis());
        for (int side = -2; side <= 1; side++) {
            level.setBlock(center.relative(facing.getClockWise(), side).above(2), beam, 2);
        }
        BlockPos roots = RealmFeatureGround.surface(level, center.relative(facing.getOpposite(), 2).getX(),
                center.relative(facing.getOpposite(), 2).getZ());
        if (level.isEmptyBlock(roots)) level.setBlock(roots, MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get().defaultBlockState(), 2);
    }

    private static void support(WorldGenLevel level, BlockPos floor) {
        for (int depth = 1; depth <= 3; depth++) {
            BlockPos below = floor.below(depth);
            if (!level.isEmptyBlock(below)) break;
            level.setBlock(below, BlockRegistry.TAINTED_ROCK.get().defaultBlockState(), 2);
        }
    }
}
