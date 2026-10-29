package org.brahypno.maledict.data;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.level.block.Block;
import org.brahypno.maledict.registry.MaledictBlocks;
import org.brahypno.maledict.registry.MaledictItems;

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
        dropSelf(MaledictBlocks.WITHERED_LEAF_LITTER.get());
        dropSelf(MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get());
        dropSelf(MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get());
        dropSelf(MaledictBlocks.WITHERLIGHT_MUSHROOM.get());
        dropSelf(MaledictBlocks.WITHERLIGHT_LAMP.get());
        add(MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get(), block -> createOreDrop(block, MaledictItems.MALIGNANT_LEAD_FRAGMENT.get()));
        add(MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get(), block -> createOreDrop(block, MaledictItems.MALIGNANT_LEAD_FRAGMENT.get()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(
                MaledictBlocks.MNEMONIC_OBELISK.get(),
                MaledictBlocks.SOULWOOD_OBELISK.get(),
                MaledictBlocks.WITHERED_LEAF_LITTER.get(),
                MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get(),
                MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get(),
                MaledictBlocks.WITHERLIGHT_MUSHROOM.get(),
                MaledictBlocks.WITHERLIGHT_LAMP.get(),
                MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get(),
                MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get());
    }
}
