package org.brahypno.maledict.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;

import java.util.concurrent.CompletableFuture;

public final class MaledictBlockTags extends BlockTagsProvider {
    public MaledictBlockTags(PackOutput output,
                             CompletableFuture<HolderLookup.Provider> lookupProvider,
                             ExistingFileHelper existingFiles) {
        super(output, lookupProvider, Maledict.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
    }
}
