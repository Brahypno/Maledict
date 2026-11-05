package org.brahypno.maledict.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;

public final class MaledictParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Maledict.MODID);

    /**
     * The vanilla soul-fire flame with its sprite tinted violet; the Demiurge's death vent uses it
     * so the escaping soul still reads as soul fire, only in the statue's own colour.
     */
    public static final RegistryObject<SimpleParticleType> PURPLE_SOUL_FIRE =
            PARTICLE_TYPES.register("purple_soul_fire", () -> new SimpleParticleType(false));

    private MaledictParticles() {
    }
}
