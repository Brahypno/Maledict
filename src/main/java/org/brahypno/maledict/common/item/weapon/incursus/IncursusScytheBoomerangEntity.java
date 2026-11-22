package org.brahypno.maledict.common.item.weapon.incursus;

import com.sammy.malum.common.entity.scythe.ScytheBoomerangEntity;
import com.sammy.malum.registry.common.DamageTypeRegistry;
import com.sammy.malum.registry.common.SoundRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.brahypno.changelib.DamageHelper.LivingTargetHelper;
import org.brahypno.changelib.DamageHelper.attribute.AttackItemProvider;
import org.brahypno.maledict.common.enchantment.DiplopiaThrows;
import org.brahypno.maledict.registry.MaledictSounds;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.helpers.ItemHelper;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Malum's rebound projectile with the Incursus Blade's sweep and magic channels. The freezing
 * channel is a {@code powder_snow_damage} attribute.
 */
public final class IncursusScytheBoomerangEntity extends ScytheBoomerangEntity
        implements AttackItemProvider {
    private final boolean echo;

    /**
     * 「收回」只响一次：飞镰掉头（{@code returnTimer} 落到 0）的那一 tick。
     */
    private boolean recallAnnounced;

    /**
     * One living root per throw; multipart hitboxes must not re-open the probe per part.
     */
    private final Set<UUID> hitLivingTargets = new HashSet<>();

    public IncursusScytheBoomerangEntity(Level level, double x, double y, double z) {
        this(level, x, y, z, false);
    }

    /**
     * An echo is a Diplopia phantom: it uses the same damage channels but never touches the
     * thrower's inventory. It discards itself on the return leg and is never saved.
     */
    public IncursusScytheBoomerangEntity(
            Level level, double x, double y, double z, boolean echo) {
        super(level, x, y, z);
        this.echo = echo;
    }

    /**
     * 投掷与接住之间，玩家听到的只有 Malum 的旋转声（{@code SCYTHE_SPINS}）+ 接住时的
     * {@code SCYTHE_CATCH}；本模组补的是「掉头回手」这一下。判定用 {@code returnTimer} 而不是
     * {@code flyBack()}：后者只在撞墙和强化命中时被调用，普通投掷的返程不会走它。
     */
    @Override
    public void tick() {
        if (echo){
            if (!level().isClientSide() && DiplopiaThrows.reachedOwnerOnReturn(this)){
                discard();
                return;
            }
            super.tick();
            return;
        }
        super.tick();
        if (recallAnnounced || level().isClientSide() || returnTimer > 0){
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
    public boolean shouldBeSaved() {
        return !echo && super.shouldBeSaved();
    }

    /**
     * The thrown scythe is the authoritative item snapshot while it is in flight.
     */
    @Override
    public ItemStack getAttackItem() {
        return getItem();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (level().isClientSide()){
            return;
        }

        Entity ownerEntity = getOwner();
        if (ownerEntity instanceof LivingEntity owner){
            Entity hitEntity = hitResult.getEntity();
            LivingEntity livingRoot = LivingTargetHelper.getLivingTarget(hitEntity);
            Entity target = livingRoot != null ? livingRoot : hitEntity;
            if (livingRoot != null && !hitLivingTargets.add(livingRoot.getUUID())){
                return;
            }
            ItemStack heldItem = owner.getMainHandItem();
            ItemStack scythe = getItem();
            owner.setItemInHand(InteractionHand.MAIN_HAND, scythe);

            // 回旋按刀自己的精魂档结算；无忧符文的抬档由 DamageRequestEvent 统一处理。
            boolean hit = IncursusBladeItem.dealSkillDamage(scythe, target,
                                                            DamageTypeHelper.create(level(), DamageTypeRegistry.SCYTHE_SWEEP, this, owner), damage)
                                           .instantEffective();
            if (hit){
                ItemHelper.applyEnchantments(owner, target, scythe);
                int fireAspect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, scythe);
                if (fireAspect > 0 && target instanceof LivingEntity livingTarget){
                    livingTarget.setSecondsOnFire(fireAspect * 4);
                }
                // 魔法属性由 AttributeDamageRegistry 在 scythe_sweep 命中后独立派发。
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
