package org.brahypno.maledict.compat.maid;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.brahypno.maledict.Maledict;

/**
 * Loads the Touhou Little Maid handlers only when that mod is present. The reference to MaidEvents
 * sits behind the ModList guard, so the JVM resolves that class only when the branch is taken.
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaledictMaidIntegration {
    public static final String MAID_MOD_ID = "touhou_little_maid";

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded(MAID_MOD_ID))
            return;
        event.enqueueWork(MaidEvents::register);
    }

    private MaledictMaidIntegration() {
    }
}
