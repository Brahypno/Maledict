package org.brahypno.maledict.common.entity.demiurge;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Native ground navigation classifies breakable obstacles as their post-destruction state.
 *
 * <p><b>1.21 适配：</b>路径 API 换了一代 —— {@code BlockPathTypes} 改名 {@code PathType}，
 * {@code getBlockPathType(BlockGetter, int, int, int)} 换成
 * {@code getPathType(PathfindingContext, int, int, int)}，而 {@link PathfindingContext} 持有的是
 * {@code CollisionGetter}，1.20.1 那种「塞一个 {@link BlockGetter} 假视图给父类」的做法不再成立。
 * 这里改成直接判定：可破坏方块按「破坏后的格子」给出 {@code WALKABLE}（下方有支撑）或
 * {@code OPEN}（悬空），其余情况仍交给父类。
 */
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
                                  && EventHooks.canEntityGrief(mob.level(), mob);
            }

            @Override
            public void done() {
                breakingAllowed = false;
                super.done();
            }

            @Override
            public PathType getPathType(PathfindingContext context, int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                if (breakingAllowed && y >= Mth.floor(mob.getY())
                    && DemiurgeBlockBreaking.canBreak(context.getBlockState(pos), context.level(), pos, mob)) {
                    // 1.20.1 把一个「已挖掉」的假 BlockGetter 交给父类，由它按挖掉后的状态分类——
                    // 那个格子的方块状态就是它自己的流体状态（水/岩浆/空气）。1.21 的 getPathType 只拿到
                    // PathfindingContext，塞不进假视图，所以这里重放父类在那份视图上会给出的结果：
                    // 流体先分（岩浆对地面怪是 -1.0，等于不可通行），否则按下方是否有支撑给 WALKABLE/OPEN。
                    FluidState fluid = context.getBlockState(pos).getFluidState();
                    if (fluid.is(FluidTags.LAVA)) return PathType.LAVA;
                    if (fluid.is(FluidTags.WATER)) return PathType.WATER;
                    BlockPos below = pos.below();
                    boolean supported = !context.getBlockState(below).getCollisionShape(context.level(), below).isEmpty();
                    return supported ? PathType.WALKABLE : PathType.OPEN;
                }
                if (bridgesGap(context.level(), pos))
                    return PathType.WALKABLE;
                return super.getPathType(context, x, y, z);
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
                if (floor < pos.getY() - .5 && bridgesGap(mob.level(), pos))
                    return pos.getY();
                return floor;
            }

            private boolean bridgesGap(BlockGetter level, BlockPos pos) {
                int range = Mth.ceil(mob.getBbWidth() * .5F);
                if (range <= 0)
                    return false;
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    || !level.getFluidState(pos).isEmpty())
                    return false;
                BlockPos below = pos.below();
                if (!level.getFluidState(below).isEmpty()
                    || !level.getBlockState(below).getCollisionShape(level, below).isEmpty())
                    return false;
                boolean left = false, right = false, back = false, front = false;
                for (int dx = -range; dx <= range; dx++) {
                    for (int dz = -range; dz <= range; dz++) {
                        if (dx == 0 && dz == 0)
                            continue;
                        BlockPos support = below.offset(dx, 0, dz);
                        if (level.getBlockState(support).getCollisionShape(level, support).isEmpty())
                            continue;
                        if (dx < 0)
                            left = true;
                        if (dx > 0)
                            right = true;
                        if (dz < 0)
                            back = true;
                        if (dz > 0)
                            front = true;
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
}
