package org.brahypno.maledict;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.network.MaledictNetwork;
import org.brahypno.maledict.registry.MaledictCreativeTabs;
import org.brahypno.maledict.registry.MaledictEnchantments;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.registry.MaledictItems;

@Mod(Maledict.MODID)
public final class Maledict {
    public static final String MODID = "maledict";

    @SuppressWarnings({"removal"})
    public Maledict() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MaledictConfig.COMMON_SPEC);
        MaledictEnchantments.ENCHANTMENTS.register(modBus);
        MaledictEntities.ENTITY_TYPES.register(modBus);
        MaledictItems.ITEMS.register(modBus);
        MaledictCreativeTabs.CREATIVE_TABS.register(modBus);
        MaledictNetwork.register();
    }
}
