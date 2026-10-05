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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

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
    private static final EntityDataAccessor<Integer> PECK_TICKS =
            SynchedEntityData.defineId(RavenEntity.class, EntityDataSerializers.INT);
    @Nullable
    private RavenEntity companion;
    @Nullable
    private Vec3 landingTarget;
    private boolean landingRequested;
    private int hopCooldown;
    private int perchCooldown = 100;
    private final List<UUID> trustedPlayers = new ArrayList<>(2);
    @Nullable
    private UUID breedingPlayer;
    public float previousFlightProgress;
    public float flightProgress;

    public RavenEntity(EntityType<? extends RavenEntity> type, Level level) {
        super(type, level);
        jumpControl = new RavenJumpControl();
        setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
        setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
        setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0F);
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
        entityData.define(PECK_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AttackGoal());
        goalSelector.addGoal(2, new LandingGoal());
        goalSelector.addGoal(3, new EatFleshGoal());
        goalSelector.addGoal(4, new BreedGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return !isFlying() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new FollowTrustedPlayerGoal());
        goalSelector.addGoal(6, new FollowParentGoal(this, 1.0D));
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
        if (!level().isClientSide) {
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
                if (landingRequested && landingTarget != null
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
                setDeltaMovement(getDeltaMovement().scale(0.88D));
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

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean undead = source.getEntity() instanceof LivingEntity attacker
                && attacker.getMobType() == MobType.UNDEAD;
        return super.hurt(source, RavenCombat.incomingDamage(amount, undead));
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
                && level().noCollision(this, getBoundingBox().move(point.subtract(position())))
                ? point : null;
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
                    && getHealth() < getMaxHealth() && getTarget() == null && !isFlying() && remaining > 0;
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
            if (distanceToSqr(food) <= 2.25D && getSensing().hasLineOfSight(food)) {
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
                navigation.moveTo(food, 1.0D);
                pathCooldown = 10;
            }
        }

        @Override
        public void stop() {
            navigation.stop();
            food = null;
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
            if (--pathCooldown <= 0) {
                navigation.moveTo(target.getX(), target.getY() + 0.3D, target.getZ(), 1.2D);
                pathCooldown = 8;
            }
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            double reach = 1.0D + target.getBbWidth() * 0.5D;
            if (attackCooldown == 0 && distanceToSqr(target) <= reach * reach
                    && getSensing().hasLineOfSight(target)) {
                doHurtTarget(target);
                attackCooldown = 20;
            }
        }

        @Override
        public void stop() {
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
                if (distanceToSqr(mate) < 4.0D) {
                    navigation.stop();
                    return;
                }
            }
            if (--pathCooldown <= 0 && destination != null) {
                navigation.moveTo(destination.x, destination.y, destination.z, 1.0D);
                pathCooldown = 20;
            }
        }

        @Override
        public void stop() {
            navigation.stop();
            if (isFlying() && getTarget() == null) {
                requestLanding(groundDestination);
            }
            releaseCompanion();
            mate = null;
        }
    }
}
