package org.brahypno.maledict.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.RemembranceBowItem;

@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RemembranceBowFovHandler {
    @SubscribeEvent
    public static void adjustFovWhileDrawing(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = player.getUseItem();
        if (!player.isUsingItem() || !(stack.getItem() instanceof RemembranceBowItem bow)
                || bow.shouldAutoReleaseAtFullCharge()) {
            return;
        }

        float progress = player.getTicksUsingItem()
                * bow.getDrawSpeedMultiplier(stack) / BowItem.MAX_DRAW_DURATION;
        progress = Mth.clamp(progress, 0.0F, 1.0F);
        progress *= progress;
        event.setNewFovModifier(event.getNewFovModifier() * (1.0F - progress * 0.15F));
    }

    private RemembranceBowFovHandler() {
    }
}
