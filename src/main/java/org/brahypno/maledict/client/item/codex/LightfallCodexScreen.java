package org.brahypno.maledict.client.item.codex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sammy.malum.MalumMod;
import com.sammy.malum.client.screen.codex.BookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntryBuilder;
import com.sammy.malum.client.screen.codex.WidgetDesign;
import com.sammy.malum.client.screen.codex.WidgetDesignType;
import com.sammy.malum.client.screen.codex.objects.progression.ProgressionEntryObject;
import com.sammy.malum.client.screen.codex.objects.progression.ScreenOpenerObject;
import com.sammy.malum.client.screen.codex.pages.CyclingPage;
import com.sammy.malum.client.screen.codex.pages.recipe.SpiritInfusionPage;
import com.sammy.malum.client.screen.codex.pages.recipe.SpiritRiteRecipePage;
import com.sammy.malum.client.screen.codex.pages.recipe.vanilla.SmeltingPage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextItemPage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextPage;
import com.sammy.malum.client.screen.codex.pages.text.SpiritRiteTextPage;
import com.sammy.malum.client.screen.codex.pages.text.TextPage;
import com.sammy.malum.client.screen.codex.screens.progression.AbstractProgressionCodexScreen;
import com.sammy.malum.client.screen.codex.screens.progression.ArcanaProgressionScreen;
import com.sammy.malum.client.screen.codex.screens.progression.ProgressionScreenHolder;
import com.sammy.malum.registry.common.MalumSoundEvents;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.registry.MaledictRites;

import java.awt.Color;
import java.util.function.Consumer;

/**
 * The Lightfall Codex's own page. The Blighted Star entry (the Weeping Wither page) has moved
 * here from the Void book. The two nodes below it return to the Arcana and Void views.
 *
 * <p>与 1.20.1 的两点结构差异（1.8.2 的骨架决定的，不是取舍）：
 * <ul>
 *   <li>条目表就是基类的 {@code entries} 字段，由 {@code setupEntries()} 在基类构造器里填；
 *       1.20.1 是子类自建一个 {@code List} 再从 {@link #getEntries()} 交回去。</li>
 *   <li>「打开时停在哪一条」不再是覆盖 {@code setupObjects()} 去 {@code faceObject(get(0))}
 *       ——基类在 {@code setupObjects()} 之后还会调 {@code faceOrigin()}，那个覆写会被覆盖掉。
 *       1.8.2 的做法是给该条目的控件打 {@code setOrigin()}，据此把腐枯凋灵设为原点。</li>
 * </ul>
 */
public final class LightfallCodexScreen extends AbstractProgressionCodexScreen {
    public static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/gui/book/lightfall_background.png");

    /**
     * 屏幕持有者。构造器只能交给 {@link ProgressionScreenHolder}（它才懒建实例），
     * 不能在静态初始化里直接 {@code new} 本屏幕——那样会在类初始化阶段就构造屏幕。
     */
    static final ProgressionScreenHolder<LightfallCodexScreen> SCREEN =
            new ProgressionScreenHolder<>(LightfallCodexScreen::new, MalumSoundEvents.ARCANA_TRANSITION_NORMAL);

    private static final String WEEPING_WITHER = "void.maledict.weeping_wither";
    private static final String AUFHEBEN_ARMOR_ENTRY = "void.maledict.aufheben_lightward";

    /**
     * 拘魂仪式：**只在学会之后出现**，坐标 (1,8) 在腐枯凋灵 (0,10) 右边一格。
     * 两半仪式都给：灵魂木附加、符文木解除，各配一页配方。
     *
     * <p>1.8.2 里两半是两条独立注册的仪式（{@code corrupted_maledict_soulbinding_rite} /
     * {@code maledict_soulbinding_rite}），1.20.1 那条「一条仪式两套木头」的写法不存在了；
     * 因此也不再需要 {@code SoulwoodSpiritRiteRecipePage}——配方页的辉光改由仪式自己的
     * {@code isCorrupted()} 决定，两条仪式各说各的。
     */
    private static final String SOULBINDING_RITE_ENTRY = "void.maledict.soulbinding_rite";
    private static final int SOULBINDING_RITE_X = 1;
    private static final int SOULBINDING_RITE_Y = 8;

    /**
     * 扬弃逐光盔甲 (4,12)，坐标与 1.20.1 相同。
     */
    private static final int AUFHEBEN_ARMOR_X = 4;
    private static final int AUFHEBEN_ARMOR_Y = 12;

    private static final String LUMENFALL_BISMUTH_ENTRY = "void.maledict.lumenfall_bismuth";
    private static final int LUMENFALL_BISMUTH_X = 2;
    private static final int LUMENFALL_BISMUTH_Y = 3;
    private static final String EUTECTIC_ENTRY = "void.maledict.refracted_lead_bismuth_eutectic";
    private static final int EUTECTIC_X = 3;
    private static final int EUTECTIC_Y = 9;

