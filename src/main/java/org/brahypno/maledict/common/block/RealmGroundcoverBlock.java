package org.brahypno.maledict.common.block;

import com.sammy.malum.registry.common.block.MalumBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Low, collectible groundcover; Malum's blighted soil does not sustain ordinary bushes. */
public class RealmGroundcoverBlock extends BushBlock {
    public static final MapCodec<RealmGroundcoverBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(), Codec.INT.fieldOf("height").forGetter(block -> block.height)
    ).apply(instance, RealmGroundcoverBlock::new));
    private final int height;
    private final VoxelShape shape;

    public RealmGroundcoverBlock(Properties properties, int height) {
        super(properties);
        this.height = height;
        shape = Block.box(1, 0, 1, 15, height, 15);
    }

    @Override
    protected MapCodec<? extends RealmGroundcoverBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }

    @Override
    protected boolean mayPlaceOn(BlockState ground, BlockGetter level, BlockPos pos) {
        return ground.is(MalumBlocks.BLIGHTED_EARTH.get())
                || ground.is(BlockTags.DIRT) || ground.is(BlockTags.LOGS)
                || ground.is(MalumBlocks.TAINTED_ROCK.get()) || ground.is(MalumBlocks.TWISTED_ROCK.get())
                || ground.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }
}
