package org.brahypno.maledict.common.realm.wither;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.brahypno.maledict.registry.MaledictEntities;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Server-authoritative skill scheduler for the Weeping Wither. The controller owns the phase flags,
 * cooldowns and the single active cast; the entity supplies movement, targets and skull spawning.
 */
public final class WeepingWitherSkillController {
    public static final int OSSUARY_COOLDOWN = 14 * 20;
    public static final int OSSUARY_COOLDOWN_FINAL = 10 * 20;
    public static final int VOLLEY_COOLDOWN = 20 * 20;
    public static final int VOLLEY_COOLDOWN_FINAL = 15 * 20;
    public static final int PULSE_COOLDOWN = 12 * 20;
    public static final int PULSE_COOLDOWN_FINAL = 9 * 20;
    public static final int ECHO_COOLDOWN = 30 * 20;
    public static final int ECHO_COOLDOWN_FINAL = 24 * 20;
    public static final int BLIGHT_CLOUD_COOLDOWN = 6 * 20;
    public static final int BLIGHT_CLOUD_COOLDOWN_FINAL = 4 * 20;
    public static final int GLOBAL_INTERVAL = 2 * 20;
    public static final int GLOBAL_INTERVAL_FINAL = 30;
    public static final int OSSUARY_WINDUP = 16;
    public static final int VOLLEY_WINDUP = 16;
    public static final int VOLLEY_ROUND_GAP = 8;
    public static final int VOLLEY_CAST_TICKS = VOLLEY_WINDUP + 2 * VOLLEY_ROUND_GAP;
    public static final int PULSE_WINDUP = 12;
    public static final int ECHO_WINDUP = 10;
    public static final int ECHO_SUMMON_LIFETIME = 30 * 20;
    public static final int MAX_SUMMONS = 5;
    public static final int PULSE_RADIUS = 7;
    public static final float PULSE_DAMAGE = 4.0F;
    public static final double PULSE_KNOCKBACK = 1.5D;
    private static final int HALF_HEALTH_SUMMONS = 3;
    private static final int COMBAT_START_TICKS = 8 * 20;
    /** The ordinary main-head danger roll, used per skull by the pre-half volley. */
    private static final float PRE_HALF_VOLLEY_DANGEROUS_CHANCE = 0.001F;
    private static final DustParticleOptions BONE_DUST =
            new DustParticleOptions(new Vector3f(0.72F, 0.69F, 0.63F), 0.7F);
    private static final DustParticleOptions SEEP_DUST =
            new DustParticleOptions(new Vector3f(0.32F, 0.18F, 0.36F), 0.7F);
    private static final String CLOUD_TAG = "WeepingBlightCloud";

    private enum Cast {
        NONE, OSSUARY, VOLLEY, PULSE, ECHO
    }

    private final WeepingWitherEntity wither;
    private Cast cast = Cast.NONE;
    private int castTicks;
    private int volleyDangerMask;
    private boolean volleyEmpowered;
    @Nullable private BlockPos ossuaryGround;
    private boolean ossuaryRing;
    private int ossuaryCooldown;
    private int volleyCooldown;
    private int pulseCooldown;
    private int echoCooldown;
    private int globalCooldown;
    private int blightCloudCooldown;
    private int combatTicks;
    private int closeTicks;
    private boolean validTarget;
    private boolean halfHealthEmpowered;
    private boolean halfHealthSummonUsed;

    WeepingWitherSkillController(WeepingWitherEntity wither) {
        this.wither = wither;
    }

