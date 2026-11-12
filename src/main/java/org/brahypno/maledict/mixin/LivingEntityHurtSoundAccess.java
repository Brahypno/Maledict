package org.brahypno.maledict.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reads the hurt sound of an existing living entity.
 *
 * <p>{@code LivingEntity#getHurtSound(DamageSource)} is {@code protected} and no public Forge or
 * Malum hook exposes it; the only other way to learn which sound a mob plays when hurt would be to
 * let it be hurt and listen, which cannot happen at the moment a soul is sealed. The accessor is
 * used once per binding, on the dying entity, so the soulbinding sound fallback can record the very
 * sound that entity would have played for that damage source.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityHurtSoundAccess {
    @Invoker("getHurtSound")
    SoundEvent maledict$getHurtSound(DamageSource source);
}
