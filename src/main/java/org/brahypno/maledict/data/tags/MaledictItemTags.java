package org.brahypno.maledict.data.tags;

import com.sammy.malum.registry.common.MalumTags;
import com.sammy.malum.registry.common.item.MalumItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.brahypno.changelib.equipment.EquipmentProtectionTags;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictTags;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.concurrent.CompletableFuture;

public final class MaledictItemTags extends ItemTagsProvider {

    private static final TagKey<Item> GOETY_GRAVE_GLOVE =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("goety", "grave_glove_boost"));

    private static final TagKey<Item> L2_NO_SEAL =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("l2hostility", "no_seal"));

    private static final TagKey<Item> CURIOS_CHARM =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "charm"));
    private static final TagKey<Item> CURIOS_DELUSION =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "delusion"));

    /**
     * Curios 的符文槽；同名 tag 与 Malum 自带的合并，不会被我们顶掉。
     */
    private static final TagKey<Item> CURIOS_RUNE =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "rune"));

    public MaledictItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagsProvider.TagLookup<Block>> blockTags, ExistingFileHelper existingFiles) {
        super(output, lookupProvider, blockTags, Maledict.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ItemTags.AXES).add(MaledictItems.LOVER_AXE.get(), MaledictItems.MELTFALL_AXE.get());
        tag(ItemTags.ARROWS)
                .add(MaledictItems.SACRED_SPIRIT_ARROW.get())
                .add(MaledictItems.WICKED_SPIRIT_ARROW.get())
                .add(MaledictItems.ARCANE_SPIRIT_ARROW.get())
                .add(MaledictItems.ELDRITCH_SPIRIT_ARROW.get())
                .add(MaledictItems.AERIAL_SPIRIT_ARROW.get())
                .add(MaledictItems.AQUEOUS_SPIRIT_ARROW.get())
                .add(MaledictItems.EARTHEN_SPIRIT_ARROW.get())
                .add(MaledictItems.INFERNAL_SPIRIT_ARROW.get());
        tag(ItemTags.BOW_ENCHANTABLE).add(MaledictItems.REMEMBRANCE_BOW.get(), MaledictItems.ELEGY_BOW.get());
        // 附魔资格。1.20.1 由 EnchantmentCategory 的类判定给出，1.21 换成数据 tag，而且是嵌套的。
        // 武器与斧早已在下面的 #minecraft:swords / #minecraft:axes 里，标签一嵌套就自动拿到
        // sharp_weapon / weapon / fire_aspect / mining / mining_loot / durability / vanishing，
        // 所以这里只补三处真正的缺口：
        //   1) 四个护甲槽位 —— enchantable/armor、durability、equippable（绑定诅咒）、vanishing 全由它们嵌套；
        //   2) 两把弓 —— 原版 enchantable/durability 直接列 minecraft:bow，并不吃 #enchantable/bow，
        //      所以只加 enchantable/bow 会拿不到耐久系附魔（1.20.1 的 BREAKABLE 是给到的）；
        //   3) 神侵恶刃 —— 1.20.1 里它是 Malum 的 ScytheItem（extends Item，不是 SwordItem），只有 BREAKABLE
        //      那一档，所以只进 durability，不给 #swords（给了等于凭空多出锋利）。
        tag(ItemTags.HEAD_ARMOR).add(MaledictItems.AUFHEBEN_LIGHTWARD_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(MaledictItems.AUFHEBEN_LIGHTWARD_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(MaledictItems.AUFHEBEN_LIGHTWARD_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(MaledictItems.AUFHEBEN_LIGHTWARD_BOOTS.get());
        tag(ItemTags.DURABILITY_ENCHANTABLE).add(MaledictItems.REMEMBRANCE_BOW.get(),
                                                 MaledictItems.ELEGY_BOW.get(),
                                                 MaledictItems.INCURSUS_BLADE.get());
        // 坠光秘典是可上书架的书；同时照 1.20.1 藏到虚空阶段之后。
        tag(ItemTags.BOOKSHELF_BOOKS).add(MaledictItems.LIGHTFALL_CODEX.get());
        tag(MalumTags.ItemTags.SCYTHES_COMMON).add(MaledictItems.INCURSUS_BLADE.get());
        // 1.20.1 还额外写了出站 forge:scythe 给别的 mod 读；1.21.1 没有 forge 命名空间，NeoForge 21.1 的
        // Tags.Items 里也没有 SCYTHE（没有 c:scythe 等价物），所以只保留入站 malum:scythe 那条 1:1 搬运。
        tag(MalumTags.ItemTags.SCYTHES)
                .add(MaledictItems.INCURSUS_BLADE.get())
                .addOptionalTag(GOETY_GRAVE_GLOVE)
                .addOptional(ResourceLocation.fromNamespaceAndPath("goety", "death_scythe"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("goety", "dark_metal_scythe"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("goety", "dark_scythe"));
        // 1.20.1 的 soul_hunter_weapon 改名成 soul_shatter_capable_weapon，成员是逐项搬的，四把魂武器都要在。
        tag(MalumTags.ItemTags.SOUL_SHATTER_CAPABLE_WEAPON)
                .add(MaledictItems.INCURSUS_BLADE.get(), MaledictItems.LOVER_AXE.get(),
                     MaledictItems.MELTFALL_AXE.get(), MaledictItems.YEARNING_SWORD.get());
        tag(MalumTags.ItemTags.MAGIC_CAPABLE_WEAPON).add(MaledictItems.INCURSUS_BLADE.get(),
                MaledictItems.LOVER_AXE.get(), MaledictItems.MELTFALL_AXE.get(),
                MaledictItems.FORSAKEN_VOW.get());
        tag(MalumTags.ItemTags.HIDDEN_UNTIL_VOID).add(MaledictItems.INCURSUS_BLADE.get());
        tag(MalumTags.ItemTags.HIDDEN_UNTIL_VOID).add(MaledictItems.ELEGY_BOW.get());
        // 哭泣石与腐枯头颅是腐枯凋灵的召唤材料；照 1.20.1 一并藏到虚空阶段之后。
        tag(MalumTags.ItemTags.HIDDEN_UNTIL_VOID).add(MaledictItems.CRYING_TAINTED_ROCK.get());
        tag(MalumTags.ItemTags.HIDDEN_UNTIL_VOID).add(MaledictItems.CRYING_TWISTED_ROCK.get());
        tag(MalumTags.ItemTags.HIDDEN_UNTIL_VOID).add(MaledictItems.BLIGHTED_WITHER_SKULL.get());
        // 法典的「虚空页」本身就讲虚空，与其余虚空物件一起藏。
        tag(MalumTags.ItemTags.HIDDEN_UNTIL_VOID).add(MaledictItems.LIGHTFALL_CODEX.get());
        tag(GOETY_GRAVE_GLOVE).add(MaledictItems.INCURSUS_BLADE.get());
        tag(L2_NO_SEAL)
                .add(MaledictItems.INCURSUS_BLADE.get(), MaledictItems.ELEGY_BOW.get(), MaledictItems.AGE_OF_ENLIGHTENMENT.get(),
                     MaledictItems.HANGMAN_FOOL.get(), MaledictItems.EIDOLON_OF_THE_HIGH_PRIESTESS.get(),
                     MaledictItems.LOVER_AXE.get(), MaledictItems.EMPEROR_IN_THE_EGGSHELL.get());
        tag(CURIOS_CHARM).add(MaledictItems.AGE_OF_ENLIGHTENMENT.get(),
                              MaledictItems.EMPEROR_IN_THE_EGGSHELL.get());
        tag(CURIOS_DELUSION).add(MaledictItems.HANGMAN_FOOL.get(), MaledictItems.EIDOLON_OF_THE_HIGH_PRIESTESS.get());
        tag(CURIOS_RUNE).add(MaledictItems.RUNE_OF_SATIATION.get(),
                             MaledictItems.RUNE_OF_DECAY.get(),
                             MaledictItems.RUNE_OF_THE_PACK.get(),
                             MaledictItems.RUNE_OF_RIPENING.get(),
                             MaledictItems.RUNE_OF_STAGNANT_EVOLUTION.get(),
                             MaledictItems.RUNE_OF_ROTTEN_BONE.get(),
                             MaledictItems.RUNE_OF_MELANCHOLIA.get(),
                             MaledictItems.RUNE_OF_BLISS.get(),
                             MaledictItems.RUNE_OF_THE_FALLEN.get());
        tag(MaledictTags.VICISSITUDE_CONFISCATION_IMMUNE)
                .addOptional(ResourceLocation.fromNamespaceAndPath("sophisticatedbackpacks", "backpack"));
        // 拘魂：可封印的工具（原版五类 + Malum 的镰刀与灵魂锈钢制品）与不参与的工具黑名单。
        // 眷恋之剑与弃约之刃都是剑，靠 #minecraft:swords 自动进 soul_bindable；弃约之刃另有自己的
        // 女仆羁绊实现，所以同时进黑名单——1.20.1 就是这两条一起写的（那时用 addOptional，
        // 只因为物品还没移植）。
        tag(MaledictTags.SOUL_BINDABLE)
                .addTag(ItemTags.SWORDS)
                .addTag(ItemTags.AXES)
                .addTag(ItemTags.PICKAXES)
                .addTag(ItemTags.SHOVELS)
                .addTag(ItemTags.HOES)
                .addTag(MalumTags.ItemTags.SCYTHES)
                .add(MalumItems.SOUL_STAINED_STEEL_SWORD.get(), MalumItems.SOUL_STAINED_STEEL_AXE.get(),
                     MalumItems.SOUL_STAINED_STEEL_PICKAXE.get(), MalumItems.SOUL_STAINED_STEEL_SHOVEL.get(),
                     MalumItems.SOUL_STAINED_STEEL_HOE.get(), MalumItems.TYRVING.get(),
                     MalumItems.WEIGHT_OF_WORLDS.get());
        tag(MaledictTags.SOUL_BINDING_TOOL_BLACKLIST).add(MaledictItems.FORSAKEN_VOW.get());
        // 扬弃逐光六件套：四件护甲 + 两把可计入套装的武器，外加刃/斧两个细分。
        tag(MaledictTags.AUFHEBEN_LIGHTWARD_ARMOR)
                .add(MaledictItems.AUFHEBEN_LIGHTWARD_HELMET.get(), MaledictItems.AUFHEBEN_LIGHTWARD_CHESTPLATE.get(),
                     MaledictItems.AUFHEBEN_LIGHTWARD_LEGGINGS.get(), MaledictItems.AUFHEBEN_LIGHTWARD_BOOTS.get());
        tag(MaledictTags.AUFHEBEN_LIGHTWARD_WEAPONS)
                .add(MaledictItems.INCURSUS_BLADE.get(), MaledictItems.LOVER_AXE.get(),
                     MaledictItems.MELTFALL_AXE.get());
        tag(MaledictTags.AUFHEBEN_LIGHTWARD_BLADES).add(MaledictItems.INCURSUS_BLADE.get());
        tag(MaledictTags.AUFHEBEN_LIGHTWARD_AXES)
                .add(MaledictItems.LOVER_AXE.get(), MaledictItems.MELTFALL_AXE.get());
        tag(ItemTags.SWORDS).add(MaledictItems.BARON_CEREMONIAL_STAFF.get(),
                                 MaledictItems.YEARNING_SWORD.get(), MaledictItems.FORSAKEN_VOW.get());
        // 沉辉与反照的载体标签：故意留空生成，数据包或自有武器直接挂上去，能力不依赖任何兼容前置。
        tag(MaledictTags.LUMENFALL_CARRIERS);
        tag(MaledictTags.REFULGENCE_CARRIERS);
        tag(EquipmentProtectionTags.MAINHAND)
                .add(MaledictItems.INCURSUS_BLADE.get(), MaledictItems.ELEGY_BOW.get());
        tag(EquipmentProtectionTags.OFFHAND)
                .add(MaledictItems.INCURSUS_BLADE.get(), MaledictItems.ELEGY_BOW.get());
        tag(EquipmentProtectionTags.CURIOS)
                .add(MaledictItems.HANGMAN_FOOL.get(), MaledictItems.AGE_OF_ENLIGHTENMENT.get(),
                     MaledictItems.EIDOLON_OF_THE_HIGH_PRIESTESS.get(), MaledictItems.EMPEROR_IN_THE_EGGSHELL.get());
        tag(Tags.Items.ARMORS)
                .add(MaledictItems.AUFHEBEN_LIGHTWARD_HELMET.get(), MaledictItems.AUFHEBEN_LIGHTWARD_CHESTPLATE.get(),
                     MaledictItems.AUFHEBEN_LIGHTWARD_LEGGINGS.get(), MaledictItems.AUFHEBEN_LIGHTWARD_BOOTS.get());
        tag(EquipmentProtectionTags.ARMOR)
                .add(MaledictItems.AUFHEBEN_LIGHTWARD_HELMET.get(), MaledictItems.AUFHEBEN_LIGHTWARD_CHESTPLATE.get(),
                     MaledictItems.AUFHEBEN_LIGHTWARD_LEGGINGS.get(), MaledictItems.AUFHEBEN_LIGHTWARD_BOOTS.get());

        tag(Tags.Items.ORES).add(MaledictItems.LUMENFALL_BISMUTH_ORE.get());
        tag(Tags.Items.STORAGE_BLOCKS).add(MaledictItems.LUMENFALL_BISMUTH_BLOCK.get());
        tag(Tags.Items.INGOTS).add(MaledictItems.LUMENFALL_BISMUTH_INGOT.get());
        tag(Tags.Items.GEMS).add(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC.get());
        tag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/lumenfall_bismuth")))
                .add(MaledictItems.LUMENFALL_BISMUTH_ORE.get());
        tag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/lumenfall_bismuth")))
                .add(MaledictItems.LUMENFALL_BISMUTH_BLOCK.get());
        tag(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/lumenfall_bismuth")))
                .add(MaledictItems.LUMENFALL_BISMUTH_INGOT.get());
        tag(TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("c", "gems/refracted_lead_bismuth_eutectic")))
                .add(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC.get());
        tag(MalumTags.ItemTags.HIDDEN_UNTIL_VOID).add(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC.get());
        tag(MaledictTags.BLIGHTED_REALM_ORE_ITEMS).add(
                MaledictItems.TAINTED_MALIGNANT_LEAD_ORE.get(),
                MaledictItems.TWISTED_MALIGNANT_LEAD_ORE.get(),
                MaledictItems.LUMENFALL_BISMUTH_ORE.get());
        // 奥术通道的接管名单：恶刃与两把斧的 MAGIC_DAMAGE 由 ChangeLib 的属性伤害派发器结算。
        tag(MaledictTags.LODESTONE_MAGIC_TAKEOVER)
                .add(MaledictItems.INCURSUS_BLADE.get(), MaledictItems.LOVER_AXE.get(), MaledictItems.MELTFALL_AXE.get());

    }
}
