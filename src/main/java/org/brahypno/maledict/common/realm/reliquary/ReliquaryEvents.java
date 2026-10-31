package org.brahypno.maledict.common.realm.reliquary;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictBlocks;

@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class ReliquaryEvents {
    @SubscribeEvent
    public static void armAltarOnChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || !(chunk.getLevel() instanceof ServerLevel level)) return;
        var altar = MaledictBlocks.RELIQUARY_ALTAR.get();
        // Forge ChunkMap registers the tick container before posting ChunkEvent.Load.
        // findBlocks reads this supplied chunk's palettes/sections only; scheduleTick only queues work.
        // Do not fetch chunks, read world blocks, or spawn entities while the chunk load is completing.
        chunk.findBlocks((state, pos) -> state.is(altar),
                (pos, state) -> level.scheduleTick(pos.immutable(), altar, 20));
    }

    private ReliquaryEvents() {
    }
}
