package org.brahypno.maledict.client.item.weapon.incursus.rift;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 裂隙花的绘制时机与顶点输出。
 *
 * <p>几何完全沿用旧效果：每瓣是**水平 XZ 平面**上的四边形（局部顶点 ±1 落在 X/Z 上，Y 恒 0），
 * 绕 Y 轴转过 {@code atan2(direction.x, direction.z) - 90°}，于是贴图的 U 轴指向这瓣的外扩方向；
 * 四边形半边长就是旧效果的 {@code quadSize}（3.0–4.5 格），刀痕贴图的可见笔画只占其中一小条。
 * 写成面向相机的公告板会让八片糊到正面，和旧花完全不同。
 *
 * <p>在 {@code AFTER_PARTICLES} 画一遍：裂口内部的内容。轮廓由刀痕遮罩本身定义，
 * 不再额外描边，否则会多出一圈与"划开帷幕"冲突的颜色。
 *
 * <p>花心取 {@link IncursusRiftClientManager#anchorOf} 的实时腰部坐标，不是通知里的世界坐标快照：
 * 旧粒子天生在世界里不动，而腰环属于挥砍者，钉死在快照上会在跳跃、击退里变成远处一块黑。
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IncursusRiftRenderer {
    private static final float DISCARD_ALPHA = 0.004F;

    private static final long STARTED_NANOS = System.nanoTime();

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES){
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null){
            return;
        }
        List<IncursusRiftInstance> instances = IncursusRiftClientManager.activeInstances();
        if (instances.isEmpty()){
            return;
        }
        boolean custom = IncursusRiftClientManager.useShader();
        float partialTick = event.getPartialTick();
        if (custom){
            IncursusRiftShaders.setTime((System.nanoTime() - STARTED_NANOS) / 1.0E9F);
            IncursusRiftShaders.setWiden(IncursusRiftClientManager.widen());
            IncursusRiftShaders.setStars(IncursusRiftClientManager.starStrength());
            IncursusRiftShaders.setCamera(event.getCamera().getPosition());
        }

        PoseStack poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        boolean depthTest = IncursusRiftClientManager.respectWorldDepth();

        for (int frame = 0; frame < IncursusRiftRenderTypes.frameCount(); frame++) {
            RenderType type = IncursusRiftRenderTypes.core(frame, custom, depthTest);
            if (type == null){
                continue;
            }
            VertexConsumer vertices = buffers.getBuffer(type);
            for (IncursusRiftInstance instance : instances) {
                if (instance.maskFrame(partialTick) != frame){
                    continue;
                }
                float alpha = instance.alpha(partialTick) * IncursusRiftClientManager.intensity();
                if (alpha <= DISCARD_ALPHA){
                    continue;
                }
                emitInstance(vertices, poses, camera, instance, partialTick, alpha, custom);
            }
            buffers.endBatch(type);
        }
    }

    private static void emitInstance(
            VertexConsumer vertices, PoseStack poses, Vec3 camera,
            IncursusRiftInstance instance, float partialTick, float alpha, boolean custom) {
        float ageFraction = instance.ageFraction(partialTick);
        float instanceScale = instance.scale();
        float travel = IncursusRiftGeometry.radialDistance(
                ageFraction, instance.lifetimeTicks(), IncursusRiftClientManager.radialSpeed());
        float root = (float) (IncursusRiftGeometry.ROOT_RADIUS * instanceScale);
        float baseHalf = IncursusRiftClientManager.petalHalfSize();
        IncursusRiftPetal[] petals = instance.petals();
        // 花心每帧重取：腰环跟着挥砍者走，不钉在通知下发瞬间的世界坐标上。
        Vec3 center = IncursusRiftClientManager.anchorOf(instance, partialTick);

        poses.pushPose();
        poses.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
        Matrix4f matrix = poses.last().pose();
        for (IncursusRiftPetal petal : petals) {
            double angle = petal.angle();
            double dirX = Math.sin(angle);
            double dirZ = Math.cos(angle);
            float distance = root + travel * instanceScale * petal.speedScale();
            float offsetX = (float) (dirX * distance);
            float offsetZ = (float) (dirZ * distance);

            float half = baseHalf * petal.sizeScale() * instanceScale;
            if (half <= 1.0E-4F){
                continue;
            }
            // 与 Malum PointyDirectionalParticleBehavior 相同的朝向：水平面内朝外。
            float yaw = (float) (Math.atan2(dirX, dirZ) - Math.PI * 0.5) + petal.spin();
            float cos = (float) Math.cos(yaw);
            float sin = (float) Math.sin(yaw);

            float variant = petals.length <= 1 ? 0.0F : petal.index() / (float) (petals.length - 1);
            float red = custom ? petal.shaderSeed() : 0.055F;
            float green = custom ? ageFraction : 0.048F;
            float blue = custom ? variant : 0.075F;

            emitCorner(vertices, matrix, offsetX, offsetZ, half, cos, sin, -1.0F, 1.0F, 1.0F, 1.0F,
                       red, green, blue, alpha);
            emitCorner(vertices, matrix, offsetX, offsetZ, half, cos, sin, 1.0F, 1.0F, 1.0F, 0.0F,
                       red, green, blue, alpha);
            emitCorner(vertices, matrix, offsetX, offsetZ, half, cos, sin, 1.0F, -1.0F, 0.0F, 0.0F,
                       red, green, blue, alpha);
            emitCorner(vertices, matrix, offsetX, offsetZ, half, cos, sin, -1.0F, -1.0F, 0.0F, 1.0F,
                       red, green, blue, alpha);
        }
        poses.popPose();
    }

    /**
     * 旧效果的顶点与 UV 对应关系照抄，避免贴图方向翻掉：
     * 局部 {@code (-1,0,1)} 取 (u1,v1)、{@code (1,0,1)} 取 (u1,v0)、
     * {@code (1,0,-1)} 取 (u0,v0)、{@code (-1,0,-1)} 取 (u0,v1)。
     *
     * <p>绕 Y 轴旋转：{@code x' = x·cos + z·sin}、{@code z' = -x·sin + z·cos}；
     * 四边形的 Y 恒为 0，这正是旧效果"水平腰环"的来源。
     */
    private static void emitCorner(
            VertexConsumer vertices, Matrix4f matrix, float offsetX, float offsetZ,
            float half, float cos, float sin, float localX, float localZ, float u, float v,
            float red, float green, float blue, float alpha) {
        float x = localX * half;
        float z = localZ * half;
        float rotatedX = x * cos + z * sin;
        float rotatedZ = -x * sin + z * cos;
        vertices.vertex(matrix, rotatedX + offsetX, 0.0F, rotatedZ + offsetZ)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .endVertex();
    }

    private IncursusRiftRenderer() {
    }
}
