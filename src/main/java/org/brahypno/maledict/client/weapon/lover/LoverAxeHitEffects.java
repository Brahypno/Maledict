package org.brahypno.maledict.client.weapon.lover;

import com.sammy.malum.registry.client.ParticleRegistry;
import com.sammy.malum.registry.common.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.network.LoverAxeHitPacket;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.SimpleParticleOptions.ParticleDiscardFunctionType;
import team.lodestar.lodestone.systems.particle.SimpleParticleOptions.ParticleSpritePicker;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;
import team.lodestar.lodestone.systems.particle.world.behaviors.components.DirectionalBehaviorComponent;

import java.awt.Color;

/** Shared Meltfall/Lover impact: dislocated rings, then opposing gold and violet debris. */
public final class LoverAxeHitEffects {
    private static final Color GOLD = new Color(0xFFD979);
    private static final Color OLD_GOLD = new Color(0xAD742F);
    private static final Color VIOLET = new Color(0x513267);
    private static final Color VOID = new Color(0x17101F);
    private static final int SPLIT_TICK = 3;
    private static final int BREAK_TICK = 8;
    private static final int FRAGMENTS_PER_RING = 12;

    public static void spawn(LoverAxeHitPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        Entity target = level.getEntity(packet.targetId());
        Vec3 center = center(target, packet.center());
        for (boolean inner : new boolean[]{false, true}) {
            spawnRing(level, target, center, packet.radius(), inner);
            spawnFragments(level, target, center, packet.radius(), inner);
        }
        WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                .setColorData(ColorParticleData.create(GOLD, OLD_GOLD).build())
                .setTransparencyData(GenericParticleData.create(0.6F, 0).build())
                .setScaleData(GenericParticleData.create(packet.radius() * 0.65F, 0).build())
                .setLifetime(SPLIT_TICK)
                .enableNoClip()
                .spawn(level, center.x, center.y, center.z);
        spawnResidue(level, target, center, packet.radius());
        level.playLocalSound(center.x, center.y, center.z, SoundRegistry.TUNING_FORK_TINKERS.get(),
                SoundSource.PLAYERS, 0.45F, 1.35F, false);
        minecraft.getSoundManager().playDelayed(new SimpleSoundInstance(SoundRegistry.MALIGNANT_METAL_MOTIF.get(),
                SoundSource.PLAYERS, 0.25F, 0.65F, level.random, center.x, center.y, center.z), SPLIT_TICK);
    }

    private static void spawnRing(ClientLevel level, Entity target, Vec3 origin, float radius, boolean inner) {
        float ringRadius = radius * (inner ? 0.8F : 1.0F);
        WorldParticleBuilder.create(ParticleRegistry.CIRCLE, new DirectionalBehaviorComponent(new Vec3(0, 1, 0)))
                .setColorData(ColorParticleData.create(GOLD, OLD_GOLD).build())
                .setTransparencyData(GenericParticleData.create(0.8F, 0.65F, 0).build())
                .setScaleData(GenericParticleData.create(ringRadius).build())
                .setSpinData(SpinParticleData.create(inner ? -0.45F : 0).build())
                .setLifetime(BREAK_TICK)
                .setFrictionStrength(1)
                .enableNoClip()
                // Actors run before vanilla movement: steer to the next sample so it interpolates normally.
                .addTickActor(p -> p.setParticleSpeed(center(target, origin)
                        .add(ringOffset(radius, inner, p.getAge() + 1))
                        .subtract(p.getParticlePosition())))
                .spawn(level, origin.x, origin.y, origin.z);
    }

