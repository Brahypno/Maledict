package org.brahypno.maledict.client.item.weapon.incursus;

import com.sammy.malum.registry.common.MalumParticles;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import com.sammy.malum.visual_effects.WeaponParticleEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.client.item.weapon.incursus.rift.IncursusRiftClientManager;
import org.brahypno.maledict.client.item.weapon.incursus.rift.IncursusRiftGeometry;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusSwingReach;
import org.brahypno.maledict.network.clientbound.IncursusSlashPacket;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;
import team.lodestar.lodestone.systems.particle.world.behaviors.PointyDirectionalParticleBehavior;

/**
 * 神侵恶刃的水平腰环；绕 Y 轴排布，不使用会倾斜刀光平面的 slashAngle。
 *
 * <p>发布版走裂隙花（{@link IncursusRiftClientManager}）：八张刀痕贴图不变，材质换成星空虚空。
 * 旧粒子实现保留在 {@link #spawnLegacyParticleRing} 里，只有配置关掉裂隙、开发期用系统属性
 * {@code -Dmaledict.incursus.legacySlash=true}、或刀痕资源缺失时才会走到，用来逐帧对比。
 * 判定本身在 {@link IncursusRiftClientManager#riftReady()}，飞腾与隐藏锋刃共用同一套降级决定
 * ——它们的降级形态是同一套几何换成普通贴图（没有 shader），不需要回退到粒子。
 */
public final class IncursusBladeSlashEffects {
    private static final int RING_SEGMENTS = 8;
    private static final double RING_RADIUS = 0.5;

    public static void spawn(IncursusSlashPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (IncursusRiftClientManager.riftReady()) {
            IncursusRiftClientManager.add(packet);
            return;
        }
        spawnLegacyParticleRing(packet, level);
    }

    private static void spawnLegacyParticleRing(IncursusSlashPacket packet, net.minecraft.world.level.Level level) {
        // 旧粒子按原配方散开，整朵按这一次挥砍的伤害半径等比缩放：末态环仍然落在半径上。
        // 半径 0（reach 被写成非有限值或负数）时整朵花不画。
        double radius = IncursusSwingReach.clampRadius(packet.radius());
        if (!(radius > 0.0)){
            return;
        }
        float scale = (float) (radius / IncursusRiftGeometry.AUTHORED_RING_RADIUS);
        float charge = IncursusRiftGeometry.normalizeCharge(packet.charge());
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double yaw = i * Math.PI * 2.0 / RING_SEGMENTS;
            Vec3 direction = new Vec3(Math.sin(yaw), 0, Math.cos(yaw));
            Vec3 position = packet.center().add(direction.scale(RING_RADIUS * scale));
            var effect = WeaponParticleEffects.spawnSlashParticle(
                    level, position, MalumParticles.SLASH, MalumSpiritTypes.UMBRAL_SPIRIT);
            effect.getBuilder()
                    // 1.8.2 的带 spirit 重载不设 render type（1.20.1 那边由 SlashParticleEffects 给），
                    // 这里照原参考显式指定 TRANSPARENT，否则回退路径用默认渲染类型。
                    .setRenderType(LodestoneWorldParticleRenderType.TRANSPARENT)
                    // 蓄力叠在寿命与透明度上：轻点又薄又短，满蓄力完整绽开；两条都不碰半径。
                    .multiplyLifetime(2.5f * IncursusRiftGeometry.lifetimeFactorForCharge(charge))
                    .modifyTransparencyData(
                            data -> data.multiplyValue(4.0f * IncursusRiftGeometry.opacityForCharge(charge))
                                        .multiplyCoefficient(1.5f))
                    .setScaleData(GenericParticleData.create(
                            RandomHelper.randomBetween(level.random, 2.0f, 3.0f) * scale).build()
                                                           .multiplyCoefficient(1.5f))
                    // PointyDirectional 已由 direction 设置水平朝向；局部 X 自旋会把腰环翻到眼前。
                    .setSpinData(SpinParticleData.create(0).build())
                    .setBehavior(PointyDirectionalParticleBehavior.pointyDirectional(direction))
                    .setMotion(direction.scale(RandomHelper.randomBetween(level.random, 0.3f, 0.4f) * scale));
            effect.spawnParticles();
        }
    }

    private IncursusBladeSlashEffects() {
    }
}
