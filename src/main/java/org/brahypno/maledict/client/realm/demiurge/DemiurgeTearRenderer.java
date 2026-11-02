package org.brahypno.maledict.client.realm.demiurge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.common.realm.demiurge.DemiurgeTearEntity;

public final class DemiurgeTearRenderer extends EntityRenderer<DemiurgeTearEntity> {
    public DemiurgeTearRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public void render(DemiurgeTearEntity entity, float yaw, float partialTick,
                                 PoseStack stack, MultiBufferSource buffers, int light) {
        DemiurgeCombatEffects.beam(stack, buffers, Vec3.ZERO, new Vec3(0, .85, 0), .07F, false);
        DemiurgeCombatEffects.beam(stack, buffers, Vec3.ZERO, new Vec3(0, .42, 0), .025F, true);
        super.render(entity, yaw, partialTick, stack, buffers, light);
    }

    @Override public ResourceLocation getTextureLocation(DemiurgeTearEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
