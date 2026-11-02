package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraftforge.event.ForgeEventFactory;

/** Native ground navigation classifies breakable obstacles as their post-destruction fluid/air state. */
final class DemiurgeNavigation extends GroundPathNavigation {
    DemiurgeNavigation(DemiurgeStatueEntity statue, Level level) {
        super(statue, level);
    }

    @Override
    protected PathFinder createPathFinder(int maximumVisitedNodes) {
        nodeEvaluator = new WalkNodeEvaluator() {
            private boolean breakingAllowed;

            @Override
            public void prepare(PathNavigationRegion region, Mob mob) {
                super.prepare(region, mob);
                breakingAllowed = ((DemiurgeStatueEntity) mob).canPatrol()
                        && ForgeEventFactory.getMobGriefingEvent(mob.level(), mob);
            }

            @Override
            public void done() {
                breakingAllowed = false;
                super.done();
            }

            @Override
            public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                if (breakingAllowed && y >= Mth.floor(mob.getY())
                        && DemiurgeBlockBreaking.canBreak(level.getBlockState(pos), level, pos, mob)) {
                    return super.getBlockPathType(new ClearedBlock(level, pos), x, y, z);
                }
                return super.getBlockPathType(level, x, y, z);
            }
        };
        nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(nodeEvaluator, maximumVisitedNodes);
    }

    /** Only the classified obstacle is removed; floor support and neighbouring hazards remain real. */
    private record ClearedBlock(BlockGetter level, BlockPos cleared) implements BlockGetter {
        @Override
        public BlockState getBlockState(BlockPos pos) {
            return pos.equals(cleared) ? level.getFluidState(pos).createLegacyBlock() : level.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) { return level.getFluidState(pos); }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) { return pos.equals(cleared) ? null : level.getBlockEntity(pos); }

        @Override
        public int getHeight() { return level.getHeight(); }

        @Override
        public int getMinBuildHeight() { return level.getMinBuildHeight(); }
    }
}
