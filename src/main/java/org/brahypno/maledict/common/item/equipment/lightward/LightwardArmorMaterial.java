package org.brahypno.maledict.common.item.equipment.lightward;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 扬弃逐光护甲材质：四件护甲 2 / 4 / 4 / 2，韧性每件 1，击退抗性每件 0.1（下界合金一档）。
 *
 * <p>1.21 把这套数据变成了注册表里的 {@link ArmorMaterial} record：耐久不再属于材质，改由每件物品的
 * {@code Properties#durability} 给出；贴图走 {@link ArmorMaterial.Layer}，但本套装四件各不相同，
 * 所以{{@link LightwardArmorItem#getArmorTexture}} 按槽位覆盖，这里的 layer 只是兜底。
 * 灵魂护盾容量、恢复速率、稳固度不是材质的字段，逐件挂在物品属性上（见 {@link LightwardSetEvents} 与
 * {@code LightwardArmorAttributes}）。
 */
public final class LightwardArmorMaterial {
    /**
     * 兜底 layer：解析为 {@code maledict:textures/models/armor/aufheben_lightward_layer_1.png} /
     * {@code _layer_2.png}，与 1.20.1 的整套贴图同名。
     */
    public static final ResourceLocation LAYER_ASSET =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "aufheben_lightward");

    public static ArmorMaterial create() {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 2);
        defense.put(ArmorItem.Type.CHESTPLATE, 4);
        defense.put(ArmorItem.Type.LEGGINGS, 4);
        defense.put(ArmorItem.Type.BOOTS, 2);
        return new ArmorMaterial(
                Map.copyOf(defense), 36, SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> Ingredient.of(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC.get()),
                List.of(new ArmorMaterial.Layer(LAYER_ASSET)), 1.0F, 0.1F);
    }

    private LightwardArmorMaterial() {
    }
}
