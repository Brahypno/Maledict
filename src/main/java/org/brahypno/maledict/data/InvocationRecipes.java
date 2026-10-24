package org.brahypno.maledict.data;

import com.google.gson.JsonObject;
import com.sammy.malum.data.recipe.builder.SpiritInfusionRecipeBuilder;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.crafting.DifferenceIngredient;
import net.minecraftforge.common.crafting.PartialNBTIngredient;
import org.brahypno.maledict.common.invocation.InvocationDefinition;
import org.brahypno.maledict.common.invocation.InvocationSigilItem;
import org.brahypno.maledict.registry.MaledictRecipeTypes;

import java.util.function.Consumer;

final class InvocationRecipes {
    static void build(Consumer<FinishedRecipe> recipes) {
        definition(recipes, "sniffer", "sniffer", "default", false, InvocationDefinition.Profile.GROUND, 0x91BD59);
        definition(recipes, "allay", "allay", "default", false, InvocationDefinition.Profile.FLYING, 0x64DCEF);
        definition(recipes, "skeleton_horse", "skeleton_horse", "default", false, InvocationDefinition.Profile.GROUND, 0xE9DFC6);
        definition(recipes, "brown_mooshroom", "mooshroom", "brown", false, InvocationDefinition.Profile.GROUND, 0xBE865A);
        definition(recipes, "blue_axolotl", "axolotl", "blue", false, InvocationDefinition.Profile.WATER, 0x718BE8);
        definition(recipes, "shulker", "shulker", "default", true, InvocationDefinition.Profile.ATTACHED, 0xB28DD1);

        infusion("sniffer", Ingredient.of(Items.MOSS_BLOCK))
                .addExtraItem(Items.MUD, 4).addExtraItem(Items.BONE_MEAL, 8)
                .addExtraItem(Items.WHEAT_SEEDS, 8).addExtraItem(Items.AMETHYST_SHARD, 4)
                .addSpirit(SpiritTypeRegistry.SACRED_SPIRIT, 8).addSpirit(SpiritTypeRegistry.EARTHEN_SPIRIT, 8)
                .addSpirit(SpiritTypeRegistry.AQUEOUS_SPIRIT, 4).addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 4)
                .build(recipes, infusionId("sniffer"));
        infusion("allay", Ingredient.of(Items.NOTE_BLOCK))
                .addExtraItem(Items.AMETHYST_SHARD, 8).addExtraItem(Items.FEATHER, 4).addExtraItem(Items.GOLD_INGOT, 4)
                .addSpirit(SpiritTypeRegistry.SACRED_SPIRIT, 8).addSpirit(SpiritTypeRegistry.AERIAL_SPIRIT, 8)
                .addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 8).build(recipes, infusionId("allay"));
        infusion("skeleton_horse", Ingredient.of(Items.BONE_BLOCK))
                .addExtraItem(Items.SADDLE, 1).addExtraItem(Items.IRON_INGOT, 8).addExtraItem(Items.AMETHYST_SHARD, 4)
                .addSpirit(SpiritTypeRegistry.WICKED_SPIRIT, 8).addSpirit(SpiritTypeRegistry.SACRED_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 8).build(recipes, infusionId("skeleton_horse"));
        infusion("brown_mooshroom", Ingredient.of(Items.RED_MUSHROOM_BLOCK))
                .addExtraItem(Items.BROWN_MUSHROOM, 8).addExtraItem(Items.LIGHTNING_ROD, 1).addExtraItem(Items.COPPER_INGOT, 8)
                .addSpirit(SpiritTypeRegistry.EARTHEN_SPIRIT, 8).addSpirit(SpiritTypeRegistry.AQUEOUS_SPIRIT, 4)
                .addSpirit(SpiritTypeRegistry.INFERNAL_SPIRIT, 4).addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 8)
                .build(recipes, infusionId("brown_mooshroom"));
        infusion("blue_axolotl", ordinaryAxolotlBucket())
                .addExtraItem(Items.TROPICAL_FISH, 4).addExtraItem(Items.KELP, 8)
                .addExtraItem(Items.PRISMARINE_CRYSTALS, 4).addExtraItem(Items.AMETHYST_SHARD, 8)
                .addSpirit(SpiritTypeRegistry.AQUEOUS_SPIRIT, 16).addSpirit(SpiritTypeRegistry.SACRED_SPIRIT, 8)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 8).addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 8)
                .build(recipes, infusionId("blue_axolotl"));
        infusion("shulker", Ingredient.of(Items.CHORUS_FLOWER))
                .addExtraItem(Items.PURPUR_BLOCK, 8).addExtraItem(Items.ENDER_PEARL, 4)
                .addExtraItem(Items.END_STONE, 8).addExtraItem(Items.DIAMOND, 2)
                .addSpirit(SpiritTypeRegistry.ELDRITCH_SPIRIT, 16).addSpirit(SpiritTypeRegistry.ARCANE_SPIRIT, 16)
                .addSpirit(SpiritTypeRegistry.EARTHEN_SPIRIT, 8).build(recipes, infusionId("shulker"));
    }

    static Ingredient ordinaryAxolotlBucket() {
        CompoundTag blue = new CompoundTag();
        blue.putInt("Variant", 4);
        return DifferenceIngredient.of(Ingredient.of(Items.AXOLOTL_BUCKET), PartialNBTIngredient.of(Items.AXOLOTL_BUCKET, blue));
    }

    private static SpiritInfusionRecipeBuilder infusion(String target, Ingredient input) {
        return new MaledictRecipes.NbtSpiritInfusionRecipeBuilder(input, 1, InvocationSigilItem.create(target));
    }

    private static ResourceLocation infusionId(String target) {
        return ResourceLocation.fromNamespaceAndPath("maledict", "spirit_infusion/invocation_" + target);
    }

    private static void definition(Consumer<FinishedRecipe> recipes, String target, String entity, String variant,
                                   boolean soulwood, InvocationDefinition.Profile profile, int tint) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("maledict", "invocation/" + target);
        InvocationDefinition definition = new InvocationDefinition(id,
                ResourceLocation.fromNamespaceAndPath("minecraft", entity), variant, soulwood, profile, tint);
        recipes.accept(new FinishedRecipe() {
            @Override public void serializeRecipeData(JsonObject json) {
                definition.toJson().entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue()));
            }
            @Override public ResourceLocation getId() { return id; }
            @Override public RecipeSerializer<?> getType() { return MaledictRecipeTypes.INVOCATION_SERIALIZER.get(); }
            @Override public JsonObject serializeAdvancement() { return null; }
            @Override public ResourceLocation getAdvancementId() { return null; }
        });
    }

    private InvocationRecipes() {
    }
}
