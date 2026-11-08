package org.brahypno.maledict.client.codex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sammy.malum.MalumMod;
import com.sammy.malum.client.VoidRevelationHandler;
import com.sammy.malum.client.screen.codex.BookEntry;
import com.sammy.malum.client.screen.codex.BookWidgetStyle;
import com.sammy.malum.client.screen.codex.PlacedBookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntryBuilder;
import com.sammy.malum.client.screen.codex.objects.progression.ScreenOpenerObject;
import com.sammy.malum.client.screen.codex.screens.AbstractProgressionCodexScreen;
import com.sammy.malum.client.screen.codex.screens.ArcanaProgressionScreen;
import com.sammy.malum.registry.common.SoundRegistry;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The third book's Arcana view. It starts on the same soul-magic view as the Esoterica and
 * keeps the original Chronicles of the Void position, but that link now opens our own Void view
 * and records it. A new jump node sits directly below it.
 */
public final class LightfallArcanaScreen extends AbstractProgressionCodexScreen {
    private static final String VOID_LINK = "chronicles_of_the_void";
    private static final String LIGHTFALL_LINK = "maledict.lightfall.descent";
    private static final ResourceLocation VOID_ICON =
            MalumMod.malumPath("textures/gui/book/icons/void_button.png");
    private static final ResourceLocation WEEPING_WELL_ICON =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "textures/gui/book/icons/void_depot_button.png");

    private static LightfallArcanaScreen screen;

    private final List<PlacedBookEntry> entries = new ArrayList<>();

    private LightfallArcanaScreen() {
        super(SoundRegistry.ARCANA_SWEETENER_NORMAL, 1024, 2560);
        ArcanaProgressionScreen.getScreenInstance();
        this.isVoidTouched = true;
        this.buildEntries();
        this.setupObjects();
    }

    public static LightfallArcanaScreen getScreenInstance() {
        if (screen == null){
            screen = new LightfallArcanaScreen();
        }
        return screen;
    }

    public static void openCodexViaItem() {
        LightfallCodex.rememberView(LightfallCodex.View.ARCANA);
        LightfallArcanaScreen instance = getScreenInstance();
        instance.openScreen(true);
        instance.playSweetenedSound(SoundRegistry.ARCANA_CODEX_OPEN, 1.25F);
    }

    @Override
    public Collection<PlacedBookEntry> getEntries() {
        return entries;
    }

    @Override
    public void renderBackground(PoseStack poseStack) {
        renderBackground(poseStack, ArcanaProgressionScreen.BACKGROUND_TEXTURE, 0.2F, 0.4F);
    }

    private void buildEntries() {
        // The original Void link's slot, retargeted to our Void view.
        entries.add(buildVoidLink());
        for (PlacedBookEntry entry : ArcanaProgressionScreen.ENTRIES) {
            if (VOID_LINK.equals(entry.identifier)){
                continue;
            }
            if (entry.identifier.startsWith("maledict.") || entry.identifier.startsWith("void.maledict.")){
                continue;
            }
            entries.add(entry);
        }
        entries.add(buildLightfallLink());
    }

    private PlacedBookEntry buildVoidLink() {
        PlacedBookEntryBuilder builder = BookEntry.build(VOID_LINK, 0, -1);
        builder.setWidgetSupplier((entry, x, y) -> new ScreenOpenerObject(entry, x, y,
                                                                          () -> {
                                                                              VoidRevelationHandler.seeTheRevelation(
                                                                                      VoidRevelationHandler.RevelationType.VOID_READER);
                                                                              LightfallVoidScreen.openCodexViaItem();
                                                                          }, VOID_ICON, 20, 20));
        builder.configureWidget(widget -> widget.setStyle(BookWidgetStyle.DARK_GRAND_RUNEWOOD));
        return builder.build();
    }

    private PlacedBookEntry buildLightfallLink() {
        PlacedBookEntryBuilder builder = BookEntry.build(LIGHTFALL_LINK, 0, -2);
        builder.setWidgetSupplier((entry, x, y) -> new ScreenOpenerObject(
                entry, x, y, LightfallCodexScreen::openCodexViaItem, WEEPING_WELL_ICON, 20, 20));
        // Wavy icons use additive blending and need a dark filling to retain their colors.
        builder.configureWidget(widget -> widget.setStyle(BookWidgetStyle.DARK_GRAND_RUNEWOOD));
        return builder.build();
    }
}
