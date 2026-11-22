package org.brahypno.maledict.common.realm.reliquary;

import com.sammy.malum.registry.common.block.MalumBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.brahypno.maledict.registry.MaledictEntities;

/**
 * 一次性的召唤祭坛：落地 20 tick 后，若 12 格内有玩家且不是和平难度，就在上方放出一头泣枯凋灵，
 * 并把自身换成 Malum 的符文污秽石砖；条件不满足则继续每 20 tick 复查一次。
 * <p>
 * 排的 tick 会随区块一起存盘，所以祭坛不需要方块实体——用掉之后留下的就是一块普通石砖，没有任何持续开销。
 */
public final class ReliquaryAltarBlock extends Block {
    public ReliquaryAltarBlock(Properties properties) {
        super(properties);
    }

    /** 1.21 把 {@code BlockBehaviour#onPlace} 收成了 protected；1.20.1 那边写的是 public，覆写只需不窄于父类。 */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) level.scheduleTick(pos, this, 20);
    }

    /** 同理：{@code tick} 在 1.21 也是 protected。 */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 1.21 把 getDifficulty 挪到了 LevelAccessor 的默认方法，ServerLevel 上照旧能直接调。
        Player player = level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 12.0D, false);
        if (player != null && level.getDifficulty() != Difficulty.PEACEFUL) {
            var wither = MaledictEntities.WEEPING_WITHER.get().create(level);
            if (wither != null) {
                wither.moveTo(pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
                wither.setPersistenceRequired();
                // 把祭坛坐标交给 Boss 当锚点：它会绕这里巡逻，且不会追远。
                wither.setAltarHome(pos.above(2));
                if (level.addFreshEntity(wither)) {
                    level.setBlock(pos, MalumBlocks.RUNIC_TAINTED_ROCK_BRICKS.get().defaultBlockState(), 3);
                    return;
                }
            }
        }
        level.scheduleTick(pos, this, 20);
    }
}
