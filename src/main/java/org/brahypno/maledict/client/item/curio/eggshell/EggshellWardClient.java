package org.brahypno.maledict.client.item.curio.eggshell;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.changelib.HealthHelper.client.ClientHitFeedback;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.curio.eggshell.EmperorInTheEggshellItem;

import java.util.IdentityHashMap;
import java.util.Map;

/** One presentation per wearer, shared by the HUD and world renderers. */
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EggshellWardClient {
    private static final Map<Player, Pulse> PULSES = new IdentityHashMap<>();

    public static void register() {
        ClientHitFeedback.register(EmperorInTheEggshellItem.WARD_KEY, EggshellWardClient::onHitSettled);
    }

    private static void onHitSettled(ClientHitFeedback.Report report) {
        if (!(report.victim() instanceof Player player)) {
            return;
        }
        PULSES.computeIfAbsent(player, wearer -> new Pulse(wearer.getBbWidth(), wearer.getBbHeight()))
              .hit(player.level().getGameTime(), direction(player, report), player.yBodyRot,
                   strength(report.requested(), report.dealt()));
    }

    static float strength(float requested, float dealt) {
        // The rhythm's write-time reduction can leave admitted == requested.
        if (!(requested > 0.0F)) {
            return 0.0F;
        }
        float blockedShare = 1.0F - dealt / requested;
        return Float.isNaN(blockedShare) ? 0.0F : Mth.sqrt(Mth.clamp(blockedShare, 0.0F, 1.0F));
    }

    private static Vec3 direction(Player player, ClientHitFeedback.Report report) {
        var source = player.level().getEntity(report.attackerId());
        if (source == null || source.isRemoved() || source == player) {
            return Vec3.ZERO;
        }
        Vec3 offset = source.getBoundingBox().getCenter().subtract(report.center());
        // A projectile may already be inside the victim when the packet arrives.
        if (source instanceof Projectile && offset.horizontalDistanceSqr() < 0.25) {
            Vec3 incoming = source.getDeltaMovement().reverse();
            if (incoming.horizontalDistanceSqr() > 1.0E-4) {
                offset = incoming;
            }
        }
        return offset.horizontalDistanceSqr() >= 0.01 ? new Vec3(offset.x, 0, offset.z).normalize() : Vec3.ZERO;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PULSES.isEmpty()) {
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level == null) {
            PULSES.clear();
            return;
        }
        long now = level.getGameTime();
        PULSES.entrySet().removeIf(entry -> entry.getKey().isRemoved() || entry.getKey().level() != level
                                            || entry.getValue().expired(now));
        PULSES.forEach((wearer, pulse) -> pulse.updateSize(wearer.getBbWidth(), wearer.getBbHeight()));
    }

    static Map<Player, Pulse> pulses() {
        return PULSES;
    }

    static final class Pulse {
        private static final int FULL_COOLDOWN = 16;
        private static final int ACCENT_COOLDOWN = 3;
        private static final float FULL_DURATION = 11.0F;
        private static final float ACCENT_DURATION = 5.0F;

        private long fullTick = -FULL_COOLDOWN;
        private long accentTick = -ACCENT_COOLDOWN;
        private float fullStrength;
        private float accentStrength;
        private Vec3 direction = Vec3.ZERO;
        private float shellBearing = Float.NaN;
        private float previousWidth;
        private float width;
        private float previousHeight;
        private float height;

        Pulse(float width, float height) {
            this.previousWidth = this.width = width;
            this.previousHeight = this.height = height;
        }

        void updateSize(float width, float height) {
            previousWidth = this.width;
            previousHeight = this.height;
            this.width = Mth.lerp(0.65F, this.width, width);
            this.height = Mth.lerp(0.65F, this.height, height);
        }

        float width(float partialTick) {
            return Mth.lerp(partialTick, previousWidth, width);
        }

        float height(float partialTick) {
            return Mth.lerp(partialTick, previousHeight, height);
        }

        void hit(long tick, Vec3 direction, float bodyYaw, float strength) {
            if (tick - fullTick >= FULL_COOLDOWN) {
                fullTick = tick;
                fullStrength = strength;
                shellBearing = direction.lengthSqr() == 0 ? Float.NaN
                        : Mth.wrapDegrees((float) Mth.atan2(-direction.x, direction.z) * Mth.RAD_TO_DEG - bodyYaw);
            } else if (tick - accentTick < ACCENT_COOLDOWN) {
                return;
            }
            accentTick = tick;
            accentStrength = strength;
            this.direction = direction;
        }

        Vec3 direction() {
            return direction;
        }

        float shellBearing() {
            return shellBearing;
        }

        float accentStrength() {
            return accentStrength;
        }

        float closure(long tick, float partialTick) {
            float progress = Mth.clamp(fullAge(tick, partialTick) / 4.0F, 0.0F, 1.0F);
            return progress * progress * (3.0F - 2.0F * progress);
        }

        float fullAge(long tick, float partialTick) {
            return tick - fullTick + partialTick;
        }

        float fullAlpha(long tick, float partialTick) {
            float age = fullAge(tick, partialTick);
            float fadeIn = Mth.clamp((age + 0.65F) / 2.35F, 0.0F, 1.0F);
            return Mth.lerp(fullStrength, 0.32F, 0.64F) * fadeIn * fade(age, 4.0F, FULL_DURATION);
        }

        float accentAlpha(long tick, float partialTick) {
            return Mth.lerp(accentStrength, 0.15F, 1.0F) * fade(accentAge(tick, partialTick), 1.0F, ACCENT_DURATION);
        }

        float accentAge(long tick, float partialTick) {
            return tick - accentTick + partialTick;
        }

        boolean expired(long tick) {
            return tick - fullTick >= FULL_COOLDOWN && tick - accentTick >= ACCENT_DURATION;
        }

        private static float fade(float age, float start, float end) {
            float remaining = Mth.clamp((end - age) / (end - start), 0.0F, 1.0F);
            return remaining * remaining;
        }
    }

    private EggshellWardClient() {
    }
}