    public void tick() {
        if (wither.level().isClientSide) return;
        tickCooldowns();
        evaluateHalfHealth();
        if (wither.isReforming() || wither.isDeadOrDying() || wither.getInvulnerableTicks() > 0) {
            cancelCast();
            return;
        }
        updateTargetState();
        if (cast != Cast.NONE) {
            if (cast == Cast.OSSUARY || cast == Cast.VOLLEY) {
                int windup = cast == Cast.OSSUARY ? OSSUARY_WINDUP : VOLLEY_WINDUP;
                if (castTicks < windup && canStartPulse()) {
                    // The pulse is the only skill allowed to pre-empt an uncommitted wind-up.
                    cast = Cast.NONE;
                    castTicks = 0;
                    ossuaryGround = null;
                    ossuaryRing = false;
                    startPulse();
                    return;
                }
            }
            tickCast();
            return;
        }
        if (globalCooldown > 0 || !validTarget) return;
        if (canStartPulse()) startPulse();
        else if (canStartEcho()) startEcho();
        else if (canStartOssuary()) startOssuary();
        else if (canStartVolley()) startVolley();
    }

    /** Danger-skull clouds are passive and never take part in the A/C/D/E mutual exclusion. */
    public void onDangerousSkullBlockHit(BlockHitResult hit) {
        if (wither.level().isClientSide || wither.isReforming() || wither.isDeadOrDying()
                || wither.getInvulnerableTicks() > 0 || blightCloudCooldown > 0) {
            return;
        }
        if (!halfHealthEmpowered && wither.getRandom().nextFloat() >= 0.25F) return;
        Vec3 center = cloudCenter(hit.getBlockPos(), hit.getDirection());
        if (center == null || hasNearbyCloud(center)) return;
        spawnBlightCloud(center);
        blightCloudCooldown = wither.isFinalPhase() ? BLIGHT_CLOUD_COOLDOWN_FINAL : BLIGHT_CLOUD_COOLDOWN;
    }

    public boolean isEmpowered() {
        return halfHealthEmpowered;
    }

    public boolean suppressesSkullShots() {
        return cast == Cast.VOLLEY;
    }

    public void onFinalDeath() {
        discardSummons();
    }

    private void tickCooldowns() {
        ossuaryCooldown = Math.max(0, ossuaryCooldown - 1);
        volleyCooldown = Math.max(0, volleyCooldown - 1);
        pulseCooldown = Math.max(0, pulseCooldown - 1);
        echoCooldown = Math.max(0, echoCooldown - 1);
        globalCooldown = Math.max(0, globalCooldown - 1);
        blightCloudCooldown = Math.max(0, blightCloudCooldown - 1);
    }

    private void evaluateHalfHealth() {
        if (!halfHealthEmpowered && wither.getHealth() <= wither.getMaxHealth() * 0.5F) {
            halfHealthEmpowered = true;
        }
        if (!halfHealthEmpowered || halfHealthSummonUsed || wither.isReforming() || wither.isDeadOrDying()) {
            return;
        }
        // The flag records that the one-time wave has been triggered even if no slot was free.
        halfHealthSummonUsed = true;
        int capacity = Math.max(0, MAX_SUMMONS - aliveSummons());
        spawnSkeletons(Math.min(HALF_HEALTH_SUMMONS, capacity), 4.0D, 7.0D, 0);
    }

    private void updateTargetState() {
        LivingEntity target = wither.getTarget();
        validTarget = target != null && target.isAlive() && target.level() == wither.level()
                && wither.canAttack(target);
        if (!validTarget) {
            closeTicks = 0;
            return;
        }
        combatTicks++;
        if (wither.distanceToSqr(target) <= 25.0D) {
            closeTicks++;
        } else {
            closeTicks = 0;
        }
    }

    private void cancelCast() {
        if (cast != Cast.NONE) {
            cast = Cast.NONE;
            castTicks = 0;
            ossuaryGround = null;
            ossuaryRing = false;
        }
    }

