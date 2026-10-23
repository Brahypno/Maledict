package org.brahypno.maledict.client.vicissitude;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;

import java.util.List;

/** Wraps long Vicissitude action-bar messages without changing their copy or vanilla lifetime. */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class VicissitudeDialogueOverlay {
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.RECORD_OVERLAY.id())){
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ForgeGui hud = (ForgeGui) minecraft.gui;
        if (minecraft.options.hideGui || hud.overlayMessageTime <= 0){
            return;
        }
        Component message = hud.overlayMessageString;
        if (message == null || !(message.getContents() instanceof TranslatableContents translation)
            || !(translation.getKey().startsWith("message.maledict.first_vicissitude.")
                 || translation.getKey().startsWith("message.maledict.vicissitude_rite."))){
            return;
        }
        int width = event.getWindow().getGuiScaledWidth();
        List<FormattedCharSequence> lines = minecraft.font.split(message, Math.max(1, width - 40));
        if (lines.size() <= 1){
            return;
        }
        event.setCanceled(true);
        int opacity = Mth.clamp((int) ((hud.overlayMessageTime - event.getPartialTick()) * 255 / 20), 0, 255);
        if (opacity <= 8){
            return;
        }
        // Keep the last line at the vanilla action-bar height; additional lines grow upward.
        int height = event.getWindow().getGuiScaledHeight();
        int bottom = height - Math.max(Math.max(hud.leftHeight, hud.rightHeight) + 9, 68) - 4;
        int lineHeight = minecraft.font.lineHeight + 1;
        int y = bottom - (lines.size() - 1) * lineHeight;
        GuiGraphics graphics = event.getGuiGraphics();
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
