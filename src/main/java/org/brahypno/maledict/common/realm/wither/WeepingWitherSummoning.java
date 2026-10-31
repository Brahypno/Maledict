package org.brahypno.maledict.common.realm.wither;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import org.brahypno.maledict.registry.MaledictBlocks;
import org.brahypno.maledict.registry.MaledictEntities;

public final class WeepingWitherSummoning {
    private static BlockPattern pattern;

    public static void trySpawn(Level level, BlockPos pos) {
        if (level.isClientSide || level.getDifficulty() == Difficulty.PEACEFUL) return;
        if (pattern == null) {
            pattern = BlockPatternBuilder.start().aisle("^^^", "TST", "~T~")
                    .where('^', BlockInWorld.hasState(BlockStatePredicate.forBlock(MaledictBlocks.BLIGHTED_WITHER_SKULL.get())))
                    .where('T', BlockInWorld.hasState(BlockStatePredicate.forBlock(MaledictBlocks.CRYING_TWISTED_ROCK.get())))
                    .where('S', BlockInWorld.hasState(BlockStatePredicate.forBlock(MaledictBlocks.CRYING_TAINTED_ROCK.get())))
                    .where('~', block -> block.getState().isAir()).build();
        }
        var match = pattern.find(level, pos);
        if (match == null) return;
        var wither = MaledictEntities.WEEPING_WITHER.get().create(level);
        if (wither == null) return;
        BlockPos base = match.getBlock(1, 2, 0).getPos();
        float rotation = match.getForwards().getAxis() == Direction.Axis.X ? 0 : 90;
        wither.moveTo(base.getX() + 0.5D, base.getY() + 0.55D, base.getZ() + 0.5D, rotation, 0);
        wither.yBodyRot = rotation;
        wither.makeInvulnerable();
        if (!level.addFreshEntity(wither)) return;
        CarvedPumpkinBlock.clearPatternBlocks(level, match);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, wither.getBoundingBox().inflate(50))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, wither);
        }
        CarvedPumpkinBlock.updatePatternBlocks(level, match);
    }

    private WeepingWitherSummoning() {
    }
}
