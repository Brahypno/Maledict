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
 * Boss 的一次性网络事件在每个客户端只消费一次：按 key 去重，重复包忽略，后进服的玩家不会补播旧事件。
 */
@OnlyIn(Dist.CLIENT)
public final class FirstVicissitudeClientEvents {
    private static final int DEDUPE_MEMORY = 128;
    /** 这个距离内没有距离衰减，超过 {@link #SHAKE_MAX_DISTANCE} 就完全没有。 */
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
            case VicissitudeEffectPacket.EVENT_SUMMON_CONSUME ->
                    VicissitudeSummoningEffects.consumeTotemLayer(level, position);
            case VicissitudeEffectPacket.EVENT_SUMMON_OPEN -> shake(position, 30, 0.95F, true);
            case VicissitudeEffectPacket.EVENT_SUMMON_EMERGE -> {
                FirstVicissitudeEffects.spawnGatherFlash(level, position, 24);
                shake(position, 100, 1.1F, true);
            }
            case VicissitudeEffectPacket.EVENT_SUMMON_ARRIVE -> {
                FirstVicissitudeEffects.spawnReleaseBurst(level, position, true);
                shake(position, 25, 1.4F, true);
            }
            case VicissitudeEffectPacket.EVENT_RELEASE ->
                    FirstVicissitudeEffects.spawnReleaseBurst(level, position, phaseTwo);
            case VicissitudeEffectPacket.EVENT_TRANSITION_FLASH -> {
                FirstVicissitudeEffects.spawnGatherFlash(level, position, 12);
                shake(position, 16, 1.15F);
            }
            case VicissitudeEffectPacket.EVENT_DEATH_CORE -> {
                FirstVicissitudeEffects.spawnGatherFlash(level, position, 16);
                shake(position, 12, 1.05F);
            }
            case VicissitudeEffectPacket.EVENT_DEATH_EXTINGUISH ->
                    FirstVicissitudeEffects.spawnGatherFlash(level, position, 10);
            case VicissitudeEffectPacket.EVENT_HEAVY_IMPACT -> {
                FirstVicissitudeEffects.spawnReleaseBurst(level, position, true);
                shake(position, 12, 1.25F);
            }

            case VicissitudeEffectPacket.EVENT_UNSTICK ->
                    FirstVicissitudeEffects.spawnGatherFlash(level, position, 8);
            case VicissitudeEffectPacket.EVENT_SCYTHE_CATCH ->
                    FirstVicissitudeEffects.spawnReleaseBurst(level, position, true);
            default -> {
            }
        }
    }

    /** 带位置的短促攻击/衰减震屏，强度乘客户端设置，并由 Lodestone 按距离和朝向衰减。 */
    public static void shake(Vec3 position, int duration, float intensity) {
        shake(position, duration, intensity, false);
    }

    private static void shake(Vec3 position, int duration, float intensity, boolean summoning) {
        float scale = (float) (double) MaledictConfig.SCREENSHAKE_INTENSITY.get();
        if (scale <= 0.0F) {
            return;
        }
        ScreenshakeInstance instance = new PositionedScreenshakeInstance(
                // The elevated portal already sits outside the ordinary eight-block combat range.
                // Lodestone halves near-field strength when facing away, then cubes it and
                // ignores values below 0.1. Combat peaks must exceed 0.93 to remain visible there.
                duration, position, summoning ? 32.0F : SHAKE_FULL_STRENGTH_DISTANCE,
                summoning ? 64.0F : SHAKE_MAX_DISTANCE, Easing.QUAD_IN)
                .setIntensity(intensity * scale * (summoning ? 0.65F : 0.85F), intensity * scale, 0)
                .setEasing(Easing.QUAD_IN, Easing.QUAD_OUT);
        ScreenshakeHandler.addScreenshake(instance);
    }
}
