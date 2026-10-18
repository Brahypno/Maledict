package org.brahypno.maledict.common.realm;

import com.mojang.logging.LogUtils;
import com.sammy.malum.registry.common.MalumAttachmentTypes;
import com.sammy.malum.core.handlers.WeepingWellRejectionHandler;
import org.brahypno.maledict.network.WellTravelPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

public final class WeepingWellTravel {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String COOLDOWN = "maledict:well_cooldown";
    public static final String EMERGENCE_UNTIL = "maledict:well_emergence_until";
    private static final int COOLDOWN_TICKS = 80;

    /** Return true only when the original rejection has been replaced or a transit cooldown handled it. */
    public static boolean tryTraverse(ServerPlayer player) {
        ServerLevel source = player.serverLevel();
        boolean returning = BlightedRealm.is(source);
        if (!returning && (!source.dimension().equals(Level.OVERWORLD) || !WellAdmission.hasVisionEquipment(player))) {
            return false;
        }
        long now = player.server.overworld().getGameTime();
        if (player.getPersistentData().getLong(COOLDOWN) > now) {
            clearContact(player);
            return true;
        }
        ServerLevel target = player.server.getLevel(returning ? Level.OVERWORLD : BlightedRealm.LEVEL);
        if (target == null) return false;
        try {
            var links = WeepingWellLinks.get(source);
            BlockPos sourceCore = WellPlacement.source(source, player);
            GlobalPos identity = GlobalPos.of(source.dimension(), sourceCore);
            var link = links.find(identity);
            WeepingWellLinks.Endpoint destination;
            if (link == null) {
                var originLanding = WellPlacement.near(source, player, sourceCore.above(2), 8);
                // A liquid-only entrance need not have a rim, but its paired return must still be safe.
                Vec3 home = originLanding.orElseGet(() -> fallback(player, source));
                var origin = new WeepingWellLinks.Endpoint(source.dimension(), sourceCore, home);
                var built = WellPlacement.build(target, player, scaled(sourceCore, returning), links);
                if (built.isEmpty()) return failed(player, returning);
                destination = built.get();
                if (returning) links.pair(destination, origin);
                else links.pair(origin, destination);
            } else {
                destination = link.other(identity);
                if (!WellPlacement.isWell(target.getBlockState(destination.core().pos()))) {
                    var rebuilt = WellPlacement.build(target, player, destination.core().pos(), links);
                    if (rebuilt.isEmpty()) return failed(player, returning);
                    links.replaceEndpoint(link, rebuilt.get());
                    destination = rebuilt.get();
                }
            }
            Vec3 feet = destination.landing();
            if (!WellPlacement.safe(target, player, feet)) {
                Optional<Vec3> nearby = WellPlacement.near(target, player, BlockPos.containing(feet), 8);
                if (nearby.isEmpty()) return failed(player, returning);
                feet = nearby.get();
            }
            var exit = WellPlacement.exit(target, player, destination.core().pos());
            if (exit.isPresent() && feet.distanceTo(exit.get()) < 16) {
                if (!teleport(player, target, exit.get(), true)) return false;
                // Carry the ascent gently toward the verified rim, instead of falling straight back into the entrance.
                Vec3 outward = feet.subtract(exit.get()).multiply(1, 0, 1).normalize().scale(0.28D);
                player.setDeltaMovement(outward.x, player.getDeltaMovement().y, outward.z);
                return true;
            }
            return teleport(player, target, feet, true);
        } catch (RuntimeException exception) {
            LOGGER.error("Could not traverse weeping well for {} from {}", player.getUUID(), source.dimension().location(), exception);
            return failed(player, returning);
        }
    }

    public static BlockPos scaled(BlockPos source, boolean returning) {
        double factor = returning ? BlightedRealm.COORDINATE_SCALE : 1.0D / BlightedRealm.COORDINATE_SCALE;
        return BlockPos.containing(source.getX() * factor, source.getY(), source.getZ() * factor);
    }

