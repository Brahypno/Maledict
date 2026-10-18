package org.brahypno.maledict.common.curio;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.common.entity.HangmanTrapEntity;
import org.brahypno.maledict.common.item.HangmanFoolItem;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Each player's round snapshots nearby circles in distance order; each projectile spends one turn. */
public final class HangmanFoolRelay {
    public static final double CIRCLE_RANGE = 16.0D;
    public static final double AIM_RANGE = 64.0D;
    private static final String PROCESSED = "maledict:hangman_relay_processed";
    private static final Map<Player, ArrayDeque<UUID>> ROUNDS = new WeakHashMap<>();

    public static void redirect(Projectile projectile) {
        if (projectile.getPersistentData().getBoolean(PROCESSED)
                || !(projectile.getOwner() instanceof Player player)
                || HangmanFoolItem.equippedStage(player) < 3) {
            return;
        }
        HitResult target = aimTarget(player, projectile);
        HangmanTrapEntity circle = nextCircle(player, projectile, target);
        if (circle == null) {
            return;
        }
        Vec3 origin = emissionPosition(circle, projectile);
        Vec3 direction = target.getLocation().subtract(origin).normalize();
        Vec3 velocity = direction.scale(projectile.getDeltaMovement().length());
        projectile.setPos(origin);
        projectile.xo = origin.x;
        projectile.yo = origin.y;
        projectile.zo = origin.z;
        HangmanFoolProjectiles.applyMotion(projectile, direction, false);
        projectile.setDeltaMovement(velocity);
        // Fireball acceleration follows the new velocity; accelerationPower is unchanged.
        projectile.getPersistentData().putBoolean(PROCESSED, true);
    }

    /** Restoring a trap's payload is a contact hit, not another shot from the player. */
    public static void markTrapRelease(Projectile projectile) {
        projectile.getPersistentData().putBoolean(PROCESSED, true);
    }

    private static HangmanTrapEntity nextCircle(Player player, Projectile projectile, HitResult target) {
        var nearby = player.level().getEntitiesOfClass(HangmanTrapEntity.class,
                player.getBoundingBox().inflate(CIRCLE_RANGE), circle -> circle.isAlive()
                        && circle.isOwnedBy(player) && circle.distanceToSqr(player) <= CIRCLE_RANGE * CIRCLE_RANGE
                        && canReach(circle, projectile, target));
        var round = ROUNDS.computeIfAbsent(player, ignored -> new ArrayDeque<>());
        while (!round.isEmpty()) {
            UUID next = round.removeFirst();
            for (HangmanTrapEntity circle : nearby) {
                if (circle.getUUID().equals(next)) {
                    return circle;
                }
            }
        }
        nearby.sort(Comparator.comparingDouble((HangmanTrapEntity circle) -> circle.distanceToSqr(player))
                .thenComparingInt(Entity::getId));
        nearby.forEach(circle -> round.addLast(circle.getUUID()));
        if (nearby.isEmpty()) {
            return null;
        }
        round.removeFirst();
        return nearby.get(0);
    }

    private static Vec3 emissionPosition(HangmanTrapEntity circle, Projectile projectile) {
        Vec3 normal = Vec3.atLowerCornerOf(circle.face().getNormal());
        double halfWidth = projectile.getBbWidth() * 0.5D;
        double halfHeight = projectile.getBbHeight() * 0.5D;
        // Entity positions are at the bottom of their box. Keep the whole shot outside floors, walls and ceilings.
        return circle.position().add(normal.multiply(halfWidth + 0.05D, halfHeight + 0.05D, halfWidth + 0.05D))
                .subtract(0, halfHeight, 0);
    }

    private static boolean canReach(HangmanTrapEntity circle, Projectile projectile, HitResult target) {
        Vec3 origin = emissionPosition(circle, projectile);
        AABB box = projectile.getBoundingBox().move(origin.subtract(projectile.position()));
        if (circle.level().getBlockCollisions(projectile, box).iterator().hasNext()
                || origin.distanceToSqr(target.getLocation()) < 1.0E-12D) {
            return false;
        }
        BlockHitResult obstruction = circle.level().clip(new ClipContext(origin, target.getLocation(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
        // The aimed block is a valid endpoint, even when approached from a different face.
        return obstruction.getType() == HitResult.Type.MISS
                || target.getType() == HitResult.Type.BLOCK && target instanceof BlockHitResult block
                && obstruction.getBlockPos().equals(block.getBlockPos());
    }

    static HitResult aimTarget(Player player, Projectile projectile) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(AIM_RANGE));
        BlockHitResult block = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        var entity = ProjectileUtil.getEntityHitResult(player, eye, block.getLocation(), new AABB(eye, end).inflate(1.0D),
                target -> target != projectile && !target.isSpectator() && target.isPickable()
                        && !player.isPassengerOfSameVehicle(target), eye.distanceToSqr(block.getLocation()));
        return entity == null ? block : entity;
    }

    private HangmanFoolRelay() {
    }
}
