package org.brahypno.maledict.client.codex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sammy.malum.MalumMod;
import com.sammy.malum.client.screen.codex.BookEntry;
import com.sammy.malum.client.screen.codex.BookWidgetStyle;
import com.sammy.malum.client.screen.codex.PlacedBookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntryBuilder;
import com.sammy.malum.client.screen.codex.objects.progression.ScreenOpenerObject;
import com.sammy.malum.client.screen.codex.pages.CyclingPage;
import com.sammy.malum.client.screen.codex.pages.recipe.SpiritInfusionPage;
import com.sammy.malum.client.screen.codex.pages.recipe.SpiritRiteRecipePage;
import com.sammy.malum.client.screen.codex.pages.recipe.vanilla.SmeltingPage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextItemPage;
import com.sammy.malum.client.screen.codex.pages.text.HeadlineTextPage;
import com.sammy.malum.client.screen.codex.pages.text.SpiritRiteTextPage;
import com.sammy.malum.client.screen.codex.pages.text.TextPage;
import com.sammy.malum.client.screen.codex.screens.AbstractProgressionCodexScreen;
import com.sammy.malum.registry.common.SoundRegistry;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.soulbinding.SoulbindingRite;
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
    private static final String LUMENFALL_BISMUTH_ENTRY = "void.maledict.lumenfall_bismuth";
    private static final String EUTECTIC_ENTRY = "void.maledict.refracted_lead_bismuth_eutectic";
    private static final String AUFHEBEN_ARMOR_ENTRY = "void.maledict.aufheben_lightward";

    /**
     * 拘魂仪式：**只在学会之后出现**，坐标 (1,8) 在腐枯凋灵 (0,10) 右边一格。
     * 两半仪式都给：灵魂木附加、符文木解除，各配一页配方。
     */
    private static final String SOULBINDING_RITE_ENTRY = "void.maledict.soulbinding_rite";
    /** 灵魂木：主手工具 + 副手封印，附加后等待下一次有效击杀。key 含 corrupt，图标与标题自动走灵魂木。 */
    private static final String SOULBINDING_ATTACH_PAGE = "corrupt_maledict_soulbinding_rite";
    /** 符文木：解除封印，精魂与封印都不返还。 */
    private static final String SOULBINDING_RELEASE_PAGE = "maledict_soulbinding_rite";
    private static final int SOULBINDING_RITE_X = 1;
    private static final int SOULBINDING_RITE_Y = 8;

    /**
     * 三个 Lumenfall 条目直接照抄奥秘全典里对应条目的坐标，不另起一列：
     * 恶念之铅 (2,3)、恶念白镴 (3,9)、恶念要塞盔甲 (4,12)。
     * 这一页原有的腐枯凋灵在 (0,5)，回程节点在 (0,1)/(0,0)，都不会撞上。
     */
    private static final int LUMENFALL_BISMUTH_X = 2;
    private static final int LUMENFALL_BISMUTH_Y = 3;
    private static final int EUTECTIC_X = 3;
    private static final int EUTECTIC_Y = 9;
    private static final int AUFHEBEN_ARMOR_X = 4;
    private static final int AUFHEBEN_ARMOR_Y = 12;

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
        entries.add(buildSoulbindingRite());
        entries.add(buildLumenfallBismuth());
        entries.add(buildEutectic());
        entries.add(buildAufhebenArmor());
        entries.add(buildBack(SOUL_BACK, 0, 1, SOUL_ICON,
                              BookWidgetStyle.DARK_GRAND_SOULWOOD, LightfallArcanaScreen::openCodexViaItem));
        entries.add(buildBack(VOID_BACK, 0, 0, VOID_ICON,
                              BookWidgetStyle.DARK_GRAND_RUNEWOOD, LightfallVoidScreen::openCodexViaItem));
    }

    private PlacedBookEntry buildWeepingWither() {
        PlacedBookEntryBuilder builder = BookEntry.build(WEEPING_WITHER, 0, 10);
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

    private PlacedBookEntry buildSoulbindingRite() {
        PlacedBookEntryBuilder builder = BookEntry.build(
                SOULBINDING_RITE_ENTRY, SOULBINDING_RITE_X, SOULBINDING_RITE_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.SOULBINDING_SEAL)
                .setStyle(BookWidgetStyle.SOULWOOD));
        builder.setEntryVisibleWhen(LightfallCodex::knowsSoulbinding);
        builder.addPage(new HeadlineTextPage(SOULBINDING_RITE_ENTRY, SOULBINDING_RITE_ENTRY + ".1"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.SOULBINDING_SEAL.get()));
        SoulbindingRite rite = SoulbindingRite.get();
        if (rite != null) {
            builder.addPage(new SpiritRiteTextPage(rite, SOULBINDING_ATTACH_PAGE));
            builder.addPage(new SoulwoodSpiritRiteRecipePage(rite));
            builder.addPage(new SpiritRiteTextPage(rite, SOULBINDING_RELEASE_PAGE));
            builder.addPage(new SpiritRiteRecipePage(rite));
        }
        return builder.build();
    }

    /**
     * 沉辉铋：矿石烧炼成锭，因此正文之后补一页烧炼配方。
     */
    private PlacedBookEntry buildLumenfallBismuth() {
        PlacedBookEntryBuilder builder = BookEntry.build(
                LUMENFALL_BISMUTH_ENTRY, LUMENFALL_BISMUTH_X, LUMENFALL_BISMUTH_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.LUMENFALL_BISMUTH_INGOT)
                .setStyle(BookWidgetStyle.SOULWOOD));
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

    /**
     * 折辉铅铋共晶：正文之后接精魂灌注配方页。
     */
    private PlacedBookEntry buildEutectic() {
        PlacedBookEntryBuilder builder = BookEntry.build(
                EUTECTIC_ENTRY, EUTECTIC_X, EUTECTIC_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC)
                .setStyle(BookWidgetStyle.SOULWOOD));
        builder.addPage(new HeadlineTextPage(EUTECTIC_ENTRY, EUTECTIC_ENTRY + ".1"));
        builder.addPage(new TextPage(EUTECTIC_ENTRY + ".2"));
        builder.addPage(new TextPage(EUTECTIC_ENTRY + ".3"));
        builder.addPage(SpiritInfusionPage.fromOutput(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC.get()));
        return builder.build();
    }

    /**
     * 扬弃逐光盔甲：四件共用一张翻页配方页，写法与 Malum 的恶念要塞盔甲一致。
     */
    private PlacedBookEntry buildAufhebenArmor() {
        PlacedBookEntryBuilder builder = BookEntry.build(
                AUFHEBEN_ARMOR_ENTRY, AUFHEBEN_ARMOR_X, AUFHEBEN_ARMOR_Y);
        builder.configureWidget(widget -> widget
                .setIcon(MaledictItems.AUFHEBEN_LIGHTWARD_HELMET)
                .setStyle(BookWidgetStyle.SOULWOOD));
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
