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

/** Opaque carved stone with masked, pulsing violet emission that dies out with the statue. */
public final class DemiurgeStatueRenderer extends EntityRenderer<DemiurgeStatueEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/demiurge_statue.png");
    private final DemiurgeStatueModel model;
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(
            Maledict.MODID, "textures/entity/demiurge_statue_emissive.png");
    /** Authored death timing: violet out, black stone, then dust. */
    private static final float BLACKEN_TICKS = 40.0F;
    private static final float CRUMBLE_START = 38.0F;
    private static final float CRUMBLE_END = 76.0F;

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
            float deathTicks = entity.getDeathTicks(partialTick);
            float tint = 1.0F, glowScale = 1.0F, dissolve = 0.0F;
            if (deathTicks >= 0.0F) {
                // The purple goes out first, then the stone turns pitch black, then it flakes away.
                glowScale = 1.0F - smooth(0.0F, DemiurgeStatueEntity.DEATH_GLOW_OUT, deathTicks);
                tint = 1.0F - .95F * smooth(0.0F, BLACKEN_TICKS, deathTicks);
                dissolve = smooth(CRUMBLE_START, CRUMBLE_END, deathTicks);
            }
            if (dissolve < 1.0F) {
                model.render(stack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, tint, dissolve);
            }
            float awakeningAge = entity.awakeningAge(partialTick);
            float glow = DemiurgeGlow.strength(awakeningAge, entity.attack(), entity.attackAge(partialTick)) * glowScale;
            if (glow > 0) {
                model.renderEmissive(stack, buffers.getBuffer(RenderType.eyes(GLOW)), 0xF000F0, glow, awakeningAge);
            }
            stack.popPose();
            if (deathTicks < 0 && entity.attack() == DemiurgeAttack.LASER) {
                float age = entity.attackAge(partialTick);
                for (int side = 0; side < 2; side++) {
                    Vec3 eye = model.eye(side, origin, heading);
                    Vec3 local = eye.subtract(origin);
                    if (entity.attack().active(age)) {
                        Vec3 end = entity.laserEnd(eye, model.headForward(heading)).subtract(origin);
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

    private static float smooth(float start, float end, float value) {
        float x = Mth.clamp((value - start) / (end - start), 0.0F, 1.0F);
        return x * x * (3 - 2 * x);
    }

    @Override
    public ResourceLocation getTextureLocation(DemiurgeStatueEntity entity) {
        return TEXTURE;
    }
}
