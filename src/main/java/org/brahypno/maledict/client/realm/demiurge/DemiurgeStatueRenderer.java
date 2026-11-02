package org.brahypno.maledict.client.realm.demiurge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.demiurge.DemiurgeStatueEntity;
import org.brahypno.maledict.common.realm.demiurge.DemiurgeAttack;
import net.minecraft.world.phys.Vec3;

/** One opaque, backface-culled pass with the Blender-authored awakening poses. */
public final class DemiurgeStatueRenderer extends EntityRenderer<DemiurgeStatueEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/demiurge_statue.png");
    private final DemiurgeStatueModel model;
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/demiurge_statue_emissive.png");

    public DemiurgeStatueRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new DemiurgeStatueModel();
        shadowRadius = 2.3F;
    }

    @Override
    public void render(DemiurgeStatueEntity entity, float yaw, float partialTick,
                       PoseStack stack, MultiBufferSource buffers, int light) {
        if (!entity.isInvisible()) {
            stack.pushPose();
            float heading = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            Vec3 origin = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()),
                    Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()));
            model.animate(entity, partialTick, heading, origin);
            stack.mulPose(Axis.YP.rotationDegrees(-heading));
            // entityCutout retains default CULL; no duplicate reverse faces or full glow pass.
            model.render(stack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light);
            if (entity.attack() == DemiurgeAttack.LASER || entity.attack() == DemiurgeAttack.ROAR) {
                model.render(stack, buffers.getBuffer(RenderType.eyes(GLOW)), 0xF000F0);
            }
            stack.popPose();
            if (entity.attack() == DemiurgeAttack.LASER) {
                float age = entity.attackAge(partialTick);
                for (int side = 0; side < 2; side++) {
                    Vec3 eye = model.eye(side, origin, heading);
                    Vec3 local = eye.subtract(origin);
                    if (entity.attack().active(age)) {
                        Vec3 end = entity.laserEnd(eye).subtract(origin);
                        float pulse = 1 + .12F * Mth.sin((entity.tickCount + partialTick) * 1.7F);
                        DemiurgeCombatEffects.beam(stack, buffers, local, end, .12F * pulse, false);
                        DemiurgeCombatEffects.beam(stack, buffers, local, end, .035F, true);
                    } else if (age < entity.attack().release) {
                        float size = .03F + .18F * age / entity.attack().release;
                        DemiurgeCombatEffects.beam(stack, buffers, local.add(0, -size, 0), local.add(0, size, 0), size, true);
                    }
                }
            }
        }
        super.render(entity, yaw, partialTick, stack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DemiurgeStatueEntity entity) {
        return TEXTURE;
    }
}