    private static boolean failed(ServerPlayer player, boolean returning) {
        if (returning) return teleport(player, player.server.overworld(), fallback(player, player.server.overworld()), false);
        player.displayClientMessage(Component.translatable("message.maledict.well_unavailable"), true);
        return false; // The original Malum rejection is the entry failure fallback.
    }

    private static boolean teleport(ServerPlayer player, ServerLevel level, Vec3 feet, boolean emerge) {
        if (!(emerge ? WellPlacement.clear(level, player, feet) : WellPlacement.safe(level, player, feet))) return false;
        clearContact(player);
        player.stopRiding();
        // This overload supplies vanilla's temporary POST_TELEPORT chunk ticket.
        player.teleportTo(level, feet.x, feet.y, feet.z, Set.of(), player.getYRot(), player.getXRot());
        if (player.serverLevel() != level) return false;
        player.setOnGround(false);
        player.setDeltaMovement(emerge ? new Vec3(0, 0.95D, 0) : Vec3.ZERO);
        player.hurtMarked = true;
        player.fallDistance = 0;
        player.getPersistentData().putLong(COOLDOWN, player.server.overworld().getGameTime() + COOLDOWN_TICKS);
        applyVisualState(player, emerge);
        if (emerge) player.getPersistentData().putLong(EMERGENCE_UNTIL, player.server.overworld().getGameTime() + 100);
        PacketDistributor.sendToPlayer(player, new WellTravelPacket(emerge));
        return true;
    }

    public static void clearContact(Player player) {
        applyVisualState(player, false);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new WellTravelPacket(false));
        }
    }

    /** Both sides use Malum 1.8.2's sixty-tick rejection and contact-dependent ascent. */
    public static void applyVisualState(Player player, boolean emerge) {
        var darkness = player.getData(MalumAttachmentTypes.TOUCH_OF_DARKNESS);
        darkness.touchOfDarkness = emerge ? 100.0F : 0.0F;
        darkness.expectedTouchOfDarkness = 0;
        darkness.touchOfDarknessFalloffDelay = 0;
        var well = player.getData(MalumAttachmentTypes.WEEPING_WELL_INFO);
        well.isInRejectedState = emerge;
        well.voidRejection = emerge ? 60 : 0;
        well.primordialGoopTimer = 0;
        well.isNearWeepingWell = false;
        var gravity = player.getAttribute(Attributes.GRAVITY);
        if (gravity != null) gravity.removeModifier(WeepingWellRejectionHandler.GRAVITY_MODIFIER_ID);
    }

    private static Vec3 fallback(ServerPlayer player, ServerLevel level) {
        if (level.dimension().equals(player.getRespawnDimension()) && player.getRespawnPosition() != null) {
            var spawn = player.findRespawnPositionAndUseSpawnBlock(false,
                    net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING);
            if (!spawn.missingRespawnBlock() && spawn.newLevel() == level
                    && WellPlacement.safe(level, player, spawn.pos())) return spawn.pos();
        }
        BlockPos sharedSpawn = level.getSharedSpawnPos();
        BlockPos spawn = level.getWorldBorder().clampToBounds(sharedSpawn.getX(), sharedSpawn.getY(), sharedSpawn.getZ());
        level.getChunk(spawn);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
        var nearby = WellPlacement.near(level, player, new BlockPos(spawn.getX(), y, spawn.getZ()), 16);
        if (nearby.isPresent()) return nearby.get();
        // A destroyed/obstructed spawn must not strand a returning player. Build a small verified refuge above it.
        BlockPos floor = new BlockPos(spawn.getX(), Math.min(y + 3, level.getMaxBuildHeight() - 4), spawn.getZ());
        for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-1, 0, -1), floor.offset(1, 0, 1))) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            for (int dy = 1; dy <= 3; dy++) level.setBlockAndUpdate(pos.above(dy), Blocks.AIR.defaultBlockState());
        }
        Vec3 refuge = Vec3.atBottomCenterOf(floor.above());
        if (!WellPlacement.safe(level, player, refuge)) throw new IllegalStateException("Cannot prepare safe spawn refuge");
        return refuge;
    }

    private WeepingWellTravel() {
    }
}
