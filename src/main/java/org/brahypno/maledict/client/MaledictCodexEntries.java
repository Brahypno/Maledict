package org.brahypno.maledict.client;

import com.sammy.malum.client.screen.codex.BookEntry;
import com.sammy.malum.client.screen.codex.BookWidgetStyle;
import com.sammy.malum.client.screen.codex.PlacedBookEntryBuilder;
import com.sammy.malum.client.screen.codex.pages.recipe.SpiritInfusionPage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextItemPage;
import com.sammy.malum.client.screen.codex.pages.text.TextPage;
import com.sammy.malum.client.screen.codex.screens.VoidProgressionScreen;
import com.sammy.malum.common.events.SetupMalumCodexEntriesEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MaledictCodexEntries {
    private static final String INCURSUS_BLADE_ENTRY = "void.maledict.incursus_blade";

    @SubscribeEvent
    public static void setupEntries(SetupMalumCodexEntriesEvent event) {
        boolean alreadyAdded = VoidProgressionScreen.VOID_ENTRIES.stream()
                .anyMatch(entry -> INCURSUS_BLADE_ENTRY.equals(entry.identifier));
        if (alreadyAdded) {
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

    private MaledictCodexEntries() {
    }
}
