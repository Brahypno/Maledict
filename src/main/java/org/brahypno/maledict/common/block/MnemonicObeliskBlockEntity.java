package org.brahypno.maledict.common.block;

import com.sammy.malum.common.block.curiosities.obelisk.ObeliskCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.brahypno.maledict.registry.MaledictBlockEntities;
import org.brahypno.maledict.registry.MaledictBlocks;
import team.lodestar.lodestone.systems.multiblock.MultiBlockStructure;

import java.util.function.Supplier;

public final class MnemonicObeliskBlockEntity extends ObeliskCoreBlockEntity {
    public static final Supplier<MultiBlockStructure> STRUCTURE = () -> MultiBlockStructure.of(
            new MultiBlockStructure.StructurePiece(
                    0, 1, 0, MaledictBlocks.MNEMONIC_OBELISK_COMPONENT.get().defaultBlockState()));

    public MnemonicObeliskBlockEntity(BlockPos pos, BlockState state) {
        super(MaledictBlockEntities.MNEMONIC_OBELISK.get(), STRUCTURE.get(), pos, state);
    }
}
