package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.PartEntity;

/** A rigid animated section; all sections forward incoming hits to the same boss. */
public final class DemiurgePart extends PartEntity<DemiurgeStatueEntity> {
    private EntityDimensions dimensions = EntityDimensions.scalable(.01F, .01F);

    DemiurgePart(DemiurgeStatueEntity parent) {
        super(parent);
        noPhysics = true;
    }

    void place(AABB bounds) {
        double previousX = getX(), previousY = getY(), previousZ = getZ();
        dimensions = EntityDimensions.scalable((float) Math.max(bounds.getXsize(), bounds.getZsize()), (float) bounds.getYsize());
        setPos((bounds.minX + bounds.maxX) / 2, bounds.minY, (bounds.minZ + bounds.maxZ) / 2);
        setBoundingBox(bounds);
        xo = xOld = previousX;
        yo = yOld = previousY;
        zo = zOld = previousZ;
    }

    @Override public EntityDimensions getDimensions(Pose pose) { return dimensions; }
    @Override public boolean isPickable() { return getParent().isAlive() && getBoundingBox().getSize() > .01; }
    @Override public boolean is(Entity entity) { return this == entity || getParent() == entity; }
    @Override public boolean hurt(DamageSource source, float amount) { return getParent().hurt(source, amount); }
    @Override public InteractionResult interact(Player player, InteractionHand hand) { return getParent().mobInteract(player, hand); }
    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean shouldBeSaved() { return false; }
}
