package org.brahypno.maledict.client.realm;

import com.sammy.malum.registry.client.ParticleRegistry;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.wither.WeepingWitherEntity;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.SimpleParticleOptions.ParticleDiscardFunctionType;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;

import java.awt.Color;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Visible even in an unlit reliquary; driven by the existing synchronized reformation flag. */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WeepingWitherEffects {
    private static final Color VIOLET = new Color(0x9942D4);
    private static final Color DARK_VIOLET = new Color(0x492658);
    private static final Set<WeepingWitherEntity> REFORMING = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof WeepingWitherEntity wither) || !wither.level().isClientSide) return;
        if (wither.isReforming()) {
            if (REFORMING.add(wither)) flash(wither, 0.65F);
            gather(wither);
        } else if (REFORMING.remove(wither) && wither.isAlive()) {
            release(wither);
        }
    }

    private static Vec3 chest(WeepingWitherEntity wither) {
        double yaw = Math.toRadians(wither.yBodyRot);
        return wither.position().add(Math.sin(yaw) * 0.4D, 2.4D, -Math.cos(yaw) * 0.4D);
    }

    private static void gather(WeepingWitherEntity wither) {
        Vec3 center = chest(wither);
        for (int i = 0; i < 3; i++) {
            double angle = wither.tickCount * 0.35D + i * Math.PI * 2 / 3;
            Vec3 offset = new Vec3(Math.cos(angle) * 1.25D, 0.35D + i * 0.35D, Math.sin(angle) * 1.25D);
            wisp().setScaleData(GenericParticleData.create(0.11F, 0.045F).build())
                    .setLifetime(20).setFrictionStrength(1)
                    .setMotion(offset.scale(-1.0D / 20))
                    .spawn(wither.level(), center.x + offset.x, center.y + offset.y, center.z + offset.z);
        }
        if (wither.tickCount % 3 == 0) {
            double angle = wither.tickCount * 0.45D;
            Vec3 offset = new Vec3(Math.cos(angle) * 0.8D, 0.4D, Math.sin(angle) * 0.8D);
            WorldParticleBuilder.create(ParticleRegistry.STRANGE_SMOKE)
                    .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                    .setFullBrightLighting()
                    .setColorData(ColorParticleData.create(VIOLET, DARK_VIOLET).build())
                    .setTransparencyData(GenericParticleData.create(0, 0.45F, 0).build())
                    .setDiscardFunction(ParticleDiscardFunctionType.ENDING_CURVE_INVISIBLE)
                    .setScaleData(GenericParticleData.create(0.3F, 0.55F).build())
                    .setLifetime(24).setMotion(offset.scale(-0.04D))
                    .enableNoClip().enableForcedSpawn()
                    .spawn(wither.level(), center.x + offset.x, center.y + offset.y, center.z + offset.z);
        }
    }

    private static void release(WeepingWitherEntity wither) {
        Vec3 center = chest(wither);
        flash(wither, 1.2F);
        for (int i = 0; i < 32; i++) {
            double angle = i * Math.PI * 2 / 32;
            double x = Math.cos(angle), z = Math.sin(angle);
            wisp().setScaleData(GenericParticleData.create(0.16F, 0.025F).build())
                    .setLifetime(24).setFrictionStrength(0.96F)
                    .setMotion(x * 0.13D, -0.035D - (i % 3) * 0.015D, z * 0.13D)
                    .spawn(wither.level(), center.x + x * 0.35D, center.y + (i % 3 - 1) * 0.3D,
                            center.z + z * 0.35D);
        }
    }

    private static WorldParticleBuilder wisp() {
        return WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                .setFullBrightLighting()
                .setColorData(ColorParticleData.create(VIOLET, DARK_VIOLET).build())
                .setTransparencyData(GenericParticleData.create(0.85F, 0).build())
                .enableNoClip().enableForcedSpawn();
    }

    private static void flash(WeepingWitherEntity wither, float scale) {
        Vec3 center = chest(wither);
        wisp().setScaleData(GenericParticleData.create(scale, 0).build())
                .setLifetime(12).spawn(wither.level(), center.x, center.y, center.z);
    }

    private WeepingWitherEffects() {}
}
