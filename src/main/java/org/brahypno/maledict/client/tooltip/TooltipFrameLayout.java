package org.brahypno.maledict.client.tooltip;

import java.util.ArrayList;
import java.util.List;

/** Pixel geometry, independent of the renderer so small and asymmetric frames can be checked. */
public record TooltipFrameLayout(int x, int y, int width, int height) {
    // TooltipRenderUtil adds three pixels of padding and a one-pixel outer silhouette.
    private static final int VANILLA_EXTENT = 4;

    public static TooltipFrameLayout around(int x, int y, int contentWidth, int contentHeight,
                                            Insets outset) {
        return new TooltipFrameLayout(x - VANILLA_EXTENT - outset.left(),
                y - VANILLA_EXTENT - outset.top(),
                contentWidth + 2 * VANILLA_EXTENT + outset.left() + outset.right(),
                contentHeight + 2 * VANILLA_EXTENT + outset.top() + outset.bottom());
    }

    public TooltipFrameLayout topDecoration(Insets outset, int decorationWidth, int decorationHeight,
                                             int overlap) {
        int backgroundWidth = width - outset.left() - outset.right();
        return new TooltipFrameLayout(x + outset.left() + (backgroundWidth - decorationWidth) / 2,
                y + outset.top() + overlap - decorationHeight, decorationWidth, decorationHeight);
    }

    /** Matches GuiGraphics, including its single-component height adjustment. */
    public static Size measure(List<Size> components) {
        int width = 0;
        int height = components.size() == 1 ? -2 : 0;
        for (Size component : components) {
            width = Math.max(width, component.width());
            height += component.height();
        }
        return new Size(width, height);
    }

    /** Stretch edges/center, keep corners fixed; shrink borders if the destination is tiny. */
    public List<Slice> slices(int textureWidth, int textureHeight, Insets border, boolean drawCenter) {
        return slices(textureWidth, textureHeight, border, drawCenter, false);
    }

    public List<Slice> slices(int textureWidth, int textureHeight, Insets border,
                              boolean drawCenter, boolean tileEdges) {
        int left = Math.min(border.left(), width / 2);
        int right = Math.min(border.right(), width / 2);
        int top = Math.min(border.top(), height / 2);
        int bottom = Math.min(border.bottom(), height / 2);
        int[] xs = {x, x + left, x + width - right, x + width};
        int[] ys = {y, y + top, y + height - bottom, y + height};
        int[] us = {0, border.left(), textureWidth - border.right(), textureWidth};
        int[] vs = {0, border.top(), textureHeight - border.bottom(), textureHeight};
        List<Slice> slices = new ArrayList<>(9);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (!drawCenter && row == 1 && column == 1) continue;
                int w = xs[column + 1] - xs[column];
                int h = ys[row + 1] - ys[row];
                int sourceWidth = us[column + 1] - us[column];
                int sourceHeight = vs[row + 1] - vs[row];
                if (w > 0 && h > 0 && sourceWidth > 0 && sourceHeight > 0) {
                    boolean horizontalTile = tileEdges && column == 1 && row != 1;
                    boolean verticalTile = tileEdges && row == 1 && column != 1;
                    int tileWidth = horizontalTile ? sourceWidth : w;
                    int tileHeight = verticalTile ? sourceHeight : h;
                    for (int offsetY = 0; offsetY < h; offsetY += tileHeight) {
                        for (int offsetX = 0; offsetX < w; offsetX += tileWidth) {
                            int drawnWidth = Math.min(tileWidth, w - offsetX);
                            int drawnHeight = Math.min(tileHeight, h - offsetY);
                            slices.add(new Slice(xs[column] + offsetX, ys[row] + offsetY,
                                    drawnWidth, drawnHeight, us[column], vs[row],
                                    horizontalTile ? drawnWidth : sourceWidth,
                                    verticalTile ? drawnHeight : sourceHeight));
                        }
                    }
                }
            }
        }
        return slices;
    }

    public record Size(int width, int height) {}

    /** All inset values are in GUI pixels, in left / top / right / bottom order. */
    public record Insets(int left, int top, int right, int bottom) {
        public Insets {
            if (left < 0 || top < 0 || right < 0 || bottom < 0) {
                throw new IllegalArgumentException("Tooltip insets must be nonnegative");
            }
        }

        public static Insets all(int value) {
            return new Insets(value, value, value, value);
        }
    }

    public record Slice(int x, int y, int width, int height,
                        int u, int v, int sourceWidth, int sourceHeight) {}
}
