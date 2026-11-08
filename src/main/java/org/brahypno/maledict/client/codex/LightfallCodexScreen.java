package org.brahypno.maledict.client.codex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sammy.malum.MalumMod;
import com.sammy.malum.client.screen.codex.BookEntry;
import com.sammy.malum.client.screen.codex.BookWidgetStyle;
import com.sammy.malum.client.screen.codex.PlacedBookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntryBuilder;
import com.sammy.malum.client.screen.codex.objects.progression.ScreenOpenerObject;
import com.sammy.malum.client.screen.codex.pages.recipe.SpiritInfusionPage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextPage;
import com.sammy.malum.client.screen.codex.pages.text.TextPage;
import com.sammy.malum.client.screen.codex.screens.AbstractProgressionCodexScreen;
import com.sammy.malum.registry.common.SoundRegistry;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The Lightfall Codex's own page. The Blighted Star entry (the Weeping Wither page) has moved
 * here from the Void book. The two nodes below it return to the Arcana and Void views.
 */
public final class LightfallCodexScreen extends AbstractProgressionCodexScreen {
    public static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/gui/book/lightfall_background.png");

    private static final String WEEPING_WITHER = "void.maledict.weeping_wither";
    private static final String SOUL_BACK = "chronicles_of_the_soul";
    private static final String VOID_BACK = "chronicles_of_the_void";
    private static final ResourceLocation SOUL_ICON =
            MalumMod.malumPath("textures/gui/book/icons/arcana_button.png");
    private static final ResourceLocation VOID_ICON =
            MalumMod.malumPath("textures/gui/book/icons/void_button.png");

    private static LightfallCodexScreen screen;

    private final List<PlacedBookEntry> entries = new ArrayList<>();

    private LightfallCodexScreen() {
        super(SoundRegistry.ARCANA_SWEETENER_NORMAL, 1024, 2560);
        this.buildEntries();
        this.setupObjects();
    }

    public static LightfallCodexScreen getScreenInstance() {
        if (screen == null){
            screen = new LightfallCodexScreen();
        }
        return screen;
    }

    public static void openCodexViaItem() {
        LightfallCodex.rememberView(LightfallCodex.View.LIGHTFALL);
        LightfallCodexScreen instance = getScreenInstance();
        instance.openScreen(true);
        instance.playSweetenedSound(SoundRegistry.ARCANA_CODEX_OPEN, 1.25F);
    }

    @Override
    public Collection<PlacedBookEntry> getEntries() {
        return entries;
    }

    @Override
    public void renderBackground(PoseStack poseStack) {
        renderBackground(poseStack, BACKGROUND, 0.2F, 0.4F);
    }

    @Override
    public void setupObjects() {
        super.setupObjects();
        // The page should open on the moved Weeping Wither entry, with the returns below it.
        if (!bookObjectHandler.isEmpty()){
            faceObject(bookObjectHandler.get(0));
        }
    }

    private void buildEntries() {
        entries.add(buildWeepingWither());
        entries.add(buildBack(SOUL_BACK, 0, 1, SOUL_ICON,
                              BookWidgetStyle.DARK_GRAND_SOULWOOD, LightfallArcanaScreen::openCodexViaItem));
        entries.add(buildBack(VOID_BACK, 0, 0, VOID_ICON,
                              BookWidgetStyle.DARK_GRAND_RUNEWOOD, LightfallVoidScreen::openCodexViaItem));
    }

    private PlacedBookEntry buildWeepingWither() {
        PlacedBookEntryBuilder builder = BookEntry.build(WEEPING_WITHER, 0, 5);
        builder.configureWidget(widget -> widget.setIcon(MaledictItems.BLIGHTED_STAR)
                                                .setStyle(BookWidgetStyle.SOULWOOD));
        builder.addPage(new HeadlineTextPage(WEEPING_WITHER, WEEPING_WITHER + ".1"));
        builder.addPage(new TextPage(WEEPING_WITHER + ".2"));
        builder.addPage(new TextPage(WEEPING_WITHER + ".summoning"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.CRYING_TAINTED_ROCK.get()));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.CRYING_TWISTED_ROCK.get()));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.BLIGHTED_WITHER_SKULL.get()));
        return builder.build();
    }

    private PlacedBookEntry buildBack(
            String identifier, int x, int y, ResourceLocation icon,
            BookWidgetStyle style, Runnable opener) {
        PlacedBookEntryBuilder builder = BookEntry.build(identifier, x, y);
        builder.setWidgetSupplier((entry, posX, posY) -> new ScreenOpenerObject(
                entry, posX, posY, opener, icon, 20, 20));
        builder.configureWidget(widget -> widget.setStyle(style));
        return builder.build();
    }
}
