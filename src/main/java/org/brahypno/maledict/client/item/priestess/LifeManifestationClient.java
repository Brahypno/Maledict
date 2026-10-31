package org.brahypno.maledict.client.item.priestess;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.curio.EidolonOfTheHighPriestessItem;
import org.brahypno.maledict.common.curio.priestess.LifeManifestation;

@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class LifeManifestationClient {
    private static LocalPlayer viewer;
    private static boolean active;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        viewer = Minecraft.getInstance().player;
        active = viewer != null && viewer.isAlive() && !viewer.isSpectator()
                && EidolonOfTheHighPriestessItem.equippedStage(viewer) >= 1;
    }

    /** Called by vanilla's existing outline predicate, with no allocation or inventory access. */
    public static boolean reveals(Entity candidate) {
        return active && viewer == Minecraft.getInstance().player && LifeManifestation.reveals(viewer, candidate);
    }

    private LifeManifestationClient() {}
}
