package org.brahypno.maledict.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.brahypno.maledict.registry.MaledictEntities;

/** A neutral Vicissitude bird; trust is earned through breeding, never by taming adults. */
public final class RavenEntity extends Animal implements FlyingAnimal {
    /** About 1.71 blocks on ordinary ground, leaving clearance over one-block ledges. */
    private static final float GROUND_JUMP_POWER = 0.50F;
    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(RavenEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HOVERING =
            SynchedEntityData.defineId(RavenEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PECK_TICKS =
            SynchedEntityData.defineId(RavenEntity.class, EntityDataSerializers.INT);
    @Nullable
    private RavenEntity companion;
    @Nullable
    private Vec3 landingTarget;
    @Nullable
    private Vec3 hoverAnchor;
    private boolean landingRequested;
    private final RavenDangerEscape dangerEscape = new RavenDangerEscape();
    private int hopCooldown;
    private int perchCooldown = 100;
    private final List<UUID> trustedPlayers = new ArrayList<>(2);
    @Nullable
    private UUID breedingPlayer;
    public float previousFlightProgress;
    public float flightProgress;
    public float previousHoverProgress;
    public float hoverProgress;

    public RavenEntity(EntityType<? extends RavenEntity> type, Level level) {
        super(type, level);
        jumpControl = new RavenJumpControl();
        setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
        setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
        setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0F);
        setPathfindingMalus(BlockPathTypes.DAMAGE_OTHER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FLYING_SPEED, 0.85D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FLYING, false);
        entityData.define(HOVERING, false);
        entityData.define(PECK_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new EscapeDangerGoal());
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AttackGoal());
        goalSelector.addGoal(2, new LandingGoal());
        goalSelector.addGoal(3, new EatFleshGoal());
        goalSelector.addGoal(4, new BreedGoal(this, 1.0D) {
            @Override
            public void tick() {
                if (partner != null) {
                    moveToGroundTarget(partner.position(), 1.0D);
                    if (!isFlying() && !landingRequested && !((RavenEntity) partner).isFlying()) {
                        super.tick();
                    } else {
                        getLookControl().setLookAt(partner, 20.0F, 20.0F);
                    }
                }
            }

            @Override
            public void stop() {
                finishGroundTravel(partner == null ? null : partner.position());
                super.stop();
            }
        });
        goalSelector.addGoal(5, new FollowTrustedPlayerGoal());
        goalSelector.addGoal(6, new FollowRavenParentGoal());
        goalSelector.addGoal(7, new IdleGoal());
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, RavenEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
                10, true, false, target -> target.getMobType() == MobType.UNDEAD));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new GroundPathNavigation(this, level);
    }

    @Override
    public MobType getMobType() {
        return VicissitudeBossEntity.VICISSITUDE;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof RavenEntity) && !trusts(target.getUUID()) && super.canAttack(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target != null && (target instanceof RavenEntity || trusts(target.getUUID())) ? null : target);
    }

    @Override
    public boolean isFlying() {
        return entityData.get(FLYING);
    }

    public boolean isHovering() {
        return entityData.get(HOVERING);
    }

    private void setHovering(@Nullable Vec3 anchor) {
        hoverAnchor = anchor;
        entityData.set(HOVERING, anchor != null);
    }

    private void setFlying(boolean flying) {
        if (isFlying() == flying) {
            return;
        }
        navigation.stop();
        entityData.set(FLYING, flying);
        setNoGravity(flying);
        setXRot(0.0F);
        if (flying) {
            FlyingPathNavigation flightNavigation = new FlyingPathNavigation(this, level());
            flightNavigation.setCanFloat(true);
            flightNavigation.setCanPassDoors(true);
            flightNavigation.setCanOpenDoors(false);
            navigation = flightNavigation;
            moveControl = new FlyingMoveControl(this, 20, true);
            if (onGround()) {
                setDeltaMovement(getDeltaMovement().add(0.0D, 0.35D, 0.0D));
            }
        } else {
            setHovering(null);
            landingRequested = false;
            landingTarget = null;
            navigation = new GroundPathNavigation(this, level());
            moveControl = new MoveControl(this);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        previousFlightProgress = flightProgress;
        flightProgress = Math.max(0.0F, Math.min(1.0F,
                flightProgress + (isFlying() ? 0.15F : -0.15F)));
        previousHoverProgress = hoverProgress;
        hoverProgress = Mth.clamp(hoverProgress + (isHovering() ? 0.2F : -0.2F), 0.0F, 1.0F);
        if (!level().isClientSide) {
            dangerEscape.updateBurning(isOnFire());
            if (perchCooldown > 0) {
                perchCooldown--;
            }
            if (entityData.get(PECK_TICKS) > 0) {
                entityData.set(PECK_TICKS, entityData.get(PECK_TICKS) - 1);
            }
            if (getTarget() != null || (companion != null && !companion.isAlive())) {
                releaseCompanion();
            }
        }
    }

    @Override
    protected float getJumpPower() {
        return GROUND_JUMP_POWER * getBlockJumpFactor() + getJumpBoostPower();
    }

    @Override
    protected float getBlockSpeedFactor() {
        BlockState feet = level().getBlockState(blockPosition());
        if (feet.is(Blocks.SOUL_SAND) || (feet.getBlock().getSpeedFactor() == 1.0F
                && !feet.is(Blocks.WATER) && !feet.is(Blocks.BUBBLE_COLUMN)
                && level().getBlockState(getBlockPosBelowThatAffectsMyMovement()).is(Blocks.SOUL_SAND))) {
            return 1.0F;
        }
        return super.getBlockSpeedFactor();
    }

    @Override
    protected void jumpFromGround() {
        super.jumpFromGround();
        hopCooldown = 10;
    }

    public int getPeckTicks() {
        return entityData.get(PECK_TICKS);
    }

    @Override
    public void travel(Vec3 input) {
        if (isFlying() && !isInWater() && !isInLava()) {
            if (isEffectiveAi()) {
                if (isHovering() && hoverAnchor != null) {
                    Vec3 correction = hoverAnchor.subtract(position()).scale(0.35D);
                    if (correction.lengthSqr() > 0.0144D) {
                        correction = correction.normalize().scale(0.12D);
                    }
                    setDeltaMovement(correction);
                } else if (landingRequested && landingTarget != null
                        && position().subtract(landingTarget).horizontalDistanceSqr() < 1.0D
                        && Math.abs(getY() - landingTarget.y) < 1.5D) {
                    // Brake over the actual landing point, then descend; do not carry cruise drift down.
                    Vec3 offset = landingTarget.subtract(position());
                    Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z).scale(0.25D);
                    if (horizontal.lengthSqr() > 0.0144D) {
                        horizontal = horizontal.normalize().scale(0.12D);
                    }
                    double vertical = offset.horizontalDistanceSqr() > 0.0225D
                            ? Math.max(-0.08D, Math.min(0.08D, (offset.y + 0.4D) * 0.25D))
                            : Math.max(-0.14D, Math.min(-0.04D, offset.y - 0.04D));
                    setDeltaMovement(horizontal.x, vertical, horizontal.z);
                } else {
                    moveRelative(getSpeed() * 0.12F, input);
                }
                move(MoverType.SELF, getDeltaMovement());
                setDeltaMovement(isHovering() ? Vec3.ZERO : getDeltaMovement().scale(0.88D));
            }
            calculateEntityAnimation(false);
        } else {
            super.travel(input);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity living) || !canAttack(living)) {
            return false;
        }
        entityData.set(PECK_TICKS, 6);
        float base = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = RavenCombat.attackDamage(base, level().getDifficulty().getId(),
                living.getMobType() == MobType.UNDEAD);
        boolean hit = target.hurt(damageSources().mobAttack(this), damage);
        if (hit) {
            doEnchantDamageEffects(this, target);
            setLastHurtMob(target);
        }
        return hit;
    }

    public void onFeatherHit(LivingEntity target) {
        doEnchantDamageEffects(this, target);
        setLastHurtMob(target);
    }

    private void shootFeather(LivingEntity target) {
        if (!hasClearFeatherShot(position(), target)) {
            return;
        }
        Vec3 origin = getEyePosition();
        level().addFreshEntity(new RavenFeatherEntity(this, origin, target));
        entityData.set(PECK_TICKS, 6);
        playSound(SoundEvents.ARROW_SHOOT, 0.7F, 1.4F);
    }

    private boolean hasClearFeatherShot(Vec3 feet, LivingEntity target) {
        Vec3 origin = feet.add(0.0D, getEyeHeight(), 0.0D);
        Vec3 aim = target.getBoundingBox().getCenter();
        if (level().clip(new ClipContext(origin, aim, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) {
            return false;
        }
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(origin, aim).inflate(0.3D),
                        bystander -> bystander != this && bystander != target && bystander.isAlive()
                                && bystander.getMobType() != MobType.UNDEAD)
                .stream().noneMatch(bystander -> RavenShotSafety.crossesBody(bystander.getBoundingBox(), origin, aim));
    }

    @Nullable
    private Vec3 findFeatherPosition(LivingEntity target) {
        List<Vec3> positions = new ArrayList<>();
        for (int height = 0; height < 3; height++) {
            for (int radius : new int[]{4, 7, 10}) {
                for (int direction = 0; direction < 8; direction++) {
                    double angle = direction * Math.PI / 4.0D;
                    Vec3 point = target.position().add(Math.cos(angle) * radius,
                            1.5D + height * 2.0D, Math.sin(angle) * radius);
                    if (RavenRangedAttackCycle.canHover(point.distanceToSqr(target.position()), true)
                            && level().getFluidState(BlockPos.containing(point)).isEmpty()
                            && level().getFluidState(BlockPos.containing(point).above()).isEmpty()
                            && level().noCollision(this, getBoundingBox().move(point.subtract(position())))
                            && !isDangerousAt(point)
                            && hasClearFeatherShot(point, target)) {
                        positions.add(point);
                    }
                }
            }
        }
        positions.sort(Comparator.comparingDouble(this::distanceToSqr));
        for (Vec3 point : positions) {
            Path path = navigation.createPath(BlockPos.containing(point), 0);
            if (path != null && path.canReach()) {
                return point;
            }
        }
        return null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean undead = source.getEntity() instanceof LivingEntity attacker
                && attacker.getMobType() == MobType.UNDEAD;
        boolean hit = super.hurt(source, RavenCombat.incomingDamage(amount, undead));
        if (hit && !level().isClientSide) {
            boolean environmental = source.getEntity() == null && (source.is(DamageTypeTags.IS_FIRE)
                    || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH));
            dangerEscape.onDamage(environmental, isOnFire());
        }
        return hit;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.FALL) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean grounded, BlockState state, BlockPos pos) {
        // Flight and ground hops never accumulate fall damage.
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 6;
    }

    public static boolean checkRavenSpawnRules(EntityType<RavenEntity> type, ServerLevelAccessor level,
            MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type)
                && level.getFluidState(pos).isEmpty()
                && level.getFluidState(pos.above()).isEmpty();
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
        // Animal's terrain/light preference must not restrict the custom Nether spawn placement.
        return true;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType reason, @Nullable SpawnGroupData group, @Nullable CompoundTag tag) {
        return super.finalizeSpawn(level, difficulty, reason,
                group == null ? new AgeableMobGroupData(false) : group, tag);
    }

    public boolean trusts(UUID player) {
        return trustedPlayers.contains(player);
    }

    public List<UUID> getTrustedPlayers() {
        return List.copyOf(trustedPlayers);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return trustedPlayers.isEmpty();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.ROTTEN_FLESH);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack food = player.getItemInHand(hand);
        if (!isFood(food)) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide) {
            return InteractionResult.CONSUME;
        }
        RavenFeeding.Meal meal = RavenFeeding.evaluate(getHealth(), getMaxHealth(), getAge(), canFallInLove());
        if (!meal.consumed()) {
            return InteractionResult.PASS;
        }
        usePlayerItem(player, hand, food);
        if (meal.healthAfter() > getHealth()) {
            heal(meal.healthAfter() - getHealth());
        }
        if (meal.grow()) {
            ageUp(getSpeedUpSecondsWhenFeeding(-getAge()), true);
        }
        if (meal.breed()) {
            setInLove(player);
        }
        showEating();
        return InteractionResult.SUCCESS;
    }

    private void showEating() {
        entityData.set(PECK_TICKS, 6);
        playSound(SoundEvents.GENERIC_EAT, 0.6F, 1.2F);
        gameEvent(GameEvent.EAT);
        level().broadcastEntityEvent(this, (byte) 61);
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == 61) {
            Vec3 mouth = getEyePosition().add(getViewVector(1.0F).scale(isBaby() ? 0.3D : 0.6D));
            for (int i = 0; i < 6; i++) {
                level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ROTTEN_FLESH)),
                        mouth.x, mouth.y, mouth.z, random.nextGaussian() * 0.02D,
                        random.nextDouble() * 0.03D, random.nextGaussian() * 0.02D);
            }
        } else {
            super.handleEntityEvent(event);
        }
    }

    @Override
    public void setInLove(@Nullable Player player) {
        super.setInLove(player);
        breedingPlayer = player == null ? null : player.getUUID();
        if (isFlying()) {
            requestLanding(null);
        }
    }

    @Override
    public void resetLove() {
        super.resetLove();
        breedingPlayer = null;
    }

    @Nullable
    @Override
    public RavenEntity getBreedOffspring(ServerLevel level, AgeableMob partner) {
        RavenEntity child = MaledictEntities.RAVEN.get().create(level);
        if (child != null && partner instanceof RavenEntity other) {
            child.trustedPlayers.addAll(RavenTrust.fromBreeding(breedingPlayer, other.breedingPlayer));
            if (!child.trustedPlayers.isEmpty()) {
                child.setPersistenceRequired();
            }
        }
        return child;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Flying", isFlying());
        tag.putInt("PerchCooldown", perchCooldown);
        ListTag trusted = new ListTag();
        for (UUID player : trustedPlayers) {
            trusted.add(NbtUtils.createUUID(player));
        }
        tag.put("Trusted", trusted);
        if (breedingPlayer != null) {
            tag.putUUID("BreedingPlayer", breedingPlayer);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFlying(tag.getBoolean("Flying"));
        setHovering(null);
        setNoGravity(isFlying());
        landingRequested = isFlying();
        perchCooldown = tag.contains("PerchCooldown") ? tag.getInt("PerchCooldown") : 100;
        trustedPlayers.clear();
        ListTag trusted = tag.getList("Trusted", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < Math.min(2, trusted.size()); i++) {
            UUID player = NbtUtils.loadUUID(trusted.get(i));
            if (!trustedPlayers.contains(player)) {
                trustedPlayers.add(player);
            }
        }
        breedingPlayer = tag.hasUUID("BreedingPlayer") ? tag.getUUID("BreedingPlayer") : null;
        if (!trustedPlayers.isEmpty()) {
            setPersistenceRequired();
        }
    }

    private void requestLanding(@Nullable Vec3 preferred) {
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
            setFlying(false);
        } else {
            landingRequested = true;
            landingTarget = preferred == null ? null : landingPointAt(BlockPos.containing(preferred));
            if (landingTarget == null && preferred != null) {
                landingTarget = landingPointAt(BlockPos.containing(preferred).above());
            }
        }
    }

    private void moveToGroundTarget(Vec3 destination, double speed) {
        if (landingRequested) {
            return;
        }
        double distanceSquared = position().subtract(destination).horizontalDistanceSqr();
        RavenGroundTravel.Action action = RavenGroundTravel.next(isFlying(), distanceSquared);
        if (action == RavenGroundTravel.Action.TAKE_OFF) {
            setFlying(true);
        }
        if (action == RavenGroundTravel.Action.LAND) {
            Vec3 point = findLandingPointNear(destination);
            requestLanding(point == null ? destination : point);
        } else if (isFlying()) {
            navigation.moveTo(destination.x, destination.y + 1.5D, destination.z, speed);
        } else {
            navigation.moveTo(destination.x, destination.y, destination.z, speed);
        }
    }

    @Nullable
    private Vec3 findLandingPointNear(Vec3 destination) {
        BlockPos origin = BlockPos.containing(destination);
        for (int radius = 0; radius <= 2; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int dy = 1; dy >= -3; dy--) {
                        Vec3 point = landingPointAt(origin.offset(dx, dy, dz));
                        if (point != null && point.subtract(destination).horizontalDistanceSqr() <= 4.0D) {
                            return point;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void finishGroundTravel(@Nullable Vec3 destination) {
        navigation.stop();
        if (isFlying() && getTarget() == null && !landingRequested) {
            requestLanding(destination);
        }
    }

    @Nullable
    private Vec3 landingPointAt(BlockPos feet) {
        BlockPos floor = feet.below();
        if (!level().getFluidState(floor).isEmpty()
                || level().getBlockState(floor).getCollisionShape(level(), floor).isEmpty()) {
            return null;
        }
        Vec3 point = new Vec3(feet.getX() + 0.5D,
                WalkNodeEvaluator.getFloorLevel(level(), feet), feet.getZ() + 0.5D);
        BlockPos body = BlockPos.containing(point);
        return level().getFluidState(body).isEmpty() && level().getFluidState(body.above()).isEmpty()
                && !isDangerousAt(point)
                && level().noCollision(this, getBoundingBox().move(point.subtract(position())))
                ? point : null;
    }

    private boolean isDangerousAt(Vec3 feet) {
        AABB body = getBoundingBox().move(feet.subtract(position())).deflate(0.001D);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(body.minX, body.minY - 0.08D, body.minZ),
                BlockPos.containing(body.maxX, body.maxY, body.maxZ))) {
            BlockState state = level().getBlockState(pos);
            BlockPathTypes type = state.getBlockPathType(level(), pos, this);
            if (WalkNodeEvaluator.isBurningBlock(state) || state.is(Blocks.CACTUS)
                    || state.is(Blocks.SWEET_BERRY_BUSH) || type == BlockPathTypes.DAMAGE_FIRE
                    || type == BlockPathTypes.DAMAGE_OTHER || type == BlockPathTypes.LAVA) {
                return true;
            }
        }
        return false;
    }

    /** Emergency steering must leave a fire node even when ordinary navigation rejects its neighbors. */
    private final class EscapeDangerGoal extends Goal {
        private Vec3 dangerOrigin = Vec3.ZERO;
        @Nullable
        private Vec3 destination;
        private int searchCooldown;

        private EscapeDangerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return dangerEscape.shouldEscape(isDangerousAt(position()), isOnFire());
        }

        @Override
        public void start() {
            dangerOrigin = position();
            dangerEscape.begin(isOnFire());
            releaseCompanion();
            navigation.stop();
            setHovering(null);
            landingRequested = false;
            landingTarget = null;
            setDeltaMovement(Vec3.ZERO);
            setFlying(true);
            moveControl = new FlyingMoveControl(RavenEntity.this, 20, true);
            destination = null;
            searchCooldown = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return !RavenDangerEscape.reachedSafety(isDangerousAt(position()), position().distanceToSqr(dangerOrigin));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Nullable
        private Vec3 findDestination() {
            Vec3 best = null;
            double nearest = Double.MAX_VALUE;
            for (int radius = 0; radius <= 6; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                            continue;
                        }
                        for (int dy = 0; dy <= 4; dy++) {
                            Vec3 point = position().add(dx, dy, dz);
                            Vec3 offset = point.subtract(position());
                            double distance = offset.lengthSqr();
                            if (distance >= nearest || !RavenDangerEscape.reachedSafety(false, point.distanceToSqr(dangerOrigin))) {
                                continue;
                            }
                            if (!level().getFluidState(BlockPos.containing(point)).isEmpty()
                                    || !level().getFluidState(BlockPos.containing(point).above()).isEmpty()
                                    || isDangerousAt(point)
                                    || !level().noCollision(RavenEntity.this, getBoundingBox().expandTowards(offset))) {
                                continue;
                            }
                            best = point;
                            nearest = distance;
                        }
                    }
                }
            }
            return best;
        }

        @Override
        public void tick() {
            dangerEscape.whileEscaping(isOnFire());
            if (--searchCooldown <= 0 || (destination != null && isDangerousAt(destination))) {
                destination = findDestination();
                searchCooldown = 10;
            }
            if (destination != null) {
                // A short, collision-checked escape bypasses the dangerous start node, not world collision.
                navigation.stop();
                moveControl.setWantedPosition(destination.x, destination.y, destination.z, 1.3D);
                getLookControl().setLookAt(destination.x, destination.y, destination.z);
                if (isInLava()) {
                    setDeltaMovement(getDeltaMovement().x, Math.max(0.35D, getDeltaMovement().y), getDeltaMovement().z);
                }
            } else if (isDangerousAt(position()) && level().noCollision(RavenEntity.this,
                    getBoundingBox().expandTowards(0.0D, 0.5D, 0.0D))) {
                setDeltaMovement(getDeltaMovement().x, Math.max(0.35D, getDeltaMovement().y), getDeltaMovement().z);
            }
        }

        @Override
        public void stop() {
            dangerEscape.finish();
            navigation.stop();
            destination = null;
            if (getTarget() == null) {
                requestLanding(null);
            }
        }
    }

    @Nullable
    private Vec3 findLandingPoint() {
        BlockPos origin = blockPosition();
        for (int radius = 0; radius <= 6; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int dy = 2; dy >= -16; dy--) {
                        Vec3 point = landingPointAt(origin.offset(dx, dy, dz));
                        if (point != null) {
                            return point;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void releaseCompanion() {
        if (companion != null) {
            if (companion.companion == this) {
                companion.companion = null;
            }
            companion = null;
        }
    }

    private final class RavenJumpControl extends JumpControl {
        private RavenJumpControl() {
            super(RavenEntity.this);
        }

        @Override
        public void tick() {
            if (hopCooldown > 0) {
                hopCooldown--;
            }
            if (!isFlying() && onGround() && !isInWater() && !isInLava()
                    && !navigation.isDone() && hopCooldown == 0) {
                // LivingEntity consumes both hopping and obstacle requests before travel.
                jump();
            }
            super.tick();
        }
    }

    private final class EatFleshGoal extends Goal {
        @Nullable
        private ItemEntity food;
        private int remaining;
        private int pathCooldown;

        private EatFleshGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isFlying() || getTarget() != null || getHealth() >= getMaxHealth()) {
                return false;
            }
            food = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(8.0D, 4.0D, 8.0D),
                            item -> item.isAlive() && !item.hasPickUpDelay() && isFood(item.getItem()))
                    .stream().min(Comparator.comparingDouble(RavenEntity.this::distanceToSqr)).orElse(null);
            return food != null;
        }

        @Override
        public boolean canContinueToUse() {
            return food != null && food.isAlive() && isFood(food.getItem())
                    && getHealth() < getMaxHealth() && getTarget() == null && remaining > 0;
        }

        @Override
        public void start() {
            releaseCompanion();
            remaining = 200;
            pathCooldown = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (food == null) {
                return;
            }
            remaining--;
            getLookControl().setLookAt(food, 20.0F, 20.0F);
            if (!isFlying() && onGround() && distanceToSqr(food) <= 2.25D
                    && getSensing().hasLineOfSight(food)) {
                ItemStack stack = food.getItem();
                stack.shrink(1);
                if (stack.isEmpty()) {
                    food.discard();
                } else {
                    food.setItem(stack);
                }
                heal(RavenFeeding.HEAL_AMOUNT);
                showEating();
                remaining = 0;
            } else if (--pathCooldown <= 0) {
                moveToGroundTarget(food.position(), 1.0D);
                pathCooldown = 10;
            }
        }

        @Override
        public void stop() {
            finishGroundTravel(food == null ? null : food.position());
            food = null;
        }
    }

    private final class FollowRavenParentGoal extends Goal {
        @Nullable
        private RavenEntity parent;
        private int pathCooldown;

        private FollowRavenParentGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!isBaby() || getTarget() != null) {
                return false;
            }
            parent = level().getEntitiesOfClass(RavenEntity.class, getBoundingBox().inflate(8.0D, 4.0D, 8.0D),
                            bird -> bird.isAlive() && !bird.isBaby())
                    .stream().min(Comparator.comparingDouble(RavenEntity.this::distanceToSqr)).orElse(null);
            return parent != null && distanceToSqr(parent) >= 9.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return isBaby() && getTarget() == null && parent != null && parent.isAlive()
                    && distanceToSqr(parent) <= 256.0D
                    && (isFlying() || position().subtract(parent.position()).horizontalDistanceSqr() > 4.0D);
        }

        @Override
        public void start() {
            releaseCompanion();
            pathCooldown = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (parent != null) {
                getLookControl().setLookAt(parent, 20.0F, 20.0F);
                if (--pathCooldown <= 0) {
                    moveToGroundTarget(parent.position(), 1.0D);
                    pathCooldown = 10;
                }
            }
        }

        @Override
        public void stop() {
            finishGroundTravel(parent == null ? null : parent.position());
            parent = null;
        }
    }

    private boolean canPerchOn(Player player) {
        return perchCooldown == 0 && !isLeashed() && !isPassenger() && !isInLove()
                && !player.isSpectator() && !player.isShiftKeyDown() && !player.getAbilities().flying
                && !player.isFallFlying() && !player.isInWater() && !player.isInPowderSnow
                && !player.isPassenger() && player.onGround()
                && (player.getShoulderEntityLeft().isEmpty() || player.getShoulderEntityRight().isEmpty());
    }

    private boolean perchOn(ServerPlayer player) {
        if (!canPerchOn(player) || !trusts(player.getUUID())) {
            return false;
        }
        CompoundTag bird = new CompoundTag();
        bird.putString("id", getEncodeId());
        saveWithoutId(bird);
        bird.putBoolean("Flying", false);
        bird.putBoolean("NoGravity", false);
        bird.putInt("PerchCooldown", 100);
        ListTag motion = new ListTag();
        for (int i = 0; i < 3; i++) {
            motion.add(net.minecraft.nbt.DoubleTag.valueOf(0.0D));
        }
        bird.put("Motion", motion);
        if (player.setEntityOnShoulder(bird)) {
            releaseCompanion();
            discard();
            return true;
        }
        return false;
    }

    private final class FollowTrustedPlayerGoal extends Goal {
        @Nullable
        private ServerPlayer player;
        private int pathCooldown;

        private FollowTrustedPlayerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isInLove() || isLeashed() || isPassenger()) {
                return false;
            }
            player = trustedPlayers.stream().map(level()::getPlayerByUUID)
                    .filter(candidate -> candidate instanceof ServerPlayer && candidate.isAlive()
                            && !candidate.isSpectator())
                    .map(candidate -> (ServerPlayer) candidate)
                    .min(Comparator.comparingDouble(RavenEntity.this::distanceToSqr)).orElse(null);
            return player != null && (distanceToSqr(player) > 9.0D || canPerchOn(player));
        }

        @Override
        public boolean canContinueToUse() {
            return player != null && player.isAlive() && player.level() == level() && !player.isSpectator()
                    && getTarget() == null && !isInLove() && !isLeashed() && !isPassenger()
                    && (distanceToSqr(player) > 4.0D || canPerchOn(player));
        }

        @Override
        public void start() {
            releaseCompanion();
            landingRequested = false;
            landingTarget = null;
            setFlying(true);
            pathCooldown = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (player == null) {
                return;
            }
            getLookControl().setLookAt(player, 30.0F, 30.0F);
            if (getBoundingBox().intersects(player.getBoundingBox()) && perchOn(player)) {
                return;
            }
            if (--pathCooldown <= 0) {
                navigation.moveTo(player.getX(), player.getY() + 1.0D, player.getZ(), 1.0D);
                pathCooldown = 8;
            }
        }

        @Override
        public void stop() {
            navigation.stop();
            if (!isRemoved() && isFlying() && getTarget() == null && !isInLove()) {
                requestLanding(null);
            }
            player = null;
        }
    }

    private final class AttackGoal extends Goal {
        private int attackCooldown;
        private int pathCooldown;
        private final RavenRangedAttackCycle rangedCycle = new RavenRangedAttackCycle();
        @Nullable
        private Vec3 hoverPoint;
        @Nullable
        private Vec3 shootingPosition;
        private int positionSearchCooldown;

        private AttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && getTarget().isAlive() && canAttack(getTarget());
        }

        @Override
        public void start() {
            releaseCompanion();
            landingRequested = false;
            landingTarget = null;
            setFlying(true);
            setHovering(null);
            hoverPoint = null;
            shootingPosition = null;
            positionSearchCooldown = 0;
            rangedCycle.cancelCharge();
            pathCooldown = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            boolean visible = getSensing().hasLineOfSight(target);
            double distanceSquared = distanceToSqr(target);
            if (RavenRangedAttackCycle.canHover(distanceSquared, true) && !isInWater() && !isInLava()
                    && hasClearFeatherShot(position(), target)) {
                shootingPosition = null;
                positionSearchCooldown = 0;
                if (hoverPoint == null) {
                    Vec3 point = position().add(0.0D, onGround() ? 1.25D : 0.0D, 0.0D);
                    if (!isDangerousAt(point) && level().noCollision(RavenEntity.this,
                            getBoundingBox().move(point.subtract(position())))) {
                        hoverPoint = point;
                    }
                }
                if (hoverPoint != null) {
                    if (!isHovering()) {
                        navigation.stop();
                        if (onGround() || position().distanceToSqr(hoverPoint) > 0.0625D) {
                            moveControl.setWantedPosition(hoverPoint.x, hoverPoint.y, hoverPoint.z, 0.65D);
                            rangedCycle.tick(false, level().getGameTime());
                            return;
                        }
                        setHovering(hoverPoint);
                    }
                    Vec3 facing = target.position().subtract(position());
                    float yaw = (float) (Mth.atan2(facing.z, facing.x) * Mth.RAD_TO_DEG) - 90.0F;
                    setYRot(yaw);
                    yBodyRot = yaw;
                    if (rangedCycle.tick(true, level().getGameTime())) {
                        shootFeather(target);
                    }
                    return;
                }
            }
            setHovering(null);
            hoverPoint = null;
            rangedCycle.tick(false, level().getGameTime());
            if (distanceSquared >= 9.0D && !isInWater() && !isInLava()) {
                if (--positionSearchCooldown <= 0) {
                    shootingPosition = findFeatherPosition(target);
                    positionSearchCooldown = 20;
                    pathCooldown = 0;
                }
                if (shootingPosition != null) {
                    if (--pathCooldown <= 0) {
                        navigation.moveTo(shootingPosition.x, shootingPosition.y, shootingPosition.z, 1.0D);
                        pathCooldown = 8;
                    }
                    if (navigation.isDone() && position().distanceToSqr(shootingPosition) < 2.25D) {
                        moveControl.setWantedPosition(shootingPosition.x, shootingPosition.y, shootingPosition.z, 0.65D);
                    }
                    return;
                }
            }
            if (--pathCooldown <= 0) {
                navigation.moveTo(target.getX(), target.getY() + 0.3D, target.getZ(), 1.2D);
                pathCooldown = 8;
            }
            double reach = 1.0D + target.getBbWidth() * 0.5D;
            if (attackCooldown == 0 && distanceToSqr(target) <= reach * reach
                    && visible) {
                doHurtTarget(target);
                attackCooldown = 20;
            }
        }

        @Override
        public void stop() {
            setHovering(null);
            hoverPoint = null;
            shootingPosition = null;
            rangedCycle.cancelCharge();
            navigation.stop();
            requestLanding(null);
        }
    }

    private final class LandingGoal extends Goal {
        private int pathCooldown;

        private LandingGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return landingRequested && isFlying() && getTarget() == null;
        }

        @Override
        public void start() {
            navigation.stop();
            pathCooldown = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (landingTarget != null && onGround()
                    && position().subtract(landingTarget).horizontalDistanceSqr() <= 0.0225D
                    && Math.abs(getY() - landingTarget.y) < 0.1D) {
                setDeltaMovement(Vec3.ZERO);
                setFlying(false);
                return;
            }
            if (--pathCooldown <= 0) {
                Vec3 checked = landingTarget == null ? null
                        : landingPointAt(BlockPos.containing(landingTarget));
                if (checked == null && landingTarget != null) {
                    checked = landingPointAt(BlockPos.containing(landingTarget).above());
                }
                if (checked == null || checked.distanceToSqr(landingTarget) > 0.0001D) {
                    landingTarget = findLandingPoint();
                }
                if (landingTarget != null) {
                    navigation.moveTo(landingTarget.x, landingTarget.y, landingTarget.z, 0.55D);
                }
                pathCooldown = 20;
            }
        }

        @Override
        public void stop() {
            navigation.stop();
        }
    }

    /** Idle sessions alternate safe ground hops, short flights and exclusive pairs. */
    private final class IdleGoal extends Goal {
        @Nullable
        private Vec3 destination;
        @Nullable
        private Vec3 groundDestination;
        @Nullable
        private RavenEntity mate;
        private boolean flight;
        private int remaining;
        private int pathCooldown;

        private IdleGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null || isInLove() || isInWater() || isInLava()
                    || random.nextInt(reducedTickDelay(60)) != 0) {
                return false;
            }
            mate = companion;
            if (mate == null && random.nextInt(3) == 0) {
                mate = level().getEntitiesOfClass(RavenEntity.class, getBoundingBox().inflate(10.0D),
                                bird -> bird != RavenEntity.this && bird.isAlive() && bird.getTarget() == null
                                        && !bird.isFlying() && bird.companion == null)
                        .stream().min(Comparator.comparingDouble(RavenEntity.this::distanceToSqr))
                        .orElse(null);
            }
            flight = mate == null && random.nextInt(3) == 0;
            destination = mate == null ? LandRandomPos.getPos(RavenEntity.this, 10, 4) : mate.position();
            groundDestination = destination;
            if (flight && destination != null) {
                destination = destination.add(0.0D, 3.0D + random.nextInt(3), 0.0D);
                if (!level().noCollision(RavenEntity.this, getBoundingBox().move(
                        destination.subtract(position())))) {
                    destination = null;
                }
            }
            return destination != null;
        }

        @Override
        public void start() {
            if (mate != null) {
                companion = mate;
                mate.companion = RavenEntity.this;
            }
            setFlying(flight);
            remaining = mate != null ? 200 + random.nextInt(200) : 100 + random.nextInt(100);
            pathCooldown = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null && !isInLove() && remaining > 0
                    && (mate == null ? !navigation.isDone()
                    : companion == mate && mate.isAlive() && mate.getTarget() == null);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            remaining--;
            if (mate != null) {
                getLookControl().setLookAt(mate, 20.0F, 20.0F);
                destination = mate.position();
                if (position().subtract(mate.position()).horizontalDistanceSqr() <= 4.0D) {
                    finishGroundTravel(mate.position());
                    return;
                }
            }
            if (--pathCooldown <= 0 && destination != null) {
                if (flight) {
                    navigation.moveTo(destination.x, destination.y, destination.z, 1.0D);
                } else {
                    moveToGroundTarget(destination, 1.0D);
                }
                pathCooldown = 20;
            }
        }

        @Override
        public void stop() {
            finishGroundTravel(mate == null ? groundDestination : mate.position());
            releaseCompanion();
            mate = null;
        }
    }
}
