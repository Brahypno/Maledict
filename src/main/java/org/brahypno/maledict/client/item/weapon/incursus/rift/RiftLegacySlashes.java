package org.brahypno.maledict.client.item.weapon.incursus.rift;

import com.sammy.malum.client.PointyDirectionalBehaviorComponent;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.visual_effects.SlashParticleEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;

/**
 * 技能模式的降级路径：把已经算好的刀痕交回 Malum 的原版刀光。
 *
 * <p>飞腾与隐藏锋刃的粒子原来由服务端直接下发，改造后服务端只发"裂隙通知"；
 * 如果客户端这边贴图缺失或被玩家关掉裂隙，那些粒子就再也不会出现。
 * 所以这里按同一批 {@link RiftStroke} 逐道重建 Malum 粒子——位置、朝向、自旋、尺寸、
 * 速度、延迟与寿命全部取自刀痕本身，数量也与原版一致（48 / 12 / 300），
 * 于是"关掉裂隙"看到的就是改造前的那套效果。
 *
 * <p>朝向由两条基向量反解回 {@code (direction, spinOffset)}，见 {@link RiftOrientation#spinOf}。
 * 颜色沿用工程里既有的幽影配色：精魂构建器会拿 RGB 亮度当透明度，与钠的透明像素补色冲突。
 */
public final class RiftLegacySlashes {

    public static void spawn(IncursusRiftInstance instance) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null){
            return;
        }
        Vec3 anchor = instance.center();
        for (RiftStroke stroke : instance.strokes()) {
            Vec3 direction = stroke.axisU();
            float spin = (float) RiftOrientation.spinOf(stroke.axisU(), stroke.axisV());
            Vec3 position = anchor.add(stroke.offset());
            var effect = SlashParticleEffects.spawnSlashParticle(
                    minecraft.level, position,
                    SpiritTypeRegistry.UMBRAL_SPIRIT.createColorData().build());
            effect.getBuilder()
                  .setRenderType(LodestoneWorldParticleRenderType.TRANSPARENT)
                  .setScaleData(GenericParticleData.create(stroke.halfExtent()).build())
                  .setSpinData(SpinParticleData.create(0).setSpinOffset(spin).build())
                  .setBehavior(new PointyDirectionalBehaviorComponent(direction))
                  .setMotion(stroke.velocity())
                  .setLifeDelay((int) stroke.startTick())
                  .setLifetime((int) stroke.lifetimeTicks());
            effect.spawnParticles();
        }
    }

    private RiftLegacySlashes() {
    }
}
