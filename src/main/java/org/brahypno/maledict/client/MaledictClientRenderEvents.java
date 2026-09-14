package org.brahypno.maledict.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.vfx.FirstVicissitudeEffects;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Client side driver for the boss' ground warnings.
 *
 * <p>The candidate list is refreshed once per client tick and the rune particles are spawned from
 * the same pass, so the warning cost never scales with the frame rate. The warnings themselves are
 * pure world space particles: they no longer need a render layer of their own, and they stay
 * visible even when the boss is outside the frustum.
 */
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class MaledictClientRenderEvents {
    private static final double MARKER_SEARCH_RADIUS = 96.0D;
    private static final List<FirstVicissitudeBossEntity> MARKERS = new ArrayList<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MARKERS.clear();
        ClientLevel level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (level == null || player == null) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(MARKER_SEARCH_RADIUS);
        for (FirstVicissitudeBossEntity boss
                : level.getEntitiesOfClass(FirstVicissitudeBossEntity.class, area)) {
            if (boss.isGroundMarkerActive()) {
                MARKERS.add(boss);
            }
        }
        FirstVicissitudeEffects.tickGroundMarkers(MARKERS);
    }

    private MaledictClientRenderEvents() {
    }
}
