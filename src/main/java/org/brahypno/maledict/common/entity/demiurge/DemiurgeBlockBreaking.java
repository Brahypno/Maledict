package org.brahypno.maledict.common.entity.demiurge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Wither-strength contact destruction, limited to blocks obstructing the moving front.
 */
final class DemiurgeBlockBreaking {
    private DemiurgeBlockBreaking() {}

    static boolean canBreak(BlockState state, BlockGetter level, BlockPos pos, Mob statue) {
        // This boss is not a WitherBoss subclass, so the vanilla immunity tag
        // must be applied explicitly as well as the block's permission check.
        if (statue instanceof DemiurgeStatueEntity demiurge && demiurge.isTerrainBlocked(pos))
            return false;
        return !state.isAir() && !state.is(BlockTags.WITHER_IMMUNE) && state.canEntityDestroy(level, pos, statue);
    }

    static boolean clearAhead(DemiurgeStatueEntity statue, float yaw) {
        // 1.5 ticks of travel: enough for the next steering arc, not just the next
        // single step. The swept volume below also covers the side of a turn.
        return clearAhead(statue, yaw, Math.max(.55, statue.horizontalStep() * 1.5 + .2));
    }

    static boolean clearAhead(DemiurgeStatueEntity statue, float yaw, double reach) {
        var level = statue.level();
        boolean allowed = EventHooks.canEntityGrief(level, statue);
        AABB current = statue.getBoundingBox();
        Vec3 forward = Vec3.directionFromRotation(0, yaw).scale(Math.max(.35, reach));
        // Sweep from the body's current position to its predicted position. The
        // old one-tick translated box only looked straight ahead, so a turning
        // body had to touch the side blocks before it could decide to break them.
        var area = current.minmax(current.move(forward)).deflate(.001);
        var shape = Shapes.create(area);
        boolean passable = true, destroyed = false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
                                                   BlockPos.containing(area.maxX, area.maxY, area.maxZ))) {
            var state = level.getBlockState(pos);
            if (state.isAir() || state == state.getFluidState().createLegacyBlock())
                continue;
            var collision = state.getCollisionShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ());
            boolean solid = !collision.isEmpty();
            if (!solid && state.is(BlockTags.REPLACEABLE_BY_TREES))
                continue;
            if (solid && !Shapes.joinIsNotEmpty(shape, collision, BooleanOp.AND))
                continue;
            if (solid && isStep(level, pos, statue, collision))
                continue;
            boolean breakable = canBreak(state, level, pos, statue);
            if (allowed && breakable
                && EventHooks.onEntityDestroyBlock(statue, pos, state)
                && level.destroyBlock(pos, false, statue)) {
                // No item drops: this is terrain pressure, and a five-block-wide
                // wall shatter can otherwise spawn dozens of item entities and
                // produce a visible tick hitch exactly on the breaking tick.
                destroyed = true;
            } else {
                // Remember only genuine vetoes. mobGriefing being off is a
                // reversible setting, not proof that the block can never be
                // broken; marking it there would poison the pathfinder after the
                // rule is restored.
                if (solid && (!breakable || allowed))
                    statue.rememberBlockedTerrain(pos);
                if (solid)
                    passable = false;
            }
        }
        if (destroyed)
            level.levelEvent(null, 1022, statue.blockPosition(), 0);
        return passable;
    }

    /**
     * A block whose top is within the mob's step height and whose collision starts
     * at or below the feet is a step, not a wall. Let vanilla's step-up handling
     * take it instead of breaking it or declaring the path blocked; this also
     * keeps protected steps passable when mobGriefing is off.
     */
    private static boolean isStep(
            Level level, BlockPos pos,
            DemiurgeStatueEntity statue, VoxelShape collision) {
        double bottom = collision.min(Direction.Axis.Y);
        double top = collision.max(Direction.Axis.Y);
        if (top <= statue.getY() + .001)
            return true;
        if (bottom > statue.getY() + .05
            || top > statue.getY() + statue.maxUpStep() + .001)
            return false;
        // A capped one-block wall is not a step; the block above would still be
        // an obstacle even after climbing, so let the destruction path handle it.
        BlockPos above = pos.above();
        return level.getBlockState(above).getCollisionShape(level, above).isEmpty();
    }
}