    private void tickCast() {
        castTicks++;
        Level level = wither.level();
        if (cast == Cast.OSSUARY) {
            if (ossuaryGround == null) {
                cast = Cast.NONE;
                return;
            }
            if (castTicks < OSSUARY_WINDUP) {
                warnOssuary();
            } else {
                spawnOssuaryFangs();
                cast = Cast.NONE;
                ossuaryGround = null;
                ossuaryRing = false;
            }
            return;
        }
        if (cast == Cast.VOLLEY) {
            chargeVolleyEffect();
            if (castTicks >= VOLLEY_WINDUP && (castTicks - VOLLEY_WINDUP) % VOLLEY_ROUND_GAP == 0) {
                fireVolleyRound((castTicks - VOLLEY_WINDUP) / VOLLEY_ROUND_GAP);
            }
            if (castTicks >= VOLLEY_CAST_TICKS) cast = Cast.NONE;
            return;
        }
        if (cast == Cast.PULSE) {
            warnPulse(level);
            if (castTicks >= PULSE_WINDUP) {
                releasePulse();
                cast = Cast.NONE;
            }
            return;
        }
        if (cast == Cast.ECHO) {
            warnEcho(level);
            if (castTicks >= ECHO_WINDUP) {
                spawnSkeletons(2, 4.0D, 7.0D, ECHO_SUMMON_LIFETIME);
                cast = Cast.NONE;
            }
        }
    }

    private void warnEcho(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        double radius = 1.2D + castTicks * 0.16D;
        for (int index = 0; index < 8; index++) {
            double angle = index * Math.PI / 4.0D;
            serverLevel.sendParticles(SEEP_DUST,
                    wither.getX() + Math.cos(angle) * radius,
                    wither.getY() + 0.6D,
                    wither.getZ() + Math.sin(angle) * radius,
                    1, 0.0D, 0.15D, 0.0D, 0.0D);
        }
    }

    private boolean canStartPulse() {
        if (pulseCooldown > 0 || closeTicks < 10) return false;
        LivingEntity target = wither.getTarget();
        return target != null && target.isAlive() && wither.canAttack(target)
                && wither.distanceToSqr(target) <= 25.0D;
    }

    private boolean canStartEcho() {
        if (echoCooldown > 0 || combatTicks < COMBAT_START_TICKS) return false;
        LivingEntity target = wither.getTarget();
        if (target == null || !target.isAlive() || !wither.canAttack(target)) return false;
        if (wither.distanceToSqr(target) > 400.0D) return false;
        if (aliveSummons() >= 2) return false;
        return findSkeletonSpot(4.0D, 7.0D) != null;
    }

    private boolean canStartOssuary() {
        if (ossuaryCooldown > 0) return false;
        LivingEntity target = wither.getTarget();
        if (target == null || !target.isAlive() || !wither.canAttack(target)) return false;
        double distance = Math.sqrt(wither.distanceToSqr(target));
        if (distance < 4.0D || distance > 16.0D) return false;
        if (isSustainedFlyer(target)) return false;
        return findStandGround(wither.level(), target.position(), 6, 4) != null;
    }

    private boolean canStartVolley() {
        if (volleyCooldown > 0) return false;
        LivingEntity target = wither.getTarget();
        if (target == null || !target.isAlive() || !wither.canAttack(target)) return false;
        double distance = Math.sqrt(wither.distanceToSqr(target));
        return distance >= 10.0D && distance <= 28.0D && wither.hasLineOfSight(target);
    }

    private void startPulse() {
        pulseCooldown = wither.isFinalPhase() ? PULSE_COOLDOWN_FINAL : PULSE_COOLDOWN;
        beginCast(Cast.PULSE);
        playSound(SoundEvents.WITHER_HURT, 1.0F);
    }

