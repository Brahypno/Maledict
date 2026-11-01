package org.brahypno.maledict.common.realm.demiurge;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.common.vicissitude.VicissitudeBossEntity;

/** Summonable stone-statue appearance preview; combat and awakening are not authored yet. */
public final class DemiurgeStatueEntity extends VicissitudeBossEntity {
    public DemiurgeStatueEntity(EntityType<? extends DemiurgeStatueEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public void travel(Vec3 input) {
        // Keep normal gravity and ground collision, without locomotion.
        super.travel(Vec3.ZERO);
    }
}
