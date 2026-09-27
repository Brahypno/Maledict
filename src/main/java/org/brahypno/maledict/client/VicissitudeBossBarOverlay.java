package org.brahypno.maledict.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.network.BossBarStylePacket;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 无常的自定义血条。
 *
 * <p>拦掉原版那条（{@link CustomizeGuiOverlayEvent.BossEventProgress} 可取消），按自己的两张贴图重画。
 * 皮肤号由 {@link BossBarStylePacket} 从服务端镜像过来，见 {@code VicissitudeBossEvent}。
 *
 * <p><b>几何必须和 {@code art/first-vicissitude/tools/make_boss_bar.py} 里的常量一致。</b>
 * 贴图是脚本生成的，改那边就得改这边：
 * <ul>
 *   <li>base 256×16：v0–5 空槽、v5–10 填充，各是 182×5 的实心条</li>
 *   <li>overlay 256×32：上下两条贯通轨夹出一条槽，血条走槽里，卡扣和挂件横穿而过</li>
 * </ul>
 *
 * <p><b>overlay 是"骨架"，不是美术。</b>当前这批是脚本画的程序员美术（直角、斜切、渐变）。
 * 想换成手绘版只要覆盖
 * {@code assets/maledict/textures/gui/boss_bar/first_vicissitude_bar_phase_*_frame.png}，
 * 尺寸保持 256×32、并按 {@link #BAR_IN_OVERLAY_Y} 那条槽的位置留透即可，这个类一行都不用改。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class VicissitudeBossBarOverlay {

    // ---------------------------------------------------------------- 几何（对齐 make_boss_bar.py）

    /** 原版血条本体就是 182×5，不能改。 */
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    /** base 贴图的声明尺寸，UV 归一化用它。 */
    private static final int BASE_TEX_WIDTH = 256;
    private static final int BASE_TEX_HEIGHT = 16;
    /** overlay 贴图尺寸，1:1 绘制。 */
    private static final int OVERLAY_WIDTH = 256;
    private static final int OVERLAY_HEIGHT = 32;

    /** 血条本体相对 {@code event.getX()/getY()}。 */
    private static final int BAR_OFFSET_X = 1;
    private static final int BAR_OFFSET_Y = 7;
    /** overlay 相对 {@code event.getX()/getY()}。 */
    private static final int OVERLAY_OFFSET_X = -6;
    private static final int OVERLAY_OFFSET_Y = -9;

    /** 血条在 overlay 画布里的位置，由上面两组偏移量推出来，脚本画轨道时绕开它。 */
    private static final int BAR_IN_OVERLAY_X = BAR_OFFSET_X - OVERLAY_OFFSET_X;
    private static final int BAR_IN_OVERLAY_Y = BAR_OFFSET_Y - OVERLAY_OFFSET_Y;

    /**
     * 下一条血条往下挪多少。
     *
     * <p><b>必须 ≥ overlay 高度（32）。</b> 原版每画完一条就 {@code y += increment}，
     * 而每条的实际占位是 {@code [y - 9, y + 23)}——名字在 {@code y - 9}，overlay 有 32 高。
     * increment 小于 32 时，下一条的名字会落进上一条 overlay 的底部（中央挂件正好在那儿）。
     *
     * <p>同类模组用的是 25，看着没出事，是因为它们的 overlay 在中间那几行基本是空的；
     * 我们中央挂件的下尖会顶到，所以取满高。原版默认值是 {@code 10 + 字高} = 19。
     * 屏幕高 {@code guiHeight / 3} 以外不再画后续血条，按常见的 240 高算这里能站 3 条。
     */
    private static final int INCREMENT = OVERLAY_HEIGHT;

    /** 名字跟着 01_SPEC 的冷白走。 */
    private static final int NAME_COLOUR = 0xE6EDF5;

    private static final ResourceLocation[] BASE = {
            texture("first_vicissitude_bar_phase_one"),
            texture("first_vicissitude_bar_phase_two"),
    };
    private static final ResourceLocation[] OVERLAY = {
            texture("first_vicissitude_bar_phase_one_frame"),
            texture("first_vicissitude_bar_phase_two_frame"),
    };

    /** 血条 UUID → 皮肤号。断线要清，否则 UUID 会一直攒着。 */
    private static final Map<UUID, Integer> STYLES = new HashMap<>();

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Maledict.MODID,
                "textures/gui/boss_bar/" + name + ".png");
    }

    /** 由 {@link BossBarStylePacket} 驱动。 */
    public static void accept(BossBarStylePacket packet) {
        if (packet.style() == BossBarStylePacket.STYLE_NONE) {
            STYLES.remove(packet.bar());
        } else {
            STYLES.put(packet.bar(), packet.style());
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STYLES.clear();
    }

    /**
     * 必须用 {@code HIGHEST}：别的模组也会挂这个事件，谁先取消谁说了算。
     * 不是我们的血条就原样放行，交回原版渲染。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBossEventProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
        Integer style = STYLES.get(event.getBossEvent().getId());
        if (style == null) {
            return;
        }
        event.setCanceled(true);
        render(event, Mth.clamp(style, 0, BASE.length - 1));
        event.setIncrement(INCREMENT);
    }

    private static void render(CustomizeGuiOverlayEvent.BossEventProgress event, int style) {
        GuiGraphics gui = event.getGuiGraphics();
        Minecraft minecraft = Minecraft.getInstance();
        int barX = event.getX() + BAR_OFFSET_X;
        int barY = event.getY() + BAR_OFFSET_Y;

        // 1) 空槽
        gui.blit(BASE[style], barX, barY, 0, 0, BAR_WIDTH, BAR_HEIGHT,
                 BASE_TEX_WIDTH, BASE_TEX_HEIGHT);

        // 2) 按血量截断的填充。getProgress() 在 LerpingBossEvent 上已经是插值过的，
        //    掉血动画白送，不用自己做。
        int filled = Mth.floor(Mth.clamp(event.getBossEvent().getProgress(), 0.0F, 1.0F) * BAR_WIDTH);
        if (filled > 0) {
            gui.blit(BASE[style], barX, barY, 0, BAR_HEIGHT, filled, BAR_HEIGHT,
                     BASE_TEX_WIDTH, BASE_TEX_HEIGHT);
        }

        // 3) 名字。原版画在 y-9（居中、带阴影），照抄才和别的 BOSS 血条对齐。
        Component name = event.getBossEvent().getName();
        int nameX = gui.guiWidth() / 2 - minecraft.font.width(name) / 2;
        gui.drawString(minecraft.font, name, nameX, event.getY() - 9, NAME_COLOUR);

        // 4) overlay 最后画，盖住血条两端和名字两侧——所以 overlay 在这些位置必须是透明的。
        gui.blit(OVERLAY[style],
                 event.getX() + OVERLAY_OFFSET_X, event.getY() + OVERLAY_OFFSET_Y,
                 0, 0, OVERLAY_WIDTH, OVERLAY_HEIGHT, OVERLAY_WIDTH, OVERLAY_HEIGHT);
    }

    private VicissitudeBossBarOverlay() {
    }
}
