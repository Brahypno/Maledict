package org.brahypno.maledict.common.realm.wither;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.Level;

/**
 * Vanilla evoker fang mechanics with the Weeping Wither's bone texture. The delay field is private
 * in {@link EvokerFangs}; loading the same NBT key the vanilla class persists is the supported way
 * for this subclass to choose a shorter rise.
 */
public final class OssuaryFangs extends EvokerFangs {
    public OssuaryFangs(EntityType<? extends OssuaryFangs> type, Level level) {
        super(type, level);
    }

    public void setWarmup(int ticks) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Warmup", ticks);
        readAdditionalSaveData(tag);
    }
}
