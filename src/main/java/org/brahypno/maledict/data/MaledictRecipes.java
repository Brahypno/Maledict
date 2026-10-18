package org.brahypno.maledict.data;

import com.google.gson.JsonObject;
import com.sammy.malum.datagen.recipe.builder.RunicWorkbenchRecipeBuilder;
import com.sammy.malum.datagen.recipe.builder.SpiritInfusionRecipeBuilder;
import com.sammy.malum.datagen.recipe.builder.SpiritRepairRecipeBuilder;
import com.sammy.malum.datagen.recipe.builder.VoidFavorRecipeBuilder;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import com.sammy.malum.registry.common.item.MalumItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.core.HolderLookup;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.HangmanFoolItem;
import org.brahypno.maledict.common.item.SpiritArrowType;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.function.Consumer;

public final class MaledictRecipes extends RecipeProvider {
    public MaledictRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipes) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, MalumItems.MALIGNANT_LEAD.get())
                .requires(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get(), 8)
                .unlockedBy("has_malignant_lead_fragment", has(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get()))
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "malignant_lead_from_fragments"));

        for (SpiritArrowType arrowType : SpiritArrowType.values()) {
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, MaledictItems.getSpiritArrow(arrowType).get(), 1)
                               .define('S', arrowType.getSpiritType().getSpiritShard())
                               .define('#', Items.STICK)
                               .define('F', Items.FEATHER)
                               .pattern("S")
                               .pattern("#")
                               .pattern("F")
                               .unlockedBy("has_spirit", has(arrowType.getSpiritType().getSpiritShard()))
                               .save(recipes);
        }

        new SpiritInfusionRecipeBuilder(MaledictItems.BLEEDING_EYE.get(), 1, MaledictItems.HANGMAN_FOOL.get(), 1)
                .addExtraItem(MalumItems.HEX_ASH.get(), 4)
                .addExtraItem(MalumItems.FUSED_CONSCIOUSNESS.get(), 3)
                .addSpirit(MalumSpiritTypes.ELDRITCH_SPIRIT, 6)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "hangman_fool"));

        new VoidFavorRecipeBuilder(
                DataComponentIngredient.of(true, new ItemStack(MaledictItems.HANGMAN_FOOL.get())), HangmanFoolItem.create(1))
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "hangman_fool_stage_1"));

        new SpiritInfusionRecipeBuilder(DataComponentIngredient.of(true, HangmanFoolItem.create(1)),
                HangmanFoolItem.create(2))
                .addExtraItem(MalumItems.MNEMONIC_FRAGMENT.get(), 6)
                .addExtraItem(MalumItems.AURIC_EMBERS.get(), 6)
                .addExtraItem(MalumItems.VOID_SALTS.get(), 6)
                .addExtraItem(MaledictItems.AGE_OF_ENLIGHTENMENT.get(), 1)
                .addSpirit(MalumSpiritTypes.ELDRITCH_SPIRIT, 16)
                .addSpirit(MalumSpiritTypes.ARCANE_SPIRIT, 8)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "hangman_fool_stage_2"));

        new SpiritInfusionRecipeBuilder(MalumItems.EDGE_OF_DELIVERANCE.get(), 1, MaledictItems.INCURSUS_BLADE.get(), 1)
                .addExtraItem(Items.NETHER_STAR, 6)
                .addExtraItem(MalumItems.FUSED_CONSCIOUSNESS.get(), 6)
                .addExtraItem(Items.ENDER_EYE, 6)
                .addExtraItem(MalumSpiritTypes.UMBRAL_SPIRIT.getSpiritShard(), 1)
                .addSpirit(MalumSpiritTypes.SACRED_SPIRIT, 64)
                .addSpirit(MalumSpiritTypes.WICKED_SPIRIT, 64)
                .addSpirit(MalumSpiritTypes.ARCANE_SPIRIT, 64)
                .addSpirit(MalumSpiritTypes.ELDRITCH_SPIRIT, 64)
                .addSpirit(MalumSpiritTypes.AERIAL_SPIRIT, 64)
                .addSpirit(MalumSpiritTypes.AQUEOUS_SPIRIT, 64)
                .addSpirit(MalumSpiritTypes.EARTHEN_SPIRIT, 64)
                .addSpirit(MalumSpiritTypes.INFERNAL_SPIRIT, 64)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "incursus_blade"));

        new SpiritRepairRecipeBuilder(new net.neoforged.neoforge.common.crafting.SizedIngredient(Ingredient.of(Items.NETHER_STAR), 1), 1.0f)
                .withValidItem(MaledictItems.INCURSUS_BLADE.get())
                .addSpirit(MalumSpiritTypes.SACRED_SPIRIT, 4)
                .addSpirit(MalumSpiritTypes.ARCANE_SPIRIT, 4)
                .addSpirit(MalumSpiritTypes.AERIAL_SPIRIT, 4)
                .addSpirit(MalumSpiritTypes.AQUEOUS_SPIRIT, 4)
                .addSpirit(MalumSpiritTypes.EARTHEN_SPIRIT, 4)
                .addSpirit(MalumSpiritTypes.INFERNAL_SPIRIT, 4)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "incursus_blade"));

        new SpiritInfusionRecipeBuilder(MalumItems.BRILLIANT_OBELISK.get(), 1, MaledictItems.MNEMONIC_OBELISK.get(), 1)
                .addExtraItem(MalumItems.MNEMONIC_FRAGMENT.get(), 2)
                .addExtraItem(MalumItems.VOID_SALTS.get(), 2)
                .addExtraItem(MalumItems.BLOCK_OF_NULL_SLATE.get(), 2)
                .addExtraItem(MalumItems.SOULWOOD_PLANKS.get(), 2)
                .addSpirit(MalumSpiritTypes.AERIAL_SPIRIT, 16)
                .addSpirit(MalumSpiritTypes.ELDRITCH_SPIRIT, 16)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "mnemonic_obelisk"));

        new SpiritInfusionRecipeBuilder(MalumItems.RUNEWOOD_OBELISK.get(), 1, MaledictItems.SOULWOOD_OBELISK.get(), 1)
                .addExtraItem(MalumItems.MALIGNANT_LEAD.get(), 2)
                .addExtraItem(MalumItems.VOID_SALTS.get(), 2)
                .addExtraItem(MalumItems.SOULWOOD_PLANKS.get(), 2)
                .addSpirit(MalumSpiritTypes.AERIAL_SPIRIT, 16)
                .addSpirit(MalumSpiritTypes.ELDRITCH_SPIRIT, 8)
                .addSpirit(MalumSpiritTypes.INFERNAL_SPIRIT, 8)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "soulwood_obelisk"));

        new SpiritInfusionRecipeBuilder(MalumItems.SOULWOOD_LOG.get(), 3, MaledictItems.REMEMBRANCE_BOW.get(), 1)
                .addExtraItem(MalumItems.WARP_FLUX.get(), 11)
                .addExtraItem(MalumItems.ASTRAL_WEAVE.get(), 11)
                .addSpirit(MalumSpiritTypes.AERIAL_SPIRIT, 11)
                .addSpirit(MalumSpiritTypes.SACRED_SPIRIT, 11)
                .addSpirit(MalumSpiritTypes.AQUEOUS_SPIRIT, 11)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "remembrance_bow"));

        new SpiritRepairRecipeBuilder(new net.neoforged.neoforge.common.crafting.SizedIngredient(Ingredient.of(MalumItems.SOULWOOD_LOG.get()), 1), 1.0f)
                .withValidItem(MaledictItems.REMEMBRANCE_BOW.get())
                .addSpirit(MalumSpiritTypes.AERIAL_SPIRIT, 11)
                .addSpirit(MalumSpiritTypes.SACRED_SPIRIT, 11)
                .addSpirit(MalumSpiritTypes.AQUEOUS_SPIRIT, 11)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "remembrance_bow"));

        new SpiritInfusionRecipeBuilder(MaledictItems.REMEMBRANCE_BOW.get(), 1, MaledictItems.ELEGY_BOW.get(), 1)
                .addExtraItem(MalumItems.NULL_SLATE.get(), 11)
                .addExtraItem(MalumItems.VOID_SALTS.get(), 11)
                .addExtraItem(MalumItems.MALIGNANT_PEWTER_INGOT.get(), 2)
                .addSpirit(MalumSpiritTypes.WICKED_SPIRIT, 11)
                .addSpirit(MalumSpiritTypes.ELDRITCH_SPIRIT, 11)
                .addSpirit(MalumSpiritTypes.INFERNAL_SPIRIT, 11)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "elegy_bow"));

        new RunicWorkbenchRecipeBuilder(com.sammy.malum.registry.common.MalumSoundEvents.RUNIC_WORKBENCH_SHAPES_RUNE_WOODEN, MaledictItems.RUNE_OF_SATIATION.get(), 1)
                .setPrimaryInput(MalumItems.RUNEWOOD_LOG.get(), 4)
                .setSecondaryInput(MalumSpiritTypes.SACRED_SPIRIT, 32)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "rune_of_satiation"));

        new RunicWorkbenchRecipeBuilder(com.sammy.malum.registry.common.MalumSoundEvents.RUNIC_WORKBENCH_SHAPES_RUNE_WOODEN, MaledictItems.RUNE_OF_DECAY.get(), 1)
                .setPrimaryInput(MalumItems.RUNEWOOD_LOG.get(), 4)
                .setSecondaryInput(MalumSpiritTypes.WICKED_SPIRIT, 32)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "rune_of_decay"));

        new RunicWorkbenchRecipeBuilder(com.sammy.malum.registry.common.MalumSoundEvents.RUNIC_WORKBENCH_SHAPES_RUNE_WOODEN, MaledictItems.RUNE_OF_THE_PACK.get(), 1)
                .setPrimaryInput(MalumItems.SOULWOOD_LOG.get(), 4)
                .setSecondaryInput(MalumSpiritTypes.WICKED_SPIRIT, 32)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "rune_of_the_pack"));

        new RunicWorkbenchRecipeBuilder(com.sammy.malum.registry.common.MalumSoundEvents.RUNIC_WORKBENCH_SHAPES_RUNE_WOODEN, MaledictItems.RUNE_OF_RIPENING.get(), 1)
                .setPrimaryInput(MalumItems.SOULWOOD_LOG.get(), 4)
                .setSecondaryInput(MalumSpiritTypes.SACRED_SPIRIT, 32)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "rune_of_ripening"));

        new SpiritInfusionRecipeBuilder(
                MalumItems.MALIGNANT_PEWTER_PLATING.get(), 4, MaledictItems.MALIGNANT_PEWTER_TABLET.get(), 2)
                .addExtraItem(MalumItems.MNEMONIC_FRAGMENT.get(), 4)
                .addSpirit(MalumSpiritTypes.AQUEOUS_SPIRIT, 8)
                .addSpirit(MalumSpiritTypes.EARTHEN_SPIRIT, 8)
                .addSpirit(MalumSpiritTypes.WICKED_SPIRIT, 8)
                .addSpirit(MalumSpiritTypes.SACRED_SPIRIT, 8)
                .addSpirit(MalumSpiritTypes.ARCANE_SPIRIT, 8)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(
                        Maledict.MODID, "malignant_pewter_tablet"));

        new RunicWorkbenchRecipeBuilder(com.sammy.malum.registry.common.MalumSoundEvents.RUNIC_WORKBENCH_SHAPES_RUNE_GENERIC, MaledictItems.RUNE_OF_STAGNANT_EVOLUTION.get(), 1)
                .setPrimaryInput(MaledictItems.MALIGNANT_PEWTER_TABLET.get(), 1)
                .setSecondaryInput(MalumItems.FUSED_CONSCIOUSNESS.get(), 2)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(
                        Maledict.MODID, "rune_of_stagnant_evolution"));

        new RunicWorkbenchRecipeBuilder(com.sammy.malum.registry.common.MalumSoundEvents.RUNIC_WORKBENCH_SHAPES_RUNE_GENERIC, MaledictItems.RUNE_OF_ROTTEN_BONE.get(), 1)
                .setPrimaryInput(MaledictItems.MALIGNANT_PEWTER_TABLET.get(), 1)
                .setSecondaryInput(MalumItems.LIVING_FLESH.get(), 64)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(
                        Maledict.MODID, "rune_of_rotten_bone"));

        // 4 tainted_rock + 16 幽影精魂：Malum 污染石档的模子（例如 rune_of_culling 就是 16 邪恶精魂），
        // 成本差异全在幽影精魂比基础精魂稀有多少上。
        new RunicWorkbenchRecipeBuilder(com.sammy.malum.registry.common.MalumSoundEvents.RUNIC_WORKBENCH_SHAPES_RUNE_STONE, MaledictItems.RUNE_OF_MELANCHOLIA.get(), 1)
                .setPrimaryInput(MalumItems.TAINTED_ROCK.get(), 4)
                .setSecondaryInput(MalumSpiritTypes.UMBRAL_SPIRIT, 16)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(
                        Maledict.MODID, "rune_of_melancholia"));

        // 4 null_slate + 32 幽影精魂：Malum 虚空档的模子（那一档都是 16 枚基础精魂），翻倍的精魂
        // 换的是「幽影精魂比基础精魂稀有得多」，与抑郁符文同档、同为这一档里最贵的一枚。
        new RunicWorkbenchRecipeBuilder(com.sammy.malum.registry.common.MalumSoundEvents.RUNIC_WORKBENCH_SHAPES_RUNE_VOID, MaledictItems.RUNE_OF_BLISS.get(), 1)
                .setPrimaryInput(MalumItems.NULL_SLATE.get(), 4)
                .setSecondaryInput(MalumSpiritTypes.UMBRAL_SPIRIT, 32)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(
                        Maledict.MODID, "rune_of_bliss"));

        new RunicWorkbenchRecipeBuilder(com.sammy.malum.registry.common.MalumSoundEvents.RUNIC_WORKBENCH_SHAPES_RUNE_GENERIC, MaledictItems.RUNE_OF_THE_FALLEN.get(), 1)
                .setPrimaryInput(MaledictItems.MALIGNANT_PEWTER_TABLET.get(), 1)
                .setSecondaryInput(MalumItems.AURIC_EMBERS.get(), 64)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(
                        Maledict.MODID, "rune_of_the_fallen"));

        new SpiritRepairRecipeBuilder(new net.neoforged.neoforge.common.crafting.SizedIngredient(Ingredient.of(MalumItems.NULL_SLATE.get()), 1), 1.0f)
                .withValidItem(MaledictItems.ELEGY_BOW.get())
                .addSpirit(MalumSpiritTypes.WICKED_SPIRIT, 11)
                .addSpirit(MalumSpiritTypes.ELDRITCH_SPIRIT, 11)
                .addSpirit(MalumSpiritTypes.INFERNAL_SPIRIT, 11)
                .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "elegy_bow"));
    }

}
