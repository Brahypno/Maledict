package org.brahypno.maledict.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.model.AgeOfEnlightenmentModel;
import org.brahypno.maledict.client.model.FirstVicissitudeBossModel;
import org.brahypno.maledict.registry.MaledictEntities;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MaledictEntityRenderers {
    private static AgeOfEnlightenmentModel ageOfEnlightenmentModel;

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MaledictEntities.SPIRIT_ARROW.get(), SpiritArrowRenderer::new);
        event.registerEntityRenderer(MaledictEntities.FIRST_VICISSITUDE.get(), FirstVicissitudeBossRenderer::new);
        event.registerEntityRenderer(MaledictEntities.VICISSITUDE_LIGHT_ORB.get(), VicissitudeLightOrbRenderer::new);
        event.registerEntityRenderer(MaledictEntities.VICISSITUDE_SPIRIT_BOLT.get(), VicissitudeSpiritBoltRenderer::new);
        event.registerEntityRenderer(MaledictEntities.VICISSITUDE_SCYTHE.get(), VicissitudeScytheRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FirstVicissitudeBossRenderer.LAYER,
                FirstVicissitudeBossModel::createBodyLayer);
        event.registerLayerDefinition(AgeOfEnlightenmentModel.LAYER,
                AgeOfEnlightenmentModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void bakeLayers(EntityRenderersEvent.AddLayers event) {
        ageOfEnlightenmentModel = new AgeOfEnlightenmentModel(
                event.getEntityModels().bakeLayer(AgeOfEnlightenmentModel.LAYER));
    }

    /** 面具模型实例，供饰品渲染器使用；在 {@code AddLayers} 阶段烘焙。 */
    public static AgeOfEnlightenmentModel ageOfEnlightenmentModel() {
        return ageOfEnlightenmentModel;
    }

    private MaledictEntityRenderers() {
    }
}
