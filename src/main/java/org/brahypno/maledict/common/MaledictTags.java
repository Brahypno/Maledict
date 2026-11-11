package org.brahypno.maledict.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.brahypno.maledict.Maledict;

/** Shared tag keys; generated JSON lives under {@code data/maledict/tags/items} and {@code .../blocks}. */
public final class MaledictTags {
    /** Ores this mod generates in the Blighted Realm; modpacks may extend either side. */
    public static final TagKey<Block> BLIGHTED_REALM_ORES = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "blighted_realm_ores"));
    public static final TagKey<Item> BLIGHTED_REALM_ORE_ITEMS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "blighted_realm_ores"));

    /** Confiscation-immune curios; the generated tag is empty on purpose, modpacks extend it via a data pack. */

    public static final TagKey<Item> VICISSITUDE_CONFISCATION_IMMUNE = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "vicissitude_confiscation_immune"));

    public static final TagKey<Item> AUFHEBEN_LIGHTWARD_ARMOR = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "aufheben_lightward_armor"));

    public static final TagKey<Item> AUFHEBEN_LIGHTWARD_WEAPONS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "aufheben_lightward_weapons"));

    public static final TagKey<Item> AUFHEBEN_LIGHTWARD_BLADES = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "aufheben_lightward_blades"));
    public static final TagKey<Item> AUFHEBEN_LIGHTWARD_AXES = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "aufheben_lightward_axes"));

    /** Main-hand tools the Rite of Soulbinding may arm. */
    public static final TagKey<Item> SOUL_BINDABLE = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "soul_bindable"));

    /** Entity types the Rite of Soulbinding must never capture. Empty on purpose; packs extend it. */
    public static final TagKey<EntityType<?>> SOUL_BINDING_BLACKLIST = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "soul_binding_blacklist"));

    private MaledictTags() {
    }
}
