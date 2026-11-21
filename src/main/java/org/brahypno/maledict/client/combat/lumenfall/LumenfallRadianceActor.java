package org.brahypno.maledict.client.combat.lumenfall;

import com.sammy.malum.visual_effects.WeepingWellParticleEffects;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleTypes;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.network.particle.NetworkedParticleEffectPositionData;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.render_types.LodestoneWorldParticleRenderType;
import team.lodestar.lodestone.systems.particle.world.LodestoneWorldParticle;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

/**
 * 哭泣之井放射光晕的「可调强度」版本，<b>只在客户端加载</b>。
 *
 * <p>原样照抄 Malum 那发写死强度的光晕（它用的是 {@code WeepingWellParticleEffects} 的
 * sparks / specs / square 三个公开构建器），把里面写死的量换成按 {@code strength} 缩放，
 * 其余动作、颜色渐变、渲染类型都保持一致：
 * <ul>
 *   <li>主爆发次数 64 → {@code round(64 × strength)}</li>
 *   <li>每束光的缩放倍率 1.5 → {@code 1.0 + 0.5 × strength}</li>
 *   <li>中心大光点 3 圈 × 每圈 5 → {@code max(1, round(3 × strength))} 圈 ×
 *       {@code max(1, round(5 × strength))}</li>
 *   <li>方块状光片 27 → {@code round(27 × strength)}</li>
 * </ul>
 * {@code strength = 1} 与 Malum 原本那一发等价，{@code 0} 什么都不画。配色是沉辉铋的浅虹彩三色
 * （蓝紫银主色、虹彩粉紫折光、青绿边缘反光），照 Malum 原来的三色轮转依次铺开 —— 击杀那一下就会
 * 呈现折光而不是一片灰白。
 *
 * <p>另有一个独立于强度的 {@code radius}：<b>空间铺开范围</b>的倍率。强度只管粒子多少与大小，
 * 范围管「飞多远、铺多开」—— 定时释放是单体小号效果，不能像群伤散辉那样铺开，所以给它传一个
 * 远小于 1 的值。范围同时会缩掉三样东西：粒子初速、环形光点的随机偏移，以及中心那几片大方形光片
 * （它们是固定尺寸的构件，只属于完整尺寸的爆发，{@code radius} 小于 0.9 时直接不放）。
 *
 * <p>1.8.2 侧的两处改写：Malum 那三个构建器收的参数没变，但 Lodestone 1.8.2 去掉了
 * {@code setDiscardFunction(ENDING_CURVE_INVISIBLE)}（整类 {@code ParticleDiscardFunctionType} 都没了），
 * 光点的透明度本来就收到 0，少了这条只是粒子会挂着不可见的剩余寿命，观感不变；
 * {@code setGravityStrength} 也更名为 {@code setGravity}。
 */
public final class LumenfallRadianceActor {
    /**
     * 沉辉铋的虹彩三色：主色 → 折光 → 边缘反光，按 Malum 原本的三色轮转依次使用。
     */
    private static final List<Color> COLORS = List.of(
            new Color(0xA9B8EF),
            new Color(0xE29BDA),
            new Color(0x76D6D0));

