package org.brahypno.maledict.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/**
 * 把「物品 -> 饰品渲染器」的绑定注册给 Curios。
 * 必须在客户端初始化阶段完成：Curios 会在 {@code EntityRenderersEvent.AddLayers} 里调用 {@code CuriosRendererRegistry.load()} 冻结这张表。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaledictCurioRenderers {
    @SubscribeEvent
    public static void registerCurioRenderers(FMLClientSetupEvent event) {
        event.enqueueWork(() -> CuriosRendererRegistry.register(
                MaledictItems.AGE_OF_ENLIGHTENMENT.get(), AgeOfEnlightenmentCurioRenderer::new));
    }

    private MaledictCurioRenderers() {
    }
}
