package org.brahypno.maledict.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.brahypno.maledict.Maledict;

public final class MaledictParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Maledict.MODID);

    /**
     * The vanilla soul-fire flame with its sprite tinted violet; the Demiurge's death vent uses it
     * so the escaping soul still reads as soul fire, only in the statue's own colour. The sprite is
     * the vanilla one, see {@code assets/maledict/particles/purple_soul_fire.json}.
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PURPLE_SOUL_FIRE =
            PARTICLE_TYPES.register("purple_soul_fire", () -> new SimpleParticleType(false));

    private MaledictParticles() {
    }
}
