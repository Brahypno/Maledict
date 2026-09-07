package org.brahypno.maledict.registry;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.enchantment.EctoplasmEnchantment;

public final class MaledictEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Maledict.MODID);

    public static final RegistryObject<Enchantment> ECTOPLASM =
            ENCHANTMENTS.register("ectoplasm", EctoplasmEnchantment::new);

    private MaledictEnchantments() {
    }
}
