package org.brahypno.maledict.common.realm.reliquary;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictBlocks;

/**
 * 区块重新载入时把祭坛重新武装起来。
 * <p>
 * 排队的 tick 未必来得及在区块卸载前执行（生成后立刻卸载、或服务器重启），
 * 所以每次 {@code ChunkEvent.Load} 都扫一遍本区块里的祭坛方块，重新排一次 20 tick。
 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class ReliquaryEvents {
    @SubscribeEvent
    public static void armAltarOnChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || !(chunk.getLevel() instanceof ServerLevel level)) return;
        var altar = MaledictBlocks.RELIQUARY_ALTAR.get();
        // 1.20.1 的 Forge 与 1.21 的 NeoForge 都在 ChunkMap 登记好 tick 容器之后才发 ChunkEvent.Load。
        // findBlocks 只读这份区块自己的 palette/section，scheduleTick 只是入队：
        // 不要在区块加载尚未收尾时取别处的区块、读世界方块或生成实体。
        // 带坐标的 BiPredicate 重载在 1.21.1 已标「将移除」，这里只用按方块状态过滤的那个。
        chunk.findBlocks(state -> state.is(altar),
                (pos, state) -> level.scheduleTick(pos.immutable(), altar, 20));
    }

    private ReliquaryEvents() {
    }
}
