package org.brahypno.maledict.client.tooltip;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

/**
 * Item-specific tooltip colors on the client Forge bus.
 */
@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class MaledictTooltipColors {
    /**
     * 殷红色，沿用原版提示框的透明度。
     */
    private static final int BLEEDING_EYE_BACKGROUND = 0xFF3B0B0B;

    private MaledictTooltipColors() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        if (event.getItemStack().is(MaledictItems.BLEEDING_EYE.get())){
            event.setBackground(BLEEDING_EYE_BACKGROUND);
        }
    }
}
