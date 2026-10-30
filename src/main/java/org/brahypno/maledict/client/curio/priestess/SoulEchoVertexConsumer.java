package org.brahypno.maledict.client.curio.priestess;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Tints the original model's vertices without changing the body or its shared renderer. */
final class SoulEchoVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float alpha;

    SoulEchoVertexConsumer(VertexConsumer delegate, float alpha) {
        this.delegate = delegate;
        this.alpha = alpha;
    }

    @Override public VertexConsumer vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override public VertexConsumer color(int red, int green, int blue, int opacity) {
        delegate.color((int) (red * 0.55F), (int) (green * 0.95F), (int) (blue * 0.9F), (int) (opacity * alpha));
        return this;
    }

    @Override public VertexConsumer uv(float u, float v) {
        delegate.uv(u, v);
        return this;
    }

    @Override public VertexConsumer overlayCoords(int u, int v) {
        delegate.overlayCoords(u, v);
        return this;
    }

    @Override public VertexConsumer uv2(int u, int v) {
        delegate.uv2(u, v);
        return this;
    }

    @Override public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }

    @Override public void endVertex() { delegate.endVertex(); }
    @Override public void defaultColor(int red, int green, int blue, int opacity) {
        delegate.defaultColor((int) (red * 0.55F), (int) (green * 0.95F), (int) (blue * 0.9F), (int) (opacity * alpha));
    }
    @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
}
