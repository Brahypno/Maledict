package org.brahypno.maledict.client.item;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.weapon.bow.RemembranceBowItem;

@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
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
