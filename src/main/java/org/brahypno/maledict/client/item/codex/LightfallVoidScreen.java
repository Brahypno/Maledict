package org.brahypno.maledict.client.item.codex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sammy.malum.MalumMod;
import com.sammy.malum.client.screen.codex.BookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntry;
import com.sammy.malum.client.screen.codex.PlacedBookEntryBuilder;
import com.sammy.malum.client.screen.codex.WidgetDesignType;
import com.sammy.malum.client.screen.codex.screens.progression.AbstractProgressionCodexScreen;
import com.sammy.malum.client.screen.codex.screens.progression.ProgressionScreenHolder;
import com.sammy.malum.client.screen.codex.screens.progression.VoidProgressionScreen;
import com.sammy.malum.registry.common.MalumSoundEvents;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;

import java.awt.Color;

/**
 * The third book's Void view. It mirrors the Void book but its return link goes back to our
 * Arcana view, and it has one extra jump node below that link.
 *
 * <p>「镜像」是照字面来的：虚空之书的条目表整份搬过来，**本模组自己的虚空条目也在内**
 * （{@code void.maledict.elegy_bow}、{@code void.maledict.incursus_blade}、
 * {@code maledict.corrupt_sacrifice_rite} 等）。只有回程节点换成指回本模组奥术视野的那一枚。
 * 1.20.1 在这里把本模组的条目一并过滤掉了，结果从坠光秘典点「虚空通史」进来看到的是缺页的视图。
 *
 * <p>1.20.1 覆盖 {@code setupObjects()} 去 {@code faceObject(bookObjectHandler.get(0))}，把视图对准
 * (0,0) 的回程节点。1.8.2 不能这么做：基类在 {@code setupObjects()} 之后还会调 {@code faceOrigin()}，
 * 焦点由逐条目的 {@code setOrigin()} 决定（虚空之书把原点打在自己的第二条例目
 * {@code void.the_weeping_well} 上，而它照原样被搬过来了）。所以这里**不覆盖 setupObjects**，
 * 让基类按 1.8.2 自己的规则定位——这也正是 1.20.1 那句注释的意图（对齐原版虚空视图）。
 */
public final class LightfallVoidScreen extends AbstractProgressionCodexScreen {
    private static final String SOUL_LINK = "chronicles_of_the_soul";
    private static final String DESCENT_LINK = "maledict.lightfall.descent";
    private static final ResourceLocation SOUL_ICON =
            MalumMod.malumPath("textures/gui/book/icons/arcana_button.png");
    private static final ResourceLocation WEEPING_WELL_ICON =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "textures/gui/book/icons/void_depot_button.png");

    static final ProgressionScreenHolder<LightfallVoidScreen> SCREEN =
            new ProgressionScreenHolder<>(LightfallVoidScreen::new, MalumSoundEvents.ARCANA_TRANSITION_EVIL);

    private LightfallVoidScreen() {
        super(MalumSoundEvents.ARCANA_SWEETENER_EVIL, 1024, 768);
        setVoidTouched(true);
        // 与 1.20.1 一样先让 Malum 的虚空之书成型：1.8.2 的 VoidProgressionScreen 构造器里会顺手
        // seeTheRevelation(VOID_READER)，并把 BookPage.isVoidThemed 打开，这一步不能省。
        VoidProgressionScreen.SCREEN.getScreen();
    }

    public static LightfallVoidScreen getScreenInstance() {
        return SCREEN.getScreen();
    }

    public static void openCodexViaItem() {
        LightfallCodex.rememberView(LightfallCodex.View.VOID);
        LightfallCodex.withoutBecomingMalumsMostRecent(() -> SCREEN.openCodexViaItem(true));
    }

    @Override
    public Color getOutlineColor() {
        return VoidProgressionScreen.OUTLINE_COLOR;
    }

    @Override
    public void renderBackground(PoseStack poseStack) {
        renderBackground(poseStack, VoidProgressionScreen.BACKGROUND_TEXTURE, 0.2F, 0.2F);
    }

    @Override
    public void setupEntries() {
        // 这里只能碰基类的 entries 字段：此刻子类的实例字段还没初始化。
        entries.add(buildSoulLink());
        // 这一页是虚空之书的整份镜像：Malum 的条目与**本模组自己的虚空条目**都照搬，只把回程节点
        // 换成指回本模组奥术视野的那一枚（下面的 SOUL_LINK）。1.20.1 在这里连本模组的条目一起过滤掉了，
        // 结果从坠光秘典点「虚空通史」进来看到的是一片缺页——那三条正是玩家要找的东西。
        for (PlacedBookEntry entry : VoidProgressionScreen.SCREEN.getScreen().getEntries()) {
            if (SOUL_LINK.equals(entry.identifier)) {
                continue;
            }
            entries.add(entry);
        }
        entries.add(buildLightfallLink());
    }

    /**
     * 回程节点：照 1.20.1 放在 (0,0)。
     *
     * <p>这里替换掉的正是虚空之书的第一条例目，所以**不要**再给它打 {@code setOrigin()}——
     * 原点仍在被搬过来的 {@code void.the_weeping_well} 上，重复标记会让 {@code faceOrigin()} 认错。
     * 跳转同样包一层 {@link MaledictScreenOpener}，不让目标持有者改写 Malum 的 {@code mostRecentScreen}。
     */
    private PlacedBookEntry buildSoulLink() {
        PlacedBookEntryBuilder builder = BookEntry.create(SOUL_LINK, 0, 0);
        builder.setWidgetSupplier((entry, x, y) -> new MaledictScreenOpener(
                entry, x, y, LightfallArcanaScreen.SCREEN, SOUL_ICON, null));
        builder.configureWidget(widget -> widget.setDesign(WidgetDesignType.GRAND.createDesign(
                WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.DARK)));
        return builder.build();
    }

    private PlacedBookEntry buildLightfallLink() {
        PlacedBookEntryBuilder builder = BookEntry.create(DESCENT_LINK, 0, -1);
        builder.setWidgetSupplier((entry, x, y) -> new MaledictScreenOpener(
                entry, x, y, LightfallCodexScreen.SCREEN, WEEPING_WELL_ICON, null));
        // 波纹图标用加算混合，底下必须垫一层深色才留得住颜色。
        builder.configureWidget(widget -> widget.setDesign(WidgetDesignType.GILDED.createDesign(
                WidgetDesignType.FrameType.SOULWOOD, WidgetDesignType.FillingType.DARK)));
        return builder.build();
    }
}
