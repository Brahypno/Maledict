package org.brahypno.maledict.common.block;

import com.sammy.malum.common.block.curiosities.obelisk.ObeliskCoreBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.brahypno.maledict.registry.MaledictBlockEntities;

public final class MnemonicObeliskCoreBlock extends ObeliskCoreBlock<MnemonicObeliskBlockEntity> {
    public MnemonicObeliskCoreBlock(BlockBehaviour.Properties properties) {
        super(properties, MaledictBlockEntities.MNEMONIC_OBELISK);
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        return 10.0f;
    }
}
