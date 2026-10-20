package org.brahypno.maledict.common.weapon.lover;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;

/** Single-target axe attack entered through the item's supported left-click hook. */
public final class LoverAxeAttack {
    private LoverAxeAttack() {}

    public static void perform(Player player, Entity entity) {
        // Forge has already posted AttackEntityEvent before calling onLeftClickEntity.
        if (!(entity instanceof LivingEntity target) || target == player || !target.isAlive()
                || !target.isAttackable() || target.skipAttackInteraction(player)) return;

        ItemStack weapon = player.getMainHandItem();
        float strength = player.getAttackStrengthScale(0.5F);
        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (0.2F + strength * strength * 0.8F);
        float enchantment = EnchantmentHelper.getDamageBonus(weapon, target.getMobType()) * strength;
        try {
            if (base <= 0 && enchantment <= 0) return;
            boolean charged = strength > 0.9F;
            boolean sprinting = charged && player.isSprinting();
            float knockback = (float) player.getAttributeValue(Attributes.ATTACK_KNOCKBACK)
                    + EnchantmentHelper.getKnockbackBonus(player) + (sprinting ? 1 : 0);
            boolean falling = charged && player.fallDistance > 0 && !player.onGround()
                    && !player.onClimbable() && !player.isInWater()
                    && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger()
                    && !player.isSprinting();
            var critical = ForgeHooks.getCriticalHit(player, target, falling, falling ? 1.5F : 1.0F);
            float damage = (critical == null ? base : base * critical.getDamageModifier()) + enchantment;

            int fireAspect = EnchantmentHelper.getFireAspect(player);
            boolean ignited = fireAspect > 0 && !target.isOnFire();
            if (ignited) target.setSecondsOnFire(1);
            float initialHealth = target.getHealth();
            Vec3 initialMotion = target.getDeltaMovement();
            if (!LoverAxeItem.attack(player, target, player.damageSources().playerAttack(player), damage)) {
                sound(player, SoundEvents.PLAYER_ATTACK_NODAMAGE);
                if (ignited) target.clearFire();
                return;
            }

            if (knockback > 0) {
                target.knockback(knockback * 0.5F, Mth.sin(player.getYRot() * Mth.DEG_TO_RAD),
                        -Mth.cos(player.getYRot() * Mth.DEG_TO_RAD));
                player.setDeltaMovement(player.getDeltaMovement().multiply(0.6D, 1, 0.6D));
                player.setSprinting(false);
            }
            if (target instanceof ServerPlayer serverTarget && target.hurtMarked) {
                serverTarget.connection.send(new ClientboundSetEntityMotionPacket(target));
                target.hurtMarked = false;
                target.setDeltaMovement(initialMotion);
            }
            if (sprinting) sound(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK);
            sound(player, critical != null ? SoundEvents.PLAYER_ATTACK_CRIT
                    : charged ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK);
            if (critical != null) player.crit(target);
            if (enchantment > 0) player.magicCrit(target);

            player.setLastHurtMob(target);
            EnchantmentHelper.doPostHurtEffects(target, player);
            EnchantmentHelper.doPostDamageEffects(player, target);
            ItemStack beforeWear = weapon.copy();
            weapon.hurtEnemy(target, player);
            if (weapon.isEmpty()) {
                ForgeEventFactory.onPlayerDestroyItem(player, beforeWear, InteractionHand.MAIN_HAND);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            float loss = initialHealth - target.getHealth();
            player.awardStat(Stats.DAMAGE_DEALT, Math.round(loss * 10));
            if (fireAspect > 0) target.setSecondsOnFire(fireAspect * 4);
            if (player.level() instanceof ServerLevel level && loss > 2) {
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5), target.getZ(),
                        (int) (loss * 0.5F), 0.1, 0, 0.1, 0.2);
            }
            player.causeFoodExhaustion(0.1F);
        } finally {
            player.resetAttackStrengthTicker();
        }
    }

    private static void sound(Player player, SoundEvent sound) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, player.getSoundSource(), 1, 1);
    }
}
