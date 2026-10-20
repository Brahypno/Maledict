package org.brahypno.maledict.client.vicissitude;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;

import java.util.List;

/** Wraps long Vicissitude action-bar messages without changing their copy or vanilla lifetime. */
@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class VicissitudeDialogueOverlay {
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.OVERLAY_MESSAGE)){
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Gui hud = minecraft.gui;
        if (minecraft.options.hideGui || hud.overlayMessageTime <= 0){
            return;
        }
        Component message = hud.overlayMessageString;
        if (message == null || !(message.getContents() instanceof TranslatableContents translation)
            || !translation.getKey().startsWith("message.maledict.first_vicissitude.")){
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        List<FormattedCharSequence> lines = minecraft.font.split(message, Math.max(1, width - 40));
        if (lines.size() <= 1){
            return;
        }
        event.setCanceled(true);
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        int opacity = Mth.clamp((int) ((hud.overlayMessageTime - partialTick) * 255 / 20), 0, 255);
        if (opacity <= 8){
            return;
        }
        // Keep the last line at the vanilla action-bar height; additional lines grow upward.
        int height = graphics.guiHeight();
        int bottom = height - Math.max(Math.max(hud.leftHeight, hud.rightHeight) + 9, 68) - 4;
        int lineHeight = minecraft.font.lineHeight + 1;
        int y = bottom - (lines.size() - 1) * lineHeight;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (FormattedCharSequence line : lines) {
            graphics.drawString(minecraft.font, line, (width - minecraft.font.width(line)) / 2,
                                y, 0xFFFFFF | (opacity << 24), true);
            y += lineHeight;
        }
        RenderSystem.disableBlend();
    }

    private VicissitudeDialogueOverlay() {
    }
}