    private static final String SOUL_BACK = "chronicles_of_the_soul";
    private static final String VOID_BACK = "chronicles_of_the_void";
    private static final ResourceLocation SOUL_ICON =
            MalumMod.malumPath("textures/gui/book/icons/arcana_button.png");
    private static final ResourceLocation VOID_ICON =
            MalumMod.malumPath("textures/gui/book/icons/void_button.png");

    private LightfallCodexScreen() {
        super(MalumSoundEvents.ARCANA_SWEETENER_NORMAL, 1024, 2560);
    }

    public static LightfallCodexScreen getScreenInstance() {
        return SCREEN.getScreen();
    }

    public static void openCodexViaItem() {
        LightfallCodex.rememberView(LightfallCodex.View.LIGHTFALL);
        // false：这一页不是虚空之书，1.20.1 也没给它设 isVoidTouched。
        LightfallCodex.withoutBecomingMalumsMostRecent(() -> SCREEN.openCodexViaItem(false));
    }

    @Override
    public Color getOutlineColor() {
        // 1.20.1 没有描边色的概念（1.8.2 才有），沿用奥秘全典那一支的颜色。
        return ArcanaProgressionScreen.OUTLINE_COLOR;
    }

    @Override
    public void renderBackground(PoseStack poseStack) {
        renderBackground(poseStack, BACKGROUND, 0.2F, 0.4F);
    }

    @Override
    public void setupEntries() {
        // 这里只能碰基类的 entries 字段：此刻子类的实例字段还没初始化。
        entries.add(buildWeepingWither());
        entries.add(buildSoulbindingRite());
        entries.add(buildAufhebenArmor());
        entries.add(buildLumenfallBismuth());
        entries.add(buildEutectic());
        entries.add(buildBack(SOUL_BACK, 0, 1, SOUL_ICON,
                              WidgetDesignType.GRAND.createDesign(
                                      WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.DARK),
                              LightfallArcanaScreen.SCREEN));
        entries.add(buildBack(VOID_BACK, 0, 0, VOID_ICON,
                              WidgetDesignType.GRAND.createDesign(
                                      WidgetDesignType.FrameType.RUNEWOOD, WidgetDesignType.FillingType.DARK),
                              LightfallVoidScreen.SCREEN));
    }

