package org.brahypno.maledict.common.block;

import com.sammy.malum.common.block.curiosities.obelisk.ObeliskCoreBlockEntity;
import com.sammy.malum.common.block.curiosities.obelisk.runewood.RunewoodObeliskBlockEntity;
import com.sammy.malum.common.block.curiosities.spirit_altar.IAltarAccelerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.brahypno.maledict.registry.MaledictBlockEntities;
import org.brahypno.maledict.registry.MaledictBlocks;
import team.lodestar.lodestone.systems.multiblock.MultiBlockStructure;

import java.util.function.Supplier;

public final class SoulwoodObeliskBlockEntity extends ObeliskCoreBlockEntity implements IAltarAccelerator {
    public static final Supplier<MultiBlockStructure> STRUCTURE = () -> MultiBlockStructure.of(
            new MultiBlockStructure.StructurePiece(
                    0, 1, 0, MaledictBlocks.SOULWOOD_OBELISK_COMPONENT.get().defaultBlockState()));

    public SoulwoodObeliskBlockEntity(BlockPos pos, BlockState state) {
        super(MaledictBlockEntities.SOULWOOD_OBELISK.get(), STRUCTURE.get(), pos, state);
    }

    @Override
    public AltarAcceleratorType getAcceleratorType() {
        return RunewoodObeliskBlockEntity.OBELISK;
    }

    @Override
    public float getAcceleration() {
        return 0.5f;
    }
}
