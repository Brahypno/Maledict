package org.brahypno.maledict.common.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Bone meal propagates the small mushroom rather than replacing it with a giant feature. */
public final class WitherlightMushroomBlock extends RealmGroundcoverBlock implements BonemealableBlock {
    public WitherlightMushroomBlock(Properties properties) {
        super(properties, 7);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + 0.3,
                    pos.getZ() + random.nextDouble(), 0, 0.005, 0);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) {
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
            if (level.isEmptyBlock(target) && state.canSurvive(level, target)) return true;
        }
        return false;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        var available = new java.util.ArrayList<BlockPos>();
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
            if (level.isEmptyBlock(target) && state.canSurvive(level, target)) {
                available.add(target.immutable());
            }
        }
        // At most three new mushrooms per application, and at least one when ground is available.
        for (int grown = 0; grown < 3 && !available.isEmpty(); grown++) {
            level.setBlockAndUpdate(available.remove(random.nextInt(available.size())), state);
        }
    }
}
