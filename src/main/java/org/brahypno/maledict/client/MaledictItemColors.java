package org.brahypno.maledict.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.SpiritArrowItem;
import org.brahypno.maledict.registry.MaledictItems;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MaledictItemColors {
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
                    if (tintIndex != 0 || !(stack.getItem() instanceof SpiritArrowItem arrowItem)) {
                        return 0xFFFFFFFF;
                    }
                    return arrowItem.getDefiningSpiritType().getItemColor().getRGB();
                },
                MaledictItems.SACRED_SPIRIT_ARROW.get(),
                MaledictItems.WICKED_SPIRIT_ARROW.get(),
                MaledictItems.ARCANE_SPIRIT_ARROW.get(),
                MaledictItems.ELDRITCH_SPIRIT_ARROW.get(),
                MaledictItems.AERIAL_SPIRIT_ARROW.get(),
                MaledictItems.AQUEOUS_SPIRIT_ARROW.get(),
                MaledictItems.EARTHEN_SPIRIT_ARROW.get(),
                MaledictItems.INFERNAL_SPIRIT_ARROW.get());
    }

    private MaledictItemColors() {
    }
}
