package org.brahypno.maledict.client.entity.demiurge;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.demiurge.DemiurgeStatueEntity;
import team.lodestar.lodestone.handlers.ScreenshakeHandler;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.screenshake.ScreenshakeBuilder;
import team.lodestar.lodestone.systems.screenshake.ScreenshakeInstance;

/**
 * The server's single roar pulse shakes nearby views once, with distance falloff.
 *
 * <p><b>1.21 适配：</b>{@code TickEvent.ClientTickEvent} 的 END 阶段换成 {@link ClientTickEvent.Post}
 * （结束阶段才跑，暂停判断保持不变）；Lodestone 的 {@code PositionedScreenshakeInstance} 构造器
 * 改为 {@code ScreenshakeBuilder} 链式构建，时长、位置、最大距离、缓动与强度参数逐项对应。
 */
@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class DemiurgeAwakeningEffects {
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (client.level == null || client.isPaused())
            return;
        for (var entity : client.level.entitiesForRendering()) {
            if (entity instanceof DemiurgeStatueEntity statue && statue.consumeRoarPulse()){
                ScreenshakeInstance instance = ScreenshakeBuilder.create().setDuration(24)
                        .placedAt(statue.position().add(0, 2.5, 0), 28.0F, Easing.QUAD_IN)
                        .setStrength(0.7F, 1.1F, 0)
                        .setEasing(Easing.QUAD_IN, Easing.QUAD_OUT).build();
                ScreenshakeHandler.addScreenshake(instance);
            }
        }
    }

    private DemiurgeAwakeningEffects() {}
}
