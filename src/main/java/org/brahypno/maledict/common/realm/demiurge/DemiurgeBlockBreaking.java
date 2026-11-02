package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.event.ForgeEventFactory;

/** Wither-strength contact destruction, limited to blocks obstructing the moving front. */
final class DemiurgeBlockBreaking {
    private DemiurgeBlockBreaking() {}

    static boolean canBreak(BlockState state, BlockGetter level, BlockPos pos, Mob statue) {
        // This boss is not a WitherBoss subclass, so the vanilla immunity tag
        // must be applied explicitly as well as the block's Forge permission.
        return !state.isAir() && !state.is(BlockTags.WITHER_IMMUNE) && state.canEntityDestroy(level, pos, statue);
    }

    static boolean clearAhead(DemiurgeStatueEntity statue, float yaw) {
        var level = statue.level();
        boolean allowed = ForgeEventFactory.getMobGriefingEvent(level, statue);
        Vec3 forward = Vec3.directionFromRotation(0, yaw).scale(.35);
        var area = statue.getBoundingBox().move(forward).deflate(.001);
        var shape = Shapes.create(area);
        boolean passable = true, destroyed = false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
                BlockPos.containing(area.maxX, area.maxY, area.maxZ))) {
            var state = level.getBlockState(pos);
            if (state.isAir() || state == state.getFluidState().createLegacyBlock()) continue;
            var collision = state.getCollisionShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ());
            boolean solid = !collision.isEmpty();
            if (solid && !Shapes.joinIsNotEmpty(shape, collision, BooleanOp.AND)) continue;
            if (allowed && canBreak(state, level, pos, statue)
                    && ForgeEventFactory.onEntityDestroyBlock(statue, pos, state)
                    && level.destroyBlock(pos, true, statue)) {
                destroyed = true;
            } else if (solid) {
                passable = false;
            }
        }
        if (destroyed) level.levelEvent(null, 1022, statue.blockPosition(), 0);
        return passable;
    }
}
