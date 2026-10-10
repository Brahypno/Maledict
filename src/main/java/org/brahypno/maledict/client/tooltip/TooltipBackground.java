package org.brahypno.maledict.client.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.client.tooltip.TooltipFrameLayout.Insets;

import java.util.Objects;

/** A whole PNG split into nine regions. Transparent pixels may form an irregular silhouette. */
public record TooltipBackground(ResourceLocation texture, int textureWidth, int textureHeight,
                                Insets border, Insets outset, Mode mode, EdgeMode edgeMode,
                                TopDecoration topDecoration) {
    public TooltipBackground(ResourceLocation texture, int textureWidth, int textureHeight,
                             Insets border, Insets outset, Mode mode) {
        this(texture, textureWidth, textureHeight, border, outset, mode, EdgeMode.STRETCH, null);
    }

    public TooltipBackground(ResourceLocation texture, int textureWidth, int textureHeight,
                             Insets border, Insets outset, Mode mode, EdgeMode edgeMode) {
        this(texture, textureWidth, textureHeight, border, outset, mode, edgeMode, null);
    }

    public TooltipBackground {
        Objects.requireNonNull(texture);
        Objects.requireNonNull(border);
        Objects.requireNonNull(outset);
        Objects.requireNonNull(mode);
        Objects.requireNonNull(edgeMode);
        if (textureWidth <= border.left() + border.right()
                || textureHeight <= border.top() + border.bottom()) {
            throw new IllegalArgumentException("Tooltip texture must have a nonempty center region");
        }
    }

    public void render(GuiGraphics graphics, TooltipFrameLayout frame) {
        graphics.pose().pushPose();
        // Color fires before vanilla adds its z=400 translation. Keep artwork behind text/background.
        graphics.pose().translate(0, 0, 399);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            for (TooltipFrameLayout.Slice slice : frame.slices(textureWidth, textureHeight, border,
                    mode == Mode.REPLACE, edgeMode == EdgeMode.TILE)) {
                graphics.blit(texture, slice.x(), slice.y(), slice.width(), slice.height(),
                        (float) slice.u(), (float) slice.v(), slice.sourceWidth(), slice.sourceHeight(),
                        textureWidth, textureHeight);
            }
            if (topDecoration != null) {
                TooltipFrameLayout decoration = frame.topDecoration(outset,
                        topDecoration.width(), topDecoration.height(), topDecoration.overlap());
                graphics.blit(topDecoration.texture(), decoration.x(), decoration.y(),
                        decoration.width(), decoration.height(), 0.0F, 0.0F,
                        topDecoration.textureWidth(), topDecoration.textureHeight(),
                        topDecoration.textureWidth(), topDecoration.textureHeight());
            }
        } finally {
            RenderSystem.disableBlend();
            graphics.pose().popPose();
        }
    }

    public enum Mode {
        /** Draw only the eight outer regions, retaining vanilla background and border colors. */
        DECORATE,
        /** Draw all nine regions and make vanilla background and border transparent. */
        REPLACE
    }

    public enum EdgeMode {
        STRETCH,
        /** Repeat edges at their natural length, cropping the final tile; corners stay fixed. */
        TILE
    }

    /** One fixed-size crest centered on the vanilla background's top edge; never tiled. */
    public record TopDecoration(ResourceLocation texture, int textureWidth, int textureHeight,
                                int width, int height, int overlap) {
        public TopDecoration {
            Objects.requireNonNull(texture);
            if (textureWidth <= 0 || textureHeight <= 0 || width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Tooltip decoration dimensions must be positive");
            }
        }
    }
}
