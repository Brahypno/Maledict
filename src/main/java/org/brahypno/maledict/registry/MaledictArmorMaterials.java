package org.brahypno.maledict.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorMaterial;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.equipment.lightward.LightwardArmorMaterial;

/**
 * 1.21 把护甲材质挪进了注册表（{@code minecraft:armor_material}），物品按
 * {@code Holder<ArmorMaterial>} 引用它。整套数据见 {@link LightwardArmorMaterial}。
 */
public final class MaledictArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(BuiltInRegistries.ARMOR_MATERIAL, Maledict.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> LIGHTWARD =
            ARMOR_MATERIALS.register("aufheben_lightward", LightwardArmorMaterial::create);

    private MaledictArmorMaterials() {
    }
}
