package org.brahypno.maledict.client.vfx;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.network.VicissitudeEffectPacket;
import team.lodestar.lodestone.handlers.ScreenshakeHandler;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.screenshake.PositionedScreenshakeInstance;
import team.lodestar.lodestone.systems.screenshake.ScreenshakeInstance;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Consumes the boss' one shot network events exactly once per client. Late joiners never
 * replay past events, and duplicates from repeated packets are ignored by key.
 */
@OnlyIn(Dist.CLIENT)
public final class FirstVicissitudeClientEvents {
    private static final int DEDUPE_MEMORY = 128;
    /** Shake is at full strength within this many blocks and gone past the maximum. */
    private static final float SHAKE_FULL_STRENGTH_DISTANCE = 8.0F;
    private static final float SHAKE_MAX_DISTANCE = 24.0F;
    private static final Set<Long> SEEN = new HashSet<>();
    private static final Deque<Long> SEEN_ORDER = new ArrayDeque<>();

    private FirstVicissitudeClientEvents() {
    }

    public static void accept(VicissitudeEffectPacket packet) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long key = ((long) packet.entityId() << 40)
                   ^ ((long) packet.eventId() << 32)
                   ^ (packet.actionSequence() & 0xFFFFFFFFL);
        if (!SEEN.add(key)) {
            return;
        }
        SEEN_ORDER.addLast(key);
        while (SEEN_ORDER.size() > DEDUPE_MEMORY) {
            SEEN.remove(SEEN_ORDER.removeFirst());
        }

        Entity entity = level.getEntity(packet.entityId());
        Vec3 position = packet.position();
        boolean phaseTwo = entity instanceof org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity boss
                           && boss.isPhaseTwoVisual();
        switch (packet.eventId()) {
            case VicissitudeEffectPacket.EVENT_RELEASE ->
                    FirstVicissitudeEffects.spawnReleaseBurst(level, position, phaseTwo);
            case VicissitudeEffectPacket.EVENT_TRANSITION_FLASH -> {
                FirstVicissitudeEffects.spawnGatherFlash(level, position, 12);
                shake(position, 16, 0.25F);
            }
            case VicissitudeEffectPacket.EVENT_DEATH_CORE -> {
                // Wing roots lose power: the only death shake in the specification.
                FirstVicissitudeEffects.spawnGatherFlash(level, position, 16);
                shake(position, 12, 0.18F);
            }
            case VicissitudeEffectPacket.EVENT_DEATH_EXTINGUISH ->
                    FirstVicissitudeEffects.spawnGatherFlash(level, position, 10);
            case VicissitudeEffectPacket.EVENT_HEAVY_IMPACT -> {
                FirstVicissitudeEffects.spawnReleaseBurst(level, position, true);
                // The one shake the player is meant to feel rather than read: longer and roughly
                // twice the peak of the original 8 tick / 0.15 base.
                shake(position, 12, 0.30F);
            }

            case VicissitudeEffectPacket.EVENT_UNSTICK ->
                    FirstVicissitudeEffects.spawnGatherFlash(level, position, 8);
            case VicissitudeEffectPacket.EVENT_SCYTHE_CATCH ->
                    FirstVicissitudeEffects.spawnReleaseBurst(level, position, true);
            default -> {
            }
        }
    }

    /**
     * Positioned, short attack/decay shake scaled by the client side intensity setting.
     * Full strength inside {@link #SHAKE_FULL_STRENGTH_DISTANCE} blocks and gone past
     * {@link #SHAKE_MAX_DISTANCE}, matching the integration specification.
     */
    public static void shake(Vec3 position, int duration, float intensity) {
        float scale = (float) (double) MaledictConfig.SCREENSHAKE_INTENSITY.get();
        if (scale <= 0.0F) {
            return;
        }
        ScreenshakeInstance instance = new PositionedScreenshakeInstance(
                duration, position, SHAKE_FULL_STRENGTH_DISTANCE, SHAKE_MAX_DISTANCE, Easing.QUAD_IN)
                .setIntensity(intensity * scale)
                .setEasing(Easing.QUAD_IN, Easing.QUAD_OUT);
        ScreenshakeHandler.addScreenshake(instance);
    }
}
