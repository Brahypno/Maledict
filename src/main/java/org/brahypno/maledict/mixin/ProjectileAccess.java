package org.brahypno.maledict.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Projectile.class)
public interface ProjectileAccess {
    /** Dispatches virtually, including potion/firework overrides of the complete onHit branch. */
    @Invoker("onHit")
    void maledict$onHit(HitResult hit);

    @Accessor("leftOwner")
    void maledict$setLeftOwner(boolean leftOwner);
}
