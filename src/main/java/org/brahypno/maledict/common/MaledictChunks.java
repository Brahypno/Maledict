package org.brahypno.maledict.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkSource;

/**
 * 区块加载检查。
 *
 * <p>1.21.1 把 {@code LevelReader#hasChunkAt} / {@code hasChunksAt} 连同它们脚下的
 * {@code LevelReader#hasChunk(int, int)} 一起标成了弃用：区块在不在场现在由
 * {@code Level#getChunkSource()} 回答（{@code ChunkSource#hasChunk} 未弃用），单点查询收口到
 * {@code Level#isLoaded(BlockPos)}。整盒查询没有同名入口，所以这里用原版自己的区块矩形迭代器
 * {@link ChunkPos#rangeClosed(ChunkPos, ChunkPos)} 加 {@code allMatch} 重写一遍，
 * 与旧的四参 {@code hasChunksAt} 等价：矩形里每个区块都要在。
 *
 * <p>不写成「只查两个角」：矩形横跨三个区块以上时，两角之间的中间列没人查。现有两个调用点
 * （{@code SummoningRite#findLanding} 的 9 格净空、{@code InvocationSpawns#validSpace} 的生物包围盒）
 * 都只有 1~2 个区块宽，角落检查今天恰好够用——那是调用点的尺寸巧合，不是这个方法的契约。
 * 矩形最多也就 2×2 个区块，逐个查是四次哈希。
 *
 * <p>旧的两参 {@code hasChunksAt(BlockPos, BlockPos)} 还会顺带比较 y 是否落在世界高度内；
 * 两个调用点都在调用前自己查过 {@code minBuildHeight}/{@code maxBuildHeight}，所以这里只判区块。
 *
 * <p>两个调用点都要求<b>不加载区块</b>：仪式代码用它在未加载地形里跳过方块读取，而不是把区块拉起来。
 */
public final class MaledictChunks {
    /** {@return 两个角点围出的盒子里，每一个触及的区块是否都已加载} */
    public static boolean loaded(Level level, BlockPos first, BlockPos second) {
        ChunkSource chunks = level.getChunkSource();
        return ChunkPos.rangeClosed(new ChunkPos(first), new ChunkPos(second))
                       .allMatch(chunk -> chunks.hasChunk(chunk.x, chunk.z));
    }

    private MaledictChunks() {}
}
