package org.brahypno.maledict.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictBlocks;

import java.util.concurrent.CompletableFuture;

public final class MaledictBlockTags extends BlockTagsProvider {
    public MaledictBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFiles) {
        super(output, lookupProvider, Maledict.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(MaledictBlocks.MNEMONIC_OBELISK.get())
                .add(MaledictBlocks.MNEMONIC_OBELISK_COMPONENT.get())
                .add(MaledictBlocks.SOULWOOD_OBELISK.get())
                .add(MaledictBlocks.SOULWOOD_OBELISK_COMPONENT.get());
    }
}