    private static void spawnFragments(ClientLevel level, Entity target, Vec3 origin, float radius, boolean inner) {
        // Unevenly spaced markers make the inner ring's reversal visible even with a symmetric circle texture.
        for (int i = 0; i < FRAGMENTS_PER_RING; i++) {
            double angle = i * Math.PI * 2 / FRAGMENTS_PER_RING + (i % 3) * 0.08;
            Vec3 start = fragmentOffset(radius, inner, angle, 0);
            WorldParticleBuilder.create(ParticleRegistry.SHARP_SPARK)
                    .setColorData(ColorParticleData.create(GOLD, OLD_GOLD).build())
                    .setTransparencyData(GenericParticleData.create(0.85F, 0.75F, 0).build())
                    .setScaleData(GenericParticleData.create(0.075F, 0.055F, 0).build())
                    .setSpinData(SpinParticleData.create(inner ? -0.3F : 0.15F).setSpinOffset((float) angle).build())
                    .setLifetime(14 + i % 5)
                    .setFrictionStrength(1)
                    .enableNoClip()
                    .addTickActor(p -> {
                        int age = p.getAge();
                        if (age < BREAK_TICK) {
                            p.setParticleSpeed(center(target, origin)
                                    .add(fragmentOffset(radius, inner, angle, age + 1))
                                    .subtract(p.getParticlePosition()));
                        } else if (age == BREAK_TICK) {
                            double releaseAngle = angle + rotation(inner, BREAK_TICK);
                            p.setParticleSpeed(new Vec3(Math.cos(releaseAngle) * 0.035,
                                    inner ? -0.06 : 0.07, Math.sin(releaseAngle) * 0.035));
                        } else {
                            p.setParticleSpeed(p.getParticleSpeed().add(0, -0.018, 0));
                        }
                    })
                    .spawn(level, origin.x + start.x, origin.y + start.y, origin.z + start.z);
        }
    }

    private static void spawnResidue(ClientLevel level, Entity target, Vec3 origin, float radius) {
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3;
            Vec3 point = origin.add(Math.cos(angle) * radius * 0.65, -0.08, Math.sin(angle) * radius * 0.65);
            Vec3 smokePoint = origin.add(Math.cos(angle) * radius * 1.05, -0.08, Math.sin(angle) * radius * 1.05);
            WorldParticleBuilder.create(ParticleRegistry.STRANGE_SMOKE)
                    // Additive blending loses near-black colors; lumitransparent keeps the violet-black smoke visible.
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .setColorData(ColorParticleData.create(VIOLET, VOID).build())
                    .setTransparencyData(GenericParticleData.create(0, 0.7F, 0).build())
                    // INVISIBLE would discard this zero-alpha particle before its first fade-in tick.
                    .setDiscardFunction(ParticleDiscardFunctionType.ENDING_CURVE_INVISIBLE)
                    .setSpritePicker(ParticleSpritePicker.WITH_AGE)
                    .setScaleData(GenericParticleData.create(0.18F, 0.35F, 0.16F).build())
                    .setSpinData(SpinParticleData.create(0.06F).setSpinOffset((float) angle).build())
                    .setLifeDelay(SPLIT_TICK)
                    .setLifetime(15)
                    .setFrictionStrength(1)
                    .setMotion(0, 0.065, 0)
                    .enableNoClip()
                    .addTickActor(p -> {
                        if (p.getAge() == 0) p.setParticlePosition(smokePoint.add(center(target, origin).subtract(origin)));
                    })
                    .spawn(level, smokePoint.x, smokePoint.y, smokePoint.z);
            WorldParticleBuilder.create(ParticleRegistry.SHARP_SPARK)
                    .setColorData(ColorParticleData.create(GOLD, OLD_GOLD).build())
                    .setTransparencyData(GenericParticleData.create(0.7F, 0).build())
                    .setScaleData(GenericParticleData.create(0.055F, 0).build())
                    .setLifeDelay(BREAK_TICK)
                    .setLifetime(10)
                    .setFrictionStrength(1)
                    .setMotion(0, -0.065, 0)
                    .setGravityStrength(0.25F)
                    .enableNoClip()
                    .addTickActor(p -> {
                        if (p.getAge() == 0) p.setParticlePosition(point.add(center(target, origin).subtract(origin)));
                    })
                    .spawn(level, point.x, point.y, point.z);
        }
    }

    private static Vec3 center(Entity target, Vec3 fallback) {
        return target == null || target.isRemoved() ? fallback : target.position().add(0, target.getBbHeight() * 0.5, 0);
    }

    private static Vec3 ringOffset(float radius, boolean inner, int age) {
        double shift = inner ? Math.max(0, Math.min(1, (age - SPLIT_TICK) / (double) (BREAK_TICK - SPLIT_TICK))) : 0;
        return new Vec3(shift * radius * 0.25, -shift * radius * 0.45, 0);
    }

    private static double rotation(boolean inner, int age) {
        return inner ? -0.45 * Math.max(0, age - SPLIT_TICK) : 0;
    }

    private static Vec3 fragmentOffset(float radius, boolean inner, double angle, int age) {
        double ringRadius = radius * (inner ? 0.8 : 1);
        double rotated = angle + rotation(inner, age);
        return ringOffset(radius, inner, age).add(Math.cos(rotated) * ringRadius, 0, Math.sin(rotated) * ringRadius);
    }

    private LoverAxeHitEffects() {}
}
