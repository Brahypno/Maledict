package org.brahypno.maledict.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings({"removal"})
public final class MaledictItemTags extends ItemTagsProvider {
    private static final TagKey<Item> MALUM_SCYTHE =
            ItemTags.create(new ResourceLocation("malum", "scythe"));
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON =
            ItemTags.create(new ResourceLocation("malum", "soul_hunter_weapon"));

    public MaledictItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagsProvider.TagLookup<Block>> blockTags, ExistingFileHelper existingFiles) {
        super(output, lookupProvider, blockTags, Maledict.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(MALUM_SCYTHE).add(MaledictItems.INCURSUS_BLADE.get());
        tag(MALUM_SOUL_HUNTER_WEAPON).add(MaledictItems.INCURSUS_BLADE.get());
    }
}
