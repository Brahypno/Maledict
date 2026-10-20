package org.brahypno.maledict.client.jei;

import com.sammy.malum.compat.jei.JEIHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.curio.HangmanFoolItem;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.function.Function;
import java.util.function.Predicate;

@JeiPlugin
public final class MaledictJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "recipe_visibility");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(MaledictItems.HANGMAN_FOOL.get(),
                new ISubtypeInterpreter<ItemStack>() {
                    @Override
                    public Object getSubtypeData(ItemStack stack, UidContext context) {
                        return HangmanFoolItem.stage(stack);
                    }

                    @Override
                    @Deprecated
                    public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                        return "stage_" + HangmanFoolItem.stage(stack);
                    }
                });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        // Add further recipe categories and output NBT conditions here.
        hideRecipesMatchingOutputs(runtime.getRecipeManager(), JEIHandler.WEEPING_WELL,
                recipe -> recipe.output,
                stack -> stack.is(MaledictItems.HANGMAN_FOOL.get()) && HangmanFoolItem.stage(stack) == 1);
    }

    /** The output condition can inspect NBT without hiding every recipe for the item's registry ID. */
    private static <T> void hideRecipesMatchingOutputs(IRecipeManager manager, RecipeType<T> type,
                                                       Function<T, ItemStack> output,
                                                       Predicate<ItemStack> hiddenOutput) {
        var hiddenRecipes = manager.createRecipeLookup(type).includeHidden().get()
                .filter(recipe -> hiddenOutput.test(output.apply(recipe)))
                .toList();
        manager.hideRecipes(type, hiddenRecipes);
    }
}
