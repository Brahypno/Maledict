package org.brahypno.maledict.data;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.level.block.Block;
import org.brahypno.maledict.registry.MaledictBlocks;

import java.util.Set;
import java.util.List;

public final class MaledictBlockLoot extends BlockLootSubProvider {
    public MaledictBlockLoot() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        dropSelf(MaledictBlocks.MNEMONIC_OBELISK.get());
        dropSelf(MaledictBlocks.SOULWOOD_OBELISK.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(
                MaledictBlocks.MNEMONIC_OBELISK.get(),
                MaledictBlocks.SOULWOOD_OBELISK.get());
    }
}
