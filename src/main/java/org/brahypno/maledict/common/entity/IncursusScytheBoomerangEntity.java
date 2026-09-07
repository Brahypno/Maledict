package org.brahypno.maledict.common.entity;

import com.sammy.malum.common.entity.scythe.ScytheBoomerangEntity;
import com.sammy.malum.registry.common.DamageTypeRegistry;
import com.sammy.malum.registry.common.SoundRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.helpers.ItemHelper;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;

/**
 * Malum's rebound projectile with the Incursus Blade's frozen damage channel.
 */
public final class IncursusScytheBoomerangEntity extends ScytheBoomerangEntity {
    private final float frozenDamage;

    public IncursusScytheBoomerangEntity(
            Level level, double x, double y, double z, float frozenDamage) {
        super(level, x, y, z);
        this.frozenDamage = frozenDamage;
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (level().isClientSide()){
            return;
        }

        Entity ownerEntity = getOwner();
        if (ownerEntity instanceof LivingEntity owner){
            Entity target = hitResult.getEntity();
            ItemStack heldItem = owner.getMainHandItem();
            ItemStack scythe = getItem();
            owner.setItemInHand(InteractionHand.MAIN_HAND, scythe);

            target.invulnerableTime = 0;
            boolean hit = DamageProbe.mediumDamageMethod(
                    target,
                    DamageTypeHelper.create(level(), DamageTypeRegistry.SCYTHE_SWEEP, this, owner),
                    damage).success();
            if (hit){
                ItemHelper.applyEnchantments(owner, target, scythe);
                int fireAspect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, scythe);
                if (fireAspect > 0 && target instanceof LivingEntity livingTarget){
                    livingTarget.setSecondsOnFire(fireAspect * 4);
                }
                if (magicDamage > 0.0f && canTakeAdditionalDamage(target)){
                    target.invulnerableTime = 0;
                    DamageProbe.mediumDamageMethod(
                            target,
                            DamageTypeHelper.create(level(), DamageTypeRegistry.VOODOO, this, owner),
                            magicDamage);
                }
                if (frozenDamage > 0.0f && canTakeAdditionalDamage(target)){
                    target.invulnerableTime = 0;
                    DamageProbe.mediumDamageMethod(
                            target,
                            DamageTypeHelper.create(level(), DamageTypes.FREEZE, this, owner),
                            frozenDamage);
                }
                enemiesHit++;
                returnTimer += 2;
            }

            owner.setItemInHand(InteractionHand.MAIN_HAND, heldItem);
            SoundHelper.playSound(
                    this,
                    SoundRegistry.SCYTHE_SWEEP.get(),
                    1.0f,
                    RandomHelper.randomBetween(level().getRandom(), 0.75f, 1.25f));
        }

        if (isEnhanced()){
            returnTimer = 0;
            Entity owner = getOwner();
            if (owner instanceof LivingEntity){
                flyBack(owner);
            }
        }
    }

    private static boolean canTakeAdditionalDamage(Entity target) {
        return !(target instanceof LivingEntity livingTarget) || !livingTarget.isDeadOrDying();
    }
}
