package org.brahypno.maledict.client.realm.demiurge;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.demiurge.DemiurgeStatueEntity;
import team.lodestar.lodestone.handlers.ScreenshakeHandler;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.screenshake.PositionedScreenshakeInstance;

/** The server's single roar pulse shakes nearby views once, with distance falloff. */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class DemiurgeAwakeningEffects {
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        var client = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || client.level == null || client.isPaused()) return;
        for (var entity : client.level.entitiesForRendering()) {
            if (entity instanceof DemiurgeStatueEntity statue && statue.consumeRoarPulse()) {
                ScreenshakeHandler.addScreenshake(new PositionedScreenshakeInstance(
                        24, statue.position().add(0, 2.5, 0), 8, 28, Easing.QUAD_IN)
                        .setIntensity(0.7F, 1.1F, 0).setEasing(Easing.QUAD_IN, Easing.QUAD_OUT));
            }
        }
    }

    private DemiurgeAwakeningEffects() {}
}
