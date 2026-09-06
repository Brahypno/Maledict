package org.brahypno.maledict.common.combat;

import com.sammy.malum.core.helpers.ParticleHelper;
import com.sammy.malum.registry.common.DamageTypeRegistry;
import com.sammy.malum.registry.common.ParticleEffectTypeRegistry;
import com.sammy.malum.registry.common.SoundRegistry;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import org.brahypno.maledict.common.item.IncursusBladeItem;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;

import java.util.Comparator;
import java.util.List;

public final class IncursusBladeAttack {
    private static final String LAST_ATTACK_TICK = "maledict:last_incursus_blade_attack_tick";

    public static void perform(ServerPlayer player) {
        ItemStack weapon = player.getMainHandItem();
        if (player.isSpectator() || !(weapon.getItem() instanceof IncursusBladeItem)){
            return;
        }

        long gameTime = player.level().getGameTime();
        if (player.getPersistentData().contains(LAST_ATTACK_TICK)
            && player.getPersistentData().getLong(LAST_ATTACK_TICK) == gameTime){
            return;
        }
        player.getPersistentData().putLong(LAST_ATTACK_TICK, gameTime);

        double reach = player.getAttributeValue(ForgeMod.ENTITY_REACH.get());
        AABB searchArea = player.getBoundingBox().inflate(reach);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                target -> isValidTarget(player, target, reach));
        targets.sort(Comparator.comparingDouble(player::distanceToSqr));

        player.swing(InteractionHand.MAIN_HAND, true);
        playSlashEffect(player);

        float attackStrength = Mth.clamp(
                (player.attackStrengthTicker + 0.5f) / player.getCurrentItemAttackStrengthDelay(),
                0.0f,
                1.0f);
        boolean attacked = false;
        for (LivingEntity target : targets) {
            float damage = calculateDamage(player, weapon, target, attackStrength);
            if (damage > 0.0f){
                DamageProbe.mediumDamageMethod(
                        target,
                        DamageTypeHelper.create(player.level(), DamageTypeRegistry.SCYTHE_MELEE, player),
                        damage);
                weapon.hurtEnemy(target, player);
                IncursusBladeEffects.applySacredEffect(player, target, weapon);
                attacked = true;
            }
        }
        if (attacked){
            IncursusBladeEffects.applyAerialEffect(player, weapon);
            player.resetAttackStrengthTicker();
        }
    }

    private static float calculateDamage(ServerPlayer player, ItemStack weapon, LivingEntity target, float attackStrength) {
        float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float enchantmentDamage = EnchantmentHelper.getDamageBonus(weapon, target.getMobType());
        baseDamage *= 0.2f + attackStrength * attackStrength * 0.8f;
        enchantmentDamage *= attackStrength;
        float damage = baseDamage + enchantmentDamage;

        boolean vanillaCritical = attackStrength > 0.9f
                                  && player.fallDistance > 0.0f
                                  && !player.onGround()
                                  && !player.onClimbable()
                                  && !player.isInWater()
                                  && !player.hasEffect(MobEffects.BLINDNESS)
                                  && !player.isPassenger()
                                  && !player.isSprinting();
        CriticalHitEvent criticalHit =
                ForgeHooks.getCriticalHit(player, target, vanillaCritical,
                                          vanillaCritical ?
                                          1.5f + (float) IncursusBladeItem.getStat(weapon, IncursusBladeItem.WICKED_CRITICAL_DAMAGE) / 100.0f : 1.0f);
        if (criticalHit != null){
            damage *= criticalHit.getDamageModifier();
            player.crit(target);
        }
        return damage;
    }

    private static void playSlashEffect(ServerPlayer player) {
        // TODO: Replace this temporary Tyrving slash sound with the Incursus Blade sound.
        SoundHelper.playSound(
                player,
                SoundRegistry.TYRVING_SLASH.get(),
                1.0f,
                RandomHelper.randomBetween(player.getRandom(), 1.0f, 1.5f));
        ParticleHelper.createSlashingEffect(ParticleEffectTypeRegistry.SCYTHE_SLASH)
                      .setSpiritType(SpiritTypeRegistry.ARCANE_SPIRIT)
                      .setSlashAngle(0.0f)
                      .spawnForwardSlashingParticle(player);
    }

    private static boolean isValidTarget(ServerPlayer player, LivingEntity target, double reach) {
        if (target == player || !target.isAlive() || !target.isAttackable()
            || target.skipAttackInteraction(player) || !player.hasLineOfSight(target)){
            return false;
        }
        if (target instanceof Player otherPlayer && !player.canHarmPlayer(otherPlayer)){
            return false;
        }
        double inclusiveReach = reach + target.getBbWidth() * 0.5;
        return player.distanceToSqr(target) <= inclusiveReach * inclusiveReach;
    }

    private IncursusBladeAttack() {
    }
}
