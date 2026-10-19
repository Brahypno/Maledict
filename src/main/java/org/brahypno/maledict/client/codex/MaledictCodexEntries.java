package org.brahypno.maledict.client.codex;

import com.sammy.malum.client.screen.codex.BookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntryBuilder;
import com.sammy.malum.client.screen.codex.WidgetDesignType;
import com.sammy.malum.client.screen.codex.objects.progression.IconObject;
import com.sammy.malum.client.screen.codex.objects.progression.RiteEntryObject;
import com.sammy.malum.client.screen.codex.pages.EntryReference;
import com.sammy.malum.client.screen.codex.pages.recipe.RuneworkingPage;
import com.sammy.malum.client.screen.codex.pages.recipe.SpiritInfusionPage;
import com.sammy.malum.client.screen.codex.pages.recipe.SpiritRiteRecipePage;
import com.sammy.malum.client.screen.codex.pages.recipe.vanilla.CraftingPage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextItemPage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextPage;
import com.sammy.malum.client.screen.codex.pages.text.SpiritRiteTextPage;
import com.sammy.malum.client.screen.codex.pages.text.TextPage;
import com.sammy.malum.client.screen.codex.screens.progression.ArcanaProgressionScreen;
import com.sammy.malum.client.screen.codex.screens.progression.VoidProgressionScreen;
import com.sammy.malum.core.systems.events.SetupMalumCodexEntriesEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.common.item.SpiritArrowType;
import org.brahypno.maledict.common.rite.SacrificeRite;
import org.brahypno.maledict.common.rite.SacrificeRiteType;
import org.brahypno.maledict.common.rite.SummoningRite;
import org.brahypno.maledict.common.rite.VicissitudeRiteType;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.registry.MaledictRites;

import java.util.Arrays;
import java.util.List;

