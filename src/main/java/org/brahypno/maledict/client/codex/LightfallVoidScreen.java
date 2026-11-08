package org.brahypno.maledict.client.codex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sammy.malum.MalumMod;
import com.sammy.malum.client.screen.codex.BookEntry;
import com.sammy.malum.client.screen.codex.BookWidgetStyle;
import com.sammy.malum.client.screen.codex.PlacedBookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntryBuilder;
import com.sammy.malum.client.screen.codex.objects.progression.ScreenOpenerObject;
import com.sammy.malum.client.screen.codex.screens.AbstractProgressionCodexScreen;
import com.sammy.malum.client.screen.codex.screens.VoidProgressionScreen;
import com.sammy.malum.registry.common.SoundRegistry;
import org.brahypno.maledict.Maledict;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * The third book's Void view. It mirrors the Void book but its return link goes back to our
 * Arcana view, and it has one extra jump node below that link.
 */
public final class LightfallVoidScreen extends AbstractProgressionCodexScreen {
    private static final String SOUL_LINK = "chronicles_of_the_soul";
    private static final String DESCENT_LINK = "maledict.lightfall.descent";
    private static final ResourceLocation SOUL_ICON =
            MalumMod.malumPath("textures/gui/book/icons/arcana_button.png");
    private static final ResourceLocation WEEPING_WELL_ICON =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "textures/gui/book/icons/void_depot_button.png");

    private static LightfallVoidScreen screen;

    private final List<PlacedBookEntry> entries = new ArrayList<>();

    private LightfallVoidScreen() {
        super(SoundRegistry.ARCANA_SWEETENER_EVIL, 1024, 768);
        VoidProgressionScreen.getScreenInstance();
        this.buildEntries();
        this.setupObjects();
    }

    public static LightfallVoidScreen getScreenInstance() {
        if (screen == null){
            screen = new LightfallVoidScreen();
        }
        return screen;
    }

    public static void openCodexViaItem() {
        LightfallCodex.rememberView(LightfallCodex.View.VOID);
        LightfallVoidScreen instance = getScreenInstance();
        instance.openScreen(true);
        instance.playSweetenedSound(SoundRegistry.ARCANA_CODEX_OPEN, 1.25F);
    }

    @Override
    public Collection<PlacedBookEntry> getEntries() {
        return entries;
    }

    @Override
    public void renderBackground(PoseStack poseStack) {
        renderBackground(poseStack, VoidProgressionScreen.BACKGROUND_TEXTURE, 0.2F, 0.2F);
    }

    @Override
    public void setupObjects() {
        super.setupObjects();
        // Match the original Void transition view: center on the return link at (0, 0).
        if (!bookObjectHandler.isEmpty()){
            faceObject(bookObjectHandler.get(0));
        }
    }

    private void buildEntries() {
        entries.add(buildSoulLink());
        for (PlacedBookEntry entry : VoidProgressionScreen.VOID_ENTRIES){
            if (SOUL_LINK.equals(entry.identifier)){
                continue;
            }
            if (entry.identifier.startsWith("maledict.") || entry.identifier.startsWith("void.maledict.")){
                continue;
            }
            entries.add(entry);
        }
        entries.add(buildLightfallLink());
    }

    private PlacedBookEntry buildSoulLink() {
        PlacedBookEntryBuilder builder = BookEntry.build(SOUL_LINK, 0, 0);
        builder.setWidgetSupplier((entry, x, y) -> new ScreenOpenerObject(entry, x, y,
                () -> LightfallArcanaScreen.openCodexViaItem(), SOUL_ICON, 20, 20));
        builder.configureWidget(widget -> widget.setStyle(BookWidgetStyle.DARK_GRAND_SOULWOOD));
        return builder.build();
    }

    private PlacedBookEntry buildLightfallLink() {
        PlacedBookEntryBuilder builder = BookEntry.build(DESCENT_LINK, 0, -1);
        builder.setWidgetSupplier((entry, x, y) -> new ScreenOpenerObject(
                entry, x, y, LightfallCodexScreen::openCodexViaItem, WEEPING_WELL_ICON, 20, 20));
        // Wavy icons use additive blending and need a dark filling to retain their colors.
        builder.configureWidget(widget -> widget.setStyle(BookWidgetStyle.DARK_GILDED_SOULWOOD));
        return builder.build();
    }
}
