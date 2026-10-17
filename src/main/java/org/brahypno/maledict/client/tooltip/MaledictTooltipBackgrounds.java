package org.brahypno.maledict.client.tooltip;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

/** Item-to-artwork registrations on the client MOD bus. */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaledictTooltipBackgrounds {
    public static final TooltipBackground AGE_OF_ENLIGHTENMENT = new TooltipBackground(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID,
                    "textures/gui/tooltip/age_of_enlightenment.png"),
            96, 144,
            // Keep the whole shoulder and primary feathers inside the fixed upper corners.
            new TooltipFrameLayout.Insets(36, 88, 36, 28),
            // Match the artwork's inner rail to the vanilla silhouette; spare transparent pixels
            // lie underneath the vanilla background instead of leaving a visible gap.
            new TooltipFrameLayout.Insets(25, 22, 25, 22),
            TooltipBackground.Mode.DECORATE, TooltipBackground.EdgeMode.STRETCH,
            new TooltipBackground.TopDecoration(
                    ResourceLocation.fromNamespaceAndPath(Maledict.MODID,
                            "textures/gui/tooltip/age_of_enlightenment_crest.png"),
                    72, 24, 72, 24, 2));

    public static final TooltipBackground HANGMAN_FOOL = new TooltipBackground(
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID,
                    "textures/gui/tooltip/hangman_fool.png"),
            96, 144,
            // Wings remain in the fixed lower corners; only the bare upper rails stretch.
            new TooltipFrameLayout.Insets(32, 20, 32, 104),
            new TooltipFrameLayout.Insets(23, 14, 23, 15),
            TooltipBackground.Mode.DECORATE, TooltipBackground.EdgeMode.STRETCH);

    private MaledictTooltipBackgrounds() {}

    @SubscribeEvent
    public static void register(RegisterTooltipBackgroundsEvent event) {
        event.register(MaledictItems.AGE_OF_ENLIGHTENMENT.get(), AGE_OF_ENLIGHTENMENT);
        event.register(MaledictItems.HANGMAN_FOOL.get(), HANGMAN_FOOL);
    }
}
