package org.brahypno.maledict.common.entity.vicissitude;

import net.minecraft.network.syncher.SynchedEntityData;
import com.sammy.malum.common.entity.FloatingEntity;
import com.sammy.malum.registry.common.MalumSoundEvents;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import com.sammy.malum.visual_effects.SpiritLightSpecs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.registry.MaledictEntities;
import team.lodestar.lodestone.systems.particle.ParticleEffectSpawner;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * An attacking variant of Malum's pneuma void: winds up, homes through terrain, applies the
 * phase-one attack on arrival.
 *
 * <p>Malum's {@code owner} is this orb's victim, not its caster; the caster lives in
 * {@link #casterUUID}.
 */
public final class VicissitudeLightOrbEntity extends FloatingEntity {
    public static final String ATTACK_MESSAGE_KEY = "message.maledict.first_vicissitude.attack";
    private static final String CASTER_TAG = "OrbCaster";

    private UUID casterUUID;
    private Entity caster;

    public VicissitudeLightOrbEntity(EntityType<? extends VicissitudeLightOrbEntity> type, Level level) {
        super(type, level);
        maxAge = 200;
    }

    public VicissitudeLightOrbEntity(ServerLevel level, LivingEntity caster, LivingEntity target,
                                     Vec3 position) {
        this(MaledictEntities.VICISSITUDE_LIGHT_ORB.get(), level);
        setCaster(caster);
        setDestination(new com.sammy.malum.common.entity.FloatingItemDestinationData(target));
        setPos(position);
        Vec3 forward = getDestination().getDestination(level).orElse(position).subtract(position).normalize().scale(0.12D);
        setDeltaMovement(forward.add(
                random.triangle(0.0D, 0.12D),
                random.triangle(0.05D, 0.02D),
                random.triangle(0.0D, 0.12D)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel level && (getDestination() == null
                || getDestination().getEntityCollector(level).isEmpty())) {
            discard();
            return;
        }
        super.tick();
        if (level().isClientSide) {
            Vec3 pos = getOffsetPosition(0.5F);
            spawnParticles(pos.x, pos.y, pos.z);
        }
    }

    @Override
    public void collect(ServerLevel level) {
        LivingEntity owner = getDestination().getEntityCollector(level).orElse(null);
        if (owner == null || !owner.isAlive()) {
            return;
        }
        owner.setHealth(Math.min(owner.getHealth(), 1.0F));
        playSound(MalumSoundEvents.SPIRIT_PICKUP.get(), 0.4F,
                Mth.nextFloat(random, 0.8F, 1.1F));
    }

    @Override
    public float getFriction() {
        return 0.9F;
    }

    private void spawnParticles(double x, double y, double z) {
        Vec3 motion = getDeltaMovement().normalize().scale(0.05D);
        ParticleEffectSpawner particles = SpiritLightSpecs.spiritLightSpecs(
                level(), new Vec3(x, y, z), MalumSpiritTypes.ELDRITCH_SPIRIT);
        particles.getBuilder().setMotion(motion);
        particles.getBloomBuilder().setMotion(motion);
        particles.spawnParticles();
    }

    /** Ownership check used by the boss to enforce the live orb cap. */
    public boolean isOwnedBy(Entity entity) {
        return entity != null && entity == caster();
    }

    private void setCaster(LivingEntity caster) {
        casterUUID = caster.getUUID();
        this.caster = caster;
    }

    /** Rebuilt on demand from {@link #casterUUID}, so an orb loaded from disk cannot hold a stale entity. */
    @Nullable
    private Entity caster() {
        if (caster == null && casterUUID != null && level() instanceof ServerLevel serverLevel) {
            caster = serverLevel.getEntity(casterUUID);
        }
        return caster;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (casterUUID != null) {
            tag.putUUID(CASTER_TAG, casterUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        casterUUID = tag.hasUUID(CASTER_TAG) ? tag.getUUID(CASTER_TAG) : null;
        caster = null;
    }
}
