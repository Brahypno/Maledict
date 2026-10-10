package org.brahypno.maledict.client.infrared;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.vfx.VicissitudeSummoningPostProcessor;
import team.lodestar.lodestone.systems.postprocess.PostProcessHandler;

/**
 * 把常驻的后处理器交给 Lodestone：登记一次之后，{@code PostProcessHandler} 会在每个
 * {@code AFTER_LEVEL} 阶段按 {@code isActive} 决定跑不跑它。注册本身不建链，链是首次真正生效时才载入的。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class InfraredPostProcessorSetup {
    @SubscribeEvent
    public static void registerPostProcessor(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PostProcessHandler.addInstance(InfraredPostProcessor.INSTANCE);
            PostProcessHandler.addInstance(VicissitudeSummoningPostProcessor.INSTANCE);
        });
    }

    private InfraredPostProcessorSetup() {
    }
}
