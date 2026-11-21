package org.brahypno.maledict.common.entity.wither;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.LootTable;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A slightly stronger wither skeleton variant. Summoned individuals belong to one Weeping Wither,
 * never drop loot and may expire after a limited lifetime; command-spawned individuals keep vanilla
 * drops and do not expire.
 * <p>
 * A summon fights only what its Weeping Wither fights. It never targets the wither or a sibling
 * summon, and a friendly hit (the wither's own skulls, fangs or pulse clipping it) is not aggro:
 * vanilla's {@code HurtByTargetGoal} would otherwise make it turn on the wither it was summoned by.
 */
@SuppressWarnings("this-escape")
public class WeepingWitherSkeletonEntity extends WitherSkeleton {
    private static final String OWNER_TAG = "WeepingOwner";
    private static final String LIFETIME_TAG = "WeepingLifetime";
    /**
     * How often a summon re-reads its owner's target; the target also arrives at spawn time.
     */
    private static final int OWNER_TARGET_INTERVAL = 10;
    /**
     * The summoner is looked up inside this radius when its chunk is not the skeleton's.
     */
    private static final double OWNER_SEARCH_RADIUS = 128.0D;
    @Nullable
    private UUID ownerUuid;
    private int lifetime;
    @Nullable
    private WeepingWitherEntity cachedOwner;

    public WeepingWitherSkeletonEntity(EntityType<? extends WeepingWitherSkeletonEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WitherSkeleton.createAttributes()
                             .add(Attributes.MAX_HEALTH, 26.0D)
                             .add(Attributes.ATTACK_DAMAGE, 5.0D)
                             .add(Attributes.MOVEMENT_SPEED, 0.27D)
                             .add(Attributes.ARMOR, 2.0D);
    }

    /**
     * The vanilla scans (player, iron golem, turtle, piglin) stay for command-spawned individuals
     * only. A summon takes its target from the Weeping Wither through {@link #followOwnerTarget()}.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.removeAllGoals(goal -> goal instanceof NearestAttackableTargetGoal<?>);
        targetSelector.addGoal(2, new UnsummonedTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new UnsummonedTargetGoal<>(this, IronGolem.class, true));
        targetSelector.addGoal(3, new UnsummonedTargetGoal<>(this, Turtle.class, 10, true, false,
                                                            Turtle.BABY_ON_LAND_SELECTOR));
        targetSelector.addGoal(3, new UnsummonedTargetGoal<>(this, AbstractPiglin.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        // WitherSkeleton.finalizeSpawn resets the attack attribute to the vanilla 4.
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0D);
        return result;
    }

    /**
     * Links this skeleton to its summoner; a lifetime of zero means it persists until killed or cleaned up.
     */
    public void summonBy(WeepingWitherEntity owner, int lifetimeTicks) {
        ownerUuid = owner.getUUID();
        cachedOwner = owner;
        lifetime = Math.max(0, lifetimeTicks);
        setPersistenceRequired();
        setCanPickUpLoot(false);
    }

    public boolean isOwnedBy(WeepingWitherEntity owner) {
        return ownerUuid != null && ownerUuid.equals(owner.getUUID());
    }

    @Nullable
    public UUID getWeepingOwnerUuid() {
        return ownerUuid;
    }

    public boolean isSummoned() {
        return ownerUuid != null;
    }

    public int remainingLifetime() {
        return lifetime;
    }

    /**
     * The summoner, any wither and the siblings summoned by the same wither are friendly. The check
     * is off for command-spawned individuals, which keep vanilla hostility.
     */
    public boolean isWeepingAlly(@Nullable Entity entity) {
        if (entity == null || entity == this || ownerUuid == null)
            return false;
        if (entity instanceof WitherBoss)
            return true;
        return entity instanceof WeepingWitherSkeletonEntity sibling
               && ownerUuid.equals(sibling.ownerUuid);
    }

    /**
     * A friendly hit must not become aggro, and a targeted goal must not hand the skeleton a
     * friendly target: vanilla target goals write {@code setTarget} directly in {@code start} and
     * {@code canContinueToUse}, so the alliance rule is enforced here as well.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (isWeepingAlly(target))
            return;
        super.setTarget(target);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target != null && !isWeepingAlly(target) && super.canAttack(target);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return isWeepingAlly(entity) || super.isAlliedTo(entity);
    }

    @Nullable
    private WeepingWitherEntity resolveOwner() {
        WeepingWitherEntity owner = cachedOwner;
        if (owner != null && !owner.isRemoved() && owner.isAlive() && owner.level() == level())
            return owner;
        cachedOwner = null;
        if (ownerUuid == null || !(level() instanceof ServerLevel serverLevel))
            return null;
        if (serverLevel.getEntity(ownerUuid) instanceof WeepingWitherEntity loaded){
            cachedOwner = loaded;
        }else {
            var nearby = serverLevel.getEntitiesOfClass(WeepingWitherEntity.class,
                                                        getBoundingBox().inflate(OWNER_SEARCH_RADIUS),
                                                        candidate -> ownerUuid.equals(candidate.getUUID()));
            if (!nearby.isEmpty())
                cachedOwner = nearby.get(0);
        }
        return cachedOwner;
    }

    /**
     * A summon fights the Weeping Wither's current opponent instead of picking unrelated fights.
     */
    private void followOwnerTarget() {
        WeepingWitherEntity owner = resolveOwner();
        if (owner == null)
            return;
        LivingEntity wanted = owner.getTarget();
        if (wanted == null || wanted == getTarget() || !canAttack(wanted))
            return;
        setTarget(wanted);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || ownerUuid == null)
            return;
        if (lifetime > 0 && --lifetime == 0){
            discard();
            return;
        }
        if (tickCount % OWNER_TARGET_INTERVAL == 0)
            followOwnerTarget();
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        // Summoned skeletons are a boss mechanic, not a farm: no equipment, loot table or experience.
        if (isSummoned())
            return;
        super.dropAllDeathLoot(level, source);
    }

    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        // Command-spawned individuals keep the vanilla wither skeleton table.
        return EntityType.WITHER_SKELETON.getDefaultLootTable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null)
            tag.putUUID(OWNER_TAG, ownerUuid);
        tag.putInt(LIFETIME_TAG, lifetime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerUuid = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        cachedOwner = null;
        lifetime = Math.max(0, tag.getInt(LIFETIME_TAG));
        if (ownerUuid != null){
            setPersistenceRequired();
            setCanPickUpLoot(false);
        }
    }

    /**
     * One of the vanilla scans, disabled while the skeleton is a summon.
     */
    private static final class UnsummonedTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
        private final WeepingWitherSkeletonEntity skeleton;

        UnsummonedTargetGoal(
                WeepingWitherSkeletonEntity skeleton, Class<T> targetType, boolean mustSee) {
            super(skeleton, targetType, mustSee);
            this.skeleton = skeleton;
        }

        UnsummonedTargetGoal(
                WeepingWitherSkeletonEntity skeleton, Class<T> targetType, int randomInterval,
                boolean mustSee, boolean mustReach, @Nullable Predicate<LivingEntity> selector) {
            super(skeleton, targetType, randomInterval, mustSee, mustReach, selector);
            this.skeleton = skeleton;
        }

        @Override
        public boolean canUse() {
            return !skeleton.isSummoned() && super.canUse();
        }
    }
}
