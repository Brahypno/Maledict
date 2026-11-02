package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.common.vicissitude.VicissitudeBossEntity;
import org.brahypno.maledict.config.MaledictConfig;

/** Falling tears test living targets along their travel; blocks never participate. */
public final class DemiurgeTearEntity extends Projectile {
    public static final int LIFETIME = 30;
    public static final double FALL_SPEED = .9;
    private int life;

    public DemiurgeTearEntity(EntityType<? extends DemiurgeTearEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public static boolean canHit(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive()
                && !(living instanceof VicissitudeBossEntity)
                && !(living instanceof Player player && (player.isCreative() || player.isSpectator()));
    }

    @Override
    public void tick() {
        super.tick();
        if (++life > LIFETIME) {
            if (!level().isClientSide) discard();
            return;
        }
        Vec3 from = position(), to = from.add(getDeltaMovement());
        if (!level().isClientSide) {
            var hit = ProjectileUtil.getEntityHitResult(level(), this, from, to,
                    getBoundingBox().expandTowards(getDeltaMovement()).inflate(.2),
                    candidate -> candidate != getOwner() && canHit(candidate), .18F);
            if (hit != null && hit.getEntity() instanceof LivingEntity living) {
                DemiurgeDifficulty.hurt(living, damageSources().indirectMagic(this, getOwner()), MaledictConfig.DEMIURGE_RAIN_DAMAGE.get());
                setPos(hit.getLocation());
                discard();
                return;
            }
        }
        setPos(to);
    }

    @Override protected void defineSynchedData() {}
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TearLife", life);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        life = tag.getInt("TearLife");
    }
}
