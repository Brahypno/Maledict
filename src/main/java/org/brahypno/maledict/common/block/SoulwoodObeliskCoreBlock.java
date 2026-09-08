package org.brahypno.maledict.common.block;

import com.sammy.malum.common.block.curiosities.obelisk.ObeliskCoreBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.brahypno.maledict.registry.MaledictBlockEntities;

public final class SoulwoodObeliskCoreBlock extends ObeliskCoreBlock<SoulwoodObeliskBlockEntity> {
    public SoulwoodObeliskCoreBlock(BlockBehaviour.Properties properties) {
        super(properties, MaledictBlockEntities.SOULWOOD_OBELISK);
    }
}
