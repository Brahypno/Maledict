package org.brahypno.maledict.common.entity.demiurge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.brahypno.maledict.common.entity.vicissitude.VicissitudeBossEntity;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.registry.MaledictParticles;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The False Creation pillar: a temporary, non-block stone shaft that rises around the player,
 * blocks lasers and the serpent sweep, can be broken by players, and explodes on a timer.
 * The cast raises an inner ring of up to three and an outer half-ring of up to three; each
 * pillar is planned and spawned independently, so a bad terrain vertex never cancels the whole
 * array.
 *
 * <p>This entity never edits the world and never creates a vanilla explosion. All damage is
 * delivered through the same difficulty-scaled path as the statue's other attacks; terrain,
 * the boss itself, and other pillars are never damaged.</p>
 *
 * <p>Pillar lifetime is deliberately transient: the entity is not saved, and the owner discards
 * every remaining pillar when it dies. A chunk unload therefore removes its pillars instead of
 * restoring armed bombs later.</p>
 */
public final class DemiurgePillarEntity extends Entity {
    public enum Phase {
        WARNING,
        RISING,
        ARMED,
        EMPOWERED,
        /**
         * Ripped out of the ground by the statue's charge and flying as a stone projectile.
         */
        LAUNCHED,
        EXPLODING,
        DESTROYED
    }

    public static final int WARNING_TICKS = 16;
    public static final int RISE_TICKS = 8;
    public static final int ARMED_TICKS = 30;
    public static final int EMPOWERED_FUSE_TICKS = 5;
    /**
     * The old sphere was measured from the middle of the shaft, so a player standing at the aim
     * point was usually 3.5 blocks from the centre and never inside the 3.5 radius. The explosion is
     * now a column around the whole shaft; five blocks horizontally reaches the inner ring and the
     * centre of the array.
     */
    public static final double NORMAL_RADIUS = 5.0;
    public static final double EMPOWERED_RADIUS = 5.5;
    public static final double RISE_LAUNCH = .85;
    public static final float WIDTH = 1.25F;
    /**
     * Faster than the statue's 0.8 blocks/tick charge, so the stone outruns the body.
     */
    public static final double LAUNCH_SPEED = 1.7;
    public static final int LAUNCH_TICKS = 30;
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 5;
    /**
     * Up to three close pillars plus three on the outer half, in a hexagonal layout.
     */
    public static final int INNER_PILLARS = 3;
    public static final int OUTER_PILLARS = 3;
    private static final double[] INNER_RADII = {3.6, 4.2, 3.2};
    private static final double[] OUTER_RADII = {7.2, 8.0, 6.6};

