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

    /**
     * Weapons whose Lodestone {@code MAGIC_DAMAGE} is handled by Maledict's attribute dispatcher
     * instead of Lodestone's own {@code LodestoneAttributeEventHandler}. Datapacks can extend this
     * to other Maledict-compatible weapons.
     */
    public static final TagKey<Item> LODESTONE_MAGIC_TAKEOVER = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "lodestone_magic_takeover"));

    /** Main-hand tools the Rite of Soulbinding may arm. */
    public static final TagKey<Item> SOUL_BINDABLE = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "soul_bindable"));

    /**
     * 沉辉的能力载体。空着生成，数据包或 Maledict 自己的物品可以直接挂上来 —— 能力不认 Tetra，
     * 只认「这件东西是不是载体」，见 {@code MaledictAbilities}。
     */
    public static final TagKey<Item> LUMENFALL_CARRIERS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "lumenfall_carriers"));

    /** 反照的能力载体，同上。 */
    public static final TagKey<Item> REFULGENCE_CARRIERS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "refulgence_carriers"));

    /** Soulbinding-shaped items that must never be armed, released or re-bound (the transformed maid sword). */
    public static final TagKey<Item> SOUL_BINDING_TOOL_BLACKLIST = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "soul_binding_tool_blacklist"));

    /** Entity types the Rite of Soulbinding must never capture. Empty on purpose; packs extend it. */
    public static final TagKey<EntityType<?>> SOUL_BINDING_BLACKLIST = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "soul_binding_blacklist"));

    private MaledictTags() {
    }
}
