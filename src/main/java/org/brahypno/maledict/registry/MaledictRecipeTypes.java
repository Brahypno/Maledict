package org.brahypno.maledict.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.invocation.InvocationDefinition;

public final class MaledictRecipeTypes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Maledict.MODID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Maledict.MODID);
    public static final RegistryObject<RecipeSerializer<InvocationDefinition>> INVOCATION_SERIALIZER =
            SERIALIZERS.register("invocation", InvocationDefinition.Serializer::new);
    public static final RegistryObject<RecipeType<InvocationDefinition>> INVOCATION =
            TYPES.register("invocation", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "maledict:invocation";
                }
            });

    private MaledictRecipeTypes() {
    }
}
