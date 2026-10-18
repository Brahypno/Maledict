package org.brahypno.maledict.registry;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public enum MaledictItemTiers implements Tier {
    INCURSUS;

    @Override
    public int getUses() {
        return 2500;
    }

    @Override
    public float getSpeed() {
        return 8.0f;
    }

    @Override
    public float getAttackDamageBonus() {
        return 4.0f;
    }

    @Override
    public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
        return net.minecraft.tags.BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return 40;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.NETHER_STAR);
    }
}