    private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(
            DemiurgePillarEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> PILLAR_HEIGHT = SynchedEntityData.defineId(
            DemiurgePillarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RISE = SynchedEntityData.defineId(
            DemiurgePillarEntity.class, EntityDataSerializers.FLOAT);

    private UUID owner;
    private float health = 1;
    private int delay;
    private int age;
    private int phaseAge;
    private final Set<UUID> riseHits = new HashSet<>();
    private Vec3 launchVelocity = Vec3.ZERO;
    private int launchAge;
    private final Set<UUID> launchHits = new HashSet<>();

    public DemiurgePillarEntity(EntityType<? extends DemiurgePillarEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public static void summonArray(DemiurgeStatueEntity owner, List<Spot> spots) {
        if (!(owner.level() instanceof ServerLevel level))
            return;
        int delay = 0;
        for (Spot spot : spots) {
            AABB box = pillarBox(spot.position().x, spot.position().y, spot.position().z, spot.height());
            if (!canPlace(level, owner, box))
                continue;
            DemiurgePillarEntity pillar = MaledictEntities.DEMIURGE_PILLAR.get().create(level);
            if (pillar == null)
                continue;
            pillar.setOwner(owner);
            pillar.setPillarHeight(spot.height());
            pillar.setPos(spot.position());
            pillar.refreshDimensions();
            pillar.health = (float) Math.max(1, MaledictConfig.DEMIURGE_PILLAR_HEALTH.get());
            pillar.setDelay(delay);
            if (level.addFreshEntity(pillar))
                delay += 8;
        }
    }

    public record Spot(Vec3 position, int height) {}

    static List<Spot> planArray(DemiurgeStatueEntity owner, Vec3 target) {
        Level level = owner.level();
        if (level.isClientSide)
            return List.of();
        BlockPos ground = groundBelow(level, target.x, target.z, target.y, 0);
        if (ground == null)
            return List.of();
        Vec3 center = new Vec3(target.x, ground.getY(), target.z);
        // If the boss is close, rotate the hexagon so its inner triangle does not
        // point straight into its own head or neck.
        double baseAngle = Math.atan2(owner.getZ() - center.z, owner.getX() - center.x) + Math.PI / 3;

        List<Spot> spots = new ArrayList<>(INNER_PILLARS + OUTER_PILLARS);
        for (int index = 0; index < INNER_PILLARS; index++) {
            double angle = baseAngle + index * Math.PI * 2 / INNER_PILLARS;
            Spot spot = findSpot(level, owner, center, angle, spots, INNER_RADII);
            if (spot != null)
                spots.add(spot);
        }
        // The outer ring sits in the gaps of the inner ring, on the other half
        // of each sector, and is planned independently: a failed inner vertex
        // no longer discards the whole cast.
        for (int index = 0; index < OUTER_PILLARS; index++) {
            double angle = baseAngle + Math.PI / 3 + index * Math.PI * 2 / OUTER_PILLARS;
            Spot spot = findSpot(level, owner, center, angle, spots, OUTER_RADII);
            if (spot != null)
                spots.add(spot);
        }
        if (spots.isEmpty())
            return spots;
        // An array leaves a way out of the centre, even along an uneven slope.
        for (int direction = 0; direction < 8; direction++) {
            double angle = direction * Math.PI / 4;
            double x = center.x + Math.cos(angle) * 2.2;
            double z = center.z + Math.sin(angle) * 2.2;
            BlockPos exit = groundBelow(level, x, z, center.y, 1);
            if (exit == null || exit.getY() < center.y - 1)
                continue;
            double y = exit.getY();
            AABB escape = new AABB(x - .3, y, z - .3, x + .3, y + 1.8, z + .3);
            if (clearOfBlocks(level, escape) && !blockedByPillar(escape, spots))
                return spots;
        }
        return List.of();
    }

    @Nullable
    private static Spot findSpot(
            Level level, DemiurgeStatueEntity owner, Vec3 center, double baseAngle,
            List<Spot> taken, double[] radii) {
        for (double radius : radii) {
            for (float jitter : new float[]{0, 14, -14, 28, -28}) {
                double angle = baseAngle + Math.toRadians(jitter);
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                BlockPos base = groundBelow(level, x, z, center.y, 1);
                if (base == null)
                    continue;
                int height = headroom(level, base);
                if (height < MIN_HEIGHT)
                    continue;
                AABB box = pillarBox(base.getX() + .5, base.getY(), base.getZ() + .5, height);
                if (blockedByPillar(box, taken))
                    continue;
                Vec3 position = new Vec3(base.getX() + .5, base.getY(), base.getZ() + .5);
                if (position.subtract(center).horizontalDistanceSqr() < 9)
                    continue;
                if (!canPlace(level, owner, box))
                    continue;
                return new Spot(position, height);
            }
        }
        return null;
    }

    private static boolean canPlace(Level level, DemiurgeStatueEntity owner, AABB box) {
        return clearOfBlocks(level, box)
               && !intersectsBody(owner, box)
               && level.getEntitiesOfClass(DemiurgeStatueEntity.class, box.inflate(32),
                                           boss -> boss != owner && intersectsBody(boss, box)).isEmpty()
               && level.getEntitiesOfClass(DemiurgePillarEntity.class, box.inflate(.4)).isEmpty();
    }

    private static boolean intersectsBody(DemiurgeStatueEntity statue, AABB box) {
        for (var part : statue.getParts()) {
            AABB bounds = part.getBoundingBox();
            if (bounds.getSize() > 0 && bounds.intersects(box))
                return true;
        }
        return false;
    }

    @Nullable
    private static BlockPos groundBelow(Level level, double x, double z, double y, int stepUp) {
        int start = Mth.floor(y + 1.0E-5) + stepUp;
        for (int scan = start; scan >= start - 7; scan--) {
            BlockPos base = BlockPos.containing(x, scan, z);
            BlockState floor = level.getBlockState(base.below());
            if (!level.getBlockState(base).getCollisionShape(level, base).isEmpty())
                continue;
            if (!level.getFluidState(base).isEmpty())
                continue;
            if (!floor.isFaceSturdy(level, base.below(), Direction.UP))
                continue;
            return base;
        }
        return null;
    }

    private static int headroom(Level level, BlockPos base) {
        int open = 0;
        for (int step = 0; step <= MAX_HEIGHT; step++) {
            BlockPos pos = base.above(step);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty())
                break;
            if (!level.getFluidState(pos).isEmpty())
                break;
            open++;
        }
        return Math.min(MAX_HEIGHT, open);
    }

    private static AABB pillarBox(double x, double y, double z, int height) {
        double half = WIDTH / 2;
        return new AABB(x - half, y, z - half, x + half, y + height, z + half);
    }

    private static boolean clearOfBlocks(Level level, AABB box) {
        // The stone may touch its supporting block exactly at the lower face.
        return !level.getBlockCollisions(null, box.deflate(1.0E-5)).iterator().hasNext();
    }

    private static boolean blockedByPillar(AABB box, List<Spot> spots) {
        for (Spot spot : spots) {
            if (pillarBox(spot.position().x, spot.position().y, spot.position().z, spot.height()).intersects(box)) {
                return true;
            }
        }
        return false;
    }

    public void setOwner(DemiurgeStatueEntity owner) {
        this.owner = owner.getUUID();
    }

    public boolean isOwnedBy(DemiurgeStatueEntity statue) {
        return owner != null && owner.equals(statue.getUUID());
    }

    public void setDelay(int delay) {
        this.delay = Math.max(0, delay);
        setInvisible(this.delay > 0);
    }

    public void setPillarHeight(int height) {
        entityData.set(PILLAR_HEIGHT, Mth.clamp(height, 1, MAX_HEIGHT));
        refreshDimensions();
    }

    public int pillarHeight() {
        return entityData.get(PILLAR_HEIGHT);
    }

    public float pillarHealth() {
        return health;
    }

    public void setPillarHealth(float health) {
        this.health = Math.max(1, health);
    }

    public float rise() {
        return entityData.get(RISE);
    }

    public Phase phase() {
        int ordinal = entityData.get(PHASE);
        return Phase.values()[Mth.clamp(ordinal, 0, Phase.values().length - 1)];
    }

    private void setPhase(Phase phase) {
        entityData.set(PHASE, (byte) phase.ordinal());
        entityData.set(RISE, phase == Phase.WARNING || phase == Phase.RISING ? .12F : 1.0F);
        phaseAge = 0;
    }

    public boolean blocksLaser() {
        Phase phase = phase();
        return phase == Phase.RISING || phase == Phase.ARMED || phase == Phase.EMPOWERED;
    }

    public boolean blocksSweep() {
        Phase phase = phase();
        return phase == Phase.ARMED || phase == Phase.EMPOWERED;
    }

    public boolean canBlockMovement() {
        Phase phase = phase();
        return phase == Phase.RISING || phase == Phase.ARMED || phase == Phase.EMPOWERED;
    }

    /**
     * Only a stone that is already out of the ground can be picked up by the charge.
     */
    public boolean canLaunch() {
        Phase phase = phase();
        return phase == Phase.RISING || phase == Phase.ARMED || phase == Phase.EMPOWERED;
    }

    /**
     * The charge rips the pillar out of the ground and sends it along the locked charge direction.
     * The flight is intentionally faster than the statue, breaks a tunnel for its own shaft, and
     * ends in the same empowered explosion the sweep triggers.
     */
    public boolean launch(Vec3 direction) {
        if (level().isClientSide || isRemoved() || !canLaunch())
            return false;
        Vec3 horizontal = new Vec3(direction.x, 0, direction.z);
        if (horizontal.lengthSqr() < 1.0E-4)
            return false;
        launchVelocity = horizontal.normalize().scale(LAUNCH_SPEED).add(0, .04, 0);
        launchAge = 0;
        launchHits.clear();
        setInvisible(false);
        setPhase(Phase.LAUNCHED);
        entityData.set(RISE, 1.0F);
        playSound(SoundEvents.STONE_BREAK, 2.2F, .55F);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(MaledictParticles.PURPLE_SOUL_FIRE.get(),
                                      getX(), getY() + pillarHeight() * .5, getZ(), 20, .4, .7, .4, .02);
        }
        return true;
    }

    /**
     * A sweep hit does not destroy the pillar: it switches the timer to the shorter, stronger
     * fuse. Re-entering is a no-op, and a pillar already destroyed cannot be re-armed.
     */
    public void empower() {
        if (level().isClientSide || phase() != Phase.ARMED)
            return;
        setPhase(Phase.EMPOWERED);
        playSound(SoundEvents.STONE_BREAK, 2.4F, .65F);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(MaledictParticles.PURPLE_SOUL_FIRE.get(),
                                      getX(), getY() + pillarHeight(), getZ(), 22, .35, .55, .35, .02);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide)
            return;
        if (delay > 0) {
            if (--delay == 0) {
                setInvisible(false);
                setPhase(Phase.WARNING);
                playSound(SoundEvents.STONE_PLACE, 1.6F, .45F);
            }
            return;
        }
        Phase phase = phase();
        if (phase == Phase.DESTROYED)
            return;
        age++;
        phaseAge++;
        switch (phase) {
            case WARNING -> {
                entityData.set(RISE, .12F);
                warningParticles();
                if (phaseAge >= WARNING_TICKS) {
                    setPhase(Phase.RISING);
                    playSound(SoundEvents.STONE_BREAK, 2.0F, .55F);
                }
            }
            case RISING -> {
                float rise = Math.min(1, phaseAge / (float) RISE_TICKS);
                entityData.set(RISE, rise);
                riseDamage(rise);
                if (phaseAge >= RISE_TICKS) {
                    entityData.set(RISE, 1.0F);
                    setPhase(Phase.ARMED);
                }
            }
            case ARMED -> {
                armedParticles();
                if (phaseAge >= ARMED_TICKS)
                    explode(false);
            }
            case EMPOWERED -> {
                empoweredParticles();
                if (phaseAge >= EMPOWERED_FUSE_TICKS)
                    explode(true);
            }
            case LAUNCHED -> tickLaunched();
            case EXPLODING -> {
                if (phaseAge >= 2)
                    discard();
            }
            case DESTROYED -> {
            }
        }
    }

