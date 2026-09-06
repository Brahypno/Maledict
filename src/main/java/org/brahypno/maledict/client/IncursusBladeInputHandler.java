package org.brahypno.maledict.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.IncursusBladeItem;
import org.brahypno.maledict.network.MaledictNetwork;

@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class IncursusBladeInputHandler {
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.isAttack()
                || event.getHand() != InteractionHand.MAIN_HAND
                || minecraft.player == null
                || !(minecraft.player.getMainHandItem().getItem() instanceof IncursusBladeItem)) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(true);
        MaledictNetwork.sendRadialAttack();
        minecraft.player.resetAttackStrengthTicker();
    }

    private IncursusBladeInputHandler() {
    }
}
