package org.brahypno.maledict.data;

import com.sammy.malum.registry.common.item.ItemTagRegistry;
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

    private static final TagKey<Item> GOETY_GRAVE_GLOVE =
            ItemTags.create(new ResourceLocation("goety", "grave_glove_boost"));

    private static final TagKey<Item> FORGE_SCYTHE =
            ItemTags.create(new ResourceLocation("forge", "scythe"));

    private static final TagKey<Item> L2_NO_SEAL =
            ItemTags.create(new ResourceLocation("l2hostility", "no_seal"));

    public MaledictItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagsProvider.TagLookup<Block>> blockTags, ExistingFileHelper existingFiles) {
        super(output, lookupProvider, blockTags, Maledict.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ItemTags.ARROWS)
                .add(MaledictItems.SACRED_SPIRIT_ARROW.get())
                .add(MaledictItems.WICKED_SPIRIT_ARROW.get())
                .add(MaledictItems.ARCANE_SPIRIT_ARROW.get())
                .add(MaledictItems.ELDRITCH_SPIRIT_ARROW.get())
                .add(MaledictItems.AERIAL_SPIRIT_ARROW.get())
                .add(MaledictItems.AQUEOUS_SPIRIT_ARROW.get())
                .add(MaledictItems.EARTHEN_SPIRIT_ARROW.get())
                .add(MaledictItems.INFERNAL_SPIRIT_ARROW.get());
        tag(FORGE_SCYTHE).add(MaledictItems.INCURSUS_BLADE.get());
        tag(ItemTagRegistry.SCYTHE).add(MaledictItems.INCURSUS_BLADE.get());
        tag(ItemTagRegistry.SOUL_HUNTER_WEAPON).add(MaledictItems.INCURSUS_BLADE.get());
        tag(ItemTagRegistry.MAGIC_CAPABLE_WEAPON).add(MaledictItems.INCURSUS_BLADE.get());
        tag(ItemTagRegistry.HIDDEN_UNTIL_VOID).add(MaledictItems.INCURSUS_BLADE.get());
        tag(GOETY_GRAVE_GLOVE).add(MaledictItems.INCURSUS_BLADE.get());
        tag(L2_NO_SEAL).add(MaledictItems.INCURSUS_BLADE.get());
    }
}
