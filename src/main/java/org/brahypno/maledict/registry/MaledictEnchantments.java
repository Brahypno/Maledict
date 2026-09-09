package org.brahypno.maledict.registry;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.enchantment.EctoplasmEnchantment;
import org.brahypno.maledict.common.enchantment.ReminiscenceEnchantment;

public final class MaledictEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Maledict.MODID);

    public static final RegistryObject<Enchantment> ECTOPLASM =
            ENCHANTMENTS.register("ectoplasm", EctoplasmEnchantment::new);
    public static final RegistryObject<Enchantment> REMINISCENCE =
            ENCHANTMENTS.register("reminiscence", ReminiscenceEnchantment::new);

    private MaledictEnchantments() {
    }
}
