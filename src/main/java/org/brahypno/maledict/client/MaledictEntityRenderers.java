package org.brahypno.maledict.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictEntities;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MaledictEntityRenderers {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MaledictEntities.SPIRIT_ARROW.get(), SpiritArrowRenderer::new);
        event.registerEntityRenderer(MaledictEntities.FIRST_VICISSITUDE.get(), FirstVicissitudeBossRenderer::new);
        event.registerEntityRenderer(MaledictEntities.VICISSITUDE_LIGHT_ORB.get(), VicissitudeLightOrbRenderer::new);
    }

    private MaledictEntityRenderers() {
    }
}
