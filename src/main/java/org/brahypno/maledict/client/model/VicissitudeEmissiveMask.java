package org.brahypno.maledict.client.model;

/** Conservative UV rectangle test; built once from the active resource-pack texture. */
final class VicissitudeEmissiveMask {
    private final int width, height;
    private final int[] sums;

    VicissitudeEmissiveMask(int width, int height, int[] rgba) {
        this.width = width;
        this.height = height;
        sums = new int[(width + 1) * (height + 1)];
        for (int y = 0; y < height; y++) {
            int row = 0;
            for (int x = 0; x < width; x++) {
                // RGB is additive under RenderType.eyes; do not discard zero-alpha RGB.
                if ((rgba[y * width + x] & 0x00ffffff) != 0) row++;
                sums[(y + 1) * (width + 1) + x + 1] = sums[y * (width + 1) + x + 1] + row;
            }
        }
    }

    boolean touches(float minU, float minV, float maxU, float maxV) {
        // One-texel padding includes linear filtering. Keep edge/wrapped UVs conservatively.
        int x0 = (int) Math.floor(minU * width) - 1;
        int y0 = (int) Math.floor(minV * height) - 1;
        int x1 = (int) Math.floor(maxU * width) + 2;
        int y1 = (int) Math.floor(maxV * height) + 2;
        if (x0 < 0 || y0 < 0 || x1 > width || y1 > height) return true;
        int stride = width + 1;
        return sums[y1 * stride + x1] - sums[y0 * stride + x1]
                - sums[y1 * stride + x0] + sums[y0 * stride + x0] > 0;
    }
}
