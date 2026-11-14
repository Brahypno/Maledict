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
                breakingAllowed = ((DemiurgeStatueEntity) mob).canNavigate()
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
                if (bridgesGap(level, pos)) return BlockPathTypes.WALKABLE;
                return super.getBlockPathType(level, x, y, z);
            }

            /**
             * The raised front is over five blocks wide; a narrow missing floor is
             * carried by the body rather than stepped into. Treat the air over a
             * gap as a floor only while solid ground remains within half the body
             * width on at least one side. Wider gaps keep the normal OPEN/fall
             * handling.
             */
            @Override
            protected double getFloorLevel(BlockPos pos) {
                double floor = super.getFloorLevel(pos);
                if (floor < pos.getY() - .5 && bridgesGap(mob.level(), pos)) return pos.getY();
                return floor;
            }

            private boolean bridgesGap(BlockGetter level, BlockPos pos) {
                int range = Mth.ceil(mob.getBbWidth() * .5F);
                if (range <= 0) return false;
                BlockState state = level.getBlockState(pos);
                if (!state.getFluidState().isEmpty() || !state.getCollisionShape(level, pos).isEmpty()) return false;
                BlockPos below = pos.below();
                if (!level.getFluidState(below).isEmpty()
                        || !level.getBlockState(below).getCollisionShape(level, below).isEmpty()) return false;
                boolean left = false, right = false, back = false, front = false;
                for (int dx = -range; dx <= range; dx++) {
                    for (int dz = -range; dz <= range; dz++) {
                        if (dx == 0 && dz == 0) continue;
                        BlockPos support = below.offset(dx, 0, dz);
                        if (level.getBlockState(support).getCollisionShape(level, support).isEmpty()) continue;
                        if (dx < 0) left = true;
                        if (dx > 0) right = true;
                        if (dz < 0) back = true;
                        if (dz > 0) front = true;
                    }
                }
                // Support on opposite sides means a narrow floor gap the body can
                // span. Support on one side only is a cliff edge; do not walk off it.
                return (left && right) || (front && back);
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
