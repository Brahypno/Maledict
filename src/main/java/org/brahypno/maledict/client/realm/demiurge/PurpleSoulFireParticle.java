package org.brahypno.maledict.client.realm.demiurge;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.RisingParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/** Vanilla soul-fire particle with its blue sprite tinted violet; only the colour differs. */
public final class PurpleSoulFireParticle extends RisingParticle {
    private final SpriteSet sprites;

    private PurpleSoulFireParticle(ClientLevel level, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.scale(1.5F);
        this.setSpriteFromAge(sprites);
        this.setColor(.72F, .38F, 1.0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            PurpleSoulFireParticle particle =
                    new PurpleSoulFireParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
            particle.setAlpha(1.0F);
            return particle;
        }
    }
}
