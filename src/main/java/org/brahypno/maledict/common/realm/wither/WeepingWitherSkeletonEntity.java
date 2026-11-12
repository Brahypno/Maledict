package org.brahypno.maledict.common.realm.wither;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A slightly stronger wither skeleton variant. Summoned individuals belong to one Weeping Wither,
 * never drop loot and may expire after a limited lifetime; command-spawned individuals keep vanilla
 * drops and do not expire.
 */
public class WeepingWitherSkeletonEntity extends WitherSkeleton {
    private static final String OWNER_TAG = "WeepingOwner";
    private static final String LIFETIME_TAG = "WeepingLifetime";
    @Nullable private UUID ownerUuid;
    private int lifetime;

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

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                        @Nullable CompoundTag data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, data);
        // WitherSkeleton.finalizeSpawn resets the attack attribute to the vanilla 4.
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0D);
        return result;
    }

    /** Links this skeleton to its summoner; a lifetime of zero means it persists until killed or cleaned up. */
    public void summonBy(WeepingWitherEntity owner, int lifetimeTicks) {
        ownerUuid = owner.getUUID();
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

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || ownerUuid == null || lifetime <= 0) return;
        if (--lifetime == 0) {
            discard();
        }
    }

    @Override
    protected void dropAllDeathLoot(DamageSource source) {
        // Summoned skeletons are a boss mechanic, not a farm: no equipment, loot table or experience.
        if (isSummoned()) return;
        super.dropAllDeathLoot(source);
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        // Command-spawned individuals keep the vanilla wither skeleton table.
        return EntityType.WITHER_SKELETON.getDefaultLootTable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) tag.putUUID(OWNER_TAG, ownerUuid);
        tag.putInt(LIFETIME_TAG, lifetime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerUuid = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        lifetime = Math.max(0, tag.getInt(LIFETIME_TAG));
        if (ownerUuid != null) {
            setPersistenceRequired();
            setCanPickUpLoot(false);
        }
    }
}
