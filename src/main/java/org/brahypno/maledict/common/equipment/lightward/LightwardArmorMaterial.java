package org.brahypno.maledict.common.equipment.lightward;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

/**
 * 扬弃逐光护甲材质：四件护甲 2 / 4 / 4 / 2，韧性每件 1，击退抗性每件 0.1（下界合金一档）。
 * 灵魂护盾容量、恢复速率、稳固度不是 {@link ArmorMaterial} 的字段，由
 * {@link LightwardArmorItem#getDefaultAttributeModifiers} 逐件附加。
 */
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
            case HELMET, BOOTS -> 2;
            case CHESTPLATE, LEGGINGS -> 4;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 36;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC.get());
    }

    @Override
    public String getName() {
        return Maledict.MODID + ":aufheben_lightward";
    }

    @Override
    public float getToughness() {
        return 1.0F;
    }

    /**
     * 每件 0.1，四件叠加 0.4；再往上已经没有可加的意义，属性本身在 1.0 封顶。
     */
    @Override
    public float getKnockbackResistance() {
        return 0.1F;
    }
}
