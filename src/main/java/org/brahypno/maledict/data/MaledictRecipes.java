package org.brahypno.maledict.data;

import com.sammy.malum.data.recipe.builder.SpiritInfusionRecipeBuilder;
import com.sammy.malum.data.recipe.builder.SpiritRepairRecipeBuilder;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.function.Consumer;

public final class MaledictRecipes extends RecipeProvider {
    public MaledictRecipes(PackOutput output) {
        super(output);
    }

    @Override
    @SuppressWarnings({"removal"})
    protected void buildRecipes(Consumer<FinishedRecipe> recipes) {
        new SpiritInfusionRecipeBuilder(
                ItemRegistry.EDGE_OF_DELIVERANCE.get(), 1,
                MaledictItems.INCURSUS_BLADE.get(), 1)
                .addExtraItem(Items.NETHER_STAR, 6)
                .addExtraItem(Items.GOLDEN_APPLE, 6)
                .addExtraItem(Items.ENDER_EYE, 6)
                .addSpirit(SpiritTypeRegistry.SACRED_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.WICKED_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.AERIAL_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.AQUEOUS_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.EARTHEN_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.INFERNAL_SPIRIT, 64)
                .addSpirit(SpiritTypeRegistry.UMBRAL_SPIRIT, 1)
                .build(recipes, new ResourceLocation(Maledict.MODID, "spirit_infusion/incursus_blade"));

        new SpiritRepairRecipeBuilder(1.0f, Ingredient.of(Items.NETHER_STAR), 1)
                .addItem(MaledictItems.INCURSUS_BLADE.get())
                .addSpirit(SpiritTypeRegistry.SACRED_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.WICKED_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.AERIAL_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.AQUEOUS_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.EARTHEN_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.INFERNAL_SPIRIT, 4)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(
                        Maledict.MODID, "spirit_repair/incursus_blade"));
    }
}
