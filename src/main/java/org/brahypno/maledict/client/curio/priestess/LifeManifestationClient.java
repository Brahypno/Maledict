package org.brahypno.maledict.client.curio.priestess;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.curio.priestess.EidolonOfTheHighPriestessItem;
import org.brahypno.maledict.common.curio.priestess.LifeManifestation;

@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT)
public final class LifeManifestationClient {
    private static LocalPlayer viewer;
    private static boolean active;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
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
