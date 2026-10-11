package org.brahypno.maledict.client.tooltip;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

/**
 * Item-specific tooltip colors on the client Forge bus.
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
