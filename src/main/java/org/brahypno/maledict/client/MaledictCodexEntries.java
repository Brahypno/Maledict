package org.brahypno.maledict.client;

import com.sammy.malum.client.screen.codex.BookEntry;
import com.sammy.malum.client.screen.codex.BookWidgetStyle;
import com.sammy.malum.client.screen.codex.PlacedBookEntryBuilder;
import com.sammy.malum.client.screen.codex.pages.recipe.SpiritInfusionPage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextPage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextItemPage;
import com.sammy.malum.client.screen.codex.pages.text.TextPage;
import com.sammy.malum.client.screen.codex.screens.ArcanaProgressionScreen;
import com.sammy.malum.client.screen.codex.screens.VoidProgressionScreen;
import com.sammy.malum.common.events.SetupMalumCodexEntriesEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MaledictCodexEntries {
    private static final String REMEMBRANCE_BOW_ENTRY = "maledict.remembrance_bow";
    private static final String INCURSUS_BLADE_ENTRY = "void.maledict.incursus_blade";
    private static final String OBELISKS_ENTRY = "void.maledict.obelisks";
    private static final String SOULWOOD_OBELISK_PAGE = OBELISKS_ENTRY + ".soulwood_obelisk";
    private static final String MNEMONIC_OBELISK_PAGE = OBELISKS_ENTRY + ".mnemonic_obelisk";

    @SubscribeEvent
    public static void setupEntries(SetupMalumCodexEntriesEvent event) {
        addRemembranceBowEntry();
        addObelisksEntry();
        addIncursusBladeEntry();
    }

    private static void addRemembranceBowEntry() {
        if (containsEntry(ArcanaProgressionScreen.ENTRIES, REMEMBRANCE_BOW_ENTRY)) {
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.build(REMEMBRANCE_BOW_ENTRY, 2, 13);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.REMEMBRANCE_BOW)
                .setStyle(BookWidgetStyle.SOULWOOD));
        builder.addPage(new HeadlineTextItemPage(
                REMEMBRANCE_BOW_ENTRY,
                REMEMBRANCE_BOW_ENTRY + ".1",
                MaledictItems.REMEMBRANCE_BOW.get()));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.REMEMBRANCE_BOW.get()));

        ArcanaProgressionScreen.ENTRIES.add(builder.build());
    }

    private static void addObelisksEntry() {
        if (containsEntry(VoidProgressionScreen.VOID_ENTRIES, OBELISKS_ENTRY)) {
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.build(OBELISKS_ENTRY, -1, 8);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.SOULWOOD_OBELISK)
                .setStyle(BookWidgetStyle.SOULWOOD));
        builder.addPage(new HeadlineTextPage(
                SOULWOOD_OBELISK_PAGE,
                SOULWOOD_OBELISK_PAGE + ".1"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.SOULWOOD_OBELISK.get()));
        builder.addPage(new HeadlineTextPage(
                MNEMONIC_OBELISK_PAGE,
                MNEMONIC_OBELISK_PAGE + ".1"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.MNEMONIC_OBELISK.get()));

        VoidProgressionScreen.VOID_ENTRIES.add(builder.build());
    }

    private static void addIncursusBladeEntry() {
        if (containsEntry(VoidProgressionScreen.VOID_ENTRIES, INCURSUS_BLADE_ENTRY)) {
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.build(INCURSUS_BLADE_ENTRY, 6, 11);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.INCURSUS_BLADE)
                .setStyle(BookWidgetStyle.SOULWOOD));
        builder.addPage(new HeadlineTextItemPage(
                INCURSUS_BLADE_ENTRY,
                INCURSUS_BLADE_ENTRY + ".1",
                MaledictItems.INCURSUS_BLADE.get()));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.INCURSUS_BLADE.get()));
        builder.addPage(new TextPage(INCURSUS_BLADE_ENTRY + ".2"));
        builder.afterUmbralCrystal();

        VoidProgressionScreen.VOID_ENTRIES.add(builder.build());
    }

    private static boolean containsEntry(Iterable<? extends BookEntry> entries, String identifier) {
        for (BookEntry entry : entries) {
            if (identifier.equals(entry.identifier)) {
                return true;
            }
        }
        return false;
    }

    private MaledictCodexEntries() {
    }
}
