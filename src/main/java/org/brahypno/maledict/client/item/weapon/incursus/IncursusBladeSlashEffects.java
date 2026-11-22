package org.brahypno.maledict.client.item.weapon.incursus;

import com.sammy.malum.client.PointyDirectionalBehaviorComponent;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.visual_effects.SlashParticleEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.client.item.weapon.incursus.rift.IncursusRiftClientManager;
import org.brahypno.maledict.client.item.weapon.incursus.rift.IncursusRiftGeometry;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusSwingReach;
import org.brahypno.maledict.network.IncursusSlashPacket;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;

/**
 * 神侵恶刃的水平腰环；绕 Y 轴排布，不使用会倾斜刀光平面的 slashAngle。
 *
 * <p>两条路径的大小都只由报文里的伤害半径决定，与蓄力无关：画出来的圈就是服务端真正能打到的圈。
 *
 * <p>发布版走裂隙花（{@link IncursusRiftClientManager}）：八张刀痕贴图不变，材质换成星空虚空。
 * 旧粒子实现保留在 {@link #spawnLegacyParticleRing} 里，只有开发期用系统属性
 * {@code -Dmaledict.incursus.legacySlash=true} 或刀痕资源缺失时才会走到，用来逐帧对比。
 * 判定本身在 {@link IncursusRiftClientManager#riftReady()}，飞腾与隐藏锋刃共用同一套降级决定。
 */
public final class IncursusBladeSlashEffects {
    private static final int RING_SEGMENTS = 8;
    private static final double RING_RADIUS = 0.5;

    public static void spawn(IncursusSlashPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null){
            return;
        }
        if (IncursusRiftClientManager.riftReady()){
            IncursusRiftClientManager.add(packet);
            return;
        }
        spawnLegacyParticleRing(packet);
    }

    private static void spawnLegacyParticleRing(IncursusSlashPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null){
            return;
        }
        // 旧粒子按原配方散开，整朵按这一次挥砍的伤害半径等比缩放：末态环仍然落在半径上。
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
            // 只借幽影配色：精魂构建器会强制用 RGB 亮度作透明度，与钠的透明像素补色冲突。
            var effect = SlashParticleEffects.spawnSlashParticle(level, position,
                                                                 SpiritTypeRegistry.UMBRAL_SPIRIT.createColorData().build());
            effect.getBuilder()
                  .setRenderType(LodestoneWorldParticleRenderType.TRANSPARENT)
                  // 保留幽影构建器原有的寿命、透明度与尺寸曲线调整，再叠一层蓄力：轻点更薄更短，
                  // 与裂隙花那条 A+B 曲线保持一致。
                  .multiplyLifetime(2.5f * IncursusRiftGeometry.lifetimeFactorForCharge(charge))
                  .modifyData(builder -> builder.getParticleOptions().transparencyData,
                              data -> data.multiplyValue(4.0f * IncursusRiftGeometry.opacityForCharge(charge))
                                          .multiplyCoefficient(1.5f))
                  .setScaleData(GenericParticleData.create(
                                                           RandomHelper.randomBetween(level.random, 2.0f, 3.0f) * scale)
                                                   .build().multiplyCoefficient(1.5f))
                  // PointyDirectional 已由 direction 设置水平朝向；局部 X 自旋会把腰环翻到眼前。
                  .setSpinData(SpinParticleData.create(0).build())
                  .setBehavior(new PointyDirectionalBehaviorComponent(direction))
                  .setMotion(direction.scale(RandomHelper.randomBetween(level.random, 0.3f, 0.4f) * scale));
            effect.spawnParticles();
        }
    }

    private IncursusBladeSlashEffects() {
    }
}
