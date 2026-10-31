package org.brahypno.maledict.common.realm.reliquary;

import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.brahypno.maledict.registry.MaledictEntities;

/** Scheduled ticks survive chunk saves; the spent altar is an ordinary block with no ongoing work. */
public final class ReliquaryAltarBlock extends Block {
    public ReliquaryAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide) level.scheduleTick(pos, this, 20);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Player player = level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 12.0D, false);
        if (player != null && level.getDifficulty() != Difficulty.PEACEFUL) {
            var wither = MaledictEntities.WEEPING_WITHER.get().create(level);
            if (wither != null) {
                wither.moveTo(pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
                wither.setPersistenceRequired();
                wither.setAltarHome(pos.above(2));
                if (level.addFreshEntity(wither)) {
                    level.setBlock(pos, BlockRegistry.RUNIC_TAINTED_ROCK_BRICKS.get().defaultBlockState(), 3);
                    return;
                }
            }
        }
        level.scheduleTick(pos, this, 20);
    }
}
