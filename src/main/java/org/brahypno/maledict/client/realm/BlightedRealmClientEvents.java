package org.brahypno.maledict.client.realm;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.BlightedRealm;

@EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class BlightedRealmClientEvents {
    @SubscribeEvent
    public static void weather(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        if (level != null && BlightedRealm.is(level)) {
            level.setRainLevel(0);
            level.setThunderLevel(0);
        }
    }

    private BlightedRealmClientEvents() {
    }
}
