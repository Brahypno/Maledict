package org.brahypno.maledict.client.item.codex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sammy.malum.MalumMod;
import com.sammy.malum.client.VoidRevelationHandler;
import com.sammy.malum.client.screen.codex.BookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntryBuilder;
import com.sammy.malum.client.screen.codex.WidgetDesignType;
import com.sammy.malum.client.screen.codex.screens.progression.AbstractProgressionCodexScreen;
import com.sammy.malum.client.screen.codex.screens.progression.ArcanaProgressionScreen;
import com.sammy.malum.client.screen.codex.screens.progression.ProgressionScreenHolder;
import com.sammy.malum.registry.common.MalumSoundEvents;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;

import java.awt.Color;

/**
 * The third book's Arcana view. It starts on the same soul-magic view as the Esoterica and
 * keeps the original Chronicles of the Void position, but that link now opens our own Void view
 * and records it. A new jump node sits directly below it.
 *
 * <p>1.20.1 直接照抄 {@code ArcanaProgressionScreen.ENTRIES} 这份静态条目表；1.8.2 的条目表是每个屏幕
 * 自己的实例字段、由 {@code setupEntries()} 现填（Malum 那侧还会在同一个构造流程里被
 * {@code SetupMalumCodexEntriesEvent} 补进本模组的条目）。所以这里先取一次奥秘全典的屏幕实例，
 * 再整份搬过来。**本模组自己的 arcana 条目也在内**：这一页是奥术全典的镜像，1.20.1 在这里把它们过滤掉，
 * 结果从坠光秘典进来看到的是缺了本模组内容的一本「奥术全典」。
 *
 * <p>条目对象是共享的 {@link PlacedBookEntry}（同 1.20.1 引用同一份静态表）；控件对象由每个屏幕自己的
 * {@code EntryObjectHandler} 各自新建，所以「原点」这类逐控件标记会跟着 {@code configureWidget} 一起带过来。
 */
public final class LightfallArcanaScreen extends AbstractProgressionCodexScreen {
    private static final String VOID_LINK = "chronicles_of_the_void";
    private static final String LIGHTFALL_LINK = "maledict.lightfall.descent";
    private static final ResourceLocation VOID_ICON =
            MalumMod.malumPath("textures/gui/book/icons/void_button.png");
    private static final ResourceLocation WEEPING_WELL_ICON =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "textures/gui/book/icons/void_depot_button.png");

    static final ProgressionScreenHolder<LightfallArcanaScreen> SCREEN =
            new ProgressionScreenHolder<>(LightfallArcanaScreen::new, MalumSoundEvents.ARCANA_TRANSITION_NORMAL);

    private LightfallArcanaScreen() {
        super(MalumSoundEvents.ARCANA_SWEETENER_NORMAL, 1024, 2560);
        // 1.20.1 在构造器里就把这一页标成「已被虚空触及」；1.8.2 同名字段是 protected + setter。
        setVoidTouched(true);
        // 与 1.20.1 一样先让 Malum 的奥秘全典成型：它的 setupEntries 会设好 BookPage.isVoidThemed、
        // 并让本模组的 arcana 条目经 SetupMalumCodexEntriesEvent 落进去。
        ArcanaProgressionScreen.SCREEN.getScreen();
    }

    public static LightfallArcanaScreen getScreenInstance() {
        return SCREEN.getScreen();
    }

    public static void openCodexViaItem() {
        LightfallCodex.rememberView(LightfallCodex.View.ARCANA);
        LightfallCodex.withoutBecomingMalumsMostRecent(() -> SCREEN.openCodexViaItem(true));
    }

    @Override
    public Color getOutlineColor() {
        return ArcanaProgressionScreen.OUTLINE_COLOR;
    }

    @Override
    public void renderBackground(PoseStack poseStack) {
        // 视差系数照 1.20.1 原样（1.8.2 的奥秘全典自己用 0.1F/0.4F，这里是纯观感差异）。
        renderBackground(poseStack, ArcanaProgressionScreen.BACKGROUND_TEXTURE, 0.2F, 0.4F);
    }

    @Override
    public void setupEntries() {
        // 这里只能碰基类的 entries 字段：此刻子类的实例字段还没初始化。
        entries.add(buildVoidLink());
        // 奥术全典的整份镜像：Malum 的条目与本模组自己的 arcana 条目都照搬，只把「虚空通史」节点换成
        // 指回本模组虚空视野的那一枚（上面的 VOID_LINK）。条目对象是共享引用，没有第二份文案。
        for (PlacedBookEntry entry : ArcanaProgressionScreen.SCREEN.getScreen().getEntries()) {
            if (VOID_LINK.equals(entry.identifier)) {
                continue;
            }
            entries.add(entry);
        }
        entries.add(buildLightfallLink());
    }

    private PlacedBookEntry buildVoidLink() {
        PlacedBookEntryBuilder builder = BookEntry.create(VOID_LINK, 0, -1);
        // 开屏之前先记下「已见过虚空启示」；跳转本身由 MaledictScreenOpener 负责还原 Malum 的
        // mostRecentScreen。click 只在悬停时被调用（BookObject#tryClick 先看 isHoveredOver）。
        builder.setWidgetSupplier((entry, x, y) -> new MaledictScreenOpener(
                entry, x, y, LightfallVoidScreen.SCREEN, VOID_ICON,
                () -> VoidRevelationHandler.seeTheRevelation(VoidRevelationHandler.RevelationType.VOID_READER)));
        builder.configureWidget(widget -> widget.setDesign(WidgetDesignType.GRAND.createDesign(
                WidgetDesignType.FrameType.RUNEWOOD, WidgetDesignType.FillingType.DARK)));
        return builder.build();
    }

    private PlacedBookEntry buildLightfallLink() {
        PlacedBookEntryBuilder builder = BookEntry.create(LIGHTFALL_LINK, 0, -2);
        builder.setWidgetSupplier((entry, x, y) -> new MaledictScreenOpener(
                entry, x, y, LightfallCodexScreen.SCREEN, WEEPING_WELL_ICON, null));
        // 波纹图标用加算混合，底下必须垫一层深色才留得住颜色。
        builder.configureWidget(widget -> widget.setDesign(WidgetDesignType.GRAND.createDesign(
                WidgetDesignType.FrameType.RUNEWOOD, WidgetDesignType.FillingType.DARK)));
        return builder.build();
    }
}
