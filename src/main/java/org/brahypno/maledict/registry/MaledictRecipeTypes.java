package org.brahypno.maledict.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.ritual.invocation.InvocationDefinition;
import org.brahypno.maledict.common.ritual.invocation.OrdinaryAxolotlBucketIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class MaledictRecipeTypes {
    public static final DeferredRegister<IngredientType<?>> INGREDIENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, Maledict.MODID);
    public static final DeferredHolder<IngredientType<?>, IngredientType<OrdinaryAxolotlBucketIngredient>> ORDINARY_AXOLOTL_BUCKET =
            INGREDIENTS.register("ordinary_axolotl_bucket", () -> new IngredientType<>(OrdinaryAxolotlBucketIngredient.CODEC));
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Maledict.MODID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Maledict.MODID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<InvocationDefinition>> INVOCATION_SERIALIZER =
            SERIALIZERS.register("invocation", InvocationDefinition.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<InvocationDefinition>> INVOCATION =
            TYPES.register("invocation", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "maledict:invocation";
                }
            });

    private MaledictRecipeTypes() {
    }
}