    public static void spawn(
            Level level, RandomSource rand, NetworkedParticleEffectPositionData position, float strength,
            float radius) {
        if (strength <= 0.0F || radius <= 0.0F) {
            return;
        }
        double posX = position.getPosX();
        double posY = position.getPosY();
        double posZ = position.getPosZ();
        Vec3 pos = new Vec3(posX, posY, posZ);
        Consumer<LodestoneWorldParticle> spawnBehavior = particle -> particle.tick(2);
        float scaleFactor = 1.0F + 0.5F * strength;

        int bursts = Math.round(64 * strength);
        for (int i = 0; i < bursts; i++) {
            Color color = COLORS.get((i % 12) / 4);
            ColorParticleData colorData = ColorParticleData.create(color.brighter(), color)
                                                           .setCoefficient(0.5F)
                                                           .build();
            // 初速直接决定粒子飞多远：乘上范围倍率，单体释放就只在目标身上一小团。
            float xVelocity = RandomHelper.randomBetween(rand, 0.0F, 0.15F) * (rand.nextBoolean() ? 1 : -1) * radius;
            float yVelocity = RandomHelper.randomBetween(rand, 0.5F, 1.0F) * radius;
            float zVelocity = RandomHelper.randomBetween(rand, 0.0F, 0.15F) * (rand.nextBoolean() ? 1 : -1) * radius;
            float gravityStrength = RandomHelper.randomBetween(rand, 0.75F, 1.0F);
            if (rand.nextFloat() < 0.85F) {
                var sparks = WeepingWellParticleEffects.weepingWellSparks(
                        level, pos, colorData, LodestoneWorldParticleRenderType.ADDITIVE);
                sparks.getBuilder()
                      .addSpawnActor(spawnBehavior)
                      .disableNoClip()
                      .setGravity(gravityStrength / 2.0F)
                      .setMotion(xVelocity, yVelocity, zVelocity)
                      .modifyTransparencyData(data -> data.multiplyValue(2.0F))
                      .modifyScaleData(data -> data.multiplyValue(scaleFactor));
                sparks.getBloomBuilder()
                      .addSpawnActor(spawnBehavior)
                      .disableNoClip()
                      .setGravity(gravityStrength / 2.0F)
                      .setMotion(xVelocity, yVelocity, zVelocity)
                      .modifyTransparencyData(data -> data.multiplyValue(1.25F));
                sparks.spawnParticles();
            }
            if (rand.nextFloat() < 0.85F) {
                xVelocity *= 1.25F;
                yVelocity *= 0.75F;
                zVelocity *= 1.25F;
                var specs = WeepingWellParticleEffects.weepingWellSpecs(
                        level, pos, colorData, LodestoneWorldParticleRenderType.ADDITIVE);
                specs.getBuilder()
                     .addSpawnActor(spawnBehavior)
                     .disableNoClip()
                     .setGravity(gravityStrength)
                     .setMotion(xVelocity, yVelocity, zVelocity)
                     .modifyScaleData(data -> data.multiplyValue(scaleFactor));
                specs.getBloomBuilder()
                     .addSpawnActor(spawnBehavior)
                     .disableNoClip()
                     .setGravity(gravityStrength)
                     .setMotion(xVelocity, yVelocity, zVelocity)
                     .modifyTransparencyData(data -> data.multiplyValue(1.25F));
                specs.spawnParticles();
            }
        }

        int spinOffset = rand.nextInt(360);
        int rings = Math.max(1, Math.round(3 * strength));
        int perRing = Math.max(1, Math.round(5 * strength));
        for (int i = 0; i < rings; i++) {
            int lifeDelay = 4 * i;
            Color color = COLORS.get((int) ((level.getGameTime() % 3 + i) % 3));
            ColorParticleData colorData = ColorParticleData.create(color.brighter(), color)
                                                           .setCoefficient(0.5F)
                                                           .build();
            int spinDirection = rand.nextBoolean() ? 1 : -1;
            WorldParticleBuilder.create(LodestoneParticleTypes.SPARKLE_PARTICLE)
                                .setTransparencyData(GenericParticleData.create(0.1F, 0.4F, 0.0F)
                                                                        .setEasing(Easing.QUAD_IN, Easing.CIRC_IN)
                                                                        .build())
                                .setSpinData(SpinParticleData.create((0.05F + rand.nextFloat() * 0.025F) * spinDirection)
                                                             .setSpinOffset(spinOffset)
                                                             .build())
                                .setScaleData(GenericParticleData.create(3.0F * strength, 5.0F * strength, 0.6F * strength)
                                                                 .setEasing(Easing.QUARTIC_OUT, Easing.SINE_IN)
                                                                 .build())
                                .setColorData(colorData)
                                .setLifeDelay(lifeDelay)
                                .setLifetime(25 + lifeDelay)
                                .setRandomOffset(0.6F * radius)
                                .enableNoClip()
                                .setRandomMotion(0.02F, 0.02F)
                                .repeat(level, posX, posY + 0.25F, posZ, perRing);
        }

        // 方形光片是固定尺寸的构件（约 1.7–1.8 倍缩放、贴在一点上），没法按倍率缩；
        // 它是完整尺寸爆发的一部分，单体小号版不要它，否则中心又是一大团。
        if (radius < 0.9F) {
            return;
        }
        int squares = Math.round(27 * strength);
        for (int i = 0; i < squares; i++) {
            int delay = 4 + i;
            Color color = COLORS.get(((int) (level.getGameTime() - 27 + i) % 27) / 9);
            ColorParticleData colorData = ColorParticleData.create(color.brighter(), color)
                                                           .setCoefficient(0.5F)
                                                           .build();
            var square = WeepingWellParticleEffects.weepingWellSquare(level, pos, colorData);
            square.getBuilder().addSpawnActor(particle -> particle.tick(delay));
            square.spawnParticles();
        }
    }

    private LumenfallRadianceActor() {
    }
}