@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class MaledictCodexEntries {
    private static final String SPIRIT_ARROWS_ENTRY = "maledict.spirit_arrows";
    // 思念弓 (2,13) 正下方；1.8.2 的 aqueous_rites 已占用原位置 (2,14)。
    private static final int SPIRIT_ARROWS_X = 6;
    private static final int SPIRIT_ARROWS_Y = 12;
    private static final String REMEMBRANCE_BOW_ENTRY = "maledict.remembrance_bow";
    private static final String ELEGY_BOW_ENTRY = "void.maledict.elegy_bow";
    private static final int REMEMBRANCE_BOW_X = 6;
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
    private static final String BLEEDING_EYE_ENTRY = "void.maledict.bleeding_eye";
    private static final String HANGMAN_FOOL_PAGE = BLEEDING_EYE_ENTRY + ".hangman_fool";
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
     * <p>坐标是硬碰硬的：{@code SetupMalumCodexEntriesEvent} 让你往当前屏幕的 {@code getEntries()} 里
     * 塞一个带 x/y 的条目，屏幕照着坐标画，**没有任何重叠判断**。1.8.2 虚空页上 Malum 自己占了 29 个
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

    // 四枚符文只在汇总条目内部列出；地图节点使用原饱足符文的位置。
    private static final int TOTEMIC_RUNES_CONTINUED_X = -17;
    private static final int TOTEMIC_RUNES_CONTINUED_Y = 8;

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
    // EntryObjectHandler 按 top - y * 40 排布，下方十格必须减去 10。
    private static final int BLEEDING_EYE_X = SACRIFICE_RITE_X;
    private static final int BLEEDING_EYE_Y = SACRIFICE_RITE_Y - 10;

    @SubscribeEvent
    public static void setupEntries(SetupMalumCodexEntriesEvent event) {
        var entries = new MaledictCodexEntries(event.getScreen().getEntries());
        if (event.getScreen() instanceof ArcanaProgressionScreen)
            entries.setupArcana();
        else if (event.getScreen() instanceof VoidProgressionScreen)
            entries.setupVoid();
    }

    private void setupVoid() {
        addElegyBowEntry();
        addObelisksEntry();
        addIncursusBladeEntry();
        addVicissitudeRiteEntry();
        addSacrificeRiteEntry();
        addBleedingEyeEntry();
        addVoidRuneworkingEntry();
        addUmbralExperimentEntry();
    }

    private void setupArcana() {
        addSpiritArrowsEntry();
        addRemembranceBowEntry();

        addTotemicRunesContinuedEntry(List.of(
                runeEntry(RUNE_OF_SATIATION_ENTRY, MaledictItems.RUNE_OF_SATIATION),
                runeEntry(RUNE_OF_DECAY_ENTRY, MaledictItems.RUNE_OF_DECAY),
                runeEntry(RUNE_OF_THE_PACK_ENTRY, MaledictItems.RUNE_OF_THE_PACK),
                runeEntry(RUNE_OF_RIPENING_ENTRY, MaledictItems.RUNE_OF_RIPENING)));
    }

    private void addSpiritArrowsEntry() {
        if (containsEntry(entries, SPIRIT_ARROWS_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.create(SPIRIT_ARROWS_ENTRY, SPIRIT_ARROWS_X, SPIRIT_ARROWS_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.ELDRITCH_SPIRIT_ARROW)
                .setDesign(WidgetDesignType.DEFAULT.createDesign(WidgetDesignType.FrameType.RUNEWOOD, WidgetDesignType.FillingType.PAPER)));
        var arrows = Arrays.stream(SpiritArrowType.values()).map(this::spiritArrowEntry).toList();
        builder.addPage(new ReadableTextPage(SPIRIT_ARROWS_ENTRY, List.of(SPIRIT_ARROWS_ENTRY + ".1"),
                                             arrows.stream().map(reference -> reference.icon).toList()));
        builder.addPage(new EntryIndexPage("maledict.codex.arrow_index", arrows));
        entries.add(builder.build());
    }

    private EntryReference spiritArrowEntry(SpiritArrowType type) {
        DeferredHolder<Item, Item> arrow = MaledictItems.getSpiritArrow(type);
        String identifier = "maledict." + arrow.getId().getPath();
        return new EntryReference(arrow, BookEntry.create(identifier)
                                                  .addPage(new ReadableTextPage(identifier, identifier + ".1"))
                                                  .addPage(new CraftingPage(arrow.get(),
                                                                            Items.AIR, type.getSpiritType().getSpiritShard(), Items.AIR,
                                                                            Items.AIR, Items.STICK, Items.AIR,
                                                                            Items.AIR, Items.FEATHER, Items.AIR)));
    }

    private void addVoidRuneworkingEntry() {
        EntryReference stagnantEvolution = runeEntry(
                RUNE_OF_STAGNANT_EVOLUTION_ENTRY, MaledictItems.RUNE_OF_STAGNANT_EVOLUTION);
        EntryReference rottenBone = runeEntry(
                RUNE_OF_ROTTEN_BONE_ENTRY, MaledictItems.RUNE_OF_ROTTEN_BONE);
        EntryReference fallen = runeEntry(
                RUNE_OF_THE_FALLEN_ENTRY, MaledictItems.RUNE_OF_THE_FALLEN);

        if (containsEntry(entries, VOID_RUNEWORKING_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.create(VOID_RUNEWORKING_ENTRY, VOID_RUNEWORKING_X, VOID_RUNEWORKING_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.MALIGNANT_PEWTER_TABLET)
                .setDesign(WidgetDesignType.DEFAULT.createDesign(WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.PAPER)));
        builder.addPage(new HeadlineTextItemPage(
                VOID_RUNEWORKING_ENTRY,
                VOID_RUNEWORKING_ENTRY + ".1",
                MaledictItems.MALIGNANT_PEWTER_TABLET.get()));
        // 续页：英文十六行，首页放不下，故与中文同一处分段。
        builder.addPage(new TextPage(VOID_RUNEWORKING_ENTRY + ".2"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.MALIGNANT_PEWTER_TABLET.get()));
        List.of(stagnantEvolution, rottenBone, fallen).forEach(builder::addReference);
        builder.afterUmbralCrystal();

        entries.add(builder.build());
    }

    /**
     * 「幽影精魂的实验」：幽影精魂没有对应的仪式，所以这一页不讲仪式，只讲拿已经验证过的符板
     * 做自由创作；两枚成品（抑郁、无忧）用同一个选择页挂上去，页内各自「标题正文 + 符文工艺配方」，
     * 与抑郁符文当初的写法一模一样。
     */
    private void addUmbralExperimentEntry() {
        EntryReference melancholia = runeEntry(RUNE_OF_MELANCHOLIA_ENTRY, MaledictItems.RUNE_OF_MELANCHOLIA);
        EntryReference bliss = runeEntry(RUNE_OF_BLISS_ENTRY, MaledictItems.RUNE_OF_BLISS);

        if (containsEntry(entries, UMBRAL_EXPERIMENT_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.create(
                UMBRAL_EXPERIMENT_ENTRY, UMBRAL_EXPERIMENT_X, UMBRAL_EXPERIMENT_Y);
        // 纹理图标只能换掉部件供应商（ProgressionEntryObject 的 setIcon 只收物品），
        // 这里用 Malum 自己那枚幽影碎片，「研究：精魂晶体」与「幽影奥术能量」章节用的就是它。
        builder.setWidgetSupplier((entry, x, y) -> new IconObject(entry, x, y, UMBRAL_SHARD_ICON));
        builder.configureWidget(
                widget -> widget.setDesign(WidgetDesignType.DEFAULT.createDesign(WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.DARK)));
        builder.addPage(new HeadlineTextPage(
                UMBRAL_EXPERIMENT_ENTRY,
                UMBRAL_EXPERIMENT_ENTRY + ".1"));
        List.of(melancholia, bliss).forEach(builder::addReference);
        builder.afterUmbralCrystal();

        entries.add(builder.build());
    }

    private void addVicissitudeRiteEntry() {
        VicissitudeRiteType simple = SummoningRite.rite(BossDifficulty.SIMPLE);
        VicissitudeRiteType difficult = SummoningRite.rite(BossDifficulty.DIFFICULT);
        VicissitudeRiteType complete = SummoningRite.rite(BossDifficulty.COMPLETE);
        VicissitudeRiteType extreme = SummoningRite.rite(BossDifficulty.EXTREME);
        if (simple == null || difficult == null || complete == null || extreme == null
            || containsEntry(entries, RITE_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.create(RITE_ENTRY, 8, 13);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.AGE_OF_ENLIGHTENMENT)
                .setDesign(WidgetDesignType.DEFAULT.createDesign(WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.DARK)));
        builder.addPage(new HeadlineTextPage(RITE_ENTRY, RITE_ENTRY + ".1"));
        builder.addPage(new SpiritRiteRecipePage(MaledictRites.holder(simple)));
        builder.addPage(new SpiritRiteRecipePage(MaledictRites.holder(difficult)));
        builder.addPage(new TextPage(RITE_ENTRY + ".2"));
        builder.addPage(new SpiritRiteRecipePage(MaledictRites.holder(complete)));
        builder.addPage(new SpiritRiteRecipePage(MaledictRites.holder(extreme)));
        builder.afterUmbralCrystal();

        entries.add(builder.build());
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
    private void addSacrificeRiteEntry() {
        SacrificeRiteType rite = SacrificeRite.rite();
        if (rite == null || containsEntry(entries, SACRIFICE_RITE_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.create(
                SACRIFICE_RITE_ENTRY, SACRIFICE_RITE_X, SACRIFICE_RITE_Y);
        // 不给 setIcon：RiteEntryObject 自己会把仪式图标画在节点上，再塞一个物品图标会叠在一起。
        builder.setWidgetSupplier(RiteEntryObject::new);
        builder.configureWidget(
                widget -> widget.setDesign(WidgetDesignType.TOTEMIC.createDesign(WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.DARK)));
        // 仪式页：正文取问句，中央是发光的仪式图标；鼠标停在图标上会出「属极：灵魂木」「效果：…」，
        // 那条效果文案就是语言文件里的 ...corrupt_sacrifice_rite.hover。
        builder.addPage(new SpiritRiteTextPage(MaledictRites.holder(rite)));
        builder.addPage(new TextPage(SACRIFICE_RITE_PAGE + ".2"));
        builder.addPage(new SpiritRiteRecipePage(MaledictRites.holder(rite)));

        entries.add(builder.build());
    }

    private void addBleedingEyeEntry() {
        if (containsEntry(entries, BLEEDING_EYE_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.create(BLEEDING_EYE_ENTRY, BLEEDING_EYE_X, BLEEDING_EYE_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.BLEEDING_EYE)
                .setDesign(WidgetDesignType.DEFAULT.createDesign(WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.DARK)));

        builder.addPage(new HeadlineTextItemPage(
                BLEEDING_EYE_ENTRY, BLEEDING_EYE_ENTRY + ".1", MaledictItems.BLEEDING_EYE.get()));
        builder.addPage(new TextPage(BLEEDING_EYE_ENTRY + ".2"));
        builder.addPage(new HeadlineTextItemPage(
                HANGMAN_FOOL_PAGE, HANGMAN_FOOL_PAGE + ".1", MaledictItems.HANGMAN_FOOL.get()));
        builder.addPage(new TextPage(HANGMAN_FOOL_PAGE + ".2"));
        // 0、2 阶段共用物品 ID，按配方 ID 选择；1 阶段的井配方仍保持隐藏。
        builder.addPage(SpiritInfusionPage.fromId(ResourceLocation.fromNamespaceAndPath(
                Maledict.MODID, "hangman_fool")));
        builder.addPage(SpiritInfusionPage.fromId(ResourceLocation.fromNamespaceAndPath(
                Maledict.MODID, "hangman_fool_stage_2")));

        entries.add(builder.build());
    }

    private EntryReference runeEntry(String identifier, DeferredHolder<Item, Item> rune) {
        return new EntryReference(rune, BookEntry.create(identifier)
                                                 .addPage(new ReadableTextPage(identifier, identifier + ".1"))
                                                 .addPage(RuneworkingPage.fromOutput(rune.get())));
    }

    private void addTotemicRunesContinuedEntry(List<EntryReference> runes) {
        if (containsEntry(entries, TOTEMIC_RUNES_CONTINUED_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.create(
                TOTEMIC_RUNES_CONTINUED_ENTRY, TOTEMIC_RUNES_CONTINUED_X, TOTEMIC_RUNES_CONTINUED_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.RUNE_OF_SATIATION)
                .setDesign(WidgetDesignType.DEFAULT.createDesign(WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.PAPER)));
        builder.addPage(new ReadableTextPage(TOTEMIC_RUNES_CONTINUED_ENTRY,
                                             List.of(TOTEMIC_RUNES_CONTINUED_ENTRY + ".1", TOTEMIC_RUNES_CONTINUED_ENTRY + ".2"), List.of()));
        builder.addPage(new EntryIndexPage("maledict.codex.rune_index", runes));

        entries.add(builder.build());
    }

    private void addRemembranceBowEntry() {
        if (containsEntry(entries, REMEMBRANCE_BOW_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.create(
                REMEMBRANCE_BOW_ENTRY, REMEMBRANCE_BOW_X, REMEMBRANCE_BOW_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.REMEMBRANCE_BOW)
                .setDesign(WidgetDesignType.DEFAULT.createDesign(WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.PAPER)));
        builder.addPage(new HeadlineTextItemPage(
                REMEMBRANCE_BOW_ENTRY,
                REMEMBRANCE_BOW_ENTRY + ".1",
                MaledictItems.REMEMBRANCE_BOW.get()));
        // 正文分两页：英文比中文长一倍（同样的意思，英文就是占更宽），一页放不下。
        builder.addPage(new TextPage(REMEMBRANCE_BOW_ENTRY + ".2"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.REMEMBRANCE_BOW.get()));

        entries.add(builder.build());
    }

    private void addElegyBowEntry() {
        if (containsEntry(entries, ELEGY_BOW_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.create(
                ELEGY_BOW_ENTRY, REMEMBRANCE_BOW_X, REMEMBRANCE_BOW_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.ELEGY_BOW)
                .setDesign(WidgetDesignType.DEFAULT.createDesign(WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.PAPER)));
        builder.addPage(new HeadlineTextItemPage(
                ELEGY_BOW_ENTRY,
                ELEGY_BOW_ENTRY + ".1",
                MaledictItems.ELEGY_BOW.get()));
        // 「我走不出来。」的重复分成七次与四次：十一行英文独白塞不进首页的九行。
        builder.addPage(new TextPage(ELEGY_BOW_ENTRY + ".2"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.ELEGY_BOW.get()));
        builder.afterUmbralCrystal();

        entries.add(builder.build());
    }

    private void addObelisksEntry() {
        if (containsEntry(entries, OBELISKS_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.create(OBELISKS_ENTRY, -1, 8);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.SOULWOOD_OBELISK)
                .setDesign(WidgetDesignType.DEFAULT.createDesign(WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.PAPER)));
        builder.addPage(new HeadlineTextPage(
                SOULWOOD_OBELISK_PAGE,
                SOULWOOD_OBELISK_PAGE + ".1"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.SOULWOOD_OBELISK.get()));
        builder.addPage(new HeadlineTextPage(
                MNEMONIC_OBELISK_PAGE,
                MNEMONIC_OBELISK_PAGE + ".1"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.MNEMONIC_OBELISK.get()));

        entries.add(builder.build());
    }

    private void addIncursusBladeEntry() {
        if (containsEntry(entries, INCURSUS_BLADE_ENTRY)){
            return;
        }

        PlacedBookEntryBuilder builder = BookEntry.create(INCURSUS_BLADE_ENTRY, 6, 11);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.INCURSUS_BLADE)
                .setDesign(WidgetDesignType.DEFAULT.createDesign(WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.PAPER)));
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

        entries.add(builder.build());
    }

    private boolean containsEntry(Iterable<? extends BookEntry> entries, String identifier) {
        return findEntry(entries, identifier) != null;
    }

    private <T extends BookEntry> T findEntry(Iterable<T> entries, String identifier) {
        for (T entry : entries) {
            if (identifier.equals(entry.identifier)){
                return entry;
            }
        }
        return null;
    }

    private final List<PlacedBookEntry> entries;

    private MaledictCodexEntries(List<PlacedBookEntry> entries) {
        this.entries = entries;
    }
}
