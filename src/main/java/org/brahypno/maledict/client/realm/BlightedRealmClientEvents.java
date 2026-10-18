package org.brahypno.maledict.client.realm;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.BlightedRealm;

@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlightedRealmClientEvents {
    @SubscribeEvent
    public static void weather(TickEvent.ClientTickEvent event) {
        var level = Minecraft.getInstance().level;
        if (event.phase == TickEvent.Phase.END && level != null && BlightedRealm.is(level)) {
            level.setRainLevel(0);
            level.setThunderLevel(0);
        }
    }

    private BlightedRealmClientEvents() {
    }
}
