package org.brahypno.maledict.client.vicissitude;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.vicissitude.music.VicissitudeBossMusic;
import org.brahypno.maledict.client.vicissitude.vfx.FirstVicissitudeEffects;
import org.brahypno.maledict.client.vicissitude.vfx.VicissitudeSummoningEffects;
import org.brahypno.maledict.common.entity.vicissitude.FirstVicissitudeBossEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Boss 地面预警的客户端驱动：候选表每客户端 tick 刷新一次，预警粒子在同一趟里发出，开销不随帧率增长；
 * 粒子是世界空间的，Boss 出了视锥也仍然可见。
 *
 * <p>同一趟扫描顺带驱动 {@link VicissitudeBossMusic}——阶段本来就在同步数据里，
 * 音乐不需要额外的网络包，见那个类的注释。
 */
@EventBusSubscriber(modid = Maledict.MODID,
        value = Dist.CLIENT)
public final class VicissitudeClientTickEvents {
    private static final double MARKER_SEARCH_RADIUS = 96.0D;
    private static final List<FirstVicissitudeBossEntity> MARKERS = new ArrayList<>();
    /** 本 tick 附近的所有无常，交给音乐调度挑最近的一只。 */
    private static final List<FirstVicissitudeBossEntity> BOSSES = new ArrayList<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        MARKERS.clear();
        BOSSES.clear();
        ClientLevel level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (level == null || player == null) {
            VicissitudeSummoningEffects.clear();
            // 退世界时结束音乐；暂停时保留原实例，由原版音频引擎暂停和恢复。
            VicissitudeBossMusic.tick(BOSSES);
            return;
        }
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(MARKER_SEARCH_RADIUS);
        for (FirstVicissitudeBossEntity boss
                : level.getEntitiesOfClass(FirstVicissitudeBossEntity.class, area)) {
            // 先登记再跑特效：特效一旦抛异常，至少音乐这条线不会被连带饿死。
            BOSSES.add(boss);
            FirstVicissitudeEffects.tickEntityEffects(boss);
            if (boss.isGroundMarkerActive()) {
                MARKERS.add(boss);
            }
        }
        VicissitudeBossMusic.tick(BOSSES);
        VicissitudeSummoningEffects.tick(BOSSES);
        FirstVicissitudeEffects.tickGroundMarkers(MARKERS);
    }

    private VicissitudeClientTickEvents() {
    }
}
