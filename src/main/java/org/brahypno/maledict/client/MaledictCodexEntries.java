package org.brahypno.maledict.client;

import com.sammy.malum.client.screen.codex.BookEntry;
import com.sammy.malum.client.screen.codex.BookWidgetStyle;
import com.sammy.malum.client.screen.codex.PlacedBookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntryBuilder;
import com.sammy.malum.client.screen.codex.pages.EntryReference;
import com.sammy.malum.client.screen.codex.pages.EntrySelectorPage;
import com.sammy.malum.client.screen.codex.pages.recipe.RuneworkingPage;
import com.sammy.malum.client.screen.codex.pages.recipe.SpiritInfusionPage;
import com.sammy.malum.client.screen.codex.pages.recipe.SpiritRiteRecipePage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextItemPage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextPage;
import com.sammy.malum.client.screen.codex.pages.text.TextPage;
import com.sammy.malum.client.screen.codex.screens.ArcanaProgressionScreen;
import com.sammy.malum.client.screen.codex.screens.VoidProgressionScreen;
import com.sammy.malum.common.events.SetupMalumCodexEntriesEvent;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.common.rite.SummoningRite;
import org.brahypno.maledict.common.rite.VicissitudeRiteType;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.List;

@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MaledictCodexEntries {
    private static final String REMEMBRANCE_BOW_ENTRY = "maledict.remembrance_bow";
    private static final String ELEGY_BOW_ENTRY = "void.maledict.elegy_bow";
    private static final int REMEMBRANCE_BOW_X = 2;
    private static final int REMEMBRANCE_BOW_Y = 13;
    private static final String INCURSUS_BLADE_ENTRY = "void.maledict.incursus_blade";
    private static final String OBELISKS_ENTRY = "void.maledict.obelisks";
    private static final String SOULWOOD_OBELISK_PAGE = OBELISKS_ENTRY + ".soulwood_obelisk";
    private static final String MNEMONIC_OBELISK_PAGE = OBELISKS_ENTRY + ".mnemonic_obelisk";
    private static final String RITE_ENTRY = "void.maledict.vicissitude_rite";
    private static final String TOTEMIC_RUNES_CONTINUED_ENTRY = "maledict.totemic_runes_continued";
    private static final String RUNE_OF_SATIATION_ENTRY = "maledict.rune_of_satiation";
    private static final String RUNE_OF_DECAY_ENTRY = "maledict.rune_of_decay";
    private static final String RUNE_OF_THE_PACK_ENTRY = "maledict.rune_of_the_pack";
    private static final String RUNE_OF_RIPENING_ENTRY = "maledict.rune_of_ripening";
    private static final String VOID_RUNEWORKING_ENTRY = "void.maledict.runeworking";

    private static final String RUNE_OF_STAGNANT_EVOLUTION_ENTRY = "void.maledict.rune_of_stagnant_evolution";
    private static final String RUNE_OF_ROTTEN_BONE_ENTRY = "void.maledict.rune_of_rotten_bone";

    private static final int TOTEMIC_RUNES_CONTINUED_X = 4;
    private static final int TOTEMIC_RUNES_CONTINUED_Y = 15;

    /** 避开 Malum 符文条目占着的 (-15..-12, 7..10)：三枚竖排在左列 (-15, 7..9)，熟成落在 (-12, 8)。 */
    private static final int RUNE_COLUMN_X = -15;
    private static final int RUNE_OF_DECAY_Y = 7;
    private static final int RUNE_OF_SATIATION_Y = 8;
    private static final int RUNE_OF_THE_PACK_Y = 9;
    private static final int RUNE_OF_RIPENING_X = -12;
    private static final int RUNE_OF_RIPENING_Y = 8;

    private static final int VOID_RUNEWORKING_X = 6;
    private static final int VOID_RUNEWORKING_Y = 10;

    @SubscribeEvent
    public static void setupEntries(SetupMalumCodexEntriesEvent event) {
        addRemembranceBowEntry();
        addElegyBowEntry();
        addObelisksEntry();
        addIncursusBladeEntry();
        addVicissitudeRiteEntry();
        addVoidRuneworkingEntry();

        PlacedBookEntry satiationRune = addRuneEntry(RUNE_OF_SATIATION_ENTRY, MaledictItems.RUNE_OF_SATIATION,
                                                     RUNE_COLUMN_X, RUNE_OF_SATIATION_Y, BookWidgetStyle.SOULWOOD);
        PlacedBookEntry decayRune = addRuneEntry(RUNE_OF_DECAY_ENTRY, MaledictItems.RUNE_OF_DECAY,
                                                 RUNE_COLUMN_X, RUNE_OF_DECAY_Y, BookWidgetStyle.RUNEWOOD);
        PlacedBookEntry packRune = addRuneEntry(RUNE_OF_THE_PACK_ENTRY, MaledictItems.RUNE_OF_THE_PACK,
                                                RUNE_COLUMN_X, RUNE_OF_THE_PACK_Y, BookWidgetStyle.SOULWOOD);
        PlacedBookEntry ripeningRune = addRuneEntry(RUNE_OF_RIPENING_ENTRY, MaledictItems.RUNE_OF_RIPENING,
                                                    RUNE_OF_RIPENING_X, RUNE_OF_RIPENING_Y, BookWidgetStyle.SOULWOOD);
        addTotemicRunesContinuedEntry(List.of(
                new EntryReference(MaledictItems.RUNE_OF_SATIATION, satiationRune),
                new EntryReference(MaledictItems.RUNE_OF_DECAY, decayRune),
                new EntryReference(MaledictItems.RUNE_OF_THE_PACK, packRune),
                new EntryReference(MaledictItems.RUNE_OF_RIPENING, ripeningRune)));
    }

    private static void addVoidRuneworkingEntry() {
        EntryReference stagnantEvolution = voidRuneEntry(
                RUNE_OF_STAGNANT_EVOLUTION_ENTRY, MaledictItems.RUNE_OF_STAGNANT_EVOLUTION);
        EntryReference rottenBone = voidRuneEntry(
                RUNE_OF_ROTTEN_BONE_ENTRY, MaledictItems.RUNE_OF_ROTTEN_BONE);

        if (containsEntry(VoidProgressionScreen.VOID_ENTRIES, VOID_RUNEWORKING_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.build(VOID_RUNEWORKING_ENTRY, VOID_RUNEWORKING_X, VOID_RUNEWORKING_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.MALIGNANT_PEWTER_TABLET)
                .setStyle(BookWidgetStyle.DARK_SOULWOOD));
        builder.addPage(new HeadlineTextItemPage(
                VOID_RUNEWORKING_ENTRY,
                VOID_RUNEWORKING_ENTRY + ".1",
                MaledictItems.MALIGNANT_PEWTER_TABLET.get()));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.MALIGNANT_PEWTER_TABLET.get()));
        builder.addPage(new EntrySelectorPage(List.of(stagnantEvolution, rottenBone)));
        builder.afterUmbralCrystal();

        VoidProgressionScreen.VOID_ENTRIES.add(builder.build());
    }

    private static void addVicissitudeRiteEntry() {
        VicissitudeRiteType simple = SummoningRite.rite(BossDifficulty.SIMPLE);
        VicissitudeRiteType difficult = SummoningRite.rite(BossDifficulty.DIFFICULT);
        VicissitudeRiteType complete = SummoningRite.rite(BossDifficulty.COMPLETE);
        VicissitudeRiteType extreme = SummoningRite.rite(BossDifficulty.EXTREME);
        if (simple == null || difficult == null || complete == null || extreme == null
            || containsEntry(VoidProgressionScreen.VOID_ENTRIES, RITE_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.build(RITE_ENTRY, 8, 13);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.INCURSUS_BLADE)
                .setStyle(BookWidgetStyle.DARK_SOULWOOD));
        builder.addPage(new HeadlineTextPage(RITE_ENTRY, RITE_ENTRY + ".1"));
        builder.addPage(new SpiritRiteRecipePage(simple));
        builder.addPage(new SpiritRiteRecipePage(difficult));
        builder.addPage(new TextPage(RITE_ENTRY + ".2"));
        builder.addPage(new SpiritRiteRecipePage(complete));
        builder.addPage(new SpiritRiteRecipePage(extreme));
        builder.afterUmbralCrystal();

        VoidProgressionScreen.VOID_ENTRIES.add(builder.build());
    }

    private static PlacedBookEntry addRuneEntry(
            String identifier, RegistryObject<Item> rune, int x, int y,
            BookWidgetStyle style) {
        PlacedBookEntry existing = findEntry(ArcanaProgressionScreen.ENTRIES, identifier);
        if (existing != null){
            return existing;
        }

        PlacedBookEntryBuilder builder = BookEntry.build(identifier, x, y);
        builder.configureWidget(widget -> widget.setIcon(rune).setStyle(style));
        builder.addPage(new HeadlineTextPage(identifier, identifier + ".1"));
        builder.addPage(RuneworkingPage.fromOutput(rune.get()));

        PlacedBookEntry entry = builder.build();
        ArcanaProgressionScreen.ENTRIES.add(entry);
        return entry;
    }

    private static EntryReference voidRuneEntry(String identifier, RegistryObject<Item> rune) {
        return new EntryReference(rune, BookEntry.build(identifier)
                .addPage(new HeadlineTextPage(identifier, identifier + ".1"))
                .addPage(RuneworkingPage.fromOutput(rune.get())));
    }

    private static void addTotemicRunesContinuedEntry(List<EntryReference> runes) {
        if (containsEntry(ArcanaProgressionScreen.ENTRIES, TOTEMIC_RUNES_CONTINUED_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.build(
                TOTEMIC_RUNES_CONTINUED_ENTRY, TOTEMIC_RUNES_CONTINUED_X, TOTEMIC_RUNES_CONTINUED_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.RUNE_OF_SATIATION)
                .setStyle(BookWidgetStyle.SOULWOOD));
        builder.addPage(new HeadlineTextItemPage(
                TOTEMIC_RUNES_CONTINUED_ENTRY,
                TOTEMIC_RUNES_CONTINUED_ENTRY + ".1",
                MaledictItems.RUNE_OF_SATIATION.get()));
        builder.addPage(new EntrySelectorPage(runes));

        ArcanaProgressionScreen.ENTRIES.add(builder.build());
    }

    private static void addRemembranceBowEntry() {
        if (containsEntry(ArcanaProgressionScreen.ENTRIES, REMEMBRANCE_BOW_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.build(
                REMEMBRANCE_BOW_ENTRY, REMEMBRANCE_BOW_X, REMEMBRANCE_BOW_Y);
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

    private static void addElegyBowEntry() {
        if (containsEntry(VoidProgressionScreen.VOID_ENTRIES, ELEGY_BOW_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.build(
                ELEGY_BOW_ENTRY, REMEMBRANCE_BOW_X, REMEMBRANCE_BOW_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.ELEGY_BOW)
                .setStyle(BookWidgetStyle.DARK_SOULWOOD));
        builder.addPage(new HeadlineTextItemPage(
                ELEGY_BOW_ENTRY,
                ELEGY_BOW_ENTRY + ".1",
                MaledictItems.ELEGY_BOW.get()));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.ELEGY_BOW.get()));
        builder.afterUmbralCrystal();

        VoidProgressionScreen.VOID_ENTRIES.add(builder.build());
    }

    private static void addObelisksEntry() {
        if (containsEntry(VoidProgressionScreen.VOID_ENTRIES, OBELISKS_ENTRY)){
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
        if (containsEntry(VoidProgressionScreen.VOID_ENTRIES, INCURSUS_BLADE_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.build(INCURSUS_BLADE_ENTRY, 6, 11);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.INCURSUS_BLADE)
                .setStyle(BookWidgetStyle.SOULWOOD));
        // HeadlineTextItemPage 的正文从 y+75 起排、只放得下约十行，故这段文案拆成四页（同 Malum 的 Malignant Pewter）。
        builder.addPage(new HeadlineTextItemPage(
                INCURSUS_BLADE_ENTRY,
                INCURSUS_BLADE_ENTRY + ".1",
                MaledictItems.INCURSUS_BLADE.get()));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.INCURSUS_BLADE.get()));
        builder.addPage(new TextPage(INCURSUS_BLADE_ENTRY + ".2"));
        builder.addPage(new TextPage(INCURSUS_BLADE_ENTRY + ".3"));
        builder.addPage(new TextPage(INCURSUS_BLADE_ENTRY + ".4"));
        builder.afterUmbralCrystal();

        VoidProgressionScreen.VOID_ENTRIES.add(builder.build());
    }

    private static boolean containsEntry(Iterable<? extends BookEntry> entries, String identifier) {
        return findEntry(entries, identifier) != null;
    }

    private static <T extends BookEntry> T findEntry(Iterable<T> entries, String identifier) {
        for (T entry : entries) {
            if (identifier.equals(entry.identifier)){
                return entry;
            }
        }
        return null;
    }

    private MaledictCodexEntries() {
    }
}
