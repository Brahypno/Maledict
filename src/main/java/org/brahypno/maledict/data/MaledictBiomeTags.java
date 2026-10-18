package org.brahypno.maledict.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BiomeTags;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.BlightedRealm;

import java.util.concurrent.CompletableFuture;

public final class MaledictBiomeTags extends TagsProvider<Biome> {
    public MaledictBiomeTags(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                             ExistingFileHelper existingFiles) {
        super(output, Registries.BIOME, registries, Maledict.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BiomeTags.IS_OVERWORLD).add(BlightedRealm.WASTES);
    }
}
