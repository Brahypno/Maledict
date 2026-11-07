package org.brahypno.maledict.common.equipment.lightward;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

/** 扬弃逐光护甲材质；数值暂按下界合金，贴图未定。 */
public enum LightwardArmorMaterial implements ArmorMaterial {
    INSTANCE;

    private static final int[] DURABILITY_PER_TYPE = {13, 15, 16, 11};
    private static final int DURABILITY_MULTIPLIER = 37;

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return DURABILITY_PER_TYPE[type.ordinal()] * DURABILITY_MULTIPLIER;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 3;
            case CHESTPLATE -> 8;
            case LEGGINGS -> 6;
            case BOOTS -> 3;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 30;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(MaledictItems.LIGHT_FRAGMENT.get());
    }

    @Override
    public String getName() {
        return Maledict.MODID + ":aufheben_lightward";
    }

    @Override
    public float getToughness() {
        return 3.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.1F;
    }
}
