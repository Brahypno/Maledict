package org.brahypno.maledict.common.entity.demiurge;

import com.sammy.malum.registry.common.MobEffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.brahypno.changelib.DamageHelper.DamageStage;
import org.brahypno.maledict.common.entity.vicissitude.VicissitudeBossEntity;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.registry.MaledictParticles;
import org.brahypno.maledict.registry.MaledictSounds;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

/**
 * A star awakens the statue in a rain of tears; the raised serpent then patrols and fights.
 */
public final class DemiurgeStatueEntity extends VicissitudeBossEntity {
    private static final double BASE_MOVEMENT_SPEED = .30;
    /**
     * The ram reads the whole 5.1-wide body, not only the raised head bones.
     */
    private static final double CHARGE_HIT_PADDING = .65;
    /**
     * After the roar, the locked disk keeps drizzling for one minute as battle backdrop weather.
     */
    private static final int RAIN_LINGER_TICKS = 1200;
    /**
     * Half the roar's rate: a sustained damage window, not just a visual haze.
     */
    private static final int AMBIENT_RAIN_DROPS = 4;
    /**
     * Authored death sequence: violet out, black stone, then dust.
     */
    public static final int DEATH_TICKS = 80;
    /**
     * Ticks of fading violet emission; the renderer fades exactly these ticks.
     */
    public static final int DEATH_GLOW_OUT = 20;
    private static final int DEATH_DUST_START = 26;
    private static final EntityDataAccessor<Integer> BODY_CUT = SynchedEntityData.defineId(
            DemiurgeStatueEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> AWAKENING_START = SynchedEntityData.defineId(
            DemiurgeStatueEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(
            DemiurgeStatueEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ATTACK_START = SynchedEntityData.defineId(
            DemiurgeStatueEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Vector3f> BEAM_DIRECTION = SynchedEntityData.defineId(
            DemiurgeStatueEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> SWEEP_SIDE = SynchedEntityData.defineId(
            DemiurgeStatueEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SWEEP_LIMIT = SynchedEntityData.defineId(
            DemiurgeStatueEntity.class, EntityDataSerializers.FLOAT);
    private float currentHeight = 4.2F;
    private boolean roarPlayed;
    private boolean clientRoarPending;
    private final DemiurgeTrail trail = new DemiurgeTrail();
    private double slitherClock;
    private double previousSlitherClock;
    private double horizontalStep;
    private int combatDifficulty = -1;
    private Vec3 awakeningRainOrigin;
    private Vec3 attackRainOrigin;
    private Vec3 lingeringRainOrigin;
    private long lingeringRainUntil;
    private List<DemiurgePillarEntity.Spot> falseCreationSpots = List.of();
    private final Set<UUID> sweepHits = new HashSet<>();
    /**
     * Terrain that was verified as non-breakable; keeps the pathfinder from choosing it again.
     */
    private final Map<BlockPos, BlockState> blockedTerrain = new HashMap<>();
    private boolean sweepHitPillar;
    private float sweepBlockedX;
    private int stuckTicks;
    private int stuckRecoveries;
    private final DemiurgeRig.Pose pose = DemiurgeRig.INSTANCE.pose();
    private final DemiurgePart[] parts = new DemiurgePart[DemiurgeRig.INSTANCE.size];
    private final ServerBossEvent bossBar = (ServerBossEvent) new ServerBossEvent(
            getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS)
            .setDarkenScreen(true);

    public DemiurgeStatueEntity(EntityType<? extends DemiurgeStatueEntity> type, Level level) {
        super(type, level);
        setMaxUpStep(1.0F);
        moveControl = new DemiurgeMoveControl(this);
        for (int i = 0; i < parts.length; i++)
            parts[i] = new DemiurgePart(this);
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        updateParts();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MAX_HEALTH, 600).add(Attributes.ARMOR, 10)
                                     .add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED).add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    public void onAddedToWorld() {
        if (!level().isClientSide){
            if (combatDifficulty < 0)
                combatDifficulty = level().getDifficulty().getId();
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(DemiurgeDifficulty.maxHealth(Difficulty.byId(combatDifficulty)));
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(BASE_MOVEMENT_SPEED);
        }
        super.onAddedToWorld();
    }

    @Override
    protected float initialVitalityMaximum() {
        return DemiurgeDifficulty.maxHealth(Difficulty.byId(combatDifficulty));
    }

    /**
     * 3% of the authoritative pool per hit, before the 10 armour. A normal 7-9 damage sword stays
     * below this cap, so armour is the only cut it sees; a high-damage weapon is held at
     * 12/18/24 (easy/normal/hard) and loses 10.1/16.6/22.1 after armour.
     */
    @Override
    protected float getVitalityDamageLimit() {
        return getVitalityMaximum() * 0.03F;
    }

    @Override
    protected boolean isDamageImmune(DamageSource source) {
        return entityData.get(AWAKENING_START) < 0L;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return isDamageImmune(source) || super.isInvulnerableTo(source);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new DemiurgeNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new DemiurgeCombatGoal(this));
        goalSelector.addGoal(5, new DemiurgePatrolGoal(this));
        targetSelector.addGoal(1, new DemiurgeHurtByTargetGoal(this));
        targetSelector.addGoal(2, new DemiurgeNearestPlayerTargetGoal(this));
    }

    /**
     * The raid boss sits at the top health-defense grade; its own damage path still applies.
     */
    @Override
    protected DamageStage combatProtectionGrade() {
        return DamageStage.D5_RECOVERY_SOURCE;
    }

    @Override
    protected boolean canHoldAiTarget(LivingEntity candidate) {
        return super.canHoldAiTarget(candidate)
               && !isAlliedTo(candidate)
               && canAttackType(candidate.getType())
               && canAttack(candidate);
    }

    /**
     * Package-private bridge for this entity's target goals; the controller itself stays private.
     */
    void selectTargetFromGoal(@Nullable LivingEntity target) {
        selectAiTarget(target);
    }

    public boolean canPatrol() {
        return attack() == DemiurgeAttack.NONE
               && DemiurgeSlither.canPatrol(awakeningAge(0), isAlive(), getTarget() != null && getTarget().isAlive());
    }

    public boolean canFight() {
        return isAlive() && awakeningAge(0) >= DemiurgeAwakening.DURATION + DemiurgeSlither.TRANSITION_TICKS;
    }

    boolean canNavigate() {
        return canPatrol() || canFight() && attack() == DemiurgeAttack.NONE && getTarget() != null;
    }

    public DemiurgeAttack attack() {return DemiurgeAttack.values()[entityData.get(ATTACK)];}

    public float attackAge(float partialTick) {
        return attack() == DemiurgeAttack.NONE ? -1 : Math.max(0, level().getGameTime() - entityData.get(ATTACK_START) + partialTick);
    }

    boolean beginAttack(DemiurgeAttack attack, LivingEntity target) {
        if (!canFight() || attack == DemiurgeAttack.NONE)
            return false;
        if (attack == DemiurgeAttack.ROAR && !canStartRain(target))
            return false;
        if (attack == DemiurgeAttack.FALSE_CREATION){
            falseCreationSpots = DemiurgePillarEntity.planArray(this, target.position());
            if (falseCreationSpots.isEmpty())
                return false;
        }
        int side = attack == DemiurgeAttack.SWEEP ? sweepSideTowards(target.getBoundingBox()) : 0;
        if (attack == DemiurgeAttack.SWEEP && side == 0)
            return false;
        getNavigation().stop();
        attackRainOrigin = attack == DemiurgeAttack.ROAR ? headCenter() : null;
        if (attack == DemiurgeAttack.ROAR){
            // The roar's own disk becomes the backdrop after the cast; a new roar re-anchors it.
            lingeringRainOrigin = attackRainOrigin;
            lingeringRainUntil = level().getGameTime() + RAIN_LINGER_TICKS;
        }
        sweepHits.clear();
        sweepHitPillar = false;
        entityData.set(SWEEP_SIDE, side);
        entityData.set(SWEEP_LIMIT, 1.0F);
        entityData.set(ATTACK_START, level().getGameTime());
        entityData.set(ATTACK, attack.ordinal());
        if (attack == DemiurgeAttack.LASER){
            setBeamDirection(target.getBoundingBox().getCenter().subtract(eyeOrigin(0)).normalize());
        }
        return true;
    }

    Vec3 attackLocalPosition(Vec3 world) {
        return world.subtract(position()).yRot(getYRot() * Mth.DEG_TO_RAD);
    }

    /**
     * Turn toward the target along a short forward arc. Pivoting the whole
     * statue in place looked wrong on the long body; a low-speed step angled
     * toward the target lets the normal two-degree steering bend the body
     * through a small curve instead.
     */
    void turnArc(LivingEntity target) {
        float bearing = (float) Math.toDegrees(Math.atan2(getX() - target.getX(), target.getZ() - getZ()));
        float delta = Mth.wrapDegrees(bearing - getYRot());
        if (Math.abs(delta) < .001F)
            return;
        float direction = Math.signum(delta);
        float stepYaw = getYRot() + direction * Math.min(20.0F, Math.abs(delta));
        Vec3 forward = Vec3.directionFromRotation(0, stepYaw);
        getNavigation().stop();
        getMoveControl().setWantedPosition(getX() + forward.x * 2.0F, getY(),
                                           getZ() + forward.z * 2.0F, .55F);
    }

    private boolean isNearSweepingBody(AABB target) {
        double reach = DemiurgeSweep.amplitude(bodyCut()) + .5;
        for (int bone = 0; bone < parts.length; bone++) {
            if (isSweepingBone(bone) && parts[bone].getBoundingBox().inflate(reach, .3, reach).intersects(target)){
                return true;
            }
        }
        return false;
    }

    private int sweepSideTowards(AABB target) {
        if (!isNearSweepingBody(target))
            return 0;
        // Use the middle of the swinging tail, not its rear tip. The old nearest-bone pivot could
        // choose a side from the last joint and swing the mass away from a target near the tail.
        Vec3 targetCenter = target.getCenter();
        int midpoint = DemiurgeRig.INSTANCE.sweepMidpointBone(bodyCut());
        Vec3 anchor = parts[midpoint].getBoundingBox().getCenter();
        int preferred = attackLocalPosition(targetCenter).x >= attackLocalPosition(anchor).x ? 1 : -1;
        var preview = DemiurgeRig.INSTANCE.pose();
        if (sweepCanReach(preview, target, preferred))
            return preferred;
        return sweepCanReach(preview, target, -preferred) ? -preferred : 0;
    }

    private boolean sweepCanReach(DemiurgeRig.Pose preview, AABB target, int side) {
        float amplitude = DemiurgeSweep.amplitude(bodyCut());
        AABB[] previous = new AABB[parts.length];
        // Sample every other tick of the swing and its last damaging pose. The same
        // swept bounds used by damage bridge the gaps between samples.
        for (int tick = DemiurgeSweep.WINDUP_END; tick <= DemiurgeSweep.ACTIVE_END; tick += 2) {
            int age = Math.min(tick, DemiurgeSweep.ACTIVE_END - 1);
            preview.sample(awakeningAge(0), slitherClock(1), DemiurgeAttack.SWEEP, age, position(), getYRot(),
                           this::trailPoint, bodyCut(), null, new DemiurgeSweep.State(side, age, amplitude, 1));
            for (int bone = 0; bone < parts.length; bone++) {
                if (!isSweepingBone(bone))
                    continue;
                AABB current = preview.bounds(bone, position(), getYRot());
                if (DemiurgeSweep.contactBounds(previous[bone] == null ? current : previous[bone], current)
                                 .intersects(target))
                    return true;
                previous[bone] = current;
            }
        }
        return false;
    }

    private boolean isSweepingBone(int bone) {
        return DemiurgeRig.INSTANCE.canSweep(bone) && !pose.isHidden(bone)
               && parts[bone].getBoundingBox().getSize() > .01;
    }

    void finishAttack() {
        entityData.set(ATTACK, DemiurgeAttack.NONE.ordinal());
        attackRainOrigin = null;
        falseCreationSpots = List.of();
        sweepHits.clear();
        sweepHitPillar = false;
        entityData.set(SWEEP_SIDE, 0);
        entityData.set(SWEEP_LIMIT, 1.0F);
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    void aimAttack(LivingEntity target) {
        float age = attackAge(0);
        if (attack() == DemiurgeAttack.ROAR || attack() == DemiurgeAttack.SWEEP
            || attack() == DemiurgeAttack.FALSE_CREATION)
            return;
        if (attack() == DemiurgeAttack.CHARGE && age >= attack().release)
            return;
        float bearing = (float) Math.toDegrees(Math.atan2(getX() - target.getX(), target.getZ() - getZ()));
        // The charge windup only has twenty ticks before the direction locks.
        // Two degrees per tick is enough for a laser track, but not for a heavy
        // body that has to commit to a charge, so the windup turns faster.
        setYRot(Mth.approachDegrees(getYRot(), bearing,
                                    attack() == DemiurgeAttack.CHARGE ? 6.0F : 2.0F));
        if (attack() == DemiurgeAttack.LASER){
            Vec3 desired = target.getBoundingBox().getCenter().subtract(eyeOrigin(0)).normalize();
            setBeamDirection(beamDirection().lerp(desired, .08).normalize());
        }
    }

    private void setBeamDirection(Vec3 direction) {
        direction = DemiurgeAim.constrain(direction, getYRot());
        entityData.set(BEAM_DIRECTION, new Vector3f((float) direction.x, (float) direction.y, (float) direction.z));
    }

    public Vec3 beamDirection() {
        var direction = entityData.get(BEAM_DIRECTION);
        return new Vec3(direction.x, direction.y, direction.z);
    }

    /**
     * Reconstructed from synced state, so the client bends the same body the server hits.
     */
    public DemiurgeSweep.State sweepState(float partialTick) {
        if (attack() != DemiurgeAttack.SWEEP)
            return DemiurgeSweep.State.none();
        return new DemiurgeSweep.State(entityData.get(SWEEP_SIDE), attackAge(partialTick),
                                       DemiurgeSweep.amplitude(bodyCut()), entityData.get(SWEEP_LIMIT));
    }

    public Vec3 eyeOrigin(int side) {return pose.eye(side, position(), getYRot());}

    public Vec3 headCenter() {return pose.headCenter(position(), getYRot());}

    public Vec3 headForward() {return pose.headForward(getYRot());}

    boolean canStartRain(LivingEntity target) {
        return target instanceof Player && canAttack(target)
               && target.position().subtract(headCenter()).horizontalDistanceSqr() <= DemiurgeAttack.RAIN_RADIUS * DemiurgeAttack.RAIN_RADIUS;
    }

    public Vec3 laserEnd(Vec3 from) {
        return laserEnd(from, headForward());
    }

    public Vec3 laserEnd(Vec3 from, Vec3 direction) {
        Vec3 to = from.add(direction.scale(DemiurgeAttack.LASER_RANGE));
        Vec3 end = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getLocation();
        double nearest = from.distanceToSqr(end);
        // The raised pillars are entities, not blocks, so the block clip cannot see them.
        // Both the client beam and the server damage stop at the nearest pillar surface.
        for (var pillar : level().getEntitiesOfClass(DemiurgePillarEntity.class,
                                                     new AABB(from, to).inflate(.5), DemiurgePillarEntity::blocksLaser)) {
            if (pillar.getBoundingBox().contains(from))
                return from.add(direction.scale(.01));
            var hit = pillar.getBoundingBox().inflate(.04).clip(from, to);
            if (hit.isEmpty())
                continue;
            double distance = from.distanceToSqr(hit.get());
            if (distance < nearest){
                nearest = distance;
                end = hit.get();
            }
        }
        return end;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return DemiurgeTearEntity.canHit(target) && super.canAttack(target);
    }

    @Override
    public boolean isMultipartEntity() {return true;}

    @Override
    public DemiurgePart[] getParts() {return parts;}

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts != null)
            for (int i = 0; i < parts.length; i++)
                parts[i].setId(id + i + 1);
    }

    void updateParts() {
        pose.sample(awakeningAge(0), slitherClock(1), attack(), attackAge(0), position(), getYRot(),
                    this::trailPoint, bodyCut(), beamDirection(), sweepState(0));
        for (int i = 0; i < parts.length; i++)
            parts[i].place(pose.bounds(i, position(), getYRot()));
    }

    /**
     * Trailing body sections lost so far. The value only ever grows during one encounter, so the
     * floor never regrows after healing; the authored cap is {@link DemiurgeRig#maxBodyCut()}.
     */
    public int bodyCut() {
        return entityData.get(BODY_CUT);
    }

    /**
     * Health thresholds sever another four sections; the cap keeps at least eight blocks of body on
     * the floor. The server owns the value and clients only render the synced mirror.
     */
    private boolean updateBodyCut() {
        if (level().isClientSide)
            return false;
        float maximum = getMaxHealth();
        int allowed = DemiurgeRig.INSTANCE.maxBodyCut();
        int target = DemiurgeBodyLength.cutFor(maximum > 0.0F ? getHealth() / maximum : 0.0F, allowed);
        if (target <= bodyCut())
            return false;
        entityData.set(BODY_CUT, Math.min(allowed, target));
        return true;
    }

    /**
     * A severed section bursts into dust at its own end, before the rest of the body follows.
     */
    private void burstBodyCut() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        DemiurgePart end = parts[DemiurgeRig.INSTANCE.lastRetainedBone(bodyCut())];
        AABB box = end.getBoundingBox();
        double x = box.getCenter().x, y = box.getCenter().y, z = box.getCenter().z;
        serverLevel.sendParticles(ParticleTypes.ASH, x, y, z, 26,
                                  Math.max(.2, box.getXsize() * .35), Math.max(.2, box.getYsize() * .45),
                                  Math.max(.2, box.getZsize() * .35), .02);
        serverLevel.sendParticles(ParticleTypes.POOF, x, y, z, 6, .18, .22, .18, .01);
        playSound(SoundEvents.STONE_BREAK, 2.0F, .55F);
    }

    public float slitherClock(float partialTick) {
        return (float) ((previousSlitherClock + (slitherClock - previousSlitherClock) * partialTick) % DemiurgeSlither.CYCLE_TICKS);
    }

    public DemiurgeTrail.Point trailPoint(double distance, Vec3 origin, float yaw) {
        return trail.sample(distance, origin, yaw);
    }

    double horizontalStep() {
        return horizontalStep;
    }

    /**
     * Short obstacle contact is handled by the move control without dropping the path. A full
     * second without displacement is different: the selected path is not usable, so it is dropped
     * and the owning goal has to choose a reachable destination.
     */
    private void recoverFromStuck(double distance) {
        if (level().isClientSide || !canNavigate() || getNavigation().isDone()){
            stuckTicks = 0;
            return;
        }
        // A path exists but the body has not moved. The threshold is long
        // enough that a normal slow turn or a freshly issued path is not
        // cancelled before it has had a chance to run.
        if (distance >= .01){
            stuckTicks = 0;
            return;
        }
        if (++stuckTicks < 24)
            return;
        stuckTicks = 0;
        stuckRecoveries++;
        // A transient obstacle only clears velocity and keeps its path. A full
        // second without displacement means this path is not actually usable;
        // drop it so the goal can select a reachable destination.
        onTerrainBlocked();
    }

    /**
     * Called after a short run of solid, non-breakable obstacles or a long stall.
     */
    void onTerrainBlocked() {
        if (level().isClientSide)
            return;
        getNavigation().stop();
        stuckTicks = 0;
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    boolean isTerrainBlocked(BlockPos pos) {
        BlockState remembered = blockedTerrain.get(pos);
        if (remembered == null)
            return false;
        // A player or another system may replace the protected block with a
        // different one. Only the exact verified state stays blacklisted.
        if (!level().getBlockState(pos).equals(remembered)){
            blockedTerrain.remove(pos);
            return false;
        }
        return true;
    }

    void rememberBlockedTerrain(BlockPos pos) {
        if (blockedTerrain.size() > 256)
            blockedTerrain.clear();
        BlockPos immutable = pos.immutable();
        blockedTerrain.put(immutable, level().getBlockState(immutable));
    }

    /**
     * Pathfinder-backed check used before a patrol destination is accepted.
     */
    boolean canReach(Vec3 point) {
        if (level().isClientSide)
            return true;
        var path = getNavigation().createPath(BlockPos.containing(point), 1);
        return path != null && path.canReach();
    }

    /**
     * Number of acknowledged stalls; test-visible without exposing navigation internals.
     */
    public int stuckRecoveries() {
        return stuckRecoveries;
    }

    java.util.List<Vec3> patrolBody() {
        double end = Math.max(4.0, DemiurgeRig.INSTANCE.bodyEndDistance(bodyCut()) + .9);
        return DemiurgePatrolSteering.body(position(), getYRot(), trail::sample, end);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(BODY_CUT, 0);
        entityData.define(AWAKENING_START, -1L);
        entityData.define(ATTACK, 0);
        entityData.define(ATTACK_START, -1L);
        entityData.define(BEAM_DIRECTION, new Vector3f(0, 0, 1));
        entityData.define(SWEEP_SIDE, 0);
        entityData.define(SWEEP_LIMIT, 1.0F);
    }

    public float awakeningAge(float partialTick) {
        long start = entityData.get(AWAKENING_START);
        return start < 0 ? -1 : Math.max(0, level().getGameTime() - start + partialTick);
    }

    public boolean isAwake() {
        return awakeningAge(0) >= DemiurgeAwakening.DURATION;
    }

    /**
     * 刷怪蛋用：把这一只直接交给世界，处于激活后的状态。激活时间轴写在同步数据里，因此在实体进入世界之前
     * 往前拨一整段就够了 —— 觉醒的雨与怒吼被跳过，落地就是编好的终态（抬起前半身的地面站姿，
     * {@link DemiurgeSlither#HEIGHT}），但 {@code onAddedToWorld} 仍然照常按难度评级血量池。
     * <p>
     * 未激活的石像不需要这个入口：那是 {@code AWAKENING_START = -1} 的默认状态，等腐枯之星来唤醒。
     */
    public void activateOnSpawn() {
        if (level().isClientSide || entityData.get(AWAKENING_START) >= 0)
            return;
        // 刚造出来的世界里 gameTime 还没走满一整段觉醒，减出来会是负数，而负数是"从未激活"的哨兵值。
        // 那种情况下只能从 0 起算：石像会在复活时长走够 140 tick 之后自然进入战斗姿态。
        long start = Math.max(0L, level().getGameTime()
                                   - DemiurgeAwakening.DURATION - DemiurgeSlither.TRANSITION_TICKS);
        entityData.set(AWAKENING_START, start);
        // 载入存档走的是同一条路（readAdditionalSaveData）：身高要从 awakeningAge 重算并立刻刷新碰撞箱，
        // 否则 tick 里 height == currentHeight，这一次刷新就永远不会发生了。
        currentHeight = DemiurgeSlither.height(awakeningAge(0));
        refreshDimensions();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!stack.is(MaledictItems.BLIGHTED_STAR.get()) || player.isSpectator()){
            return super.mobInteract(player, hand);
        }
        if (level().getDifficulty() == Difficulty.PEACEFUL
            || !isAlive() || entityData.get(AWAKENING_START) >= 0){
            return InteractionResult.FAIL;
        }
        if (!level().isClientSide){
            updateParts();
            awakeningRainOrigin = headCenter();
            entityData.set(AWAKENING_START, level().getGameTime());
            // Explicitly costs one star, including creative mode. Both hands
            // and repeated packets share the already-started server state.
            stack.shrink(1);
            playSound(SoundEvents.GRINDSTONE_USE, 0.9F, 0.55F);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void tick() {
        Vec3 before = position();
        AABB previousBody = getBoundingBox();
        AABB[] previousBounds = new AABB[parts.length];
        for (int i = 0; i < parts.length; i++)
            previousBounds[i] = parts[i].getBoundingBox();
        previousSlitherClock = slitherClock;
        super.tick();
        if (!level().isClientSide){
            bossBar.setProgress(Mth.clamp(getHealth() / Math.max(1.0F, getMaxHealth()), 0.0F, 1.0F));
        }
        boolean severed = updateBodyCut();
        double distance = position().subtract(before).horizontalDistance();
        horizontalStep = distance;
        recoverFromStuck(distance);
        slitherClock += distance * DemiurgeSlither.CYCLE_TICKS / DemiurgeSlither.BLOCKS_PER_CYCLE;
        float age = awakeningAge(0);
        // The ground body must not follow airborne positions from spawning or
        // awakening. An empty trail already extends behind the current feet.
        if (age >= DemiurgeAwakening.DURATION + DemiurgeSlither.TRANSITION_TICKS && onGround()){
            trail.record(position(), getYRot());
        }
        float height = DemiurgeSlither.height(age);
        if (height != currentHeight){
            currentHeight = height;
            refreshDimensions();
        }
        if (!level().isClientSide && isAlive() && !roarPlayed && age >= DemiurgeAwakening.ROAR_TICK){
            roarPlayed = true;
            if (age < DemiurgeAwakening.ROAR_TICK + 20){
                playSound(MaledictSounds.DEMIURGE_AWAKENING_ROAR.get(), 3.0F, 1.0F);
                level().broadcastEntityEvent(this, DemiurgeAwakening.ROAR_EVENT);
            }
        }
        updateParts();
        if (severed)
            burstBodyCut();
        if (!level().isClientSide && isAlive())
            tickAttacks(age, previousBounds, previousBody);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        bossBar.removePlayer(player);
        super.stopSeenByPlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        if (bossBar != null)
            bossBar.setName(getDisplayName());
    }

    /**
     * During the damage window the statue must not shove a player out of its own path before the
     * contact probe runs; that push made a stationary target survive a head-on charge. Outside the
     * charge the normal body separation is kept.
     */
    @Override
    protected void pushEntities() {
        if (attack() == DemiurgeAttack.CHARGE && attack().active(attackAge(0)))
            return;
        super.pushEntities();
    }

    private void tickAttacks(float awakeningAge, AABB[] previousBounds, AABB previousBody) {
        float age = attackAge(0);
        var attack = attack();
        if (attack == DemiurgeAttack.CHARGE && attack.active(age)){
            Vec3 chargeDirection = Vec3.directionFromRotation(0, getYRot());
            Set<LivingEntity> contacts = new HashSet<>();
            // The animated head rides more than four blocks above the feet. A player standing on
            // the ground could sit under it and never touch a part box, so the ram also uses the
            // statue's full width and height for the swept tick.
            AABB ram = getBoundingBox().minmax(previousBody)
                                       .inflate(CHARGE_HIT_PADDING, .35, CHARGE_HIT_PADDING);
            contacts.addAll(level().getEntitiesOfClass(LivingEntity.class, ram, this::canAttack));
            for (int i = 0; i < parts.length; i++) {
                var box = parts[i].getBoundingBox().minmax(previousBounds[i]);
                if (box.getSize() < .01)
                    continue;
                AABB padded = box.inflate(CHARGE_HIT_PADDING, .3, CHARGE_HIT_PADDING);
                contacts.addAll(level().getEntitiesOfClass(LivingEntity.class, padded, this::canAttack));
                for (var pillar : level().getEntitiesOfClass(DemiurgePillarEntity.class, padded,
                                                             DemiurgePillarEntity::canLaunch)) {
                    pillar.launch(chargeDirection);
                }
            }
            for (var pillar : level().getEntitiesOfClass(DemiurgePillarEntity.class, ram,
                                                         DemiurgePillarEntity::canLaunch)) {
                pillar.launch(chargeDirection);
            }
            for (var victim : contacts) {
                // Merge overlapping parts before invoking hurt, and let vanilla's
                // equal-damage i-frame window govern sustained contact.
                DemiurgeDifficulty.hurt(victim, damageSources().mobAttack(this), MaledictConfig.DEMIURGE_CHARGE_DAMAGE.get());
            }
        }
        if (attack == DemiurgeAttack.LASER && attack.active(age)
            && ((int) age - attack.release) % DemiurgeAttack.LASER_INTERVAL == 0)
            fireLasers();
        if (attack == DemiurgeAttack.SWEEP && attack.active(age))
            tickSweep(previousBounds, age);
        if (attack == DemiurgeAttack.FALSE_CREATION && age >= attack.release && !falseCreationSpots.isEmpty()){
            DemiurgePillarEntity.summonArray(this, falseCreationSpots);
            falseCreationSpots = List.of();
        }
        if (attack == DemiurgeAttack.ROAR && (int) age == attack.release){
            playSound(MaledictSounds.DEMIURGE_AWAKENING_ROAR.get(), 3, 1);
            level().broadcastEntityEvent(this, DemiurgeAwakening.ROAR_EVENT);
        }
        if (awakeningAge >= 0 && awakeningAge < DemiurgeAwakening.DURATION){
            if (awakeningRainOrigin == null)
                awakeningRainOrigin = headCenter();
            releaseRain(awakeningRainOrigin);
        }else if (attack == DemiurgeAttack.ROAR){
            if (attack.active(age) && attackRainOrigin != null){
                releaseRain(attackRainOrigin);
            }else if (age >= attack.recovery && lingeringRainOrigin != null
                      && level().getGameTime() < lingeringRainUntil){
                // The roar's recovery already belongs to the backdrop drizzle.
                releaseRain(lingeringRainOrigin, AMBIENT_RAIN_DROPS);
            }
        }else if (lingeringRainOrigin != null && level().getGameTime() < lingeringRainUntil){
            // A thin drizzle after the roar performs as the encounter's background weather.
            releaseRain(lingeringRainOrigin, AMBIENT_RAIN_DROPS);
        }
    }

    private void tickSweep(AABB[] previousBounds, float age) {
        int side = entityData.get(SWEEP_SIDE);
        if (side == 0)
            return;
        List<AABB> swept = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            if (isSweepingBone(i))
                swept.add(DemiurgeSweep.contactBounds(previousBounds[i], parts[i].getBoundingBox()));
        }
        // Resolve the pillar contact for the whole tick before applying any
        // victim damage. Otherwise a later body section could reach through
        // the pillar and hit a player standing behind it in the same tick.
        if (!sweepHitPillar){
            for (AABB box : swept) {
                var pillars = level().getEntitiesOfClass(DemiurgePillarEntity.class, box,
                                                         DemiurgePillarEntity::blocksSweep);
                if (pillars.isEmpty())
                    continue;
                DemiurgePillarEntity pillar = pillars.get(0);
                double nearest = Double.MAX_VALUE;
                for (var candidate : pillars) {
                    double distance = candidate.position().distanceToSqr(position());
                    if (distance < nearest){
                        nearest = distance;
                        pillar = candidate;
                    }
                }
                pillar.empower();
                sweepHitPillar = true;
                // Freeze the strike at the pillar; the recovery still retracts the body.
                entityData.set(SWEEP_LIMIT, Math.max(0, DemiurgeSweep.fraction(age)));
                sweepBlockedX = (float) attackLocalPosition(pillar.position()).x;
                playSound(SoundEvents.STONE_HIT, 2.0F, .55F);
                break;
            }
        }
        Set<LivingEntity> contacts = new HashSet<>();
        for (AABB box : swept)
            contacts.addAll(level().getEntitiesOfClass(LivingEntity.class, box, this::canAttack));
        Vec3 push = new Vec3(side, 0, 0).yRot(-getYRot() * Mth.DEG_TO_RAD);
        for (var victim : contacts) {
            if (sweepHits.contains(victim.getUUID()))
                continue;
            if (sweepHitPillar && side * (attackLocalPosition(victim.position()).x - sweepBlockedX) > .15)
                continue;
            if (!DemiurgeDifficulty.hurt(victim, damageSources().mobAttack(this),
                                         MaledictConfig.DEMIURGE_SWEEP_DAMAGE.get()))
                continue;
            sweepHits.add(victim.getUUID());
            // LivingEntity#knockback subtracts the supplied direction.
            victim.knockback(.35, -push.x, -push.z);
            if (level() instanceof ServerLevel serverLevel){
                serverLevel.sendParticles(ParticleTypes.ASH, victim.getX(), victim.getY() + .6, victim.getZ(),
                                          8, .35, .25, .35, .01);
            }
        }
    }

    void fireLasers() {
        Set<UUID> hit = new HashSet<>();
        for (int side = 0; side < 2; side++) {
            Vec3 from = eyeOrigin(side), to = laserEnd(from);
            for (var victim : level().getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(.18), this::canAttack)) {
                var box = victim.getBoundingBox().inflate(.14);
                if ((box.contains(from) || box.clip(from, to).isPresent()) && hit.add(victim.getUUID())){
                    DemiurgeDifficulty.hurt(victim, damageSources().indirectMagic(this, this), MaledictConfig.DEMIURGE_LASER_DAMAGE.get());
                    victim.addEffect(new MobEffectInstance(MobEffectRegistry.SILENCED.get(), DemiurgeAttack.SILENCE_TICKS));
                }
            }
        }
    }

    private void releaseRain(Vec3 center) {
        releaseRain(center, 8);
    }

    /**
     * The same damaging tear is used during the roar and in the lingering drizzle.
     */
    private void releaseRain(Vec3 center, int drops) {
        for (int i = 0; i < drops; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = Math.sqrt(random.nextDouble()) * DemiurgeAttack.RAIN_RADIUS;
            var tear = MaledictEntities.DEMIURGE_TEAR.get().create(level());
            tear.setOwner(this);
            tear.setPos(center.x + Math.cos(angle) * radius, center.y + 10 + random.nextDouble() * 3,
                        center.z + Math.sin(angle) * radius);
            tear.setDeltaMovement(0, -DemiurgeTearEntity.FALL_SPEED, 0);
            level().addFreshEntity(tear);
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(5.1F, Math.max(4.2F, currentHeight));
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == DemiurgeAwakening.ROAR_EVENT){
            clientRoarPending = true;
        }else {
            super.handleEntityEvent(event);
        }
    }

    /**
     * Client-only presentation consumes a vanilla entity event without common-side client imports.
     */
    public boolean consumeRoarPulse() {
        boolean pending = clientRoarPending;
        clientRoarPending = false;
        return pending;
    }

    /**
     * The light dies first, the stone blackens, then the whole statue flakes away as dust. As with
     * the nether star, the fragment is a guaranteed spawn rather than a loot-table roll.
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        ItemEntity drop = spawnAtLocation(MaledictItems.LIGHT_FRAGMENT.get());
        if (drop != null){
            drop.setExtendedLifetime();
        }
    }

    @Override
    protected void onFinalDeath(DamageSource source) {
        super.onFinalDeath(source);
        if (!level().isClientSide){
            bossBar.setProgress(0.0F);
            bossBar.removeAllPlayers();
            lingeringRainOrigin = null;
            lingeringRainUntil = 0;
            attackRainOrigin = null;
            DemiurgePillarEntity.discardOwned(this);
        }
    }

    @Override
    protected int getDeathDurationTicks() {
        return DEATH_TICKS;
    }

    /**
     * Renderer clock for the authored death sequence, or -1 while the statue still lives.
     */
    public float getDeathTicks(float partialTick) {
        if (getHealth() > 0.0F || deathTime <= 0)
            return -1.0F;
        return Math.min(DEATH_TICKS, deathTime + partialTick);
    }

    /**
     * Vanilla soul fire in the statue's violet, leaving the head while the emission dies down.
     */
    private void ventDyingGlow() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        Vec3 head = headCenter();
        serverLevel.sendParticles(MaledictParticles.PURPLE_SOUL_FIRE.get(), head.x, head.y, head.z, 3, .6, .5, .6, .004);
    }

    /**
     * Ash flakes from the remaining stone so the render dissolve reads as crumbling, not blinking out.
     */
    private void shedDeathDust() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;
        for (int i = 0; i < 4; i++) {
            DemiurgePart part = parts[random.nextInt(parts.length)];
            AABB box = part.getBoundingBox();
            if (box.getSize() < .05)
                continue;
            double x = Mth.lerp(random.nextDouble(), box.minX, box.maxX);
            double y = Mth.lerp(random.nextDouble(), box.minY, box.maxY);
            double z = Mth.lerp(random.nextDouble(), box.minZ, box.maxZ);
            serverLevel.sendParticles(ParticleTypes.ASH, x, y, z, 2, .18, .12, .18, .012);
            if (random.nextInt(3) == 0){
                serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, .08, .05, .08, .004);
            }
        }
    }

    @Override
    protected void onDeathTick(int ticks) {
        if (level().isClientSide)
            return;
        if (ticks == 1){
            playSound(SoundEvents.STONE_BREAK, 2.5F, .45F);
        }
        if (ticks <= DEATH_GLOW_OUT){
            ventDyingGlow();
        }else if (ticks == DEATH_GLOW_OUT + 1){
            playSound(SoundEvents.GRAVEL_BREAK, 2.0F, .5F);
        }
        if (ticks >= DEATH_DUST_START && ticks < DEATH_TICKS){
            shedDeathDust();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("AwakeningStart", entityData.get(AWAKENING_START));
        tag.putBoolean("AwakeningRoarPlayed", roarPlayed);
        tag.putInt("BodyCut", bodyCut());
        if (combatDifficulty >= 0)
            tag.putInt("CombatDifficulty", combatDifficulty);
        if (awakeningRainOrigin != null){
            tag.putDouble("AwakeningRainX", awakeningRainOrigin.x);
            tag.putDouble("AwakeningRainY", awakeningRainOrigin.y);
            tag.putDouble("AwakeningRainZ", awakeningRainOrigin.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        blockedTerrain.clear();
        entityData.set(AWAKENING_START, tag.contains("AwakeningStart") ? tag.getLong("AwakeningStart") : -1L);
        entityData.set(BODY_CUT, Math.max(0, Math.min(DemiurgeRig.INSTANCE.maxBodyCut(),
                                                      tag.contains("BodyCut") ? tag.getInt("BodyCut") : 0)));
        roarPlayed = tag.getBoolean("AwakeningRoarPlayed");
        combatDifficulty = tag.contains("CombatDifficulty") ? tag.getInt("CombatDifficulty") : -1;
        awakeningRainOrigin = tag.contains("AwakeningRainX") ? new Vec3(tag.getDouble("AwakeningRainX"),
                                                                        tag.getDouble("AwakeningRainY"), tag.getDouble("AwakeningRainZ")) : null;
        currentHeight = DemiurgeSlither.height(awakeningAge(0));
        refreshDimensions();
        finishAttack();
        updateParts();
    }

    @Override
    public void travel(Vec3 input) {
        float chargeSpeed = attack().chargeSpeed(attackAge(0));
        if (!level().isClientSide && isAlive() && chargeSpeed > 0){
            Vec3 forward = Vec3.directionFromRotation(0, getYRot()).scale(chargeSpeed);
            boolean clear = DemiurgeBlockBreaking.clearAhead(this, getYRot(),
                                                             Math.max(.9, chargeSpeed * 1.5 + .2));
            setDeltaMovement(clear ? forward.x : 0, getDeltaMovement().y, clear ? forward.z : 0);
            super.travel(Vec3.ZERO);
        }else {
            if (!canNavigate())
                setDeltaMovement(0, getDeltaMovement().y, 0);
            super.travel(canNavigate() ? input : Vec3.ZERO);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return DemiurgeSlither.groundBlend(awakeningAge(0)) > 0
               ? getBoundingBox().inflate(42, 18, 42) : getBoundingBox();
    }
}
