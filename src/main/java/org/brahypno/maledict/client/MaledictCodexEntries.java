package org.brahypno.maledict.client;

import com.sammy.malum.client.screen.codex.BookEntry;
import com.sammy.malum.client.screen.codex.BookWidgetStyle;
import com.sammy.malum.client.screen.codex.PlacedBookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntryBuilder;
import com.sammy.malum.client.screen.codex.objects.progression.IconObject;
import com.sammy.malum.client.screen.codex.objects.progression.RiteEntryObject;
import com.sammy.malum.client.screen.codex.pages.EntryReference;
import com.sammy.malum.client.screen.codex.pages.EntrySelectorPage;
import com.sammy.malum.client.screen.codex.pages.recipe.RuneworkingPage;
import com.sammy.malum.client.screen.codex.pages.recipe.SpiritInfusionPage;
import com.sammy.malum.client.screen.codex.pages.recipe.SpiritRiteRecipePage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextItemPage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextPage;
import com.sammy.malum.client.screen.codex.pages.text.SpiritRiteTextPage;
import com.sammy.malum.client.screen.codex.pages.text.TextPage;
import com.sammy.malum.client.screen.codex.screens.ArcanaProgressionScreen;
import com.sammy.malum.client.screen.codex.screens.VoidProgressionScreen;
import com.sammy.malum.common.events.SetupMalumCodexEntriesEvent;
import com.sammy.malum.common.spiritrite.TotemicRiteType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.common.rite.SacrificeRite;
import org.brahypno.maledict.common.rite.SacrificeRiteType;
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

    /**
     * 牺牲仪式的键名：条目用 {@code maledict.corrupt_sacrifice_rite}——{@code corrupt} 是 Malum 的灵魂木
     * 惯例，配方页与地图图标靠它判材质；页面正文用短名 {@code corrupt_sacrifice_rite}，首页与仪式页共用，
     * {@code .2} 给最后一页。**只动键名，键值（各语言文案）一个字不改。**
     * 仪式只在灵魂木上兑现（符文木那一半是空效果，见 {@code SacrificeRiteType}）。
     */
    private static final String SACRIFICE_RITE_ENTRY = "maledict.corrupt_sacrifice_rite";
    private static final String SACRIFICE_RITE_PAGE = "corrupt_sacrifice_rite";
    private static final String TOTEMIC_RUNES_CONTINUED_ENTRY = "maledict.totemic_runes_continued";
    private static final String RUNE_OF_SATIATION_ENTRY = "maledict.rune_of_satiation";
    private static final String RUNE_OF_DECAY_ENTRY = "maledict.rune_of_decay";
    private static final String RUNE_OF_THE_PACK_ENTRY = "maledict.rune_of_the_pack";
    private static final String RUNE_OF_RIPENING_ENTRY = "maledict.rune_of_ripening";
    private static final String VOID_RUNEWORKING_ENTRY = "void.maledict.runeworking";

    private static final String RUNE_OF_STAGNANT_EVOLUTION_ENTRY = "void.maledict.rune_of_stagnant_evolution";
    private static final String RUNE_OF_ROTTEN_BONE_ENTRY = "void.maledict.rune_of_rotten_bone";
    private static final String UMBRAL_EXPERIMENT_ENTRY = "void.maledict.umbral_experiment";
    private static final String RUNE_OF_MELANCHOLIA_ENTRY = "void.maledict.rune_of_melancholia";
    private static final String RUNE_OF_BLISS_ENTRY = "void.maledict.rune_of_bliss";
    private static final String RUNE_OF_THE_FALLEN_ENTRY = "void.maledict.rune_of_the_fallen";

    /**
     * 接在「虚空符文工艺：拾遗」(6,10) 与「神侵恶刃」(6,11) 下面，凑成 x=6 的一列。
     *
     * <p>坐标是硬碰硬的：{@code SetupMalumCodexEntriesEvent} 只负责让你往 {@code VOID_ENTRIES} 里
     * 塞一个带 x/y 的条目，屏幕照着坐标画，**没有任何重叠判断**。虚空页上 Malum 自己占了 25 个
     * 坐标（(4,10) 是 weight_of_worlds、(5,10) 是 edge_of_deliverance、(3,9) 是 malignant_pewter…），
     * 挪这一页之前先反汇编一遍 {@code VoidProgressionScreen} 把占用表列出来。
     */
    private static final int UMBRAL_EXPERIMENT_X = 6;
    private static final int UMBRAL_EXPERIMENT_Y = 12;

    /**
     * Malum 那枚幽影碎片图标，「研究：精魂晶体」与「幽影奥术能量」章节共用的就是它。
     */
    private static final ResourceLocation UMBRAL_SHARD_ICON =
            ResourceLocation.fromNamespaceAndPath("malum", "textures/gui/book/icons/umbral_shard.png");

    private static final int TOTEMIC_RUNES_CONTINUED_X = 4;
    private static final int TOTEMIC_RUNES_CONTINUED_Y = 15;

    /**
     * 避开 Malum 符文条目占着的 (-15..-12, 7..10)：三枚竖排在左列 (-15, 7..9)，熟成落在 (-12, 8)。
     */
    private static final int RUNE_COLUMN_X = -15;
    private static final int RUNE_OF_DECAY_Y = 7;
    private static final int RUNE_OF_SATIATION_Y = 8;
    private static final int RUNE_OF_THE_PACK_Y = 9;
    private static final int RUNE_OF_RIPENING_X = -12;
    private static final int RUNE_OF_RIPENING_Y = 8;

    private static final int VOID_RUNEWORKING_X = 6;
    private static final int VOID_RUNEWORKING_Y = 10;

    /**
     * 牺牲仪式在**虚空之书**（点奥术全典里的「虚空通史」进去的那一本）里的位置：(0,-10)，也就是
     * 「灵魂通史」(0,0) 同一列往下第十格，属于这一页比较靠下的空地。
     *
     * <p>坐标是硬碰硬的：{@code EntryObjectHandler} 按 {@code top - y * 40} 摆位置，**y 越大越靠上**，
     * 往下走就是 y 更小。本模组的其它虚空条目都在 x=6~8 那一带，x=0 这条竖列只有 Malum 自己占到 y=0。
     */
    private static final int SACRIFICE_RITE_X = 0;
    private static final int SACRIFICE_RITE_Y = -10;

    @SubscribeEvent
    public static void setupEntries(SetupMalumCodexEntriesEvent event) {
        addRemembranceBowEntry();
        addElegyBowEntry();
        addObelisksEntry();
        addIncursusBladeEntry();
        addVicissitudeRiteEntry();
        addSacrificeRiteEntry();
        addVoidRuneworkingEntry();
        addUmbralExperimentEntry();

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
        EntryReference fallen = voidRuneEntry(
                RUNE_OF_THE_FALLEN_ENTRY, MaledictItems.RUNE_OF_THE_FALLEN);

        if (containsEntry(VoidProgressionScreen.VOID_ENTRIES, VOID_RUNEWORKING_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.build(VOID_RUNEWORKING_ENTRY, VOID_RUNEWORKING_X, VOID_RUNEWORKING_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.MALIGNANT_PEWTER_TABLET)
                .setStyle(BookWidgetStyle.SOULWOOD));
        builder.addPage(new HeadlineTextItemPage(
                VOID_RUNEWORKING_ENTRY,
                VOID_RUNEWORKING_ENTRY + ".1",
                MaledictItems.MALIGNANT_PEWTER_TABLET.get()));
        // 续页：英文十六行，首页放不下，故与中文同一处分段。
        builder.addPage(new TextPage(VOID_RUNEWORKING_ENTRY + ".2"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.MALIGNANT_PEWTER_TABLET.get()));
        builder.addPage(new EntrySelectorPage(List.of(stagnantEvolution, rottenBone, fallen)));
        builder.afterUmbralCrystal();

        VoidProgressionScreen.VOID_ENTRIES.add(builder.build());
    }

    /**
     * 「幽影精魂的实验」：幽影精魂没有对应的仪式，所以这一页不讲仪式，只讲拿已经验证过的符板
     * 做自由创作；两枚成品（抑郁、无忧）用同一个选择页挂上去，页内各自「标题正文 + 符文工艺配方」，
     * 与抑郁符文当初的写法一模一样。
     */
    private static void addUmbralExperimentEntry() {
        EntryReference melancholia = voidRuneEntry(RUNE_OF_MELANCHOLIA_ENTRY, MaledictItems.RUNE_OF_MELANCHOLIA);
        EntryReference bliss = voidRuneEntry(RUNE_OF_BLISS_ENTRY, MaledictItems.RUNE_OF_BLISS);

        if (containsEntry(VoidProgressionScreen.VOID_ENTRIES, UMBRAL_EXPERIMENT_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.build(
                UMBRAL_EXPERIMENT_ENTRY, UMBRAL_EXPERIMENT_X, UMBRAL_EXPERIMENT_Y);
        // 纹理图标只能换掉部件供应商（ProgressionEntryObject 的 setIcon 只收物品），
        // 这里用 Malum 自己那枚幽影碎片，「研究：精魂晶体」与「幽影奥术能量」章节用的就是它。
        builder.setWidgetSupplier((entry, x, y) -> new IconObject(entry, x, y, UMBRAL_SHARD_ICON));
        builder.configureWidget(widget -> widget.setStyle(BookWidgetStyle.DARK_SOULWOOD));
        builder.addPage(new HeadlineTextPage(
                UMBRAL_EXPERIMENT_ENTRY,
                UMBRAL_EXPERIMENT_ENTRY + ".1"));
        builder.addPage(new EntrySelectorPage(List.of(melancholia, bliss)));
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
                .setIcon(MaledictItems.AGE_OF_ENLIGHTENMENT)
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
     * 牺牲仪式：**虚空之书**里「灵魂通史」(0,0) 同一列往下第十格那一页。图标一律用仪式图标
     * （{@link RiteEntryObject} 画在地图节点上、{@code SpiritRiteTextPage} 画在书页中央），
     * 与 Malum 自己的仪式条目一致——本模组的 {@code SacrificeRiteType#getIcon()} 借的是狱火脉动图。
     * 符文木那一半什么都不做（见 {@code SacrificeRiteType}），所以配方页钉死成灵魂木那一套辉光；
     * 文案全部在语言文件里，键名见 {@link #SACRIFICE_RITE_ENTRY}。
     *
     * <p>这里有意不挂 {@code afterUmbralCrystal()}：本模组其它虚空条目都挂了，但这一条不加门槛，
     * 免得玩家在书里又找不到它。
     */
    private static void addSacrificeRiteEntry() {
        SacrificeRiteType rite = SacrificeRite.rite();
        if (rite == null || containsEntry(VoidProgressionScreen.VOID_ENTRIES, SACRIFICE_RITE_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.build(
                SACRIFICE_RITE_ENTRY, SACRIFICE_RITE_X, SACRIFICE_RITE_Y);
        // 不给 setIcon：RiteEntryObject 自己会把仪式图标画在节点上，再塞一个物品图标会叠在一起。
        builder.setWidgetSupplier(RiteEntryObject::new);
        builder.configureWidget(widget -> widget.setStyle(BookWidgetStyle.DARK_TOTEMIC_SOULWOOD));
        // 仪式页：正文取问句，中央是发光的仪式图标；鼠标停在图标上会出「属极：灵魂木」「效果：…」，
        // 那条效果文案就是语言文件里的 ...corrupt_sacrifice_rite.hover。
        builder.addPage(new SpiritRiteTextPage(rite, SACRIFICE_RITE_PAGE));
        builder.addPage(new TextPage(SACRIFICE_RITE_PAGE + ".2"));
        builder.addPage(new SoulwoodSpiritRiteRecipePage(rite));

        VoidProgressionScreen.VOID_ENTRIES.add(builder.build());
    }

    /**
     * 灵魂木版配方页：Malum 的 {@code SpiritRiteRecipePage} 看的是**条目 identifier** 里有没有 "corrupt"，
     * 而 identifier 是键名、随时可能被改；这里干脆钉死成灵魂木那一套辉光，与本仪式只在灵魂木上兑现一致。
     */
    private static final class SoulwoodSpiritRiteRecipePage extends SpiritRiteRecipePage {
        private SoulwoodSpiritRiteRecipePage(TotemicRiteType rite) {
            super(rite);
        }

        @Override
        public boolean isCorrupted() {
            return true;
        }
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
        // 续页：英文正文的行数差不多是中文的两倍，首页的九行只够放下第一句。
        builder.addPage(new TextPage(TOTEMIC_RUNES_CONTINUED_ENTRY + ".2"));
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
        // 正文分两页：英文比中文长一倍（同样的意思，英文就是占更宽），一页放不下。
        builder.addPage(new TextPage(REMEMBRANCE_BOW_ENTRY + ".2"));
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
                .setStyle(BookWidgetStyle.SOULWOOD));
        builder.addPage(new HeadlineTextItemPage(
                ELEGY_BOW_ENTRY,
                ELEGY_BOW_ENTRY + ".1",
                MaledictItems.ELEGY_BOW.get()));
        // 「我走不出来。」的重复分成七次与四次：十一行英文独白塞不进首页的九行。
        builder.addPage(new TextPage(ELEGY_BOW_ENTRY + ".2"));
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
