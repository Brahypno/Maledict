package org.brahypno.maledict.client.item.priestess;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Tints the original model's vertices without changing the body or its shared renderer. */
final class SoulEchoVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float alpha;

    SoulEchoVertexConsumer(VertexConsumer delegate, float alpha) {
        this.delegate = delegate;
        this.alpha = alpha;
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override public VertexConsumer setColor(int red, int green, int blue, int opacity) {
        delegate.setColor((int) (red * 0.55F), (int) (green * 0.95F), (int) (blue * 0.9F), (int) (opacity * alpha));
        return this;
    }

    @Override public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }

}
