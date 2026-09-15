package org.brahypno.maledict.common.enchantment;

import com.sammy.malum.registry.common.item.EnchantmentRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * A scythe enchantment that tastes what the corpse would have fed its killer: the saturation and
 * hunger a victim's drops are expected to provide, scaled by the share of its health the hit took.
 */
public final class AftertasteEnchantment extends Enchantment {
    public static final int MAX_LEVEL = 3;
    /** Share of the expected nourishment restored before any level is added. */
    public static final double BASE_RESTORE_SHARE = 0.4D;
    /** Extra share of the expected nourishment every enchantment level restores. */
    public static final double RESTORE_SHARE_PER_LEVEL = 0.1D;

    public AftertasteEnchantment() {
        super(Rarity.RARE, EnchantmentRegistry.SCYTHE_ONLY,
              new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
    }

    /** Share of the expected nourishment a scythe of this level restores, 50% to 70% at level 1-3. */
    public static double getRestoreShare(int level) {
        return BASE_RESTORE_SHARE + RESTORE_SHARE_PER_LEVEL * level;
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
        return MAX_LEVEL;
    }
}
