package org.brahypno.maledict.client.weapon.incursus;

import com.sammy.malum.client.PointyDirectionalBehaviorComponent;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.visual_effects.SlashParticleEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.network.IncursusSlashPacket;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;

/** 神侵恶刃的水平腰环；绕 Y 轴排布，不使用会倾斜刀光平面的 slashAngle。 */
public final class IncursusBladeSlashEffects {
    private static final int RING_SEGMENTS = 8;
    private static final double RING_RADIUS = 0.5;

    public static void spawn(IncursusSlashPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double yaw = i * Math.PI * 2.0 / RING_SEGMENTS;
            Vec3 direction = new Vec3(Math.sin(yaw), 0, Math.cos(yaw));
            Vec3 position = packet.center().add(direction.scale(RING_RADIUS * packet.scale()));
            var effect = SlashParticleEffects.spawnSlashParticle(level, position, SpiritTypeRegistry.UMBRAL_SPIRIT);
            effect.getBuilder()
                    .setScaleData(GenericParticleData.create(
                            RandomHelper.randomBetween(level.random, 2.0f, 3.0f) * packet.scale()).build())
                    // PointyDirectional 已由 direction 设置水平朝向；局部 X 自旋会把腰环翻到眼前。
                    .setSpinData(SpinParticleData.create(0).build())
                    .setBehavior(new PointyDirectionalBehaviorComponent(direction))
                    .setMotion(direction.scale(RandomHelper.randomBetween(level.random, 0.3f, 0.4f) * packet.scale()));
            effect.spawnParticles();
        }
    }

    private IncursusBladeSlashEffects() {
    }
}
