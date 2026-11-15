package org.brahypno.maledict.client.weapon.incursus;

import com.sammy.malum.client.PointyDirectionalBehaviorComponent;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.visual_effects.SlashParticleEffects;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.client.weapon.incursus.rift.IncursusRiftClientManager;
import org.brahypno.maledict.client.weapon.incursus.rift.IncursusRiftRenderTypes;
import org.brahypno.maledict.network.IncursusSlashPacket;
import org.slf4j.Logger;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;

/**
 * 神侵恶刃的水平腰环；绕 Y 轴排布，不使用会倾斜刀光平面的 slashAngle。
 *
 * <p>发布版走裂隙花（{@link IncursusRiftClientManager}）：八张刀痕贴图不变，材质换成星空虚空。
 * 旧粒子实现保留在 {@link #spawnLegacyParticleRing} 里，只有开发期用系统属性
 * {@code -Dmaledict.incursus.legacySlash=true} 或刀痕资源缺失时才会走到，用来逐帧对比。
 */
public final class IncursusBladeSlashEffects {
    private static final int RING_SEGMENTS = 8;
    private static final double RING_RADIUS = 0.5;
    /** 开发期对照开关，只在 -D 里给，不进玩家配置。 */
    private static final String LEGACY_SWITCH = "maledict.incursus.legacySlash";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean fallbackLogged;

    public static void spawn(IncursusSlashPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (riftActive()) {
            IncursusRiftClientManager.add(packet);
            return;
        }
        spawnLegacyParticleRing(packet);
    }

    /**
     * 裂隙路径是否可用。刀痕贴图缺失（Malum 改名）或配置关闭时退回旧粒子，
     * 保证"降级而不是整个斩击消失"。
     */
    private static boolean riftActive() {
        if (Boolean.getBoolean(LEGACY_SWITCH)) {
            return false;
        }
        try {
            if (!IncursusRiftClientManager.enabled()) {
                return false;
            }
            if (IncursusRiftRenderTypes.maskAvailable(0)) {
                return true;
            }
            if (!fallbackLogged) {
                fallbackLogged = true;
                LOGGER.warn("Incursus rift mask (malum:slash/heavy_slash_00) not found; "
                            + "falling back to the old slash particles.");
            }
            return false;
        } catch (RuntimeException ignored) {
            // 配置尚未加载：宁可画旧粒子，也不要因为一条视觉通知崩掉客户端。
            return false;
        }
    }

    private static void spawnLegacyParticleRing(IncursusSlashPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double yaw = i * Math.PI * 2.0 / RING_SEGMENTS;
            Vec3 direction = new Vec3(Math.sin(yaw), 0, Math.cos(yaw));
            Vec3 position = packet.center().add(direction.scale(RING_RADIUS * packet.scale()));
            // 只借幽影配色：精魂构建器会强制用 RGB 亮度作透明度，与钠的透明像素补色冲突。
            var effect = SlashParticleEffects.spawnSlashParticle(level, position,
                    SpiritTypeRegistry.UMBRAL_SPIRIT.createColorData().build());
            effect.getBuilder()
                    .setRenderType(LodestoneWorldParticleRenderType.TRANSPARENT)
                    // 保留幽影构建器原有的寿命、透明度与尺寸曲线调整。
                    .multiplyLifetime(2.5f)
                    .modifyData(builder -> builder.getParticleOptions().transparencyData,
                            data -> data.multiplyValue(4.0f).multiplyCoefficient(1.5f))
                    .setScaleData(GenericParticleData.create(
                            RandomHelper.randomBetween(level.random, 2.0f, 3.0f) * packet.scale())
                            .build().multiplyCoefficient(1.5f))
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
