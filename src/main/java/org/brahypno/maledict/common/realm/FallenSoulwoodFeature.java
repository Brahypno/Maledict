package org.brahypno.maledict.common.realm;

import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.brahypno.maledict.registry.MaledictBlocks;

public final class FallenSoulwoodFeature extends Feature<NoneFeatureConfiguration> {
    public FallenSoulwoodFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        var random = context.random();
        BlockPos start = RealmFeatureGround.surface(level, context.origin().getX(), context.origin().getZ());
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int length = 3 + random.nextInt(4);
        // Check the whole trunk first: no suspended logs across ravines or replacement of trees/structures.
        for (int i = 0; i < length; i++) {
            if (!RealmFeatureGround.dryGround(level, start.relative(direction, i))) return false;
        }
        var log = BlockRegistry.SOULWOOD_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, direction.getAxis());
        for (int i = 0; i < length; i++) level.setBlock(start.relative(direction, i), log, 2);
        for (int i = 0; i < length; i++) {
            BlockPos beside = start.relative(direction, i).relative(direction.getClockWise());
            var mushroom = MaledictBlocks.WITHERLIGHT_MUSHROOM.get().defaultBlockState();
            if (level.isEmptyBlock(beside) && mushroom.canSurvive(level, beside) && random.nextBoolean()) {
                level.setBlock(beside, mushroom, 2);
            }
        }
        return true;
    }
}
