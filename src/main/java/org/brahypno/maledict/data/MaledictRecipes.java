package org.brahypno.maledict.data;

import com.sammy.malum.data.recipe.builder.SpiritInfusionRecipeBuilder;
import com.sammy.malum.data.recipe.builder.SpiritRepairRecipeBuilder;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.SpiritArrowType;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.function.Consumer;

public final class MaledictRecipes extends RecipeProvider {
    public MaledictRecipes(PackOutput output) {
        super(output);
    }

    @Override
    @SuppressWarnings({"removal"})
    protected void buildRecipes(Consumer<FinishedRecipe> recipes) {
        for (SpiritArrowType arrowType : SpiritArrowType.values()) {
            ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, MaledictItems.getSpiritArrow(arrowType).get(), 1)
                               .define('S', arrowType.getSpiritType().spiritShard.get())
                               .define('#', Items.STICK)
                               .define('F', Items.FEATHER)
                               .pattern("S")
                               .pattern("#")
                               .pattern("F")
                               .unlockedBy("has_spirit", has(arrowType.getSpiritType().spiritShard.get()))
                               .save(recipes);
        }

        new SpiritInfusionRecipeBuilder(ItemRegistry.EDGE_OF_DELIVERANCE.get(), 1, MaledictItems.INCURSUS_BLADE.get(), 1)
                .addExtraItem(Items.NETHER_STAR, 6)
                .addExtraItem(ItemRegistry.FUSED_CONSCIOUSNESS.get(), 6)
                .addExtraItem(Items.ENDER_EYE, 6)
                .addExtraItem(SpiritTypeRegistry.UMBRAL_SPIRIT.spiritShard.get(), 1)
                .addSpirit(SpiritTypeRegistry.SACRED_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.WICKED_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.AERIAL_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.AQUEOUS_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.EARTHEN_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.INFERNAL_SPIRIT, 64)
                .build(recipes, new ResourceLocation(Maledict.MODID, "spirit_infusion/incursus_blade"));

        new SpiritRepairRecipeBuilder(1.0f, Ingredient.of(Items.NETHER_STAR), 1)
                .addItem(MaledictItems.INCURSUS_BLADE.get())
                .addSpirit(SpiritTypeRegistry.SACRED_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.AERIAL_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.AQUEOUS_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.EARTHEN_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.INFERNAL_SPIRIT, 4)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_repair/incursus_blade"));

        new SpiritInfusionRecipeBuilder(ItemRegistry.BRILLIANT_OBELISK.get(), 1, MaledictItems.MNEMONIC_OBELISK.get(), 1)
                .addExtraItem(ItemRegistry.MNEMONIC_FRAGMENT.get(), 2)
                .addExtraItem(ItemRegistry.VOID_SALTS.get(), 2)
                .addExtraItem(ItemRegistry.BLOCK_OF_NULL_SLATE.get(), 2)
                .addExtraItem(ItemRegistry.SOULWOOD_PLANKS.get(), 2)
                .addSpirit(SpiritTypeRegistry.AERIAL_SPIRIT, 16)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 16)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_infusion/mnemonic_obelisk"));

        new SpiritInfusionRecipeBuilder(ItemRegistry.RUNEWOOD_OBELISK.get(), 1, MaledictItems.SOULWOOD_OBELISK.get(), 1)
                .addExtraItem(ItemRegistry.MALIGNANT_LEAD.get(), 2)
                .addExtraItem(ItemRegistry.VOID_SALTS.get(), 2)
                .addExtraItem(ItemRegistry.SOULWOOD_PLANKS.get(), 2)
                .addSpirit(SpiritTypeRegistry.AERIAL_SPIRIT, 16)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 8)
                .addSpirit(SpiritTypeRegistry.INFERNAL_SPIRIT, 8)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_infusion/soulwood_obelisk"));

        new SpiritInfusionRecipeBuilder(ItemRegistry.SOULWOOD_LOG.get(), 3, MaledictItems.REMEMBRANCE_BOW.get(), 1)
                .addExtraItem(ItemRegistry.WARP_FLUX.get(), 11)
                .addExtraItem(ItemRegistry.ASTRAL_WEAVE.get(), 11)
                .addSpirit(SpiritTypeRegistry.AERIAL_SPIRIT, 11)
                .addSpirit(SpiritTypeRegistry.SACRED_SPIRIT, 11)
                .addSpirit(SpiritTypeRegistry.AQUEOUS_SPIRIT, 11)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_infusion/remembrance_bow"));

        new SpiritInfusionRecipeBuilder(MaledictItems.REMEMBRANCE_BOW.get(), 1, MaledictItems.ELEGY_BOW.get(), 1)
                .addExtraItem(ItemRegistry.NULL_SLATE.get(), 11)
                .addExtraItem(ItemRegistry.VOID_SALTS.get(), 11)
                .addExtraItem(ItemRegistry.MALIGNANT_PEWTER_INGOT.get(), 2)
                .addSpirit(SpiritTypeRegistry.WICKED_SPIRIT, 11)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 11)
                .addSpirit(SpiritTypeRegistry.INFERNAL_SPIRIT, 11)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_infusion/elegy_bow"));
    }
}
