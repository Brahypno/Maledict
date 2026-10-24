package org.brahypno.maledict.data;

import com.google.gson.JsonObject;
import com.sammy.malum.data.recipe.builder.RunicWorkbenchRecipeBuilder;
import com.sammy.malum.data.recipe.builder.SpiritInfusionRecipeBuilder;
import com.sammy.malum.data.recipe.builder.SpiritRepairRecipeBuilder;
import com.sammy.malum.data.recipe.builder.VoidFavorRecipeBuilder;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.curio.hangman.HangmanFoolItem;
import org.brahypno.maledict.common.weapon.remembrance.SpiritArrowType;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.function.Consumer;

public final class MaledictRecipes extends RecipeProvider {
    public MaledictRecipes(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> recipes) {
        InvocationRecipes.build(recipes);
        new SpiritInfusionRecipeBuilder(ItemRegistry.SOUL_STAINED_STEEL_AXE.get(), 1, MaledictItems.MELTFALL_AXE.get(), 1)
                .addExtraItem(ItemRegistry.BLIGHTED_GUNK.get(), 6)
                .addExtraItem(ItemRegistry.LIVING_FLESH.get(), 6)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 6)
                .addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 6)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_infusion/meltfall_axe"));

        new SpiritRepairRecipeBuilder(0.75f, Ingredient.of(ItemRegistry.SOUL_STAINED_STEEL_INGOT.get()), 2)
                .addItem(MaledictItems.MELTFALL_AXE.get())
                .addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 8)
                .addSpirit(SpiritTypeRegistry.EARTHEN_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.WICKED_SPIRIT, 2)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_repair/meltfall_axe"));

        new SpiritInfusionRecipeBuilder(MaledictItems.MELTFALL_AXE.get(), 1, MaledictItems.LOVER_AXE.get(), 1)
                .addExtraItem(ItemRegistry.MALIGNANT_PEWTER_INGOT.get(), 4)
                .addExtraItem(ItemRegistry.FUSED_CONSCIOUSNESS.get(), 1)
                .addExtraItem(ItemRegistry.BLOCK_OF_NULL_SLATE.get(), 3)
                .addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 18)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 18)
                .addSpirit(SpiritTypeRegistry.INFERNAL_SPIRIT, 18)
                .addSpirit(SpiritTypeRegistry.AQUEOUS_SPIRIT, 18)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_infusion/lover_axe"));

        new SpiritRepairRecipeBuilder(1.0f, Ingredient.of(ItemRegistry.MALIGNANT_PEWTER_INGOT.get()), 1)
                .addItem(MaledictItems.LOVER_AXE.get())
                .addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 18)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 18)
                .addSpirit(SpiritTypeRegistry.INFERNAL_SPIRIT, 18)
                .addSpirit(SpiritTypeRegistry.AQUEOUS_SPIRIT, 18)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_repair/lover_axe"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemRegistry.MALIGNANT_LEAD.get())
                              .requires(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get(), 8)
                              .unlockedBy("has_malignant_lead_fragment", has(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get()))
                              .save(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "malignant_lead_from_fragments"));

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

        new SpiritInfusionRecipeBuilder(MaledictItems.BLEEDING_EYE.get(), 1, MaledictItems.HANGMAN_FOOL.get(), 1)
                .addExtraItem(ItemRegistry.HEX_ASH.get(), 4)
                .addExtraItem(ItemRegistry.FUSED_CONSCIOUSNESS.get(), 3)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 6)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_infusion/hangman_fool"));

        new VoidFavorRecipeBuilder(
                StrictNBTIngredient.of(new ItemStack(MaledictItems.HANGMAN_FOOL.get())), HangmanFoolItem.create(1))
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "void_favor/hangman_fool_stage_1"));

        new NbtSpiritInfusionRecipeBuilder(StrictNBTIngredient.of(HangmanFoolItem.create(1)), 1,
                                           HangmanFoolItem.create(2))
                .addExtraItem(ItemRegistry.MNEMONIC_FRAGMENT.get(), 6)
                .addExtraItem(ItemRegistry.AURIC_EMBERS.get(), 6)
                .addExtraItem(ItemRegistry.VOID_SALTS.get(), 6)
                .addExtraItem(MaledictItems.AGE_OF_ENLIGHTENMENT.get(), 1)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 16)
                .addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 8)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_infusion/hangman_fool_stage_2"));

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
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_infusion/incursus_blade"));

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

        new SpiritRepairRecipeBuilder(1.0f, Ingredient.of(ItemRegistry.SOULWOOD_LOG.get()), 1)
                .addItem(MaledictItems.REMEMBRANCE_BOW.get())
                .addSpirit(SpiritTypeRegistry.AERIAL_SPIRIT, 11)
                .addSpirit(SpiritTypeRegistry.SACRED_SPIRIT, 11)
                .addSpirit(SpiritTypeRegistry.AQUEOUS_SPIRIT, 11)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_repair/remembrance_bow"));

        new SpiritInfusionRecipeBuilder(MaledictItems.REMEMBRANCE_BOW.get(), 1, MaledictItems.ELEGY_BOW.get(), 1)
                .addExtraItem(ItemRegistry.NULL_SLATE.get(), 11)
                .addExtraItem(ItemRegistry.VOID_SALTS.get(), 11)
                .addExtraItem(ItemRegistry.MALIGNANT_PEWTER_INGOT.get(), 2)
                .addSpirit(SpiritTypeRegistry.WICKED_SPIRIT, 11)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 11)
                .addSpirit(SpiritTypeRegistry.INFERNAL_SPIRIT, 11)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_infusion/elegy_bow"));

        new RunicWorkbenchRecipeBuilder(MaledictItems.RUNE_OF_SATIATION.get(), 1)
                .setPrimaryInput(ItemRegistry.RUNEWOOD_TABLET.get(), 1)
                .setSecondaryInput(SpiritTypeRegistry.SACRED_SPIRIT.spiritShard.get(), 32)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "runeworking/rune_of_satiation"));

        new RunicWorkbenchRecipeBuilder(MaledictItems.RUNE_OF_DECAY.get(), 1)
                .setPrimaryInput(ItemRegistry.RUNEWOOD_TABLET.get(), 1)
                .setSecondaryInput(SpiritTypeRegistry.WICKED_SPIRIT.spiritShard.get(), 32)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "runeworking/rune_of_decay"));

        new RunicWorkbenchRecipeBuilder(MaledictItems.RUNE_OF_THE_PACK.get(), 1)
                .setPrimaryInput(ItemRegistry.SOULWOOD_TABLET.get(), 1)
                .setSecondaryInput(SpiritTypeRegistry.WICKED_SPIRIT.spiritShard.get(), 32)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "runeworking/rune_of_the_pack"));

        new RunicWorkbenchRecipeBuilder(MaledictItems.RUNE_OF_RIPENING.get(), 1)
                .setPrimaryInput(ItemRegistry.SOULWOOD_TABLET.get(), 1)
                .setSecondaryInput(SpiritTypeRegistry.SACRED_SPIRIT.spiritShard.get(), 32)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "runeworking/rune_of_ripening"));

        new SpiritInfusionRecipeBuilder(
                ItemRegistry.MALIGNANT_PEWTER_PLATING.get(), 4, MaledictItems.MALIGNANT_PEWTER_TABLET.get(), 2)
                .addExtraItem(ItemRegistry.MNEMONIC_FRAGMENT.get(), 4)
                .addSpirit(SpiritTypeRegistry.AQUEOUS_SPIRIT, 8)
                .addSpirit(SpiritTypeRegistry.EARTHEN_SPIRIT, 8)
                .addSpirit(SpiritTypeRegistry.WICKED_SPIRIT, 8)
                .addSpirit(SpiritTypeRegistry.SACRED_SPIRIT, 8)
                .addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 8)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(
                        Maledict.MODID, "spirit_infusion/malignant_pewter_tablet"));

        new RunicWorkbenchRecipeBuilder(MaledictItems.RUNE_OF_STAGNANT_EVOLUTION.get(), 1)
                .setPrimaryInput(MaledictItems.MALIGNANT_PEWTER_TABLET.get(), 1)
                .setSecondaryInput(ItemRegistry.FUSED_CONSCIOUSNESS.get(), 2)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(
                        Maledict.MODID, "runeworking/rune_of_stagnant_evolution"));

        new RunicWorkbenchRecipeBuilder(MaledictItems.RUNE_OF_ROTTEN_BONE.get(), 1)
                .setPrimaryInput(MaledictItems.MALIGNANT_PEWTER_TABLET.get(), 1)
                .setSecondaryInput(ItemRegistry.LIVING_FLESH.get(), 64)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(
                        Maledict.MODID, "runeworking/rune_of_rotten_bone"));

        // 污染石符板 + 16 幽影精魂：Malum 污染石档的模子（例如 rune_of_culling 就是 16 邪恶精魂），
        // 成本差异全在幽影精魂比基础精魂稀有多少上。
        new RunicWorkbenchRecipeBuilder(MaledictItems.RUNE_OF_MELANCHOLIA.get(), 1)
                .setPrimaryInput(ItemRegistry.TAINTED_ROCK_TABLET.get(), 1)
                .setSecondaryInput(SpiritTypeRegistry.UMBRAL_SPIRIT.spiritShard.get(), 16)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(
                        Maledict.MODID, "runeworking/rune_of_melancholia"));

        // 虚空石符板 + 32 幽影精魂：Malum 虚空档的模子（那一档都是 16 枚基础精魂），翻倍的精魂
        // 换的是「幽影精魂比基础精魂稀有得多」，与抑郁符文同档、同为这一档里最贵的一枚。
        new RunicWorkbenchRecipeBuilder(MaledictItems.RUNE_OF_BLISS.get(), 1)
                .setPrimaryInput(ItemRegistry.VOID_TABLET.get(), 1)
                .setSecondaryInput(SpiritTypeRegistry.UMBRAL_SPIRIT.spiritShard.get(), 32)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(
                        Maledict.MODID, "runeworking/rune_of_bliss"));

        new RunicWorkbenchRecipeBuilder(MaledictItems.RUNE_OF_THE_FALLEN.get(), 1)
                .setPrimaryInput(MaledictItems.MALIGNANT_PEWTER_TABLET.get(), 1)
                .setSecondaryInput(ItemRegistry.AURIC_EMBERS.get(), 64)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(
                        Maledict.MODID, "runeworking/rune_of_the_fallen"));

        new SpiritRepairRecipeBuilder(1.0f, Ingredient.of(ItemRegistry.NULL_SLATE.get()), 1)
                .addItem(MaledictItems.ELEGY_BOW.get())
                .addSpirit(SpiritTypeRegistry.WICKED_SPIRIT, 11)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 11)
                .addSpirit(SpiritTypeRegistry.INFERNAL_SPIRIT, 11)
                .build(recipes, ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "spirit_repair/elegy_bow"));
    }

    /**
     * Malum's builder omits output NBT; the altar also defaults to replacing it with the input tag.
     */
    static final class NbtSpiritInfusionRecipeBuilder extends SpiritInfusionRecipeBuilder {
        private final ItemStack nbtOutput;

        NbtSpiritInfusionRecipeBuilder(Ingredient input, int inputCount, ItemStack output) {
            super(input, inputCount, output);
            nbtOutput = output.copy();
        }

        @Override
        public void build(Consumer<FinishedRecipe> recipes, ResourceLocation id) {
            recipes.accept(new Result(id) {
                @Override
                public void serializeRecipeData(JsonObject json) {
                    super.serializeRecipeData(json);
                    json.getAsJsonObject("output").addProperty("nbt", nbtOutput.getTag().toString());
                    json.addProperty("useNbtFromInput", false);
                }
            });
        }
    }
}
