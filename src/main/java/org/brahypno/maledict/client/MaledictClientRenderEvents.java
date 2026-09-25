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
 * Boss 地面预警的客户端驱动：候选表每客户端 tick 刷新一次，预警粒子在同一趟里发出，开销不随帧率增长；
 * 粒子是世界空间的，Boss 出了视锥也仍然可见。
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
