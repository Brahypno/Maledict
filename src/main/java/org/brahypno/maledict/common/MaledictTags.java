package org.brahypno.maledict.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.brahypno.maledict.Maledict;

/** Shared tag keys; generated JSON lives under {@code data/maledict/tags/items}. */
public final class MaledictTags {
    /** Confiscation-immune curios; the generated tag is empty on purpose, modpacks extend it via a data pack. */

    public static final TagKey<Item> VICISSITUDE_CONFISCATION_IMMUNE = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "vicissitude_confiscation_immune"));

    /**
     * 沉辉与反照的载体：数据包或自有武器直接挂上去，能力不依赖任何兼容前置。
     */
    public static final TagKey<Item> LUMENFALL_CARRIERS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "lumenfall_carriers"));

    /**
     * 反照的载体；同上。
     */
    public static final TagKey<Item> REFULGENCE_CARRIERS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "refulgence_carriers"));

    public static final TagKey<Block> BLIGHTED_REALM_ORES = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "blighted_realm_ores"));
    public static final TagKey<Item> BLIGHTED_REALM_ORE_ITEMS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "blighted_realm_ores"));

    /**
     * 奥术通道交给 ChangeLib 的属性伤害派发器处理的武器；Lodestone 自己那笔附加魔法伤害要被取消，
     * 否则同一刀会算两次（见 {@code LodestoneAttributeEventHandlerMixin} 与
     * {@code MaledictAttributes} 里 MAGIC_DAMAGE 的注册）。
     */
    public static final TagKey<Item> LODESTONE_MAGIC_TAKEOVER = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "lodestone_magic_takeover"));

    /**
     * 拘魂仪式可用的工具：原版四类工具 + Malum 的镰刀与灵魂锈钢制品等。
     */
    public static final TagKey<Item> SOUL_BINDABLE = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "soul_bindable"));

    /**
     * 不参与拘魂的工具（例如已经带专用羁绊实现的 {@code maledict:forsaken_vow}）。
     */
    public static final TagKey<Item> SOUL_BINDING_TOOL_BLACKLIST = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "soul_binding_tool_blacklist"));

    /**
     * 不参与拘魂的生物；默认空着生成，数据包与整合包自行扩充。
     */
    public static final TagKey<EntityType<?>> SOUL_BINDING_BLACKLIST = TagKey.create(
            Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "soul_binding_blacklist"));

    /**
     * 扬弃逐光套装：四件护甲整体、可计入套装的两件武器，以及武器细分（刃 / 斧）。
     */
    public static final TagKey<Item> AUFHEBEN_LIGHTWARD_ARMOR = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "aufheben_lightward_armor"));
    public static final TagKey<Item> AUFHEBEN_LIGHTWARD_WEAPONS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "aufheben_lightward_weapons"));
    public static final TagKey<Item> AUFHEBEN_LIGHTWARD_BLADES = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "aufheben_lightward_blades"));
    public static final TagKey<Item> AUFHEBEN_LIGHTWARD_AXES = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "aufheben_lightward_axes"));

    private MaledictTags() {
    }
}