    private void tickLaunched() {
        if (launchVelocity.lengthSqr() < 1.0E-6) {
            explode(true);
            return;
        }
        Vec3 from = position();
        Vec3 to = from.add(launchVelocity);
        double half = WIDTH / 2;
        AABB sweep = new AABB(
                Math.min(from.x, to.x) - half, Math.min(from.y, to.y), Math.min(from.z, to.z) - half,
                Math.max(from.x, to.x) + half, Math.max(from.y, to.y) + pillarHeight(),
                Math.max(from.z, to.z) + half);
        if (!clearLaunchPath(sweep)) {
            explode(true);
            return;
        }
        for (var victim : level().getEntitiesOfClass(LivingEntity.class, sweep.inflate(.18),
                                                      DemiurgePillarEntity::canHit)) {
            if (!launchHits.add(victim.getUUID()))
                continue;
            DemiurgeDifficulty.hurt(victim, damageSource(false),
                                    MaledictConfig.DEMIURGE_PILLAR_EMPOWERED_DAMAGE.get());
            Vec3 push = launchVelocity.normalize();
            // LivingEntity#knockback subtracts the supplied direction.
            victim.knockback(.6, -push.x, -push.z);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ASH, victim.getX(), victim.getY() + .6, victim.getZ(),
                                          12, .4, .3, .4, .02);
            }
            // A player stops the stone and takes the high explosion immediately.
            if (victim instanceof Player) {
                setPos(to);
                explode(true);
                return;
            }
        }
        setPos(to);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(MaledictParticles.PURPLE_SOUL_FIRE.get(),
                                      getX(), getY() + pillarHeight() * .5, getZ(), 5, .25, .35, .25, .015);
        }
        if (++launchAge >= LAUNCH_TICKS)
            explode(true);
    }

    /**
     * Break the shaft's own tunnel; an immune or protected block stops the flight.
     */
    private boolean clearLaunchPath(AABB sweep) {
        if (!(level() instanceof ServerLevel serverLevel))
            return true;
        Entity breaker = owner != null ? serverLevel.getEntity(owner) : null;
        if (breaker == null)
            breaker = this;
        if (!EventHooks.canEntityGrief(level(), breaker))
            return false;
        LivingEntity eventEntity = breaker instanceof LivingEntity living ? living : null;
        for (int x = Mth.floor(sweep.minX); x <= Mth.floor(sweep.maxX - 1.0E-7); x++) {
            for (int y = Mth.floor(sweep.minY); y <= Mth.floor(sweep.maxY - 1.0E-7); y++) {
                for (int z = Mth.floor(sweep.minZ); z <= Mth.floor(sweep.maxZ - 1.0E-7); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level().getBlockState(pos);
                    if (state.isAir() || state.getCollisionShape(level(), pos).isEmpty())
                        continue;
                    if (!state.canEntityDestroy(level(), pos, breaker))
                        return false;
                    if (eventEntity != null && !EventHooks.onEntityDestroyBlock(eventEntity, pos, state)) {
                        return false;
                    }
                    level().destroyBlock(pos, false, breaker);
                }
            }
        }
        return true;
    }

    private void riseDamage(float rise) {
        double half = WIDTH / 2;
        double top = getY() + Math.max(.15, pillarHeight() * rise);
        AABB column = new AABB(getX() - half, getY(), getZ() - half,
                               getX() + half, top, getZ() + half).inflate(.04);
        for (var victim : level().getEntitiesOfClass(LivingEntity.class, column, DemiurgePillarEntity::canHit)) {
            if (!riseHits.add(victim.getUUID()))
                continue;
            DemiurgeDifficulty.hurt(victim, damageSource(false),
                                    MaledictConfig.DEMIURGE_PILLAR_RISE_DAMAGE.get());
            Vec3 away = new Vec3(victim.getX() - getX(), 0, victim.getZ() - getZ());
            if (away.lengthSqr() < 1.0E-4) {
                double angle = random.nextDouble() * Math.PI * 2;
                away = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            } else {
                away = away.normalize();
            }
            // A real LivingEntity knockback, so resistance, events and client velocity apply.
            // LivingEntity#knockback subtracts the supplied horizontal direction.
            victim.knockback(.55, -away.x, -away.z);
            // The vertical launch stays a target velocity, not an addition, so overlapping
            // pillars cannot stack an arbitrarily large upward impulse.
            Vec3 movement = victim.getDeltaMovement();
            victim.setDeltaMovement(movement.x, Math.max(movement.y, RISE_LAUNCH), movement.z);
            victim.hasImpulse = true;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || !Float.isFinite(amount) || amount <= 0)
            return false;
        Phase phase = phase();
        if (phase == Phase.DESTROYED || phase == Phase.EXPLODING || phase == Phase.LAUNCHED)
            return false;
        if (!isPlayerDamage(source))
            return false;
        health -= amount;
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ASH, getX(), getY() + pillarHeight() * .6, getZ(),
                                      8, .35, .45, .35, .02);
        }
        playSound(SoundEvents.STONE_HIT, 1.5F, .7F);
        if (health <= 0) {
            // Player destruction is the safe option: no explosion, no chain.
            setPhase(Phase.DESTROYED);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + pillarHeight() * .5, getZ(),
                                          16, .4, .5, .4, .02);
                serverLevel.sendParticles(ParticleTypes.ASH, getX(), getY() + pillarHeight() * .5, getZ(),
                                          26, .45, .6, .45, .03);
            }
            playSound(SoundEvents.STONE_BREAK, 2.0F, .5F);
            discard();
        }
        return true;
    }

    private static boolean isPlayerDamage(DamageSource source) {
        if (source.getEntity() instanceof Player || source.getDirectEntity() instanceof Player)
            return true;
        return source.getDirectEntity() instanceof Projectile projectile
               && projectile.getOwner() instanceof Player;
    }

    private void explode(boolean empowered) {
        if (phase() == Phase.EXPLODING || phase() == Phase.DESTROYED)
            return;
        setPhase(Phase.EXPLODING);
        double radius = empowered ? EMPOWERED_RADIUS : NORMAL_RADIUS;
        double top = getY() + pillarHeight();
        Vec3 center = new Vec3(getX(), getY() + pillarHeight() * .5, getZ());
        // Measure from the shaft, not from a point halfway up it: the old sphere put a
        // ground-level player more than three blocks from the centre even when they stood
        // right beside the pillar. Every point of the column now shares the same radius.
        AABB area = new AABB(getX() - radius - 1, getY() - 1, getZ() - radius - 1,
                             getX() + radius + 1, top + 1, getZ() + radius + 1);
        for (var victim : level().getEntitiesOfClass(LivingEntity.class, area, DemiurgePillarEntity::canHit)) {
            if (Math.hypot(victim.getX() - getX(), victim.getZ() - getZ()) > radius + .45)
                continue;
            if (victim.getBoundingBox().maxY < getY() - .5 || victim.getBoundingBox().minY > top + .5)
                continue;
            Vec3 target = victim.getBoundingBox().getCenter();
            var blockHit = level().clip(new ClipContext(center, target, ClipContext.Block.COLLIDER,
                                                        ClipContext.Fluid.NONE, this));
            if (blockHit.getLocation().distanceToSqr(center) < target.distanceToSqr(center) - .25)
                continue;
            DemiurgeDifficulty.hurt(victim, damageSource(true),
                                    empowered ? MaledictConfig.DEMIURGE_PILLAR_EMPOWERED_DAMAGE.get()
                                              : MaledictConfig.DEMIURGE_PILLAR_EXPLOSION_DAMAGE.get());
            Vec3 away = new Vec3(victim.getX() - getX(), 0, victim.getZ() - getZ());
            if (away.lengthSqr() > 1.0E-4)
                victim.knockback(.4, away.normalize().x, away.normalize().z);
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(MaledictParticles.PURPLE_SOUL_FIRE.get(), center.x, center.y, center.z,
                                      empowered ? 40 : 26, radius * .35, .55, radius * .35, .025);
            serverLevel.sendParticles(ParticleTypes.ASH, center.x, center.y, center.z,
                                      empowered ? 44 : 28, radius * .4, .5, radius * .4, .03);
        }
        playSound(SoundEvents.GENERIC_EXPLODE.value(), empowered ? 2.2F : 1.6F, empowered ? .75F : .9F);
        playSound(SoundEvents.STONE_BREAK, 2.4F, .5F);
    }

    private DamageSource damageSource(boolean explosion) {
        Entity ownerEntity = owner != null && level() instanceof ServerLevel serverLevel
                             ? serverLevel.getEntity(owner) : null;
        if (ownerEntity instanceof LivingEntity living) {
            return explosion ? damageSources().indirectMagic(this, living) : damageSources().mobAttack(living);
        }
        return explosion ? damageSources().magic() : damageSources().generic();
    }

    private static boolean canHit(LivingEntity entity) {
        return DemiurgeTearEntity.canHit(entity) && !(entity instanceof VicissitudeBossEntity);
    }

    private void warningParticles() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + .15, getZ(),
                                  1, .3, .05, .3, .005);
        if (age % 3 == 0) {
            serverLevel.sendParticles(MaledictParticles.PURPLE_SOUL_FIRE.get(), getX(), getY() + .2, getZ(),
                                      2, .25, .1, .25, .01);
        }
    }

    private void armedParticles() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        serverLevel.sendParticles(MaledictParticles.PURPLE_SOUL_FIRE.get(), getX(), getY() + pillarHeight() * .55,
                                  getZ(), 2, .35, .35, .35, .012);
        if (age % 4 == 0) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + pillarHeight() * .7, getZ(),
                                      3, .3, .3, .3, .01);
        }
    }

    private void empoweredParticles() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        serverLevel.sendParticles(MaledictParticles.PURPLE_SOUL_FIRE.get(), getX(), getY() + pillarHeight() * .6,
                                  getZ(), 8, .4, .4, .4, .03);
        serverLevel.sendParticles(ParticleTypes.ASH, getX(), getY() + pillarHeight() * .5, getZ(),
                                  5, .4, .4, .4, .02);
    }

    public static void discardOwned(DemiurgeStatueEntity owner) {
        if (owner.level().isClientSide)
            return;
        for (var pillar : owner.level().getEntitiesOfClass(
                DemiurgePillarEntity.class, owner.getBoundingBox().inflate(96), p -> p.isOwnedBy(owner))) {
            pillar.discard();
        }
    }

    @Override
    public boolean isPickable() {
        return !isInvisible() && !isRemoved() && phase() != Phase.DESTROYED && phase() != Phase.LAUNCHED;
    }

    @Override
    public boolean isAttackable() {
        return !isInvisible() && phase() != Phase.DESTROYED && phase() != Phase.EXPLODING
               && phase() != Phase.LAUNCHED;
    }

    @Override
    public boolean canBeCollidedWith() {
        return canBlockMovement();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
        // Deliberately immovable: it is a temporary stone obstacle, not a moving rock.
    }

    @Override
    public boolean canBeHitByProjectile() {
        return isAttackable();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float height = pillarHeight();
        if (phase() == Phase.WARNING)
            height = 1;
        else if (phase() == Phase.RISING)
            height = Math.max(1, height * rise());
        return EntityDimensions.scalable(WIDTH, height);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == PILLAR_HEIGHT || key == RISE)
            refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PHASE, (byte) Phase.WARNING.ordinal());
        builder.define(PILLAR_HEIGHT, MIN_HEIGHT);
        builder.define(RISE, .12F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        health = tag.getFloat("PillarHealth");
        delay = tag.getInt("PillarDelay");
        age = tag.getInt("PillarAge");
        entityData.set(PHASE, tag.getByte("PillarPhase"));
        entityData.set(PILLAR_HEIGHT, tag.getInt("PillarHeight"));
        entityData.set(RISE, tag.getFloat("PillarRise"));
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null)
            tag.putUUID("Owner", owner);
        tag.putFloat("PillarHealth", health);
        tag.putInt("PillarDelay", delay);
        tag.putInt("PillarAge", age);
        tag.putByte("PillarPhase", entityData.get(PHASE));
        tag.putInt("PillarHeight", pillarHeight());
        tag.putFloat("PillarRise", rise());
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
