package org.brahypno.maledict.common.entity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import org.brahypno.maledict.common.curio.HangmanFoolProjectiles;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.mixin.ProjectileAccess;
import org.brahypno.maledict.registry.MaledictEntities;

import java.util.UUID;

/** A single ticking entity; the captured projectile is inert NBT until contact, including across saves. */
public final class HangmanTrapEntity extends Entity {
    private static final EntityDataAccessor<Integer> FACE =
            SynchedEntityData.defineId(HangmanTrapEntity.class, EntityDataSerializers.INT);
    private CompoundTag projectileData = new CompoundTag();
    private UUID owner;
    private int remainingTicks;

    public HangmanTrapEntity(EntityType<? extends HangmanTrapEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static boolean capture(Projectile projectile, BlockHitResult hit) {
        CompoundTag payload = new CompoundTag();
        if (!projectile.save(payload)) {
            return false;
        }
        HangmanTrapEntity trap = new HangmanTrapEntity(MaledictEntities.HANGMAN_TRAP.get(), projectile.level());
        trap.projectileData = payload;
        trap.owner = projectile.getOwner().getUUID();
        trap.remainingTicks = MaledictConfig.HANGMAN_TRAP_LIFETIME.get();
        trap.entityData.set(FACE, hit.getDirection().get3DDataValue());
        trap.setPos(hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.015D)));
        return projectile.level().addFreshEntity(trap);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FACE, Direction.UP.get3DDataValue());
    }

    public Direction face() {
        return Direction.from3DDataValue(entityData.get(FACE));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (--remainingTicks <= 0) {
            discard();
            return;
        }
        Entity caster = owner == null ? null : server.getEntity(owner);
        // The bounded contact query runs once per tick; no world-wide projectile scans or per-trap particles.
        double radius = MaledictConfig.HANGMAN_TRAP_RADIUS.get();
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius), entity -> canTrigger(entity, caster))) {
            trigger(target);
            break;
        }
    }

    private boolean canTrigger(LivingEntity target, Entity caster) {
        return target.isAlive() && !target.isSpectator()
                && !target.getUUID().equals(owner)
                && (caster == null || !caster.isAlliedTo(target) && !target.isAlliedTo(caster))
                && !(target instanceof TamableAnimal pet && owner != null && owner.equals(pet.getOwnerUUID()));
    }

    private void trigger(LivingEntity target) {
        discard(); // Consume before dispatching; one trap cannot hit two targets in the same tick.
        CompoundTag payload = projectileData.copy();
        payload.remove("UUID");
        Entity restored = EntityType.loadEntityRecursive(payload, level(), entity -> entity);
        if (!(restored instanceof Projectile projectile)) {
            return;
        }
        // Entity.load clamps large saved Motion components; retain the original shot's speed explicitly.
        var motion = projectileData.getList("Motion", net.minecraft.nbt.Tag.TAG_DOUBLE);
        Vec3 velocity = new Vec3(motion.getDouble(0), motion.getDouble(1), motion.getDouble(2));
        HangmanFoolProjectiles.applyMotion(projectile, velocity, false);
        projectile.setPos(target.getBoundingBox().getCenter());
        if (!level().addFreshEntity(projectile)) {
            return;
        }
        EntityHitResult hit = new EntityHitResult(target);
        try {
            if (!ForgeEventFactory.onProjectileImpact(projectile, hit)) {
                ((ProjectileAccess) projectile).maledict$onHit(hit);
            }
        } finally {
            // A stage-two wearer can reflect a released shot back into normal flight.
            if (!HangmanFoolProjectiles.wasReflected(projectile)) {
                projectile.discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        projectileData = tag.getCompound("Projectile");
        owner = tag.hasUUID("TrapOwner") ? tag.getUUID("TrapOwner") : null;
        remainingTicks = tag.getInt("RemainingTicks");
        entityData.set(FACE, tag.getInt("Face"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Projectile", projectileData);
        if (owner != null) {
            tag.putUUID("TrapOwner", owner);
        }
        tag.putInt("RemainingTicks", remainingTicks);
        tag.putInt("Face", entityData.get(FACE));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
