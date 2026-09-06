package org.brahypno.maledict;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.network.MaledictNetwork;

@Mod(Maledict.MODID)
public final class Maledict {
    public static final String MODID = "maledict";

    public Maledict() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MaledictConfig.COMMON_SPEC);
        MaledictItems.ITEMS.register(modBus);
        MaledictNetwork.register();
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.COMBAT) {
            event.accept(MaledictItems.INCURSUS_BLADE);
        }
    }
}
