package org.brahypno.maledict.client.entity.weepingWither;

import com.sammy.malum.registry.common.MalumParticles;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.wither.WeepingWitherEntity;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleTypes;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;

import java.awt.*;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Visible even in an unlit reliquary; driven by the existing synchronized reformation flag.
 * <p>1.21.1 的等价事件是 {@link EntityTickEvent.Pre}（{@code LivingEvent.LivingTickEvent} 已移除）；
 * Lodestone 1.8.2 把 {@code setFrictionStrength} 换成了 {@code setFriction}，并且不再有
 * {@code setDiscardFunction}——粒子随生命周期结束自然消散，曲线的末段本来就是 0 透明度。
 */
@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class WeepingWitherEffects {
    private static final Color VIOLET = new Color(0x9942D4);
    private static final Color DARK_VIOLET = new Color(0x492658);
    private static final Set<WeepingWitherEntity> REFORMING = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent
    public static void tick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof WeepingWitherEntity wither) || !wither.level().isClientSide)
            return;
        if (wither.isReforming()){
            if (REFORMING.add(wither))
                flash(wither, 0.65F);
            gather(wither);
        }else if (REFORMING.remove(wither) && wither.isAlive()){
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
                  .setLifetime(20).setFriction(1)
                  .setMotion(offset.scale(-1.0D / 20))
                  .spawn(wither.level(), center.x + offset.x, center.y + offset.y, center.z + offset.z);
        }
        if (wither.tickCount % 3 == 0){
            double angle = wither.tickCount * 0.45D;
            Vec3 offset = new Vec3(Math.cos(angle) * 0.8D, 0.4D, Math.sin(angle) * 0.8D);
            WorldParticleBuilder.create(MalumParticles.STRANGE_SMOKE)
                                .setRenderType(LodestoneWorldParticleRenderType.LUMITRANSPARENT)
                                .setFullBrightLighting()
                                .setColorData(ColorParticleData.create(VIOLET, DARK_VIOLET).build())
                                .setTransparencyData(GenericParticleData.create(0, 0.45F, 0).build())
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
                  .setLifetime(24).setFriction(0.96F)
                  .setMotion(x * 0.13D, -0.035D - (i % 3) * 0.015D, z * 0.13D)
                  .spawn(wither.level(), center.x + x * 0.35D, center.y + (i % 3 - 1) * 0.3D,
                         center.z + z * 0.35D);
        }
    }

    private static WorldParticleBuilder wisp() {
        return WorldParticleBuilder.create(LodestoneParticleTypes.WISP_PARTICLE)
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