    private void startEcho() {
        echoCooldown = wither.isFinalPhase() ? ECHO_COOLDOWN_FINAL : ECHO_COOLDOWN;
        beginCast(Cast.ECHO);
        playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F);
    }

    private void startOssuary() {
        LivingEntity target = wither.getTarget();
        if (target == null) return;
        BlockPos ground = findStandGround(wither.level(), target.position(), 6, 4);
        if (ground == null) return;
        ossuaryGround = ground.immutable();
        ossuaryRing = shouldUseOssuaryRing();
        ossuaryCooldown = wither.isFinalPhase() ? OSSUARY_COOLDOWN_FINAL : OSSUARY_COOLDOWN;
        beginCast(Cast.OSSUARY);
        playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.0F);
    }

    /** The half-health ring is the optional layout, enabled on Hard; the final phase always uses it. */
    private boolean shouldUseOssuaryRing() {
        return wither.isFinalPhase()
                || (halfHealthEmpowered && wither.level().getDifficulty() == Difficulty.HARD);
    }

    private void startVolley() {
        volleyCooldown = wither.isFinalPhase() ? VOLLEY_COOLDOWN_FINAL : VOLLEY_COOLDOWN;
        volleyEmpowered = halfHealthEmpowered;
        volleyDangerMask = 0;
        if (volleyEmpowered) {
            int first = wither.getRandom().nextInt(9);
            int second = wither.getRandom().nextInt(8);
            if (second >= first) second++;
            volleyDangerMask = (1 << first) | (1 << second);
        }
        beginCast(Cast.VOLLEY);
        playSound(SoundEvents.WITHER_AMBIENT, 1.0F);
    }

    private void beginCast(Cast next) {
        cast = next;
        castTicks = 0;
        globalCooldown = wither.isFinalPhase() ? GLOBAL_INTERVAL_FINAL : GLOBAL_INTERVAL;
    }

    private void warnOssuary() {
        if (ossuaryRing) {
            warnOssuaryRing();
        } else {
            warnOssuaryLine();
        }
    }

    private void warnOssuaryLine() {
        Level level = wither.level();
        if (!(level instanceof ServerLevel serverLevel) || ossuaryGround == null) return;
        BlockPos ground = ossuaryGround;
        Vec3 direction = new Vec3(wither.getX() - (ground.getX() + 0.5D), 0.0D,
                wither.getZ() - (ground.getZ() + 0.5D));
        if (direction.lengthSqr() < 1.0E-4D) direction = new Vec3(1.0D, 0.0D, 0.0D);
        direction = direction.normalize();
        int length = ossuaryLength(ground);
        for (int index = 0; index < length; index++) {
            double x = ground.getX() + 0.5D + direction.x * index;
            double z = ground.getZ() + 0.5D + direction.z * index;
            Vec3 point = fangGround(level, x, z, ground.getY() - 3, ground.getY() + 3);
            if (point == null) continue;
            serverLevel.sendParticles(BONE_DUST, point.x, point.y + 0.15D, point.z, 1, 0.15D, 0.05D, 0.15D, 0.0D);
        }
    }

    private void warnOssuaryRing() {
        Level level = wither.level();
        if (!(level instanceof ServerLevel serverLevel) || ossuaryGround == null) return;
        BlockPos ground = ossuaryGround;
        double centerX = ground.getX() + 0.5D;
        double centerZ = ground.getZ() + 0.5D;
        for (int index = 0; index < 12; index++) {
            if (isOssuaryRingGap(index)) continue;
            double angle = index * (Math.PI * 2.0D / 12.0D);
            double x = centerX + Math.cos(angle) * 3.0D;
            double z = centerZ + Math.sin(angle) * 3.0D;
            Vec3 point = fangGround(level, x, z, ground.getY() - 3, ground.getY() + 3);
            if (point == null) continue;
            serverLevel.sendParticles(BONE_DUST, point.x, point.y + 0.15D, point.z, 1, 0.15D, 0.05D, 0.15D, 0.0D);
        }
    }

    private static boolean isOssuaryRingGap(int index) {
        // Two opposite 60-degree gaps keep the ring dodgeable.
        return index == 0 || index == 1 || index == 6 || index == 7;
    }

    private void spawnOssuaryFangs() {
        Level level = wither.level();
        if (!(level instanceof ServerLevel) || ossuaryGround == null) return;
        if (ossuaryRing) {
            spawnOssuaryRing(level, ossuaryGround);
        } else {
            spawnOssuaryLine(level, ossuaryGround);
        }
    }

    private int ossuaryLength(BlockPos ground) {
        double dx = wither.getX() - (ground.getX() + 0.5D);
        double dz = wither.getZ() - (ground.getZ() + 0.5D);
        return Math.min(7, Math.max(5, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz))));
    }

    private void spawnOssuaryLine(Level level, BlockPos ground) {
        int count = ossuaryLength(ground);
        Vec3 direction = new Vec3(wither.getX() - (ground.getX() + 0.5D), 0.0D,
                wither.getZ() - (ground.getZ() + 0.5D));
        if (direction.lengthSqr() < 1.0E-4D) direction = new Vec3(1.0D, 0.0D, 0.0D);
        direction = direction.normalize();
        for (int index = 0; index < count; index++) {
            double x = ground.getX() + 0.5D + direction.x * index;
            double z = ground.getZ() + 0.5D + direction.z * index;
            spawnFang(level, x, z, ground.getY(), ground.getX() + 0.5D, ground.getZ() + 0.5D);
        }
    }

    private void spawnOssuaryRing(Level level, BlockPos ground) {
        double centerX = ground.getX() + 0.5D;
        double centerZ = ground.getZ() + 0.5D;
        for (int index = 0; index < 12; index++) {
            if (isOssuaryRingGap(index)) continue;
            double angle = index * (Math.PI * 2.0D / 12.0D);
            double x = centerX + Math.cos(angle) * 3.0D;
            double z = centerZ + Math.sin(angle) * 3.0D;
            spawnFang(level, x, z, ground.getY(), centerX, centerZ);
        }
    }

    private void spawnFang(Level level, double x, double z, double referenceY, double lookX, double lookZ) {
        Vec3 point = fangGround(level, x, z, referenceY - 3, referenceY + 3);
        if (point == null) return;
        OssuaryFangs fangs = MaledictEntities.OSSUARY_FANGS.get().create(level);
        if (fangs == null) return;
        fangs.setOwner(wither);
        // The 0.8 s cast wind-up is the warning; the fangs erupt on this tick.
        fangs.setWarmup(-7);
        float yaw = (float) (Mth.atan2(lookZ - z, lookX - x) * (180.0D / Math.PI));
        fangs.moveTo(point.x, point.y, point.z, yaw, 0.0F);
        if (!level.addFreshEntity(fangs)) return;
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(BONE_DUST, point.x, point.y + 0.2D, point.z, 4, 0.25D, 0.15D, 0.25D, 0.0D);
        }
    }

    private void chargeVolleyEffect() {
        if (!(wither.level() instanceof ServerLevel serverLevel)) return;
        for (int head = 0; head < 3; head++) {
            Vec3 muzzle = wither.headPosition(head);
            serverLevel.sendParticles(SEEP_DUST, muzzle.x, muzzle.y, muzzle.z, 2, 0.2D, 0.2D, 0.2D, 0.0D);
        }
    }

    private void fireVolleyRound(int round) {
        LivingEntity target = wither.getTarget();
        if (target == null || !target.isAlive()) {
            cast = Cast.NONE;
            return;
        }
        Vec3 aimBase = target.position().add(0.0D, target.getEyeHeight() * 0.5D, 0.0D);
        for (int head = 0; head < 3; head++) {
            int shot = round * 3 + head;
            Vec3 aim = aimBase.add(
                    (wither.getRandom().nextDouble() - 0.5D) * 3.0D,
                    (wither.getRandom().nextDouble() - 0.5D) * 2.0D,
                    (wither.getRandom().nextDouble() - 0.5D) * 3.0D);
            boolean dangerous = volleyEmpowered
                    ? (volleyDangerMask & (1 << shot)) != 0
                    : wither.getRandom().nextFloat() < PRE_HALF_VOLLEY_DANGEROUS_CHANCE;
            wither.shoot(head, aim, dangerous);
        }
    }

    private void warnPulse(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        double radius = 2.0D + (PULSE_WINDUP - castTicks) * 0.4D;
        for (int index = 0; index < 12; index++) {
            double angle = index * Math.PI / 6.0D;
            serverLevel.sendParticles(SEEP_DUST,
                    wither.getX() + Math.cos(angle) * radius,
                    wither.getY() + 1.0D,
                    wither.getZ() + Math.sin(angle) * radius,
                    1, 0.0D, 0.05D, 0.0D, 0.0D);
        }
    }

    private void releasePulse() {
        if (!(wither.level() instanceof ServerLevel serverLevel)) return;
        AABB area = wither.getBoundingBox().inflate(PULSE_RADIUS);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate != wither && candidate.isAlive() && !wither.isAlliedTo(candidate)
                        && wither.canAttack(candidate) && !isOwnSummon(candidate))) {
            double dx = victim.getX() - wither.getX();
            double dz = victim.getZ() - wither.getZ();
            if (dx * dx + dz * dz < 1.0E-4D) {
                dx = wither.getRandom().nextDouble() - 0.5D;
                dz = wither.getRandom().nextDouble() - 0.5D;
            }
            victim.hurt(wither.damageSources().indirectMagic(wither, wither), PULSE_DAMAGE);
            if (victim.isAlive()) victim.knockback(PULSE_KNOCKBACK, dx, dz);
        }
        for (int index = 0; index < 48; index++) {
            double angle = index * Math.PI / 24.0D;
            serverLevel.sendParticles(index % 2 == 0 ? SEEP_DUST : BONE_DUST,
                    wither.getX() + Math.cos(angle) * PULSE_RADIUS,
                    wither.getY() + 0.8D,
                    wither.getZ() + Math.sin(angle) * PULSE_RADIUS,
                    1, 0.0D, 0.1D, 0.0D, 0.0D);
        }
        playSound(SoundEvents.WITHER_SHOOT, 1.0F);
    }

    private boolean isOwnSummon(LivingEntity candidate) {
        return candidate instanceof WeepingWitherSkeletonEntity skeleton && skeleton.isOwnedBy(wither);
    }

    private int aliveSummons() {
        return wither.level().getEntitiesOfClass(WeepingWitherSkeletonEntity.class,
                wither.getBoundingBox().inflate(128.0D),
                skeleton -> skeleton.isAlive() && skeleton.isOwnedBy(wither)).size();
    }

    private void discardSummons() {
        for (WeepingWitherSkeletonEntity skeleton : wither.level().getEntitiesOfClass(
                WeepingWitherSkeletonEntity.class, wither.getBoundingBox().inflate(128.0D),
                skeleton -> skeleton.isOwnedBy(wither))) {
            skeleton.discard();
        }
    }

    private int spawnSkeletons(int requested, double minRadius, double maxRadius, int lifetimeTicks) {
        if (requested <= 0 || !(wither.level() instanceof ServerLevel serverLevel)) return 0;
        int capacity = Math.max(0, MAX_SUMMONS - aliveSummons());
        int limit = Math.min(requested, capacity);
        int spawned = 0;
        for (int attempt = 0; attempt < 24 && spawned < limit; attempt++) {
            WeepingWitherSkeletonEntity skeleton =
                    MaledictEntities.WEEPING_WITHER_SKELETON.get().create(serverLevel);
            if (skeleton == null) return spawned;
            double angle = wither.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = minRadius + wither.getRandom().nextDouble() * (maxRadius - minRadius);
            double x = wither.getX() + Math.cos(angle) * radius;
            double z = wither.getZ() + Math.sin(angle) * radius;
            BlockPos ground = findStandGround(serverLevel, new Vec3(x, wither.getY() + 2.0D, z), 12, 4, 3);
            if (ground == null) {
                skeleton.discard();
                continue;
            }
            skeleton.moveTo(ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D,
                    wither.getRandom().nextFloat() * 360.0F, 0.0F);
            if (!serverLevel.noCollision(skeleton)) {
                skeleton.discard();
                continue;
            }
            skeleton.finalizeSpawn(serverLevel,
                    serverLevel.getCurrentDifficultyAt(ground), MobSpawnType.MOB_SUMMONED, null, null);
            skeleton.summonBy(wither, lifetimeTicks);
            if (!serverLevel.addFreshEntity(skeleton)) {
                skeleton.discard();
                continue;
            }
            spawned++;
            serverLevel.sendParticles(SEEP_DUST,
                    skeleton.getX(), skeleton.getY() + 1.0D, skeleton.getZ(),
                    12, 0.35D, 0.7D, 0.35D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.SOUL, skeleton.getX(), skeleton.getY() + 1.0D,
                    skeleton.getZ(), 6, 0.3D, 0.6D, 0.3D, 0.01D);
        }
        if (spawned > 0) playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F);
        return spawned;
    }

    @Nullable
    private BlockPos findSkeletonSpot(double minRadius, double maxRadius) {
        if (!(wither.level() instanceof ServerLevel serverLevel)) return null;
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = wither.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = minRadius + wither.getRandom().nextDouble() * (maxRadius - minRadius);
            double x = wither.getX() + Math.cos(angle) * radius;
            double z = wither.getZ() + Math.sin(angle) * radius;
            BlockPos ground = findStandGround(serverLevel, new Vec3(x, wither.getY() + 2.0D, z), 12, 4, 3);
            if (ground != null) return ground;
        }
        return null;
    }

    @Nullable
    private static BlockPos findStandGround(Level level, Vec3 center, int down, int up) {
        return findStandGround(level, center, down, up, 2);
    }

    @Nullable
    private static BlockPos findStandGround(Level level, Vec3 center, int down, int up, int airHeight) {
        BlockPos origin = BlockPos.containing(center);
        for (int offset = up; offset >= -down; offset--) {
            BlockPos pos = origin.above(offset);
            BlockPos support = pos.below();
            if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) continue;
            boolean clear = true;
            for (int height = 0; height < airHeight; height++) {
                BlockPos air = pos.above(height);
                if (!level.isEmptyBlock(air) || !level.getFluidState(air).isEmpty()) {
                    clear = false;
                    break;
                }
            }
            if (clear) return pos;
        }
        return null;
    }

    @Nullable
    private static Vec3 fangGround(Level level, double x, double z, double minY, double maxY) {
        BlockPos pos = BlockPos.containing(x, maxY, z);
        int floor = Mth.floor(minY) - 1;
        while (pos.getY() >= floor) {
            BlockPos support = pos.below();
            if (level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) {
                double extra = 0.0D;
                if (!level.isEmptyBlock(pos)) {
                    VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
                    if (!shape.isEmpty()) extra = shape.max(Direction.Axis.Y);
                }
                return new Vec3(x, support.getY() + 1.0D + extra, z);
            }
            pos = pos.below();
        }
        return null;
    }

    private static boolean isSustainedFlyer(LivingEntity target) {
        if (target.isFallFlying()) return true;
        return target instanceof Player player && player.getAbilities().flying;
    }

    @Nullable
    private Vec3 cloudCenter(BlockPos hitPos, Direction face) {
        Level level = wither.level();
        BlockPos pos = hitPos.relative(face);
        for (int attempt = 0; attempt < 4; attempt++) {
            if (!level.isEmptyBlock(pos)) {
                pos = pos.above();
                continue;
            }
            if (!level.isEmptyBlock(pos.above())) {
                pos = pos.above();
                continue;
            }
            BlockPos support = findStandGround(level, new Vec3(pos.getX() + 0.5D, pos.getY() + 1.0D,
                    pos.getZ() + 0.5D), 5, 1);
            if (support == null) {
                pos = pos.above();
                continue;
            }
            return new Vec3(support.getX() + 0.5D, support.getY() + 0.05D, support.getZ() + 0.5D);
        }
        return null;
    }

    private boolean hasNearbyCloud(Vec3 center) {
        AABB area = new AABB(center, center).inflate(4.0D);
        return !wither.level().getEntitiesOfClass(AreaEffectCloud.class, area,
                cloud -> cloud.getPersistentData().getBoolean(CLOUD_TAG)).isEmpty();
    }

    private void spawnBlightCloud(Vec3 center) {
        Level level = wither.level();
        AreaEffectCloud cloud = new AreaEffectCloud(level, center.x, center.y, center.z);
        cloud.setOwner(wither);
        cloud.setParticle(ParticleTypes.DRAGON_BREATH);
        cloud.setRadius(2.5F);
        cloud.setDuration(200);
        cloud.setWaitTime(10);
        cloud.setRadiusPerTick(0.0F);
        cloud.setFixedColor(0x3A2344);
        cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
        cloud.getPersistentData().putBoolean(CLOUD_TAG, true);
        if (level.addFreshEntity(cloud)) {
            level.levelEvent(2006, BlockPos.containing(center), 1);
        }
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound, float pitch) {
        wither.level().playSound(null, wither.getX(), wither.getY(), wither.getZ(), sound,
                SoundSource.HOSTILE, 1.0F, pitch);
    }

    public void save(CompoundTag tag) {
        tag.putInt("OssuaryCooldown", ossuaryCooldown);
        tag.putInt("VolleyCooldown", volleyCooldown);
        tag.putInt("PulseCooldown", pulseCooldown);
        tag.putInt("EchoCooldown", echoCooldown);
        tag.putInt("SkillGlobalCooldown", globalCooldown);
        tag.putInt("BlightCloudCooldown", blightCloudCooldown);
        tag.putInt("CombatTicks", combatTicks);
        tag.putInt("PulseCloseTicks", closeTicks);
        tag.putBoolean("HalfHealthEmpowered", halfHealthEmpowered);
        tag.putBoolean("HalfHealthSummonUsed", halfHealthSummonUsed);
        tag.putString("ActiveSkill", cast.name());
        tag.putInt("ActiveSkillTicks", castTicks);
        tag.putInt("VolleyDangerMask", volleyDangerMask);
        tag.putBoolean("VolleyEmpowered", volleyEmpowered);
        tag.putBoolean("OssuaryRing", ossuaryRing);
        if (ossuaryGround != null) tag.putLong("OssuaryGround", ossuaryGround.asLong());
    }

    public void restore(CompoundTag tag) {
        ossuaryCooldown = clampCooldown(tag.getInt("OssuaryCooldown"));
        volleyCooldown = clampCooldown(tag.getInt("VolleyCooldown"));
        pulseCooldown = clampCooldown(tag.getInt("PulseCooldown"));
        echoCooldown = clampCooldown(tag.getInt("EchoCooldown"));
        globalCooldown = clampCooldown(tag.getInt("SkillGlobalCooldown"));
        blightCloudCooldown = clampCooldown(tag.getInt("BlightCloudCooldown"));
        combatTicks = Math.max(0, tag.getInt("CombatTicks"));
        closeTicks = Math.max(0, tag.getInt("PulseCloseTicks"));
        halfHealthEmpowered = tag.getBoolean("HalfHealthEmpowered");
        halfHealthSummonUsed = tag.getBoolean("HalfHealthSummonUsed");
        volleyDangerMask = tag.getInt("VolleyDangerMask") & 0x1FF;
        volleyEmpowered = tag.getBoolean("VolleyEmpowered");
        ossuaryRing = tag.getBoolean("OssuaryRing");
        if (!volleyEmpowered) volleyDangerMask = 0;
        Cast saved = Cast.NONE;
        try {
            saved = Cast.valueOf(tag.getString("ActiveSkill"));
        } catch (IllegalArgumentException ignored) {
            // Older or damaged saves simply resume without an active cast.
        }
        castTicks = Math.max(0, tag.getInt("ActiveSkillTicks"));
        cast = saved;
        ossuaryGround = tag.contains("OssuaryGround") ? BlockPos.of(tag.getLong("OssuaryGround")) : null;
        if (cast == Cast.OSSUARY && (ossuaryGround == null || castTicks >= OSSUARY_WINDUP)
                || cast == Cast.VOLLEY && castTicks >= VOLLEY_CAST_TICKS
                || cast == Cast.PULSE && castTicks >= PULSE_WINDUP
                || cast == Cast.ECHO && castTicks >= ECHO_WINDUP) {
            cast = Cast.NONE;
            castTicks = 0;
            ossuaryGround = null;
        }
        if (cast != Cast.OSSUARY) {
            ossuaryGround = null;
            ossuaryRing = false;
        }
        if (cast != Cast.VOLLEY) {
            volleyEmpowered = false;
            volleyDangerMask = 0;
        }
    }

    private static int clampCooldown(int value) {
        return Math.max(0, Math.min(value, 30 * 20));
    }
}
