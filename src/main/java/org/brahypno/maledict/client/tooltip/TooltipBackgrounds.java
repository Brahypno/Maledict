package org.brahypno.maledict.client.tooltip;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.brahypno.maledict.Maledict;

import java.util.ArrayList;
import java.util.List;

/** Pure client renderer; combinations are supplied through RegisterTooltipBackgroundsEvent. */
@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class TooltipBackgrounds {
    private static List<RegisterTooltipBackgroundsEvent.Registration> backgrounds = List.of();

    private TooltipBackgrounds() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        if (event.getItemStack().isEmpty() || event.getComponents().isEmpty()) return;
        TooltipBackground background = find(event.getItemStack());
        if (background == null) return;

        List<TooltipFrameLayout.Size> components = new ArrayList<>(event.getComponents().size());
        for (ClientTooltipComponent component : event.getComponents()) {
            components.add(new TooltipFrameLayout.Size(component.getWidth(event.getFont()), component.getHeight()));
        }
        TooltipFrameLayout.Size size = TooltipFrameLayout.measure(components);
        // Color already carries the final position after wrapping, Pre handlers and the positioner.
        TooltipFrameLayout.Insets outset = TooltipFrameLayout.fitOutset(size.height(),
                background.border(), background.outset());
        TooltipFrameLayout frame = TooltipFrameLayout.around(event.getX(), event.getY(),
                size.width(), size.height(), outset);
        background.render(event.getGraphics(), frame, outset);
        if (background.mode() == TooltipBackground.Mode.REPLACE) {
            event.setBackground(0);
            event.setBorderStart(0);
            event.setBorderEnd(0);
        }
    }

    private static TooltipBackground find(ItemStack stack) {
        for (int i = backgrounds.size() - 1; i >= 0; i--) {
            var registration = backgrounds.get(i);
            if (registration.matches().test(stack)) {
                TooltipBackground combination = registration.selectCombination().apply(stack);
                if (combination != null) return combination;
            }
        }
        return null;
    }

    @EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
    public static final class ClientRegistration {
        private ClientRegistration() {}

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                var registration = new RegisterTooltipBackgroundsEvent();
                ModLoader.postEvent(registration);
                backgrounds = registration.registrations();
            });
        }
    }
}
