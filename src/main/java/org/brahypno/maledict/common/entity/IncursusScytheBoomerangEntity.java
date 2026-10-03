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
import org.brahypno.maledict.common.item.IncursusBladeItem;
import org.brahypno.maledict.registry.MaledictSounds;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.helpers.ItemHelper;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;

/**
 * Malum's rebound projectile with the Incursus Blade's frozen damage channel.
 */
public final class IncursusScytheBoomerangEntity extends ScytheBoomerangEntity {
    private final float frozenDamage;

    /** 「收回」只响一次：飞镰掉头（{@code returnTimer} 落到 0）的那一 tick。 */
    private boolean recallAnnounced;

    public IncursusScytheBoomerangEntity(
            Level level, double x, double y, double z, float frozenDamage) {
        super(level, x, y, z);
        this.frozenDamage = frozenDamage;
    }

    /**
     * 投掷与接住之间，玩家听到的只有 Malum 的旋转声（{@code SCYTHE_SPINS}）+ 接住时的
     * {@code SCYTHE_CATCH}；本模组补的是「掉头回手」这一下。判定用 {@code returnTimer} 而不是
     * {@code flyBack()}：后者只在撞墙和强化命中时被调用，普通投掷的返程不会走它。
     */
    @Override
    public void tick() {
        super.tick();
        if (recallAnnounced || level().isClientSide() || returnTimer > 0) {
            return;
        }
        recallAnnounced = true;
        SoundHelper.playSound(
                this,
                MaledictSounds.INCURSUS_BLADE_RECALL.get(),
                1.2f,
                RandomHelper.randomBetween(level().getRandom(), 0.95f, 1.05f));
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
            
            // 这一记写死 medium（不随精魂成长），但无忧符文对「这把刀打出的伤害」是一个口径，照抬一档。
            boolean hit = IncursusBladeItem.raisedByBliss(IncursusBladeItem.DamageTier.MEDIUM, owner)
                                           .deal(target,
                                                 DamageTypeHelper.create(level(), DamageTypeRegistry.SCYTHE_SWEEP, this, owner),
                                                 damage)
                                           .success();
            if (hit){
                ItemHelper.applyEnchantments(owner, target, scythe);
                int fireAspect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, scythe);
                if (fireAspect > 0 && target instanceof LivingEntity livingTarget){
                    livingTarget.setSecondsOnFire(fireAspect * 4);
                }
                IncursusBladeItem.applyTieredDamage(
                        scythe,
                        target,
                        DamageTypeHelper.create(level(), DamageTypeRegistry.VOODOO, this, owner),
                        magicDamage);
                IncursusBladeItem.applyTieredDamage(
                        scythe,
                        target,
                        DamageTypeHelper.create(level(), DamageTypes.FREEZE, this, owner),
                        frozenDamage);
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
}
