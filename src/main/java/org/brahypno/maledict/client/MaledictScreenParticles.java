package org.brahypno.maledict.client;

import com.sammy.malum.common.item.IVoidItem;
import com.sammy.malum.visual_effects.ScreenParticleEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;
import team.lodestar.lodestone.handlers.screenparticle.ParticleEmitterHandler;
import team.lodestar.lodestone.systems.particle.screen.ScreenParticleHolder;

@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaledictScreenParticles {
    private static final IVoidItem VOID_PARTICLES = new IVoidItem() {
    };

    private static final ParticleEmitterHandler.ItemParticleSupplier ESOTERICA_PARTICLES =
            new ParticleEmitterHandler.ItemParticleSupplier() {
                @Override
                public void spawnLateParticles(ScreenParticleHolder target, Level level, float partialTick,
                                               ItemStack stack, float x, float y) {
                    ScreenParticleEffects.spawnEncyclopediaEsotericaScreenParticles(target, level, partialTick);
                }
            };

    @SubscribeEvent
    public static void registerItemParticleEmitters(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ParticleEmitterHandler.registerItemParticleEmitter(
                    VOID_PARTICLES,
                    MaledictItems.INCURSUS_BLADE.get());
            ParticleEmitterHandler.registerItemParticleEmitter(
                    ESOTERICA_PARTICLES,
                    MaledictItems.ELEGY_BOW.get());
        });
    }

    private MaledictScreenParticles() {
    }
}