    /**
     * 腐枯凋灵：原来在虚空之书里，搬到本页 (0,10) 并设为原点——打开法典就停在它上面，
     * 下方紧跟着两枚回程节点。1.20.1 用 {@code faceObject(bookObjectHandler.get(0))} 达到同样效果。
     */
    private PlacedBookEntry buildWeepingWither() {
        PlacedBookEntryBuilder builder = BookEntry.create(WEEPING_WITHER, 0, 10);
        builder.configureWidget(soulwood().andThen(ProgressionEntryObject::setOrigin)
                                             .andThen(widget -> widget.setIcon(MaledictItems.BLIGHTED_STAR)));
        builder.addPage(new HeadlineTextPage(WEEPING_WITHER, WEEPING_WITHER + ".1"));
        builder.addPage(new TextPage(WEEPING_WITHER + ".2"));
        builder.addPage(new TextPage(WEEPING_WITHER + ".summoning"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.CRYING_TAINTED_ROCK.get()));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.CRYING_TWISTED_ROCK.get()));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.BLIGHTED_WITHER_SKULL.get()));
        return builder.build();
    }

    private PlacedBookEntry buildSoulbindingRite() {
        PlacedBookEntryBuilder builder = BookEntry.create(
                SOULBINDING_RITE_ENTRY, SOULBINDING_RITE_X, SOULBINDING_RITE_Y);
        builder.configureWidget(soulwood().andThen(widget -> widget.setIcon(MaledictItems.SOULBINDING_SEAL)));
        builder.setEntryCondition(LightfallCodex::knowsSoulbinding);
        builder.addPage(new HeadlineTextPage(SOULBINDING_RITE_ENTRY, SOULBINDING_RITE_ENTRY + ".1"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.SOULBINDING_SEAL.get()));
        // 灵魂木（腐化）半：附加封印。仪式页正文与配方页的辉光都取自这项仪式本身。
        builder.addPage(new SpiritRiteTextPage(
                MaledictRites.holder(MaledictRites.CORRUPTED_SOULBINDING.get())));
        builder.addPage(new SpiritRiteRecipePage(
                MaledictRites.holder(MaledictRites.CORRUPTED_SOULBINDING.get())));
        // 符文木（天然）半：解除封印，精魂与封印都不返还。
        builder.addPage(new SpiritRiteTextPage(
                MaledictRites.holder(MaledictRites.SOULBINDING.get())));
        builder.addPage(new SpiritRiteRecipePage(
                MaledictRites.holder(MaledictRites.SOULBINDING.get())));
        return builder.build();
    }

    /**
     * 扬弃逐光盔甲：四件共用一张翻页配方页，写法与 Malum 的恶念要塞盔甲一致。
     */
    private PlacedBookEntry buildAufhebenArmor() {
        PlacedBookEntryBuilder builder = BookEntry.create(
                AUFHEBEN_ARMOR_ENTRY, AUFHEBEN_ARMOR_X, AUFHEBEN_ARMOR_Y);
        builder.configureWidget(
                soulwood().andThen(widget -> widget.setIcon(MaledictItems.AUFHEBEN_LIGHTWARD_HELMET)));
        builder.addPage(new HeadlineTextItemPage(
                AUFHEBEN_ARMOR_ENTRY,
                AUFHEBEN_ARMOR_ENTRY + ".1",
                MaledictItems.AUFHEBEN_LIGHTWARD_HELMET.get()));
        builder.addPage(new TextPage(AUFHEBEN_ARMOR_ENTRY + ".2"));
        builder.addPage(new TextPage(AUFHEBEN_ARMOR_ENTRY + ".3"));
        builder.addPage(new TextPage(AUFHEBEN_ARMOR_ENTRY + ".4"));
        builder.addPage(new TextPage(AUFHEBEN_ARMOR_ENTRY + ".5"));
        builder.addPage(new CyclingPage(
                SpiritInfusionPage.fromOutput(MaledictItems.AUFHEBEN_LIGHTWARD_HELMET.get()),
                SpiritInfusionPage.fromOutput(MaledictItems.AUFHEBEN_LIGHTWARD_CHESTPLATE.get()),
                SpiritInfusionPage.fromOutput(MaledictItems.AUFHEBEN_LIGHTWARD_LEGGINGS.get()),
                SpiritInfusionPage.fromOutput(MaledictItems.AUFHEBEN_LIGHTWARD_BOOTS.get())));
        return builder.build();
    }

    private PlacedBookEntry buildLumenfallBismuth() {
        PlacedBookEntryBuilder builder = BookEntry.create(
                LUMENFALL_BISMUTH_ENTRY, LUMENFALL_BISMUTH_X, LUMENFALL_BISMUTH_Y);
        builder.configureWidget(
                soulwood().andThen(widget -> widget.setIcon(MaledictItems.LUMENFALL_BISMUTH_INGOT)));
        builder.addPage(new HeadlineTextItemPage(
                LUMENFALL_BISMUTH_ENTRY,
                LUMENFALL_BISMUTH_ENTRY + ".1",
                MaledictItems.LUMENFALL_BISMUTH_INGOT.get()));
        builder.addPage(new TextPage(LUMENFALL_BISMUTH_ENTRY + ".2"));
        builder.addPage(new TextPage(LUMENFALL_BISMUTH_ENTRY + ".3"));
        builder.addPage(new SmeltingPage(
                MaledictItems.LUMENFALL_BISMUTH_ORE.get(),
                MaledictItems.LUMENFALL_BISMUTH_INGOT.get()));
        return builder.build();
    }

    private PlacedBookEntry buildEutectic() {
        PlacedBookEntryBuilder builder = BookEntry.create(EUTECTIC_ENTRY, EUTECTIC_X, EUTECTIC_Y);
        builder.configureWidget(soulwood().andThen(
                widget -> widget.setIcon(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC)));
        builder.addPage(new HeadlineTextPage(EUTECTIC_ENTRY, EUTECTIC_ENTRY + ".1"));
        builder.addPage(new TextPage(EUTECTIC_ENTRY + ".2"));
        builder.addPage(new TextPage(EUTECTIC_ENTRY + ".3"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC.get()));
        return builder.build();
    }

    /**
     * 回程节点：1.8.2 的 {@link ScreenOpenerObject} 收的是目标屏幕的持有者（1.20.1 收 Runnable），
     * 所以这里直接传目标视野的 {@code SCREEN}，跳转一律走它自己的 {@code openCodexViaTransition()}。
     * 跳转会把 Malum 的 {@code mostRecentScreen} 改写成目标持有者，因此都包一层
     * {@link MaledictScreenOpener}，跳完再还回去。
     */
    private PlacedBookEntry buildBack(
            String identifier, int x, int y, ResourceLocation icon,
            WidgetDesign design, ProgressionScreenHolder<?> destination) {
        PlacedBookEntryBuilder builder = BookEntry.create(identifier, x, y);
        builder.setWidgetSupplier((entry, posX, posY) ->
                new MaledictScreenOpener(entry, posX, posY, destination, icon, null));
        builder.configureWidget(widget -> widget.setDesign(design));
        return builder.build();
    }

    /** 灵魂木、纸底：1.20.1 的 {@code BookWidgetStyle.SOULWOOD}。 */
    private static Consumer<ProgressionEntryObject> soulwood() {
        return widget -> widget.setDesign(WidgetDesignType.DEFAULT.createDesign(
                WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.PAPER));
    }
}
