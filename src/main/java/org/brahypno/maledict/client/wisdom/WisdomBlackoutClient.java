package org.brahypno.maledict.client.wisdom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;

/** 献祭完成时的短暂全黑；世界后处理结束后绘制，HUD 和菜单随后照常显示。 */
@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class WisdomBlackoutClient {
    private static final BlackoutCountdown COUNTDOWN = new BlackoutCountdown();

    public static void start(int ticks) {
        COUNTDOWN.start(ticks);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            COUNTDOWN.start(0);
        } else if (!minecraft.isPaused()) {
            COUNTDOWN.tick();
        }
    }

    /** GameRenderer 在 GUI 投影建立后、HUD 的 hideGui 判断前调用，F1 不会跳过黑幕。 */
    public static void render(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!COUNTDOWN.isActive() || minecraft.player == null || minecraft.level == null) {
            return;
        }
        int alpha = Math.round(COUNTDOWN.opacity(partialTick) * 255.0F);
        if (alpha == 0) {
            return;
        }
        GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        graphics.fill(0, 0, minecraft.getWindow().getGuiScaledWidth(),
                      minecraft.getWindow().getGuiScaledHeight(), alpha << 24);
        graphics.flush();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        COUNTDOWN.start(0);
    }

    private WisdomBlackoutClient() {
    }
}
