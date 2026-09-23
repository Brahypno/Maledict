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

    private static final int TOTEMIC_RUNES_CONTINUED_X = 4;
    private static final int TOTEMIC_RUNES_CONTINUED_Y = 15;

    /**
     * 四枚图腾符文在书上的落点：Malum 的符文条目占着 (-15..-12, 7..10) 那一片，
     * 空着的只有左边一列 (-15, 7..9) 和右边三格 (-12, 8..10)。
     * 于是左边一列竖着摆三枚（衰朽、饱食、兽群），第四枚熟成符文落在右边的 (-12, 8)，
     * 与 Malum 的符文排成一行。
     */
    private static final int RUNE_COLUMN_X = -15;
    private static final int RUNE_OF_DECAY_Y = 7;
    private static final int RUNE_OF_SATIATION_Y = 8;
    private static final int RUNE_OF_THE_PACK_Y = 9;
    private static final int RUNE_OF_RIPENING_X = -12;
    private static final int RUNE_OF_RIPENING_Y = 8;

    /**
     * 「虚空符文工艺：拾遗」的落点：(6, 10)，神侵恶刃条目 (6, 11) 的正下方。
     */
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

    /**
     * 虚空线的符文工艺：正文一页，接符板的精魂灌注配方，再接符文的符文工艺配方。
     */
    private static void addVoidRuneworkingEntry() {
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
        builder.addPage(RuneworkingPage.fromOutput(MaledictItems.RUNE_OF_STAGNANT_EVOLUTION.get()));
        builder.afterUmbralCrystal();

        VoidProgressionScreen.VOID_ENTRIES.add(builder.build());
    }

    /**
     * The four Vicissitude Rites share one entry: the recipe is the price and the tier, from three
     * arcane spirits all the way to two eldritch under three arcane. The rite types live in Malum's
     * table, so the entry is skipped rather than faked if that table is not ready.
     */
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

    /**
     * 一枚图腾符文自己的条目：正文一页，符文工艺配方一页——与 Malum 每枚符文条目的排法一致
     * （那边也是 {@code HeadlineTextPage} 接 {@code RuneworkingPage.fromOutput}）。
     *
     * <p>它们要先建出来，因为「图腾符文：续」的图标页要指过来：{@link EntryReference} 收的是真的
     * {@link BookEntry}，不是标识符字符串，所以这里把建好的条目交出去，而不是两边各 build 一份。
     *
     * <p>框架颜色跟着符板走：符文木的符文用 {@code RUNEWOOD}，灵魂木的用 {@code SOULWOOD}。
     */
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

    /**
     * 「图腾符文：续」：Malum 的图腾符文条目讲的是四种基础元素的仪式能刻上符板，
     * 这一条接着讲后来发现别的灵气仪式也刻得上去——只是脉动更单纯，效果与完整仪式有别。
     *
     * <p>它落在图腾符文的正对面（见 {@link #TOTEMIC_RUNES_CONTINUED_X}），读的是同一本书，
     * 所以 Malum 那边条文还在，这条就跟着它一起出现，不用另开章节。
     *
     * <p>最后一页照抄 Malum 图腾符文条目的收尾：{@link EntrySelectorPage} 摆出符文图标，
     * 点哪个进哪个条目看合成——现在摆的是我们刻出来的四枚：饱食、衰朽、兽群、熟成。
     */
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
        // An item headline page starts its body at y+75 instead of y+25 and only fits about ten
        // wrapped lines, so this entry's copy is split over four pages the way Malum splits
        // Malignant Pewter's.
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

    /**
     * 表里已有的同名条目；没有就是 {@code null}。
     *
     * <p>泛型跟着表走：图标页要引用真的条目，而 {@code ArcanaProgressionScreen.ENTRIES} 装的是
     * {@link PlacedBookEntry}，签名写成 {@code BookEntry} 的话拿回来还得再强转一次。
     */
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
