package org.brahypno.maledict.common.realm;

import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.common.Tags;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictBlocks;

/** A dry well, ruined courtyard and buried storeroom. No portal or paired-well state is created. */
public final class AbandonedWellStationFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceLocation LOOT = ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "chests/abandoned_well_station");
    private static final int ROOM_Y = -12;

    public AbandonedWellStationFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        var random = context.random();
        var origin = context.origin();
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                var floor = RealmFeatureGround.surface(level, origin.getX() + x, origin.getZ() + z);
                if (!terrain(level.getBlockState(floor.below()))
                        || !clearable(level.getBlockState(floor))) return false;
                min = Math.min(min, floor.getY());
                max = Math.max(max, floor.getY());
            }
        }
        if (max - min > 2 || max + ROOM_Y - 2 <= level.getMinBuildHeight() || max + 5 >= level.getMaxBuildHeight()) return false;
        BlockPos center = new BlockPos(origin.getX(), max, origin.getZ());
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        // Inspect the entire footprint before writing, including the underground shell and its foundation.
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, ROOM_Y - 2, -8), center.offset(8, 5, 8))) {
            var state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null || !state.getFluidState().isEmpty()
                    || !(terrain(state) || clearable(state))) return false;
        }
        for (int x = -5; x <= 7; x++) {
            for (int z = -5; z <= 5; z++) {
                var foundation = at(center, facing, x, ROOM_Y - 2, z);
                if (!level.getBlockState(foundation).isFaceSturdy(level, foundation, Direction.UP)) return false;
            }
        }
        // The courtyard levels small natural variations; its broken paving exposes earth and stone.
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                BlockPos floor = at(center, facing, x, -1, z);
                for (int depth = 1; depth <= 3 && !level.getBlockState(floor.below(depth)).isFaceSturdy(level, floor.below(depth), Direction.UP); depth++) {
                    level.setBlock(floor.below(depth), BlockRegistry.TAINTED_ROCK.get().defaultBlockState(), 2);
                }
                level.setBlock(floor, random.nextInt(4) == 0 ? BlockRegistry.BLIGHTED_EARTH.get().defaultBlockState() : masonry(random), 2);
                for (int y = 0; y <= 3; y++) level.setBlock(at(center, facing, x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        // A solid shell keeps caves and soil outside the room; a side passage leads into storage.
        for (int x = -5; x <= 7; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = ROOM_Y - 1; y <= ROOM_Y + 3; y++) {
                    boolean room = x >= -4 && x <= 2 && Math.abs(z) <= 4;
                    boolean store = x >= 4 && x <= 6 && Math.abs(z) <= 3;
                    boolean passage = x == 3 && Math.abs(z) <= 1;
                    put(level, center, facing, x, y, z,
                            y >= ROOM_Y && y < ROOM_Y + 3 && (room || store || passage) ? Blocks.AIR.defaultBlockState() : masonry(random));
                }
            }
        }
        // Break the enclosure in several places, leaving a clear entrance on the front side.
        for (int edge = -7; edge <= 7; edge++) {
            for (int side : new int[]{-1, 1}) {
                int height = random.nextInt(4);
                for (int y = 0; y < height; y++) {
                    put(level, center, facing, side * 7, y, edge, masonry(random));
                    if (!(side == 1 && Math.abs(edge) <= 2)) put(level, center, facing, edge, y, side * 7, masonry(random));
                }
            }
        }
        // Soulwood uprights and an incomplete roof make a recognisable silhouette above the walls.
        for (int x : new int[]{-5, 5}) {
            for (int z : new int[]{-4, 4}) {
                for (int y = 0; y <= (x == 5 && z == 4 ? 2 : 4); y++) {
                    put(level, center, facing, x, y, z, BlockRegistry.SOULWOOD_LOG.get().defaultBlockState());
                }
            }
        }
        var beam = BlockRegistry.SOULWOOD_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, facing.getClockWise().getAxis());
        for (int x = -5; x <= 5; x++) put(level, center, facing, x, 4, -4, beam);
        for (int x = -5; x <= -2; x++) {
            for (int z = -4; z <= 0; z++) {
                if (random.nextInt(4) != 0) put(level, center, facing, x, 5, z, BlockRegistry.SOULWOOD_PLANKS.get().defaultBlockState());
            }
        }
        // A three-wide dry shaft opens the ceiling; the supported ladder reaches from floor to rim.
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                boolean rim = Math.abs(x) == 2 || Math.abs(z) == 2;
                for (int y = ROOM_Y; y <= 0; y++) {
                    put(level, center, facing, x, y, z, rim ? masonry(random) : Blocks.AIR.defaultBlockState());
                }
            }
        }
        // The bottom opens into the chamber and the storage passage on two sides.
        for (int y = ROOM_Y; y < ROOM_Y + 3; y++) {
            put(level, center, facing, 0, y, 2, Blocks.AIR.defaultBlockState());
            put(level, center, facing, 2, y, 0, Blocks.AIR.defaultBlockState());
        }
        for (int y = ROOM_Y; y <= 0; y++) {
            put(level, center, facing, 0, y, -1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing));
        }
        // Rubble blocks one old recess while the passage and the ladder exit remain traversable.
        for (int x = -4; x <= -3; x++) {
            put(level, center, facing, x, ROOM_Y, -4, BlockRegistry.TAINTED_ROCK.get().defaultBlockState());
            put(level, center, facing, x, ROOM_Y + 1, -4, BlockRegistry.TAINTED_ROCK.get().defaultBlockState());
        }
        put(level, center, facing, -4, 0, 2, MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get().defaultBlockState());
        put(level, center, facing, 4, 0, -3, MaledictBlocks.WITHERLIGHT_MUSHROOM.get().defaultBlockState());
        put(level, center, facing, -3, 0, -3, MaledictBlocks.WITHERLIGHT_LAMP.get().defaultBlockState());
        put(level, center, facing, -3, ROOM_Y, 3, MaledictBlocks.WITHERLIGHT_LAMP.get().defaultBlockState());
        put(level, center, facing, 5, ROOM_Y, -2, MaledictBlocks.WITHERLIGHT_LAMP.get().defaultBlockState());
        BlockPos chest = at(center, facing, 5, ROOM_Y, 2);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing.getCounterClockWise()), 2);
        RandomizableContainerBlockEntity.setLootTable(level, random, chest, LOOT);
        return true;
    }

    private static boolean terrain(BlockState state) {
        return RealmFeatureGround.natural(state) || state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Tags.Blocks.ORES) || state.is(MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get())
                || state.is(MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get());
    }

    private static boolean clearable(BlockState state) {
        return state.isAir() || state.getBlock() instanceof RealmGroundcoverBlock || state.is(BlockRegistry.BLIGHTED_GROWTH.get());
    }

    private static BlockState masonry(RandomSource random) {
        return (random.nextInt(5) == 0 ? BlockRegistry.TAINTED_ROCK.get() : BlockRegistry.TAINTED_ROCK_BRICKS.get()).defaultBlockState();
    }

    private static BlockPos at(BlockPos center, Direction facing, int x, int y, int z) {
        return center.relative(facing.getClockWise(), x).relative(facing, z).above(y);
    }

    private static void put(WorldGenLevel level, BlockPos center, Direction facing, int x, int y, int z, BlockState state) {
        level.setBlock(at(center, facing, x, y, z), state, 2);
    }
}
