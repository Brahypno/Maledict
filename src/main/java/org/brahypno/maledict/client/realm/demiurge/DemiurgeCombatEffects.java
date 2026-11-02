package org.brahypno.maledict.client.realm.demiurge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

/** Crossed full-bright ribbons for violet beams and falling tears. */
final class DemiurgeCombatEffects {
    static void beam(PoseStack stack, MultiBufferSource buffers, Vec3 from, Vec3 to, float width, boolean core) {
        Vec3 direction = to.subtract(from).normalize();
        Vec3 reference = Math.abs(direction.y) > .9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = direction.cross(reference).normalize().scale(width);
        Vec3 other = direction.cross(side).normalize().scale(width);
        var buffer = buffers.getBuffer(RenderType.lightning());
        ribbon(stack, buffer, from, to, side, core);
        ribbon(stack, buffer, from, to, other, core);
    }

    private static void ribbon(PoseStack stack, VertexConsumer buffer, Vec3 from, Vec3 to, Vec3 side, boolean core) {
        vertex(stack, buffer, from.add(side), core);
        vertex(stack, buffer, to.add(side), core);
        vertex(stack, buffer, to.subtract(side), core);
        vertex(stack, buffer, from.subtract(side), core);
    }

    private static void vertex(PoseStack stack, VertexConsumer buffer, Vec3 point, boolean core) {
        buffer.vertex(stack.last().pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(core ? 220 : 105, core ? 154 : 28, 255, core ? 235 : 85).endVertex();
    }

    private DemiurgeCombatEffects() {}
}
