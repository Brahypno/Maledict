package org.brahypno.maledict.common.combat;

import com.sammy.malum.common.enchantment.scythe.AscensionEnchantment;
import com.sammy.malum.common.item.ISpiritAffiliatedItem;
import com.sammy.malum.common.item.curiosities.TemporarilyDisabledItem;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem;
import com.sammy.malum.core.helpers.ParticleHelper;
import com.sammy.malum.registry.common.DamageTypeRegistry;
import com.sammy.malum.registry.common.MobEffectRegistry;
import com.sammy.malum.registry.common.ParticleEffectTypeRegistry;
import com.sammy.malum.registry.common.SoundRegistry;
import com.sammy.malum.registry.common.item.EnchantmentRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import org.brahypno.maledict.common.entity.IncursusScytheBoomerangEntity;
import org.brahypno.maledict.common.item.IncursusBladeItem;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.helpers.ItemHelper;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;

public final class IncursusBladeEnchantments {
    public static void throwScythe(Level level, Player player, InteractionHand hand, ItemStack stack) {
        int slot = hand == InteractionHand.OFF_HAND
                   ? player.getInventory().getContainerSize() - 1
                   : player.getInventory().selected;
        if (!(player instanceof ServerPlayer serverPlayer)){
            return;
        }

        boolean enhanced = !MalumScytheItem.canSweep(player);
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float magicDamage = (float) player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_DAMAGE.get());
        float frozenDamage = (float) IncursusBladeItem.getStat(stack, IncursusBladeItem.POWDER_SNOW_DAMAGE);
        float velocity = enhanced ? 3.0f : 1.75f;
        Vec3 position = player.position().add(0.0, player.getBbHeight() * 0.5f, 0.0);
        if (enhanced){
            int angle = hand == InteractionHand.MAIN_HAND ? 225 : 90;
            double radians = Math.toRadians(angle - player.yHeadRot);
            position = player.position().add(player.getLookAngle().scale(0.5)).add(
                    0.75 * Math.sin(radians), player.getBbHeight() * 0.9f, 0.75 * Math.cos(radians));
            damage *= 1.5f;
            magicDamage *= 1.5f;
            frozenDamage *= 1.5f;
        }

        IncursusScytheBoomerangEntity scythe = new IncursusScytheBoomerangEntity(
                level, position.x, position.y, position.z, frozenDamage);
        scythe.setData(player, damage, magicDamage, slot, 8);
        scythe.setItem(stack);
        scythe.setEnhanced(enhanced);
        scythe.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, 0.0f);
        level.addFreshEntity(scythe);
        SoundHelper.playSound(
                player,
                SoundRegistry.SCYTHE_THROW.get(),
                2.0f,
                RandomHelper.randomBetween(level.getRandom(), 0.75f, 1.25f));
        TemporarilyDisabledItem.disable(serverPlayer, slot);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        player.swing(hand, true);
    }

    public static void triggerAscension(Level level, Player player, InteractionHand hand, ItemStack stack) {
        boolean enhanced = !MalumScytheItem.canSweep(player);
        player.resetFallDistance();
        if (level.isClientSide()){
            AscensionEnchantment.triggerAscension(level, player, hand, stack);
        }else {
            performAscensionAttack(level, player, stack, enhanced);
        }

        if (!player.isCreative()){
            int levelOfAscension = stack.getEnchantmentLevel(EnchantmentRegistry.ASCENSION.get());
            if (levelOfAscension < 6){
                player.getCooldowns().addCooldown(stack.getItem(), 150 - 25 * (levelOfAscension - 1));
            }
        }
        player.swing(hand, false);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
    }

    private static void performAscensionAttack(Level level, Player player, ItemStack stack, boolean enhanced) {
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float magicDamage = (float) player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_DAMAGE.get());
        float frozenDamage = (float) IncursusBladeItem.getStat(stack, IncursusBladeItem.POWDER_SNOW_DAMAGE);
        AABB area = player.getBoundingBox().inflate(4.0, 1.0, 4.0);
        var sound = SoundRegistry.SCYTHE_SWEEP.get();
        ParticleHelper.SlashParticleEffectBuilder particles = ParticleHelper
                .createSlashingEffect(ParticleEffectTypeRegistry.SCYTHE_ASCENSION_SPIN)
                .mirrorRandomly(level.getRandom());
        if (enhanced){
            damage *= 1.25f;
            magicDamage *= 1.25f;
            frozenDamage *= 1.25f;
            area = area.move(player.getLookAngle().scale(2.0)).inflate(-2.0, 1.0, -2.0);
            sound = SoundRegistry.SCYTHE_CUT.get();
            particles = ParticleHelper.createSlashingEffect(ParticleEffectTypeRegistry.SCYTHE_ASCENSION_UPPERCUT)
                                      .setVerticalSlashAngle()
                                      .setMirrored(true);
        }
        if (stack.getItem() instanceof ISpiritAffiliatedItem spiritItem){
            particles.setSpiritType(spiritItem);
        }

        boolean hitAnything = false;
        for (Entity target : level.getEntities(player, area, entity -> canHitEntity(player, entity))) {
            target.invulnerableTime = 0;
            boolean hit = DamageProbe.mediumDamageMethod(
                    target,
                    DamageTypeHelper.create(level, DamageTypeRegistry.SCYTHE_SWEEP, player),
                    damage).success();
            if (hit){
                ItemHelper.applyEnchantments(player, target, stack);
                int fireAspect = stack.getEnchantmentLevel(Enchantments.FIRE_ASPECT);
                if (fireAspect > 0 && target instanceof LivingEntity livingTarget){
                    livingTarget.setSecondsOnFire(fireAspect * 4);
                }
                if (magicDamage > 0.0f && canTakeAdditionalDamage(target)){
                    target.invulnerableTime = 0;
                    DamageProbe.mediumDamageMethod(
                            target,
                            DamageTypeHelper.create(level, DamageTypeRegistry.VOODOO, player),
                            magicDamage);
                }
                if (frozenDamage > 0.0f && canTakeAdditionalDamage(target)){
                    target.invulnerableTime = 0;
                    DamageProbe.mediumDamageMethod(
                            target,
                            DamageTypeHelper.create(level, DamageTypes.FREEZE, player),
                            frozenDamage);
                }
                SoundHelper.playSound(
                        player, sound, 2.0f,
                        RandomHelper.randomBetween(level.getRandom(), 0.75f, 1.25f));
                hitAnything = true;
            }
        }
        if (hitAnything){
            player.addEffect(new MobEffectInstance(MobEffectRegistry.ASCENSION.get(), 80, 0));
        }

        Vec3 particlePosition = player.position().add(0.0, player.getBbHeight() * 0.75, 0.0);
        Vec3 particleDirection = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        particles.spawnSlashingParticle(level, particlePosition, particleDirection);
        for (int i = 0; i < 3; i++) {
            SoundHelper.playSound(
                    player, sound, 1.0f,
                    RandomHelper.randomBetween(level.getRandom(), 1.25f, 1.75f));
        }
        SoundHelper.playSound(
                player,
                SoundRegistry.SCYTHE_ASCENSION.get(),
                2.0f,
                RandomHelper.randomBetween(level.getRandom(), 1.25f, 1.5f));
    }

    private static boolean canHitEntity(Player player, Entity entity) {
        return entity.canBeHitByProjectile()
               && entity != player
               && !player.isPassengerOfSameVehicle(entity);
    }

    private static boolean canTakeAdditionalDamage(Entity target) {
        return !(target instanceof LivingEntity livingTarget) || !livingTarget.isDeadOrDying();
    }

    private IncursusBladeEnchantments() {
    }
}
