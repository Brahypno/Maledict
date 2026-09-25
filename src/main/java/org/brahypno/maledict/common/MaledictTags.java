package org.brahypno.maledict.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.brahypno.maledict.Maledict;

/** Shared tag keys; generated JSON lives under {@code data/maledict/tags/items}. */
public final class MaledictTags {
    /** Confiscation-immune curios; the generated tag is empty on purpose, modpacks extend it via a data pack. */

    public static final TagKey<Item> VICISSITUDE_CONFISCATION_IMMUNE = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "vicissitude_confiscation_immune"));

    private MaledictTags() {
    }
}
