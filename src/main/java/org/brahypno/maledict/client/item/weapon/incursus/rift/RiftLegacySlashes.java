package org.brahypno.maledict.client.item.weapon.incursus.rift;

import com.sammy.malum.registry.common.MalumParticles;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import com.sammy.malum.visual_effects.WeaponParticleEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;
import team.lodestar.lodestone.systems.particle.world.behaviors.PointyDirectionalParticleBehavior;

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
 *
 * <p>1.20.1 的 {@code SlashParticleEffects} 与 {@code PointyDirectionalBehaviorComponent}
 * 在 Malum 1.8.2 里都已不在：前者换成 {@link WeaponParticleEffects}，后者换成 Lodestone 的
 * {@link PointyDirectionalParticleBehavior}。粒子类型的对应关系按贴图集核对过——1.20.1 的
 * {@code HEAVY_SLASH}（{@code malum:slash/heavy_slash_0x}）在 1.8.2 叫 {@code MalumParticles.SLASH}，
 * 而 1.8.2 的 {@code THIN_SLASH} 才是原来的 {@code SLASH}（{@code malum:slash/slash_0x}）。
 * 颜色取精魂而不是 {@code createColorData()}：1.8.2 的 {@code setSpirit} 内部就是
 * {@code spirit.getSpirit().createColorData().build()}，两条重载给的是同一份数据。
 */
public final class RiftLegacySlashes {

    public static void spawn(IncursusRiftInstance instance) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Vec3 anchor = instance.center();
        for (RiftStroke stroke : instance.strokes()) {
            Vec3 direction = stroke.axisU();
            float spin = (float) RiftOrientation.spinOf(stroke.axisU(), stroke.axisV());
            Vec3 position = anchor.add(stroke.offset());
            var effect = WeaponParticleEffects.spawnSlashParticle(
                    minecraft.level, position, MalumParticles.SLASH, MalumSpiritTypes.UMBRAL_SPIRIT);
            effect.getBuilder()
                  // 1.8.2 的带 spirit 重载只对幽影魂强制 LUMITRANSPARENT（见 setSpirit），
                  // 1.20.1 这条路径显式写的就是 TRANSPARENT，别让降级路径换个混合方式。
                  .setRenderType(LodestoneWorldParticleRenderType.TRANSPARENT)
                  // 刀痕自己带着尺寸、自旋、运动与寿命，原样搬进去：数量与位置都不变。
                  .setScaleData(GenericParticleData.create(stroke.halfExtent()).build())
                  .setSpinData(SpinParticleData.create(0).setSpinOffset(spin).build())
                  .setBehavior(PointyDirectionalParticleBehavior.pointyDirectional(direction))
                  .setMotion(stroke.velocity())
                  .setLifeDelay((int) stroke.startTick())
                  .setLifetime((int) stroke.lifetimeTicks());
            effect.spawnParticles();
        }
    }

    private RiftLegacySlashes() {
    }
}
