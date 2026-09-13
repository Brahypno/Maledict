package org.brahypno.maledict.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.client.vfx.FirstVicissitudeEffects;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Client side world space pass for the boss' ground warnings.
 *
 * <p>Drawing them here instead of inside the entity render layer keeps the maths in plain world
 * coordinates and, more importantly, keeps a warning visible even when the boss itself is off
 * screen. The candidate list is refreshed once per client tick.
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
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
            || MARKERS.isEmpty()) {
            return;
        }
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        FirstVicissitudeEffects.renderGroundMarkers(MARKERS, event.getPartialTick(), poseStack,
                buffers, camera);
        buffers.endBatch();
    }

    private MaledictClientRenderEvents() {
    }
}
