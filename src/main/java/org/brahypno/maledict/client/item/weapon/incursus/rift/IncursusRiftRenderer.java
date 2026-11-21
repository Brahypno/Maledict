package org.brahypno.maledict.client.item.weapon.incursus.rift;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.weapon.incursus.rift.RiftMode;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 裂隙刀痕的绘制时机与顶点输出。
 *
 * <p>两条路径并存，刻意不合并：
 *
 * <ul>
 *   <li><b>普通挥砍的八瓣花</b>沿用旧实现的绘制代码（{@link #emitPetalInstance}）：每瓣是
 *       <b>水平 XZ 平面</b>上的四边形（局部顶点 ±1 落在 X/Z 上，Y 恒 0），绕 Y 轴转过
 *       {@code atan2(direction.x, direction.z) - 90°}，贴图帧与透明度按整个实例求值。
 *       花的形状、相位与节奏沿用旧版。</li>
 *   <li><b>飞腾旋斩、强化上挑、隐藏锋刃</b>走逐道刀光的通用四边形（{@link #emitStroke}）：
 *       两道基向量决定它所在的平面，每道刀痕有自己的出生时刻、寿命与贴图帧。
 *       这些模式对应的是 Malum 的粒子公式，本来就是几百道错峰的独立刀光。</li>
 * </ul>
 *
 * <p>贴图仍是 Malum 的 {@code heavy_slash_00..03}（末帧重复，共 5 帧），刀痕遮罩 +
 * 黑暗虚空 + 多层视差星空由 {@link IncursusRiftShaders} 承担，不额外描边。
 *
 * <p>在 {@code AFTER_PARTICLES} 按遮罩帧提交顶点。普通挥砍沿用深度配置，
 * 技能刀光沿用 Malum 粒子的场景遮挡。
 *
 * <p><b>1.21 的矩阵：这里刻意不做视图旋转。</b>1.20.1 用
 * {@code RenderSystem#getInverseViewRotationMatrix()} 抵消当时挂在全局模型视图栈上的相机旋转；
 * 1.21 删掉了那个字段，而 {@code LevelRenderer#renderLevel} 在实体阶段
 * {@code RenderSystem.getModelViewStack().mul(modelViewMatrix)}，到 {@code AFTER_PARTICLES}
 * 才 pop（1.21.1 {@code LevelRenderer} 第 996 行压栈、第 1235 行弹栈）。也就是说
 * {@code ModelViewMat} 在这一段<b>已经</b>带着相机旋转（自定义 shader 的
 * {@code incursus_rift.vsh} 与它的普通贴图版本都是这么乘的），顶点只要给
 * <b>相机相对、世界朝向</b>的坐标即可，再自己转一次会变成双重旋转。
 * 事件自带的 {@code getPoseStack()} 在这一段是单位阵（同一个 renderLevel 里新建的），
 * 所以这里统一用一个平移矩阵，两套路径都按"世界坐标 - 相机"提交。
 */
@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class IncursusRiftRenderer {
    private static final float DISCARD_ALPHA = 0.004F;
    /**
     * 没有 shader 时的平色：与改造前 {@code custom = false} 分支逐值相同。
     */
    private static final float PLAIN_RED = 0.055F;
    private static final float PLAIN_GREEN = 0.048F;
    private static final float PLAIN_BLUE = 0.075F;

    private static final long STARTED_NANOS = System.nanoTime();

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        List<IncursusRiftInstance> instances = IncursusRiftClientManager.activeInstances();
        if (instances.isEmpty()) {
            return;
        }
        boolean custom = IncursusRiftClientManager.useShader();
        // 1.21 的 partialTick 是 DeltaTracker；取与粒子一致的 tick 内插值。
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (custom) {
            IncursusRiftShaders.setTime((System.nanoTime() - STARTED_NANOS) / 1.0E9F);
            IncursusRiftShaders.setWiden(IncursusRiftClientManager.widen());
            IncursusRiftShaders.setStars(IncursusRiftClientManager.starStrength());
            IncursusRiftShaders.setCamera(event.getCamera().getPosition());
        }

        Matrix4f identity = new Matrix4f();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        boolean depthTest = IncursusRiftClientManager.respectWorldDepth();
        float intensity = IncursusRiftClientManager.intensity();

        int frames = IncursusRiftRenderTypes.frameCount();
        for (int frame = 0; frame < frames; frame++) {
            RenderType meleeType = IncursusRiftRenderTypes.core(frame, custom, depthTest);
            if (meleeType == null) {
                continue;
            }

            // 未注册固定缓冲的 RenderType 共用一个 BufferBuilder。切换前必须先写完当前批次。
            VertexConsumer vertices = buffers.getBuffer(meleeType);
            for (IncursusRiftInstance instance : instances) {
                if (instance.mode() != RiftMode.MELEE_RING || instance.maskFrame(partialTick) != frame) {
                    continue;
                }
                float alpha = instance.alpha(partialTick) * intensity;
                if (alpha <= DISCARD_ALPHA) {
                    continue;
                }
                emitPetalInstance(vertices, identity, camera, instance, partialTick, alpha, custom);
            }
            RenderType skillType = IncursusRiftRenderTypes.core(frame, custom, true);
            if (skillType != meleeType) {
                buffers.endBatch(meleeType);
                vertices = buffers.getBuffer(skillType);
            }
            for (IncursusRiftInstance instance : instances) {
                if (instance.mode() == RiftMode.MELEE_RING) {
                    continue;
                }
                Vec3 anchor = instance.center().subtract(camera);
                for (RiftStroke stroke : instance.strokes()) {
                    float age = stroke.localAge(instance.age(), partialTick);
                    if (!stroke.visibleAt(age) || stroke.maskFrame(age, frames) != frame) {
                        continue;
                    }
                    float fraction = stroke.fraction(age);
                    float alpha = IncursusRiftGeometry.alpha(fraction, stroke.fadeStart()) * intensity;
                    if (alpha <= DISCARD_ALPHA) {
                        continue;
                    }
                    Vec3 center = anchor.add(stroke.offset()).add(stroke.travel(age));
                    emitStroke(vertices, identity, center, stroke, fraction, alpha, custom);
                }
            }
            buffers.endBatch(skillType);
        }
    }

    /**
     * 旧八瓣花：每瓣一个四边形，整实例共用帧号与透明度。
     *
     * <p>位置与尺寸都由实例的伤害半径推出：花瓣中心末帧圆落在半径上，刀痕半边按半径等比缩放。
     * 半径 0（reach 被写成非有限值或负数）时整朵花不画。
     */
    private static void emitPetalInstance(
            VertexConsumer vertices, Matrix4f view, Vec3 camera,
            IncursusRiftInstance instance, float partialTick, float alpha, boolean custom) {
        double radius = instance.radius();
        if (!(radius > 0.0)){
            return;
        }
        float ageFraction = instance.ageFraction(partialTick);
        int lifetimeTicks = instance.lifetimeTicks();
        float speedPerTick = IncursusRiftClientManager.radialSpeed();
        float half = IncursusRiftGeometry.halfExtentForRadius(
                radius, IncursusRiftClientManager.petalHalfSize());
        IncursusRiftPetal[] petals = instance.petals();
        // 花心每帧重取：腰环跟着挥砍者走，不钉在通知下发瞬间的世界坐标上。
        Vec3 center = IncursusRiftClientManager.anchorOf(instance, partialTick);

        Matrix4f matrix = new Matrix4f(view).translate(
                (float) (center.x - camera.x), (float) (center.y - camera.y), (float) (center.z - camera.z));
        for (IncursusRiftPetal petal : petals) {
            double angle = petal.angle();
            double dirX = Math.sin(angle);
            double dirZ = Math.cos(angle);
            double distance = IncursusRiftGeometry.petalDistance(
                    ageFraction, lifetimeTicks, speedPerTick, petal.speedScale(), radius);
            float offsetX = (float) (dirX * distance);
            float offsetZ = (float) (dirZ * distance);

            float petalHalf = half * petal.sizeScale();
            if (petalHalf <= 1.0E-4F) {
                continue;
            }
            // 与 Malum PointyDirectionalParticleBehavior 相同的朝向：水平面内朝外。
            float yaw = (float) (Math.atan2(dirX, dirZ) - Math.PI * 0.5) + petal.spin();
            float cos = (float) Math.cos(yaw);
            float sin = (float) Math.sin(yaw);

            float variant = petals.length <= 1 ? 0.0F : petal.index() / (float) (petals.length - 1);
            float red = custom ? petal.shaderSeed() : PLAIN_RED;
            float green = custom ? ageFraction : PLAIN_GREEN;
            float blue = custom ? variant : PLAIN_BLUE;

            emitPetalCorner(vertices, matrix, offsetX, offsetZ, petalHalf, cos, sin, -1.0F, 1.0F, 1.0F, 1.0F,
                    red, green, blue, alpha);
            emitPetalCorner(vertices, matrix, offsetX, offsetZ, petalHalf, cos, sin, 1.0F, 1.0F, 1.0F, 0.0F,
                    red, green, blue, alpha);
            emitPetalCorner(vertices, matrix, offsetX, offsetZ, petalHalf, cos, sin, 1.0F, -1.0F, 0.0F, 0.0F,
                    red, green, blue, alpha);
            emitPetalCorner(vertices, matrix, offsetX, offsetZ, petalHalf, cos, sin, -1.0F, -1.0F, 0.0F, 1.0F,
                    red, green, blue, alpha);
        }
    }

    /**
     * 旧效果的顶点与 UV 对应关系照抄，避免贴图方向翻掉：
     * 局部 {@code (-1,0,1)} 取 (u1,v1)、{@code (1,0,1)} 取 (u1,v0)、
     * {@code (1,0,-1)} 取 (u0,v0)、{@code (-1,0,-1)} 取 (u0,v1)。
     *
     * <p>绕 Y 轴旋转：{@code x' = x·cos + z·sin}、{@code z' = -x·sin + z·cos}；
     * 四边形的 Y 恒为 0，这正是旧效果"水平腰环"的来源。
     */
    private static void emitPetalCorner(
            VertexConsumer vertices, Matrix4f matrix, float offsetX, float offsetZ,
            float half, float cos, float sin, float localX, float localZ, float u, float v,
            float red, float green, float blue, float alpha) {
        float x = localX * half;
        float z = localZ * half;
        float rotatedX = x * cos + z * sin;
        float rotatedZ = -x * sin + z * cos;
        vertices.addVertex(matrix, rotatedX + offsetX, 0.0F, rotatedZ + offsetZ)
                .setColor(red, green, blue, alpha)
                .setUv(u, v);
    }

    /**
     * 技能模式的一道刀光：任意朝向的四边形，两条基向量都在世界空间里。
     */
    private static void emitStroke(
            VertexConsumer vertices, Matrix4f matrix, Vec3 center, RiftStroke stroke,
            float fraction, float alpha, boolean custom) {
        Vec3 axisU = stroke.axisU().scale(stroke.halfExtent());
        Vec3 axisV = stroke.axisV().scale(stroke.halfExtent());
        // 顶点颜色仍然是旧的三通道约定：r = 星点种子，g = 本道刀痕的进度，b = 批次内的相对序号。
        float red = custom ? stroke.shaderSeed() : PLAIN_RED;
        float green = custom ? fraction : PLAIN_GREEN;
        float blue = custom ? stroke.variant() : PLAIN_BLUE;

        // UV 对应关系与旧 emitPetalCorner 一致：局部 (-1,+1) 取 (1,1)、(1,+1) 取 (1,0)、
        // (1,-1) 取 (0,0)、(-1,-1) 取 (0,1)。写反了刀痕会沿长轴翻过来。
        emitStrokeCorner(vertices, matrix, center, axisU, axisV, -1.0F, 1.0F, 1.0F, 1.0F, red, green, blue, alpha);
        emitStrokeCorner(vertices, matrix, center, axisU, axisV, 1.0F, 1.0F, 1.0F, 0.0F, red, green, blue, alpha);
        emitStrokeCorner(vertices, matrix, center, axisU, axisV, 1.0F, -1.0F, 0.0F, 0.0F, red, green, blue, alpha);
        emitStrokeCorner(vertices, matrix, center, axisU, axisV, -1.0F, -1.0F, 0.0F, 1.0F, red, green, blue, alpha);
    }

    private static void emitStrokeCorner(
            VertexConsumer vertices, Matrix4f matrix, Vec3 center, Vec3 axisU, Vec3 axisV,
            float uSign, float vSign, float u, float v, float red, float green, float blue, float alpha) {
        Vec3 point = center.add(axisU.scale(uSign)).add(axisV.scale(vSign));
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v);
    }

    private IncursusRiftRenderer() {
    }
}
