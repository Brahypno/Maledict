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
import org.brahypno.maledict.common.entity.VicissitudeBossEvent;
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
 * 图稿源在 {@code art/first-vicissitude/boss-bar}，脚本负责打包到游戏尺寸：
 * <ul>
 *   <li>base 256×16：v0–5 空槽、v5–10 填充，各是 182×5 的实心条</li>
 *   <li>overlay 256×32：居中的晶体头、断环和肩甲；一阶段覆羽，二阶段骨翼与外露血槽</li>
 * </ul>
 *
 * <p>一阶段不绘制底条和血量填充，覆羽间的负空间不会泄露血量。
 * 二阶段按原有进度绘制填充，中央图标和两端护甲盖在血槽之上。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class VicissitudeBossBarOverlay {

    // ---------------------------------------------------------------- 几何（对齐 make_boss_bar.py）

    /** 沿用原版血条的 182×5 本体。 */
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    /** base 贴图的声明尺寸，UV 归一化用它。 */
    private static final int BASE_TEX_WIDTH = 256;
    private static final int BASE_TEX_HEIGHT = 16;
    /** overlay 贴图尺寸，1:1 绘制。 */
    private static final int OVERLAY_WIDTH = 256;
    private static final int OVERLAY_HEIGHT = 32;

    /** 血条本体相对 {@code event.getX()/getY()}。 */
    private static final int BAR_OFFSET_X = 0;
    private static final int BAR_OFFSET_Y = 19;
    /** overlay 相对 {@code event.getX()/getY()}。 */
    private static final int OVERLAY_OFFSET_X = -(OVERLAY_WIDTH - BAR_WIDTH) / 2;
    private static final int OVERLAY_OFFSET_Y = 0;

    /** 血条在 overlay 画布里的位置，由上面两组偏移量推出来，脚本画轨道时绕开它。 */
    private static final int BAR_IN_OVERLAY_X = BAR_OFFSET_X - OVERLAY_OFFSET_X;
    private static final int BAR_IN_OVERLAY_Y = BAR_OFFSET_Y - OVERLAY_OFFSET_Y;

    /**
     * 下一条血条往下挪多少。
     *
     * <p>每条占 {@code [y - 9, y + 32)}；下一条名字须避开当前图稿，另留 2px 间隙。
     */
    private static final int INCREMENT = OVERLAY_HEIGHT + 11;

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

        // 一阶段由覆羽遮住血量；二阶段才暴露血槽，使用原有插值进度。
        if (style == VicissitudeBossEvent.STYLE_PHASE_TWO) {
            gui.blit(BASE[style], barX, barY, 0, 0, BAR_WIDTH, BAR_HEIGHT,
                     BASE_TEX_WIDTH, BASE_TEX_HEIGHT);
            int filled = Mth.floor(Mth.clamp(event.getBossEvent().getProgress(), 0.0F, 1.0F) * BAR_WIDTH);
            if (filled > 0) {
                gui.blit(BASE[style], barX, barY, 0, BAR_HEIGHT, filled, BAR_HEIGHT,
                         BASE_TEX_WIDTH, BASE_TEX_HEIGHT);
            }
        }

        // 3) 名字。原版画在 y-9（居中、带阴影），照抄才和别的 BOSS 血条对齐。
        Component name = event.getBossEvent().getName();
        int nameX = gui.guiWidth() / 2 - minecraft.font.width(name) / 2;
        gui.drawString(minecraft.font, name, nameX, event.getY() - 9, NAME_COLOUR);

        // 4) 晶体头、肩甲与羽翼在填充上层；名字位于整个图稿上方。
        gui.blit(OVERLAY[style],
                 event.getX() + OVERLAY_OFFSET_X, event.getY() + OVERLAY_OFFSET_Y,
                 0, 0, OVERLAY_WIDTH, OVERLAY_HEIGHT, OVERLAY_WIDTH, OVERLAY_HEIGHT);
    }

    private VicissitudeBossBarOverlay() {
    }
}
