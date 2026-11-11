package org.brahypno.maledict.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictTags;

import java.util.concurrent.CompletableFuture;

/**
 * Entity-type tags for the Soulbinding rite. The blacklist is emitted empty on purpose: packs and
 * modpacks extend it, and the base mod excludes nothing by default.
 */
public final class MaledictEntityTypeTags extends TagsProvider<EntityType<?>> {
    public MaledictEntityTypeTags(PackOutput output,
                                  CompletableFuture<HolderLookup.Provider> lookupProvider,
                                  ExistingFileHelper existingFiles) {
        super(output, Registries.ENTITY_TYPE, lookupProvider, Maledict.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(MaledictTags.SOUL_BINDING_BLACKLIST);
    }
}
