package org.brahypno.maledict.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;

/**
 * 延迟池的两个血条显示，用 Forge 的官方注册方式挂在原版血量之后：{@code registerAbove} 会把它插到
 * {@code PLAYER_HEALTH} 与 {@code ARMOR_LEVEL} 之间，所以红心已经画完、护甲还没画，可以安全地在
 * 原版红心的格子上再叠一层。两个显示都画在血条自己的位置上，不占新行。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaledictGuiOverlays {

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "delayed_vitals_heal",
                            DelayedVitalsOverlay.PENDING_HEAL);
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "delayed_vitals_damage",
                            DelayedVitalsOverlay.PENDING_DAMAGE);
    }

    private MaledictGuiOverlays() {
    }
}
