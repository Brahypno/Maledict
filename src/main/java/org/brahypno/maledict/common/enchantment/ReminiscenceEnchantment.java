package org.brahypno.maledict.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.brahypno.maledict.common.item.RemembranceBowItem;

public final class ReminiscenceEnchantment extends Enchantment {
    private static final EnchantmentCategory REMEMBRANCE_BOW =
            EnchantmentCategory.create("remembrance_bow", item -> item instanceof RemembranceBowItem);

    public ReminiscenceEnchantment() {
        super(Rarity.RARE, REMEMBRANCE_BOW, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 15;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}
