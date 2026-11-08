package org.brahypno.maledict.data;

import com.sammy.malum.registry.common.item.ItemTagRegistry;
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
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.changelib.equipment.EquipmentProtectionTags;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictTags;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.concurrent.CompletableFuture;

public final class MaledictItemTags extends ItemTagsProvider {

    private static final TagKey<Item> GOETY_GRAVE_GLOVE =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("goety", "grave_glove_boost"));

    private static final TagKey<Item> FORGE_SCYTHE =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "scythe"));

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

    private static final TagKey<Item> FORGE_ORES_LUMENFALL_BISMUTH = TagKey.create(Registries.ITEM,
                                                                                   ResourceLocation.fromNamespaceAndPath("forge", "ores/lumenfall_bismuth"));
    private static final TagKey<Item> FORGE_STORAGE_BLOCKS_LUMENFALL_BISMUTH = TagKey.create(Registries.ITEM,
                                                                                             ResourceLocation.fromNamespaceAndPath("forge",
                                                                                                                                   "storage_blocks/lumenfall_bismuth"));
    private static final TagKey<Item> FORGE_INGOTS_LUMENFALL_BISMUTH = TagKey.create(Registries.ITEM,
                                                                                     ResourceLocation.fromNamespaceAndPath("forge",
                                                                                                                           "ingots/lumenfall_bismuth"));
    private static final TagKey<Item> FORGE_GEMS_REFRACTED_LEAD_BISMUTH_EUTECTIC = TagKey.create(Registries.ITEM,
                                                                                                 ResourceLocation.fromNamespaceAndPath("forge",
                                                                                                                                       "gems/refracted_lead_bismuth_eutectic"));

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
        tag(ItemTags.BOOKSHELF_BOOKS).add(MaledictItems.LIGHTFALL_CODEX.get());
        tag(FORGE_SCYTHE).add(MaledictItems.INCURSUS_BLADE.get());
        tag(ItemTagRegistry.SCYTHE)
                .add(MaledictItems.INCURSUS_BLADE.get())
                .addOptionalTag(GOETY_GRAVE_GLOVE)
                .addOptional(ResourceLocation.fromNamespaceAndPath("goety", "death_scythe"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("goety", "dark_metal_scythe"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("goety", "dark_scythe"));
        tag(ItemTagRegistry.SOUL_HUNTER_WEAPON)
                .add(MaledictItems.INCURSUS_BLADE.get(), MaledictItems.LOVER_AXE.get(), MaledictItems.MELTFALL_AXE.get());
        tag(ItemTagRegistry.MAGIC_CAPABLE_WEAPON)
                .add(MaledictItems.INCURSUS_BLADE.get(), MaledictItems.LOVER_AXE.get(), MaledictItems.MELTFALL_AXE.get());
        tag(ItemTagRegistry.HIDDEN_UNTIL_VOID).add(MaledictItems.INCURSUS_BLADE.get(), MaledictItems.ELEGY_BOW.get(),
                                                   MaledictItems.MALIGNANT_PEWTER_TABLET.get(),
                                                   MaledictItems.CRYING_TAINTED_ROCK.get(),
                                                   MaledictItems.CRYING_TWISTED_ROCK.get(), MaledictItems.BLIGHTED_WITHER_SKULL.get(),
                                                   MaledictItems.BLIGHTED_STAR.get(), MaledictItems.LIGHT_FRAGMENT.get(), MaledictItems.LIGHTFALL_CODEX.get());
        // 折辉铅铋共晶要等腐枯界深处的沉辉铋，且是后期精魂灌注产物；在玩家抵达虚空阶段前
        // 先别在 JEI/创造栏里剧透。
        tag(ItemTagRegistry.HIDDEN_UNTIL_VOID).add(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC.get());
        tag(GOETY_GRAVE_GLOVE).add(MaledictItems.INCURSUS_BLADE.get());
        tag(L2_NO_SEAL)
                .add(MaledictItems.INCURSUS_BLADE.get(), MaledictItems.ELEGY_BOW.get(), MaledictItems.AGE_OF_ENLIGHTENMENT.get(),
                     MaledictItems.HANGMAN_FOOL.get(), MaledictItems.EIDOLON_OF_THE_HIGH_PRIESTESS.get(),
                     MaledictItems.LOVER_AXE.get())
                .addOptionalTag(MaledictTags.AUFHEBEN_LIGHTWARD_ARMOR);
        tag(CURIOS_CHARM).add(MaledictItems.AGE_OF_ENLIGHTENMENT.get());
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
        tag(EquipmentProtectionTags.MAINHAND)
                .add(MaledictItems.INCURSUS_BLADE.get(), MaledictItems.ELEGY_BOW.get());
        tag(EquipmentProtectionTags.OFFHAND)
                .add(MaledictItems.INCURSUS_BLADE.get(), MaledictItems.ELEGY_BOW.get());
        tag(EquipmentProtectionTags.CURIOS)
                .add(MaledictItems.HANGMAN_FOOL.get(), MaledictItems.AGE_OF_ENLIGHTENMENT.get(),
                     MaledictItems.EIDOLON_OF_THE_HIGH_PRIESTESS.get());
        tag(EquipmentProtectionTags.ARMOR)
                .addOptionalTag(MaledictTags.AUFHEBEN_LIGHTWARD_ARMOR);

        tag(MaledictTags.AUFHEBEN_LIGHTWARD_ARMOR)
                .add(MaledictItems.AUFHEBEN_LIGHTWARD_HELMET.get(),
                     MaledictItems.AUFHEBEN_LIGHTWARD_CHESTPLATE.get(),
                     MaledictItems.AUFHEBEN_LIGHTWARD_LEGGINGS.get(),
                     MaledictItems.AUFHEBEN_LIGHTWARD_BOOTS.get());
        tag(MaledictTags.AUFHEBEN_LIGHTWARD_WEAPONS)
                .add(MaledictItems.INCURSUS_BLADE.get(), MaledictItems.LOVER_AXE.get(),
                     MaledictItems.MELTFALL_AXE.get());
        tag(MaledictTags.AUFHEBEN_LIGHTWARD_BLADES).add(MaledictItems.INCURSUS_BLADE.get());
        tag(MaledictTags.AUFHEBEN_LIGHTWARD_AXES)
                .add(MaledictItems.LOVER_AXE.get(), MaledictItems.MELTFALL_AXE.get());
        tag(Tags.Items.ARMORS)
                .add(MaledictItems.AUFHEBEN_LIGHTWARD_HELMET.get(),
                     MaledictItems.AUFHEBEN_LIGHTWARD_CHESTPLATE.get(),
                     MaledictItems.AUFHEBEN_LIGHTWARD_LEGGINGS.get(),
                     MaledictItems.AUFHEBEN_LIGHTWARD_BOOTS.get());
        tag(Tags.Items.ARMORS_HELMETS).add(MaledictItems.AUFHEBEN_LIGHTWARD_HELMET.get());
        tag(Tags.Items.ARMORS_CHESTPLATES).add(MaledictItems.AUFHEBEN_LIGHTWARD_CHESTPLATE.get());
        tag(Tags.Items.ARMORS_LEGGINGS).add(MaledictItems.AUFHEBEN_LIGHTWARD_LEGGINGS.get());
        tag(Tags.Items.ARMORS_BOOTS).add(MaledictItems.AUFHEBEN_LIGHTWARD_BOOTS.get());

        tag(Tags.Items.ORES).add(MaledictItems.LUMENFALL_BISMUTH_ORE.get());
        tag(Tags.Items.STORAGE_BLOCKS).add(MaledictItems.LUMENFALL_BISMUTH_BLOCK.get());
        tag(Tags.Items.INGOTS).add(MaledictItems.LUMENFALL_BISMUTH_INGOT.get());
        tag(FORGE_ORES_LUMENFALL_BISMUTH).add(MaledictItems.LUMENFALL_BISMUTH_ORE.get());
        tag(FORGE_STORAGE_BLOCKS_LUMENFALL_BISMUTH).add(MaledictItems.LUMENFALL_BISMUTH_BLOCK.get());
        tag(FORGE_INGOTS_LUMENFALL_BISMUTH).add(MaledictItems.LUMENFALL_BISMUTH_INGOT.get());
        tag(Tags.Items.GEMS).add(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC.get());
        tag(FORGE_GEMS_REFRACTED_LEAD_BISMUTH_EUTECTIC).add(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC.get());
        tag(MaledictTags.BLIGHTED_REALM_ORE_ITEMS).add(
                MaledictItems.TAINTED_MALIGNANT_LEAD_ORE.get(),
                MaledictItems.TWISTED_MALIGNANT_LEAD_ORE.get(),
                MaledictItems.LUMENFALL_BISMUTH_ORE.get());
    }
}
